package com.fin.mf.tools.terminal.data

import com.squareup.moshi.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Root response for Yahoo Finance Chart endpoint:
 * https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval={interval}&range={range}
 */
data class YahooChartResponse(
    @Json(name = "chart") val chart: YahooChartContent
)

data class YahooChartContent(
    @Json(name = "result") val result: List<YahooChartResult>?,
    @Json(name = "error") val error: YahooError?
)

data class YahooError(
    @Json(name = "code") val code: String?,
    @Json(name = "description") val description: String?
)

data class YahooChartResult(
    @Json(name = "meta") val meta: YahooMeta,
    @Json(name = "timestamp") val timestamp: List<Long>?,
    @Json(name = "indicators") val indicators: YahooIndicators?
)

data class YahooMeta(
    @Json(name = "currency") val currency: String?,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "exchangeName") val exchangeName: String?,
    @Json(name = "instrumentType") val instrumentType: String?,
    @Json(name = "firstTradeDate") val firstTradeDate: Long?,
    @Json(name = "regularMarketTime") val regularMarketTime: Long?,
    @Json(name = "gmtoffset") val gmtoffset: Long?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "exchangeTimezoneName") val exchangeTimezoneName: String?,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double?,
    @Json(name = "chartPreviousClose") val chartPreviousClose: Double?,
    @Json(name = "previousClose") val previousClose: Double?,
    @Json(name = "scale") val scale: Int?,
    @Json(name = "priceHint") val priceHint: Int?,
    @Json(name = "regularMarketDayHigh") val regularMarketDayHigh: Double?,
    @Json(name = "regularMarketDayLow") val regularMarketDayLow: Double?,
    @Json(name = "regularMarketVolume") val regularMarketVolume: Long?,
    @Json(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: Double?,
    @Json(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: Double?,
    @Json(name = "shortName") val shortName: String?,
    @Json(name = "longName") val longName: String?
)

data class YahooIndicators(
    @Json(name = "quote") val quote: List<YahooQuote>?,
    @Json(name = "adjclose") val adjclose: List<YahooAdjClose>?
)

data class YahooQuote(
    @Json(name = "open") val open: List<Double?>?,
    @Json(name = "high") val high: List<Double?>?,
    @Json(name = "low") val low: List<Double?>?,
    @Json(name = "close") val close: List<Double?>?,
    @Json(name = "volume") val volume: List<Long?>?
)

data class YahooAdjClose(
    @Json(name = "adjclose") val adjclose: List<Double?>?
)

/**
 * Root response for Yahoo Search autocomplete endpoint:
 * https://query2.finance.yahoo.com/v1/finance/search?q={query}&quotesCount=10&newsCount=0
 */
data class YahooSearchResponse(
    @Json(name = "quotes") val quotes: List<YahooSearchQuoteItem>?,
    @Json(name = "count") val count: Int?
)

data class YahooSearchQuoteItem(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "shortname") val shortname: String?,
    @Json(name = "longname") val longname: String?,
    @Json(name = "exchange") val exchange: String?,
    @Json(name = "quoteType") val quoteType: String?,
    @Json(name = "index") val index: String?
) {
    val displayName: String
        get() = shortname ?: longname ?: symbol
}

/**
 * Multi-timeframe periods supported by Yahoo Finance Chart.
 */
enum class TerminalTimeframe(
    val label: String,
    val rangeParam: String,
    val intervalParam: String
) {
    ONE_DAY("1D", "1d", "5m"),
    FIVE_DAYS("5D", "5d", "15m"),
    ONE_MONTH("1M", "1mo", "1d"),
    SIX_MONTHS("6M", "6mo", "1d"),
    ONE_YEAR("1Y", "1y", "1d"),
    FIVE_YEARS("5Y", "5y", "1wk")
}

/**
 * Clean UI Domain Model representing an individual plotted point.
 */
data class TerminalChartPoint(
    val epochMs: Long,
    val price: Double,
    val dateFormatted: String,
    val volume: Long = 0L
)

/**
 * Complete UI Domain Model representing a financial instrument's live quote and time-series history.
 */
data class TerminalAssetQuote(
    val symbol: String,
    val shortName: String,
    val longName: String,
    val currency: String,
    val exchange: String,
    val regularMarketPrice: Double,
    val previousClose: Double,
    val open: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val volume: Long,
    val chartPoints: List<TerminalChartPoint>
) {
    val dayChange: Double
        get() = regularMarketPrice - previousClose

    val dayChangePercent: Double
        get() = if (previousClose > 0.0) (dayChange / previousClose) * 100.0 else 0.0

    val periodReturnPercent: Double
        get() {
            val first = chartPoints.firstOrNull()?.price ?: return 0.0
            val last = chartPoints.lastOrNull()?.price ?: return 0.0
            return if (first > 0.0) ((last - first) / first) * 100.0 else 0.0
        }
}

/**
 * Normalized comparison point for synchronizing target asset vs benchmark (NIFTY 50).
 */
data class BenchmarkComparisonPoint(
    val epochMs: Long,
    val dateFormatted: String,
    val targetPct: Double,
    val benchmarkPct: Double
) {
    val alphaSpread: Double
        get() = targetPct - benchmarkPct
}
