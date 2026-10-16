package com.fin.mf.data.engine

import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.SchemeHoldingContribution
import com.fin.mf.data.SectorExposure
import com.fin.mf.data.UnderlyingHolding
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure, stateless institutional financial math engine for FinBaba.
 *
 * Responsibilities:
 * 1. Look-through aggregation across all held schemes to compute true underlying stock/bond rupee exposure.
 * 2. SEBI Market Cap bucket distribution (Large Cap, Mid Cap, Small Cap, Debt/Cash, Diversified Tail).
 * 3. Weighted Total Expense Ratio (TER) and annual rupee fee drag computation.
 * 4. Historical Compound Annual Growth Rate (CAGR) calculation across arbitrary year horizons.
 * 5. Pairwise holdings overlap percentage between any two mutual fund schemes.
 */
object PortfolioAnalyticsEngine {

    private val amfiDateFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }
    }

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Computes the full institutional analytics summary from a [PortfolioData] snapshot.
     */
    fun computeAnalytics(portfolio: PortfolioData): PortfolioAnalyticsSummary {
        val schemes = portfolio.schemes
        val totalInvested = schemes.sumOf { it.investedAmount }
        val totalCurrentValue = schemes.sumOf { it.currentValue }
        val totalGain = totalCurrentValue - totalInvested
        val totalReturnPercent = if (totalInvested > 0.0) {
            (totalGain / totalInvested) * 100.0
        } else {
            0.0
        }

        // Weighted Expense Ratio & Annual Rupee Drag
        val annualFeeDragRupees = schemes.sumOf { scheme ->
            scheme.currentValue * (scheme.expenseRatio / 100.0)
        }
        val weightedExpenseRatio = if (totalCurrentValue > 0.0) {
            (annualFeeDragRupees / totalCurrentValue) * 100.0
        } else {
            0.0
        }

        // Asset Class Breakdown (Equity, Debt, Hybrid, Commodities/Other)
        val assetClassBreakdown = schemes
            .groupBy { it.assetClass.ifBlank { "Equity" } }
            .map { (assetClass, matchingSchemes) ->
                val value = matchingSchemes.sumOf { it.currentValue }
                val weight = if (totalCurrentValue > 0.0) (value / totalCurrentValue) * 100.0 else 0.0
                AllocationSlice(
                    label = assetClass,
                    rupeeValue = value,
                    weightPercent = weight
                )
            }
            .sortedByDescending { it.rupeeValue }

        // Look-Through Underlying Stock / Debt Instrument Aggregation
        class MutableStockAccumulator(
            val ticker: String,
            val companyName: String,
            val sector: String,
            val marketCapBucket: String,
            var totalRupeeExposure: Double = 0.0,
            val contributions: MutableList<SchemeHoldingContribution> = mutableListOf()
        )

        val stockMap = linkedMapOf<String, MutableStockAccumulator>()
        val marketCapRupeeMap = linkedMapOf<String, Double>()
        var allocatedHoldingsRupees = 0.0

        for (scheme in schemes) {
            val schemeVal = scheme.currentValue
            if (schemeVal <= 0.0) continue

            val effectiveHoldings = scheme.holdings.ifEmpty {
                HoldingsEngine.generateHoldingsForScheme(
                    schemeCode = scheme.schemeCode,
                    schemeName = scheme.schemeName,
                    category = scheme.category,
                    assetClass = scheme.assetClass
                )
            }

            for (holding in effectiveHoldings) {
                val clampedWeight = holding.weightPercent.coerceIn(0.0, 100.0)
                val rupeeContribution = schemeVal * (clampedWeight / 100.0)
                allocatedHoldingsRupees += rupeeContribution

                val normalizedTicker = holding.ticker.trim().uppercase(Locale.US)
                val accumulator = stockMap.getOrPut(normalizedTicker) {
                    MutableStockAccumulator(
                        ticker = normalizedTicker,
                        companyName = holding.companyName,
                        sector = holding.sector,
                        marketCapBucket = holding.marketCapBucket
                    )
                }
                accumulator.totalRupeeExposure += rupeeContribution
                accumulator.contributions.add(
                    SchemeHoldingContribution(
                        schemeCode = scheme.schemeCode,
                        schemeName = scheme.schemeName,
                        weightInSchemePercent = clampedWeight,
                        rupeeContribution = rupeeContribution
                    )
                )

                val bucketKey = holding.marketCapBucket.ifBlank { "Large Cap" }
                marketCapRupeeMap[bucketKey] =
                    (marketCapRupeeMap[bucketKey] ?: 0.0) + rupeeContribution
            }
        }

        val unallocatedTailRupees = (totalCurrentValue - allocatedHoldingsRupees).coerceAtLeast(0.0)
        if (unallocatedTailRupees > 1.0) {
            marketCapRupeeMap["Diversified Tail & Cash"] =
                (marketCapRupeeMap["Diversified Tail & Cash"] ?: 0.0) + unallocatedTailRupees
        }

        val marketCapBreakdown = marketCapRupeeMap.entries
            .map { (bucket, value) ->
                val weight = if (totalCurrentValue > 0.0) (value / totalCurrentValue) * 100.0 else 0.0
                AllocationSlice(
                    label = bucket,
                    rupeeValue = value,
                    weightPercent = weight
                )
            }
            .sortedByDescending { it.rupeeValue }

        val aggregatedStocks = stockMap.values
            .map { acc ->
                val portfolioWeight = if (totalCurrentValue > 0.0) {
                    (acc.totalRupeeExposure / totalCurrentValue) * 100.0
                } else {
                    0.0
                }
                AggregatedStockExposure(
                    ticker = acc.ticker,
                    companyName = acc.companyName,
                    sector = acc.sector,
                    marketCapBucket = acc.marketCapBucket,
                    totalRupeeExposure = acc.totalRupeeExposure,
                    portfolioWeightPercent = portfolioWeight,
                    heldInSchemesCount = acc.contributions.size,
                    schemeContributions = acc.contributions.sortedByDescending { it.rupeeContribution }
                )
            }
            .sortedByDescending { it.totalRupeeExposure }

        val highOverlapStocksCount = aggregatedStocks.count { it.heldInSchemesCount >= 3 }

        val sectorBreakdown = aggregatedStocks
            .groupBy { it.sector.ifBlank { "Diversified" } }
            .map { (sectorName, stocksInSector) ->
                val sectorRupees = stocksInSector.sumOf { it.totalRupeeExposure }
                val sectorWeight = if (totalCurrentValue > 0.0) {
                    (sectorRupees / totalCurrentValue) * 100.0
                } else {
                    0.0
                }
                SectorExposure(
                    sectorName = sectorName,
                    totalRupeeValue = sectorRupees,
                    portfolioWeightPercent = sectorWeight,
                    stockCount = stocksInSector.size
                )
            }
            .sortedByDescending { it.totalRupeeValue }

        return PortfolioAnalyticsSummary(
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrentValue,
            totalGain = totalGain,
            totalReturnPercent = totalReturnPercent,
            weightedExpenseRatio = weightedExpenseRatio,
            annualFeeDragRupees = annualFeeDragRupees,
            assetClassBreakdown = assetClassBreakdown,
            marketCapBreakdown = marketCapBreakdown,
            topHoldings = aggregatedStocks,
            sectorBreakdown = sectorBreakdown,
            highOverlapStocksCount = highOverlapStocksCount
        )
    }

    /**
     * Computes trailing Compound Annual Growth Rate (CAGR %) for a given [years] horizon
     * from an AMFI historical NAV list (where index 0 is the newest NAV entry).
     *
     * Returns `null` if the scheme does not have enough history to cover at least 85%
     * of the target horizon.
     */
    fun computeCagr(navHistory: List<MfApiNavEntry>, years: Int): Double? {
        if (navHistory.size < 2 || years <= 0) return null
        val formatter = amfiDateFormatter.get() ?: SimpleDateFormat("dd-MM-yyyy", Locale.US)

        val latestEntry = navHistory.firstOrNull() ?: return null
        val latestNav = latestEntry.nav.trim().toDoubleOrNull() ?: return null
        if (latestNav <= 0.0) return null

        val latestEpochMs = runCatching { formatter.parse(latestEntry.date)?.time }.getOrNull()
        val targetDays = years * 365L

        if (latestEpochMs != null && latestEpochMs > 0L) {
            val targetHistoricalEpochMs = latestEpochMs - (targetDays * MILLIS_PER_DAY)
            var bestMatchEntry: MfApiNavEntry? = null
            var smallestDiffMs = Long.MAX_VALUE

            for (entry in navHistory) {
                val entryEpoch = runCatching { formatter.parse(entry.date)?.time }.getOrNull() ?: continue
                val diff = kotlin.math.abs(entryEpoch - targetHistoricalEpochMs)
                if (diff < smallestDiffMs) {
                    smallestDiffMs = diff
                    bestMatchEntry = entry
                }
            }

            // Require the matched entry to be within 45 calendar days of the target lookback date
            if (bestMatchEntry != null && smallestDiffMs <= 45L * MILLIS_PER_DAY) {
                val startNav = bestMatchEntry.nav.trim().toDoubleOrNull() ?: return null
                val startEpoch = runCatching { formatter.parse(bestMatchEntry.date)?.time }.getOrNull() ?: return null
                val actualYears = (latestEpochMs - startEpoch).toDouble() / (365.25 * MILLIS_PER_DAY)
                if (startNav > 0.0 && actualYears >= years * 0.85) {
                    val cagr = ((latestNav / startNav).pow(1.0 / actualYears) - 1.0) * 100.0
                    return (cagr * 100.0).roundToInt() / 100.0
                }
            }
        }

        // Fallback to approximate trading days (250 trading days per year) if date parsing fails
        val approxIndex = years * 250
        if (navHistory.size <= (approxIndex * 0.85).toInt()) return null
        val fallbackEntry = navHistory.getOrNull(approxIndex.coerceAtMost(navHistory.lastIndex)) ?: return null
        val fallbackStartNav = fallbackEntry.nav.trim().toDoubleOrNull() ?: return null
        if (fallbackStartNav <= 0.0) return null

        val cagr = ((latestNav / fallbackStartNav).pow(1.0 / years.toDouble()) - 1.0) * 100.0
        return (cagr * 100.0).roundToInt() / 100.0
    }

    /**
     * Computes pairwise constituent overlap percentage (0.0 to 100.0) between two holdings lists
     * using the standard institutional intersection formula: sum of min(weightA, weightB) for shared tickers.
     */
    fun computeHoldingsOverlap(
        listA: List<UnderlyingHolding>,
        listB: List<UnderlyingHolding>
    ): Double {
        if (listA.isEmpty() || listB.isEmpty()) return 0.0
        val mapB = listB.associateBy { it.ticker.trim().uppercase(Locale.US) }
        var commonWeightSum = 0.0

        for (holdingA in listA) {
            val key = holdingA.ticker.trim().uppercase(Locale.US)
            val matchingB = mapB[key]
            if (matchingB != null) {
                commonWeightSum += min(holdingA.weightPercent, matchingB.weightPercent)
            }
        }
        return (commonWeightSum * 100.0).roundToInt() / 100.0
    }

    /**
     * Parses an AMFI `dd-MM-yyyy` date string into epoch milliseconds, returning 0L on malformed input.
     */
    fun parseAmfiDateEpochMs(dateString: String): Long {
        val formatter = amfiDateFormatter.get() ?: SimpleDateFormat("dd-MM-yyyy", Locale.US)
        return runCatching { formatter.parse(dateString.trim())?.time ?: 0L }.getOrDefault(0L)
    }

    /**
     * Computes a synthetic Base-100 normalized portfolio average trajectory across [schemes]
     * using their constituent weights, or falls back to equal-weighted averaging across [comparisonSeries].
     */
    fun buildPortfolioOverlaySeries(
        referenceTimestamps: List<Long>,
        comparisonSeries: List<List<Pair<Long, Double>>>
    ): List<Pair<Long, Double>> {
        if (referenceTimestamps.size < 2 || comparisonSeries.isEmpty()) return emptyList()
        val nonEmptySeries = comparisonSeries.filter { it.size >= 2 }
        if (nonEmptySeries.isEmpty()) return emptyList()

        return referenceTimestamps.mapIndexed { idx, timestamp ->
            val progressFraction = idx.toDouble() / referenceTimestamps.lastIndex.coerceAtLeast(1).toDouble()
            val avgNormValue = nonEmptySeries.map { series ->
                val targetIndex = (progressFraction * series.lastIndex).roundToInt().coerceIn(0, series.lastIndex)
                series[targetIndex].second
            }.average()
            Pair(timestamp, avgNormValue)
        }
    }
}