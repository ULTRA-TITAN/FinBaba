package com.fin.mf.tools.terminal.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ============================================================================
// YAHOO V10 QUOTE SUMMARY RAW WIRE DTOs
// ============================================================================

@JsonClass(generateAdapter = true)
data class YahooRawValue(
    @Json(name = "raw") val raw: Double? = null,
    @Json(name = "fmt") val fmt: String? = null
)

@JsonClass(generateAdapter = true)
data class YahooRawLong(
    @Json(name = "raw") val raw: Long? = null,
    @Json(name = "fmt") val fmt: String? = null,
    @Json(name = "longFmt") val longFmt: String? = null
)

@JsonClass(generateAdapter = true)
data class YahooQuoteSummaryResponse(
    @Json(name = "quoteSummary") val quoteSummary: YahooQuoteSummaryContainer? = null
)

@JsonClass(generateAdapter = true)
data class YahooQuoteSummaryContainer(
    @Json(name = "result") val result: List<YahooQuoteSummaryResult>? = null,
    @Json(name = "error") val error: YahooApiError? = null
)

@JsonClass(generateAdapter = true)
data class YahooApiError(
    @Json(name = "code") val code: String? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class YahooQuoteSummaryResult(
    @Json(name = "summaryDetail") val summaryDetail: YahooSummaryDetail? = null,
    @Json(name = "financialData") val financialData: YahooFinancialData? = null,
    @Json(name = "defaultKeyStatistics") val defaultKeyStatistics: YahooDefaultKeyStatistics? = null,
    @Json(name = "calendarEvents") val calendarEvents: YahooCalendarEvents? = null
)

@JsonClass(generateAdapter = true)
data class YahooSummaryDetail(
    @Json(name = "trailingPE") val trailingPE: YahooRawValue? = null,
    @Json(name = "forwardPE") val forwardPE: YahooRawValue? = null,
    @Json(name = "marketCap") val marketCap: YahooRawValue? = null,
    @Json(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: YahooRawValue? = null,
    @Json(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: YahooRawValue? = null,
    @Json(name = "dividendYield") val dividendYield: YahooRawValue? = null,
    @Json(name = "dividendRate") val dividendRate: YahooRawValue? = null,
    @Json(name = "exDividendDate") val exDividendDate: YahooRawLong? = null,
    @Json(name = "payoutRatio") val payoutRatio: YahooRawValue? = null,
    @Json(name = "previousClose") val previousClose: YahooRawValue? = null,
    @Json(name = "open") val open: YahooRawValue? = null,
    @Json(name = "dayHigh") val dayHigh: YahooRawValue? = null,
    @Json(name = "dayLow") val dayLow: YahooRawValue? = null,
    @Json(name = "volume") val volume: YahooRawLong? = null,
    @Json(name = "averageVolume") val averageVolume: YahooRawLong? = null,
    @Json(name = "currency") val currency: String? = null
)

@JsonClass(generateAdapter = true)
data class YahooFinancialData(
    @Json(name = "currentPrice") val currentPrice: YahooRawValue? = null,
    @Json(name = "totalCash") val totalCash: YahooRawValue? = null,
    @Json(name = "totalDebt") val totalDebt: YahooRawValue? = null,
    @Json(name = "debtToEquity") val debtToEquity: YahooRawValue? = null,
    @Json(name = "revenueGrowth") val revenueGrowth: YahooRawValue? = null,
    @Json(name = "earningsGrowth") val earningsGrowth: YahooRawValue? = null,
    @Json(name = "operatingMargins") val operatingMargins: YahooRawValue? = null,
    @Json(name = "profitMargins") val profitMargins: YahooRawValue? = null,
    @Json(name = "returnOnEquity") val returnOnEquity: YahooRawValue? = null,
    @Json(name = "returnOnAssets") val returnOnAssets: YahooRawValue? = null,
    @Json(name = "ebitda") val ebitda: YahooRawValue? = null,
    @Json(name = "totalRevenue") val totalRevenue: YahooRawValue? = null,
    @Json(name = "financialCurrency") val financialCurrency: String? = null
)

@JsonClass(generateAdapter = true)
data class YahooDefaultKeyStatistics(
    @Json(name = "priceToBook") val priceToBook: YahooRawValue? = null,
    @Json(name = "pegRatio") val pegRatio: YahooRawValue? = null,
    @Json(name = "enterpriseValue") val enterpriseValue: YahooRawValue? = null,
    @Json(name = "enterpriseToEbitda") val enterpriseToEbitda: YahooRawValue? = null,
    @Json(name = "bookValue") val bookValue: YahooRawValue? = null,
    @Json(name = "sharesOutstanding") val sharesOutstanding: YahooRawLong? = null,
    @Json(name = "lastSplitFactor") val lastSplitFactor: String? = null,
    @Json(name = "lastSplitDate") val lastSplitDate: YahooRawLong? = null,
    @Json(name = "trailingEps") val trailingEps: YahooRawValue? = null,
    @Json(name = "forwardEps") val forwardEps: YahooRawValue? = null
)

@JsonClass(generateAdapter = true)
data class YahooCalendarEvents(
    @Json(name = "earnings") val earnings: YahooEarningsCalendar? = null,
    @Json(name = "exDividendDate") val exDividendDate: YahooRawLong? = null,
    @Json(name = "dividendDate") val dividendDate: YahooRawLong? = null
)

@JsonClass(generateAdapter = true)
data class YahooEarningsCalendar(
    @Json(name = "earningsDate") val earningsDate: List<YahooRawLong>? = null,
    @Json(name = "earningsAverage") val earningsAverage: YahooRawValue? = null,
    @Json(name = "revenueAverage") val revenueAverage: YahooRawLong? = null
)

// ============================================================================
// DOMAIN MODELS: STOCK FUNDAMENTALS & CHART SERIES
// ============================================================================

data class StockFundamentalSummary(
    val symbol: String,
    val currency: String = "INR",
    val currentPrice: Double? = null,

    // 1. Valuation & Capital Structure
    val trailingPE: Double? = null,
    val forwardPE: Double? = null,
    val priceToBook: Double? = null,
    val pegRatio: Double? = null,
    val enterpriseValue: Double? = null,
    val enterpriseToEbitda: Double? = null,
    val marketCap: Double? = null,
    val faceValue: Double? = 10.0,
    val fiftyTwoWeekHigh: Double? = null,
    val fiftyTwoWeekLow: Double? = null,

    // 2. Financial Health & Operating Efficiency (Percentages stored as 0..100 scale)
    val revenueGrowthYoY: Double? = null,
    val earningsGrowthYoY: Double? = null,
    val operatingMargin: Double? = null,
    val netProfitMargin: Double? = null,
    val roe: Double? = null,
    val roa: Double? = null,
    val totalDebt: Double? = null,
    val totalCash: Double? = null,
    val debtToEquity: Double? = null,

    // 3. Corporate Actions & Events Calendar
    val dividendYield: Double? = null,
    val dividendRate: Double? = null,
    val exDividendDateEpochSec: Long? = null,
    val payoutRatio: Double? = null,
    val earningsAnnouncementDateEpochSec: Long? = null,
    val lastSplitFactor: String? = null,
    val lastSplitDateEpochSec: Long? = null,
    val upcomingEventsDescription: String = "No pending stock splits or bonus issues announced"
) {
    val currencySymbol: String
        get() = when (currency.uppercase(Locale.US)) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }

    val isIndianEquity: Boolean
        get() = currency.equals("INR", ignoreCase = true) ||
            symbol.endsWith(".NS", ignoreCase = true) ||
            symbol.endsWith(".BO", ignoreCase = true)

    val formattedMarketCapCrores: String
        get() = formatLargeCapitalValue(marketCap)

    val formattedEnterpriseValue: String
        get() = formatLargeCapitalValue(enterpriseValue)

    val formattedTotalDebt: String
        get() = formatLargeCapitalValue(totalDebt)

    val formattedTotalCash: String
        get() = formatLargeCapitalValue(totalCash)

    val distanceTo52WeekPeakPct: Double
        get() {
            val price = currentPrice ?: return 0.0
            val high = fiftyTwoWeekHigh ?: return 0.0
            if (high <= 0.0) return 0.0
            return ((price - high) / high) * 100.0
        }

    val fiftyTwoWeekPositionFraction: Float
        get() {
            val price = currentPrice ?: return 0.5f
            val low = fiftyTwoWeekLow ?: return 0.5f
            val high = fiftyTwoWeekHigh ?: return 0.5f
            val span = high - low
            if (span <= 0.0) return 0.5f
            return ((price - low) / span).toFloat().coerceIn(0f, 1f)
        }

    val formattedDividendDate: String
        get() = formatEpochDate(exDividendDateEpochSec, fallback = "Not Declared")

    val formattedEarningsDate: String
        get() = formatEpochDate(earningsAnnouncementDateEpochSec, fallback = "TBA by Board")

    fun formatLargeCapitalValue(rawAmount: Double?): String {
        if (rawAmount == null || rawAmount <= 0.0) return "—"
        return if (isIndianEquity) {
            val crores = rawAmount / 10_000_000.0
            when {
                crores >= 100_000.0 -> String.format(Locale.US, "₹%,.2f L Cr", crores / 100_000.0)
                crores >= 1.0 -> String.format(Locale.US, "₹%,.0f Cr", crores)
                else -> String.format(Locale.US, "₹%,.2f Cr", crores)
            }
        } else {
            val billions = rawAmount / 1_000_000_000.0
            when {
                billions >= 1_000.0 -> String.format(Locale.US, "$%,.2f T", billions / 1_000.0)
                billions >= 1.0 -> String.format(Locale.US, "$%,.2f B", billions)
                else -> String.format(Locale.US, "$%,.1f M", rawAmount / 1_000_000.0)
            }
        }
    }

    private fun formatEpochDate(epochSec: Long?, fallback: String): String {
        if (epochSec == null || epochSec <= 0L) return fallback
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochSecond(epochSec))
        } catch (_: Exception) {
            fallback
        }
    }

    companion object {
        fun fromYahooResult(
            symbol: String,
            result: YahooQuoteSummaryResult,
            fallbackPrice: Double? = null
        ): StockFundamentalSummary {
            val summary = result.summaryDetail
            val financial = result.financialData
            val stats = result.defaultKeyStatistics
            val calendar = result.calendarEvents

            val resolvedCurrency = summary?.currency
                ?: financial?.financialCurrency
                ?: if (symbol.endsWith(".NS", true) || symbol.endsWith(".BO", true)) "INR" else "USD"

            val exDivEpoch = summary?.exDividendDate?.raw ?: calendar?.exDividendDate?.raw
            val earningsEpoch = calendar?.earnings?.earningsDate?.firstOrNull()?.raw
            val splitFactor = stats?.lastSplitFactor
            val splitDateEpoch = stats?.lastSplitDate?.raw

            val splitSummary = if (!splitFactor.isNullOrBlank() && splitDateEpoch != null) {
                val formattedSplitDate = try {
                    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochSecond(splitDateEpoch))
                } catch (_: Exception) {
                    ""
                }
                "Last Split: $splitFactor ($formattedSplitDate)"
            } else {
                "No recent stock split or bonus issue recorded"
            }

            // Yahoo returns ratios like 0.184 for 18.4%; convert to percentage points
            fun YahooRawValue?.toPercentagePoints(): Double? = this?.raw?.let { it * 100.0 }

            // Debt-to-Equity from Yahoo is returned on a 0..100+ scale (e.g., 42.5 means 0.43x or 42.5%)
            // Normalize to ratio (e.g., 0.43x) if > 5.0
            val normalizedDebtToEquity = financial?.debtToEquity?.raw?.let { rawDte ->
                if (abs(rawDte) > 5.0) rawDte / 100.0 else rawDte
            }

            return StockFundamentalSummary(
                symbol = symbol,
                currency = resolvedCurrency,
                currentPrice = financial?.currentPrice?.raw ?: fallbackPrice ?: summary?.previousClose?.raw,
                trailingPE = summary?.trailingPE?.raw,
                forwardPE = summary?.forwardPE?.raw,
                priceToBook = stats?.priceToBook?.raw,
                pegRatio = stats?.pegRatio?.raw,
                enterpriseValue = stats?.enterpriseValue?.raw,
                enterpriseToEbitda = stats?.enterpriseToEbitda?.raw,
                marketCap = summary?.marketCap?.raw,
                faceValue = if (resolvedCurrency.equals("INR", ignoreCase = true)) 10.0 else 1.0,
                fiftyTwoWeekHigh = summary?.fiftyTwoWeekHigh?.raw,
                fiftyTwoWeekLow = summary?.fiftyTwoWeekLow?.raw,
                revenueGrowthYoY = financial?.revenueGrowth.toPercentagePoints(),
                earningsGrowthYoY = financial?.earningsGrowth.toPercentagePoints(),
                operatingMargin = financial?.operatingMargins.toPercentagePoints(),
                netProfitMargin = financial?.profitMargins.toPercentagePoints(),
                roe = financial?.returnOnEquity.toPercentagePoints(),
                roa = financial?.returnOnAssets.toPercentagePoints(),
                totalDebt = financial?.totalDebt?.raw,
                totalCash = financial?.totalCash?.raw,
                debtToEquity = normalizedDebtToEquity,
                dividendYield = summary?.dividendYield.toPercentagePoints(),
                dividendRate = summary?.dividendRate?.raw,
                exDividendDateEpochSec = exDivEpoch,
                payoutRatio = summary?.payoutRatio.toPercentagePoints(),
                earningsAnnouncementDateEpochSec = earningsEpoch,
                lastSplitFactor = splitFactor,
                lastSplitDateEpochSec = splitDateEpoch,
                upcomingEventsDescription = splitSummary
            )
        }
    }
}