package com.fin.mf.data

import com.squareup.moshi.Json

/**
 * Central registry manifest tracking all locally saved portfolios (`portfolios_manifest.json`).
 */
data class PortfolioManifest(
    @Json(name = "activePortfolioId") val activePortfolioId: String,
    @Json(name = "portfolios") val portfolios: List<PortfolioMeta>
)

/**
 * Metadata descriptor for an individual portfolio file (`portfolio_{id}.json`).
 */
data class PortfolioMeta(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "createdAtEpochMs") val createdAtEpochMs: Long,
    @Json(name = "schemeCount") val schemeCount: Int,
    @Json(name = "totalValue") val totalValue: Double
)

/**
 * Root JSON structure matching `portfolio_seed.json` and `portfolio_{id}.json`.
 */
data class PortfolioData(
    @Json(name = "portfolioName") val portfolioName: String,
    @Json(name = "ownerName") val ownerName: String,
    @Json(name = "lastSyncedEpochMs") val lastSyncedEpochMs: Long,
    @Json(name = "schemes") val schemes: List<MutualFundScheme>
)

/**
 * Individual Mutual Fund scheme position and metadata.
 */
data class MutualFundScheme(
    @Json(name = "schemeCode") val schemeCode: Int,
    @Json(name = "schemeName") val schemeName: String,
    @Json(name = "amc") val amc: String,
    @Json(name = "category") val category: String,
    @Json(name = "assetClass") val assetClass: String,
    @Json(name = "riskLevel") val riskLevel: String,
    @Json(name = "expenseRatio") val expenseRatio: Double,
    @Json(name = "exitLoad") val exitLoad: String,
    @Json(name = "benchmark") val benchmark: String,
    @Json(name = "units") val units: Double,
    @Json(name = "investedAmount") val investedAmount: Double,
    @Json(name = "fallbackNav") val fallbackNav: Double,
    @Json(name = "lastNavDate") val lastNavDate: String,
    @Json(name = "holdings") val holdings: List<UnderlyingHolding>
) {
    val currentValue: Double
        get() = units * fallbackNav

    val absoluteGain: Double
        get() = currentValue - investedAmount

    val absoluteReturnPercent: Double
        get() = if (investedAmount > 0.0) ((currentValue - investedAmount) / investedAmount) * 100.0 else 0.0

    val averageBuyNav: Double
        get() = if (units > 0.0) investedAmount / units else 0.0
}

/**
 * Underlying equity or debt instrument held inside a Mutual Fund scheme.
 */
data class UnderlyingHolding(
    @Json(name = "ticker") val ticker: String,
    @Json(name = "companyName") val companyName: String,
    @Json(name = "sector") val sector: String,
    @Json(name = "marketCapBucket") val marketCapBucket: String,
    @Json(name = "weightPercent") val weightPercent: Double
)

/**
 * Live API response model for `https://api.mfapi.in/mf/{scheme_code}`
 */
data class MfApiResponse(
    @Json(name = "meta") val meta: MfApiMeta?,
    @Json(name = "data") val data: List<MfApiNavEntry>?,
    @Json(name = "status") val status: String?
)

data class MfApiMeta(
    @Json(name = "fund_house") val fundHouse: String?,
    @Json(name = "scheme_type") val schemeType: String?,
    @Json(name = "scheme_category") val schemeCategory: String?,
    @Json(name = "scheme_code") val schemeCode: Int?,
    @Json(name = "scheme_name") val schemeName: String?
)

data class MfApiNavEntry(
    @Json(name = "date") val date: String,
    @Json(name = "nav") val nav: String
)

/**
 * Live search item model returned from `https://api.mfapi.in/mf/search?q={query}`
 */
data class MfSearchDto(
    @Json(name = "schemeCode") val schemeCode: Int,
    @Json(name = "schemeName") val schemeName: String
)

/**
 * Advanced chart timeframe options for single and multi-fund historical graphs.
 */
enum class ChartTimeframe(val label: String, val calendarDays: Int) {
    SEVEN_DAYS("7D", 7),
    FIFTEEN_DAYS("15D", 15),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    YTD("YTD", -1),
    ONE_YEAR("1Y", 365),
    THREE_YEARS("3Y", 1095),
    FIVE_YEARS("5Y", 1825),
    SEVEN_YEARS("7Y", 2555),
    TEN_YEARS("10Y", 3650),
    CUSTOM("Custom", 0)
}

/**
 * Peer comparison model representing a mutual fund inside the Multi-Fund Comparison tab.
 */
data class FundComparisonItem(
    val schemeCode: Int,
    val schemeName: String,
    val isPortfolioScheme: Boolean,
    val latestNav: Double,
    val lastNavDate: String,
    val cagr1Y: Double?,
    val cagr3Y: Double?,
    val cagr5Y: Double?,
    val expenseRatio: Double,
    val riskLevel: String,
    val category: String,
    val benchmark: String,
    val navHistory: List<MfApiNavEntry>,
    val topHoldings: List<UnderlyingHolding>
)

/**
 * Computed Look-Through Stock/Debt Overlap model across the entire portfolio.
 */
data class AggregatedStockExposure(
    val ticker: String,
    val companyName: String,
    val sector: String,
    val marketCapBucket: String,
    val totalRupeeExposure: Double,
    val portfolioWeightPercent: Double,
    val heldInSchemesCount: Int,
    val schemeContributions: List<SchemeHoldingContribution>
)

data class SchemeHoldingContribution(
    val schemeCode: Int,
    val schemeName: String,
    val weightInSchemePercent: Double,
    val rupeeContribution: Double
)

/**
 * Computed Sector-level exposure across the entire portfolio.
 */
data class SectorExposure(
    val sectorName: String,
    val totalRupeeValue: Double,
    val portfolioWeightPercent: Double,
    val stockCount: Int
)

/**
 * Computed Asset Class & Market-Cap breakdown.
 */
data class AllocationSlice(
    val label: String,
    val rupeeValue: Double,
    val weightPercent: Double
)

/**
 * Complete institutional analytics summary calculated dynamically from active portfolio schemes.
 */
data class PortfolioAnalyticsSummary(
    val totalInvested: Double,
    val totalCurrentValue: Double,
    val totalGain: Double,
    val totalReturnPercent: Double,
    val weightedExpenseRatio: Double,
    val annualFeeDragRupees: Double,
    val assetClassBreakdown: List<AllocationSlice>,
    val marketCapBreakdown: List<AllocationSlice>,
    val topHoldings: List<AggregatedStockExposure>,
    val sectorBreakdown: List<SectorExposure>,
    val highOverlapStocksCount: Int
)

/**
 * Result report returned after a parallel live NAV sync against api.mfapi.in.
 */
data class NavSyncReport(
    val totalSchemes: Int,
    val updatedCount: Int,
    val failedCount: Int,
    val timestampEpochMs: Long,
    val errorMessages: List<String>
)