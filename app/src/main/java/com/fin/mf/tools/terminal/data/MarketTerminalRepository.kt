package com.fin.mf.tools.terminal.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Repository orchestrating Yahoo Finance Chart & Autocomplete network calls,
 * response normalization, and benchmark synchronization math.
 */
class MarketTerminalRepository(
    private val apiService: YahooFinanceApiService = defaultApiService
) {

    companion object {
        private const val BASE_URL = "https://query1.finance.yahoo.com/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        private val timeFormatIntraday = SimpleDateFormat("HH:mm", Locale.US)
        private val timeFormatFiveDay = SimpleDateFormat("dd MMM HH:mm", Locale.US)
        private val dateFormatDaily = SimpleDateFormat("dd MMM yyyy", Locale.US)

        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        private val loggingInterceptor: HttpLoggingInterceptor by lazy {
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
        }

        private val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(request)
        }

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(headerInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        }

        val defaultApiService: YahooFinanceApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YahooFinanceApiService::class.java)
        }
    }

    /**
     * Fetches chart time-series data and meta information for [symbol] across [timeframe].
     */
    suspend fun fetchChart(
        symbol: String,
        timeframe: TerminalTimeframe
    ): Result<TerminalAssetQuote> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getChart(
                symbol = symbol.trim().uppercase(Locale.US),
                range = timeframe.rangeParam,
                interval = timeframe.intervalParam
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("Yahoo Chart API error ${response.code()}: ${response.message()}")
            }

            val result = response.body()?.chart?.result?.firstOrNull()
                ?: throw IllegalStateException("No quote result found for symbol: $symbol")

            val meta = result.meta
            val timestamps = result.timestamp ?: emptyList()
            val quoteData = result.indicators?.quote?.firstOrNull()
            val closePrices = quoteData?.close ?: emptyList()

            val chartPoints = mutableListOf<TerminalChartPoint>()
            val count = minOf(timestamps.size, closePrices.size)

            for (i in 0 until count) {
                val epochSec = timestamps[i]
                val price = closePrices[i]
                if (price != null && !price.isNaN() && price > 0.0) {
                    val epochMs = epochSec * 1000L
                    val dateFormatted = formatPointDate(epochMs, timeframe)
                    chartPoints.add(
                        TerminalChartPoint(
                            epochMs = epochMs,
                            price = price,
                            dateFormatted = dateFormatted
                        )
                    )
                }
            }

            val regularPrice = meta.regularMarketPrice
                ?: chartPoints.lastOrNull()?.price
                ?: 0.0

            val prevClose = meta.chartPreviousClose
                ?: meta.previousClose
                ?: chartPoints.firstOrNull()?.price
                ?: regularPrice

            TerminalAssetQuote(
                symbol = meta.symbol,
                shortName = meta.shortName ?: meta.symbol,
                longName = meta.longName ?: meta.shortName ?: meta.symbol,
                currency = meta.currency ?: "INR",
                exchange = meta.exchangeName ?: "NSE",
                regularMarketPrice = regularPrice,
                previousClose = prevClose,
                open = quoteData?.open?.firstOrNull { it != null && it > 0.0 } ?: prevClose,
                dayHigh = meta.regularMarketDayHigh ?: chartPoints.maxOfOrNull { it.price } ?: regularPrice,
                dayLow = meta.regularMarketDayLow ?: chartPoints.minOfOrNull { it.price } ?: regularPrice,
                fiftyTwoWeekHigh = meta.fiftyTwoWeekHigh ?: regularPrice,
                fiftyTwoWeekLow = meta.fiftyTwoWeekLow ?: regularPrice,
                volume = meta.regularMarketVolume ?: 0L,
                chartPoints = chartPoints
            )
        }
    }

    /**
     * Synchronizes and normalizes the target asset vs a benchmark (default ^NSEI / NIFTY 50).
     * Calculates Relative Change % from t0:
     *   Relative_Change(t) = ((Price(t) - Price(0)) / Price(0)) * 100
     */
    suspend fun fetchBenchmarkComparison(
        targetSymbol: String,
        benchmarkSymbol: String = "^NSEI",
        timeframe: TerminalTimeframe
    ): Result<List<BenchmarkComparisonPoint>> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val targetDeferred = async { fetchChart(targetSymbol, timeframe) }
                val benchDeferred = async { fetchChart(benchmarkSymbol, timeframe) }

                val targetResult = targetDeferred.await()
                val benchResult = benchDeferred.await()

                val targetQuote = targetResult.getOrThrow()
                val benchQuote = benchResult.getOrThrow()

                val targetPoints = targetQuote.chartPoints
                val benchPoints = benchQuote.chartPoints

                if (targetPoints.isEmpty() || benchPoints.isEmpty()) {
                    return@coroutineScope emptyList()
                }

                val targetP0 = targetPoints.first().price
                val benchP0 = benchPoints.first().price

                if (targetP0 <= 0.0 || benchP0 <= 0.0) {
                    return@coroutineScope emptyList()
                }

                // Match each target point with the closest benchmark point in time
                val comparisonPoints = mutableListOf<BenchmarkComparisonPoint>()
                var benchIdx = 0

                for (targetPt in targetPoints) {
                    // Advance benchIdx to find closest timestamp
                    while (benchIdx < benchPoints.size - 1 &&
                        abs(benchPoints[benchIdx + 1].epochMs - targetPt.epochMs) < abs(benchPoints[benchIdx].epochMs - targetPt.epochMs)
                    ) {
                        benchIdx++
                    }

                    val matchedBenchPt = benchPoints[benchIdx]
                    val targetPct = ((targetPt.price - targetP0) / targetP0) * 100.0
                    val benchPct = ((matchedBenchPt.price - benchP0) / benchP0) * 100.0

                    comparisonPoints.add(
                        BenchmarkComparisonPoint(
                            epochMs = targetPt.epochMs,
                            dateFormatted = targetPt.dateFormatted,
                            targetPct = targetPct,
                            benchmarkPct = benchPct
                        )
                    )
                }

                comparisonPoints
            }
        }
    }

    /**
     * Autocomplete search for Indian stocks, ETFs, indices, and global assets.
     */
    suspend fun searchAssets(query: String): Result<List<YahooSearchQuoteItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return@runCatching emptyList()

            val response = apiService.searchAssets(
                query = cleanQuery,
                quotesCount = 12,
                newsCount = 0
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("Search API error ${response.code()}: ${response.message()}")
            }

            val quotes = response.body()?.quotes ?: emptyList()
            // Filter to items with valid symbols
            quotes.filter { it.symbol.isNotBlank() }
        }
    }

    private fun formatPointDate(epochMs: Long, timeframe: TerminalTimeframe): String {
        val date = Date(epochMs)
        return when (timeframe) {
            TerminalTimeframe.ONE_DAY -> timeFormatIntraday.format(date)
            TerminalTimeframe.FIVE_DAYS -> timeFormatFiveDay.format(date)
            TerminalTimeframe.ONE_MONTH,
            TerminalTimeframe.SIX_MONTHS,
            TerminalTimeframe.ONE_YEAR,
            TerminalTimeframe.FIVE_YEARS -> dateFormatDaily.format(date)
        }
    }
}
