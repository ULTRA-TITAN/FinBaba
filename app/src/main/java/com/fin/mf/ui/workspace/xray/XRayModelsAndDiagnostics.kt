package com.fin.mf.ui.workspace.xray

import androidx.compose.ui.graphics.Color
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Diagnostic results returned by the institutional health scoring algorithm.
 */
data class PortfolioHealthDiagnostic(
    val score: Int,
    val baseScore: Int = 100,
    val duplicateOverlapDeduction: Int,
    val duplicateOverlapPct: Double,
    val singleStockConcentrationDeduction: Int,
    val maxStockConcentrationPct: Double,
    val maxStockTicker: String,
    val marketCapDriftDeduction: Int,
    val maxDriftTier: String,
    val maxDriftPct: Double,
    val uniqueStocksCount: Int,
    val grossHoldingsCount: Int,
    val activeSharePercent: Double,
    val rating: String,
    val ratingColor: Color
)

/**
 * Benchmark mapping row for the Index Matrix component.
 */
data class BenchmarkIndexRow(
    val title: String,
    val description: String,
    val weightPercent: Double,
    val rupeeExposure: Double,
    val color: Color,
    val constituents: List<AggregatedStockExposure>
)

/**
 * Projected allocation result from the What-If Inflow Rebalance simulation.
 */
data class InflowSimulationResult(
    val inflowAmount: Double,
    val currentLargeRupees: Double,
    val currentMidRupees: Double,
    val currentSmallRupees: Double,
    val allocatedLargeRupees: Double,
    val allocatedMidRupees: Double,
    val allocatedSmallRupees: Double,
    val currentLargePct: Double,
    val currentMidPct: Double,
    val currentSmallPct: Double,
    val projectedLargePct: Double,
    val projectedMidPct: Double,
    val projectedSmallPct: Double
)

object XRayDiagnosticsEngine {

    /**
     * Institutional Health Scorecard calculation rule:
     * - Base score = 100
     * - Subtract 1 point for every 2% of duplicate overlap above 30%
     * - Subtract 5 points if any single stock concentration exceeds 10%
     * - Subtract 5 points if market-cap drift in any tier exceeds ±8%
     * - Coerced between 0 and 100
     */
    fun computeHealthDiagnostic(
        analytics: PortfolioAnalyticsSummary,
        schemes: List<MutualFundScheme>
    ): PortfolioHealthDiagnostic {
        val grossHoldingsCount = schemes.sumOf { scheme ->
            if (scheme.holdings.isNotEmpty()) scheme.holdings.size else 15
        }.coerceAtLeast(1)

        val uniqueStocksCount = analytics.topHoldings.size
        val duplicateOverlapPct = if (grossHoldingsCount > 0) {
            ((grossHoldingsCount - uniqueStocksCount).toDouble() / grossHoldingsCount * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val duplicateOverlapDeduction = if (duplicateOverlapPct > 30.0) {
            ((duplicateOverlapPct - 30.0) / 2.0).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val maxStock = analytics.topHoldings.maxByOrNull { it.portfolioWeightPercent }
        val maxStockConcentrationPct = maxStock?.portfolioWeightPercent ?: 0.0
        val maxStockTicker = maxStock?.ticker ?: "N/A"
        val singleStockDeduction = if (maxStockConcentrationPct > 10.0) 5 else 0

        val largeCapActual = analytics.marketCapBreakdown
            .firstOrNull { it.label.contains("Large", ignoreCase = true) }?.weightPercent ?: 0.0
        val midCapActual = analytics.marketCapBreakdown
            .firstOrNull { it.label.contains("Mid", ignoreCase = true) }?.weightPercent ?: 0.0
        val smallCapActual = analytics.marketCapBreakdown
            .firstOrNull { it.label.contains("Small", ignoreCase = true) }?.weightPercent ?: 0.0

        val largeDrift = abs(largeCapActual - 50.0)
        val midDrift = abs(midCapActual - 25.0)
        val smallDrift = abs(smallCapActual - 25.0)

        val maxDriftPair = listOf(
            Pair("Large Cap", largeDrift),
            Pair("Mid Cap", midDrift),
            Pair("Small Cap", smallDrift)
        ).maxByOrNull { it.second } ?: Pair("Large Cap", 0.0)

        val marketCapDriftDeduction = if (maxDriftPair.second > 8.0) 5 else 0

        val totalDeductions = duplicateOverlapDeduction + singleStockDeduction + marketCapDriftDeduction
        val finalScore = (100 - totalDeductions).coerceIn(0, 100)

        // Estimated Active Share vs Nifty 50:
        val largeCapSum = analytics.topHoldings
            .filter { it.marketCapBucket.contains("Large", ignoreCase = true) }
            .sumOf { it.portfolioWeightPercent }
        val activeShare = (100.0 - (largeCapSum * 0.44)).coerceIn(45.0, 92.0)

        val (rating, color) = when {
            finalScore >= 85 -> Pair("Institutional Grade", Color(0xFF059669))
            finalScore >= 70 -> Pair("Strong Quality", Color(0xFF10B981))
            finalScore >= 55 -> Pair("Moderate Overlap", Color(0xFFD97706))
            else -> Pair("High Concentration", Color(0xFFDC2626))
        }

        return PortfolioHealthDiagnostic(
            score = finalScore,
            baseScore = 100,
            duplicateOverlapDeduction = duplicateOverlapDeduction,
            duplicateOverlapPct = (duplicateOverlapPct * 10.0).roundToInt() / 10.0,
            singleStockConcentrationDeduction = singleStockDeduction,
            maxStockConcentrationPct = (maxStockConcentrationPct * 10.0).roundToInt() / 10.0,
            maxStockTicker = maxStockTicker,
            marketCapDriftDeduction = marketCapDriftDeduction,
            maxDriftTier = maxDriftPair.first,
            maxDriftPct = (maxDriftPair.second * 10.0).roundToInt() / 10.0,
            uniqueStocksCount = uniqueStocksCount,
            grossHoldingsCount = grossHoldingsCount,
            activeSharePercent = (activeShare * 10.0).roundToInt() / 10.0,
            rating = rating,
            ratingColor = color
        )
    }

    /**
     * Computes the 5 benchmark index rows with dynamic constituent groupings.
     */
    fun computeIndexMatrix(analytics: PortfolioAnalyticsSummary): List<BenchmarkIndexRow> {
        val holdings = analytics.topHoldings
        val totalVal = analytics.totalCurrentValue

        val nifty50Bluechips = setOf(
            "HDFCBANK", "ICICIBANK", "RELIANCE", "INFY", "TCS", "BHARTIARTL",
            "LT", "ITC", "SBIN", "AXISBANK", "KOTAKBANK", "SUNPHARMA",
            "M&M", "MARUTI", "NTPC", "POWERGRID", "TATAMOTORS", "ULTRACEMCO",
            "BAJFINANCE", "HCLTECH", "HINDUNILVR", "TITAN", "ADANIPORTS", "COALINDIA", "HAL"
        )

        val nifty50Stocks = holdings.filter {
            it.marketCapBucket.contains("Large", ignoreCase = true) &&
                    (it.ticker in nifty50Bluechips || it.portfolioWeightPercent >= 2.5)
        }

        val niftyNext50Stocks = holdings.filter {
            it.marketCapBucket.contains("Large", ignoreCase = true) && it !in nifty50Stocks
        }

        val midcapStocks = holdings.filter {
            it.marketCapBucket.contains("Mid", ignoreCase = true)
        }

        val smallcapStocks = holdings.filter {
            it.marketCapBucket.contains("Small", ignoreCase = true)
        }

        val unlistedAndDebt = holdings.filter {
            it.marketCapBucket.contains("Debt", ignoreCase = true) ||
                    it.marketCapBucket.contains("Cash", ignoreCase = true) ||
                    it.marketCapBucket.contains("Micro", ignoreCase = true) ||
                    (!it.marketCapBucket.contains("Large", ignoreCase = true) &&
                            !it.marketCapBucket.contains("Mid", ignoreCase = true) &&
                            !it.marketCapBucket.contains("Small", ignoreCase = true))
        }

        fun makeRow(
            title: String,
            desc: String,
            color: Color,
            stocks: List<AggregatedStockExposure>
        ): BenchmarkIndexRow {
            val rupees = stocks.sumOf { it.totalRupeeExposure }
            val pct = if (totalVal > 0) (rupees / totalVal * 100.0) else 0.0
            return BenchmarkIndexRow(
                title = title,
                description = desc,
                weightPercent = (pct * 10.0).roundToInt() / 10.0,
                rupeeExposure = rupees,
                color = color,
                constituents = stocks
            )
        }

        return listOf(
            makeRow("Nifty 50", "Top 50 Bluechip Leaders", Color(0xFF1E3A8A), nifty50Stocks),
            makeRow("Nifty Next 50", "Top 51-100 Large Cap Challengers", Color(0xFF2563EB), niftyNext50Stocks),
            makeRow("Nifty Midcap 150", "High-Alpha Mid Market Leaders", Color(0xFF0891B2), midcapStocks),
            makeRow("Nifty Smallcap 250", "Emerging Fast-Growth Small Caps", Color(0xFF0D9488), smallcapStocks),
            makeRow("Unlisted / US Tech / Cash", "Debt, TREPS & Global Assets", Color(0xFF64748B), unlistedAndDebt)
        )
    }

    /**
     * Simulates smart capital inflow allocation to rebalance the portfolio toward the
     * 50% Large / 25% Mid / 25% Small mandate.
     */
    fun simulateInflowRebalance(
        analytics: PortfolioAnalyticsSummary,
        inflow: Double
    ): InflowSimulationResult {
        val totalCurrentVal = analytics.totalCurrentValue
        val largeSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Large", ignoreCase = true) }
        val midSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Mid", ignoreCase = true) }
        val smallSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Small", ignoreCase = true) }

        val currLarge = largeSlice?.rupeeValue ?: (totalCurrentVal * 0.5)
        val currMid = midSlice?.rupeeValue ?: (totalCurrentVal * 0.25)
        val currSmall = smallSlice?.rupeeValue ?: (totalCurrentVal * 0.25)

        val newTotal = (totalCurrentVal + inflow).coerceAtLeast(1.0)
        val targetLargeRupees = newTotal * 0.50
        val targetMidRupees = newTotal * 0.25
        val targetSmallRupees = newTotal * 0.25

        val largeGap = (targetLargeRupees - currLarge).coerceAtLeast(0.0)
        val midGap = (targetMidRupees - currMid).coerceAtLeast(0.0)
        val smallGap = (targetSmallRupees - currSmall).coerceAtLeast(0.0)
        val totalGap = largeGap + midGap + smallGap

        val (allocLarge, allocMid, allocSmall) = if (totalGap > 0.0) {
            Triple(
                inflow * (largeGap / totalGap),
                inflow * (midGap / totalGap),
                inflow * (smallGap / totalGap)
            )
        } else {
            Triple(inflow * 0.50, inflow * 0.25, inflow * 0.25)
        }

        val currLargePct = if (totalCurrentVal > 0) (currLarge / totalCurrentVal * 100.0) else 50.0
        val currMidPct = if (totalCurrentVal > 0) (currMid / totalCurrentVal * 100.0) else 25.0
        val currSmallPct = if (totalCurrentVal > 0) (currSmall / totalCurrentVal * 100.0) else 25.0

        val projLargePct = ((currLarge + allocLarge) / newTotal) * 100.0
        val projMidPct = ((currMid + allocMid) / newTotal) * 100.0
        val projSmallPct = ((currSmall + allocSmall) / newTotal) * 100.0

        return InflowSimulationResult(
            inflowAmount = inflow,
            currentLargeRupees = currLarge,
            currentMidRupees = currMid,
            currentSmallRupees = currSmall,
            allocatedLargeRupees = allocLarge,
            allocatedMidRupees = allocMid,
            allocatedSmallRupees = allocSmall,
            currentLargePct = (currLargePct * 10.0).roundToInt() / 10.0,
            currentMidPct = (currMidPct * 10.0).roundToInt() / 10.0,
            currentSmallPct = (currSmallPct * 10.0).roundToInt() / 10.0,
            projectedLargePct = (projLargePct * 10.0).roundToInt() / 10.0,
            projectedMidPct = (projMidPct * 10.0).roundToInt() / 10.0,
            projectedSmallPct = (projSmallPct * 10.0).roundToInt() / 10.0
        )
    }
}
