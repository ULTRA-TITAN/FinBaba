package com.fin.mf.data

import android.content.Context
import com.fin.mf.network.MfApiService
import com.fin.mf.network.NetworkClient
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Single Source of Truth for the user's mutual fund portfolio and institutional analytics.
 *
 * Responsibilities:
 * 1. Seeds `portfolio_active.json` in internal storage from `assets/portfolio_seed.json` on first run.
 * 2. Persists all portfolio mutations (unit updates, scheme additions/removals, live NAV syncs) atomically.
 * 3. Executes parallel coroutines (`async`/`awaitAll` inside `supervisorScope`) against `https://api.mfapi.in/`
 *    to update all 20+ schemes simultaneously with fault isolation.
 * 4. Computes institutional look-through stock overlap, sector concentration, and fee drag.
 */
class PortfolioRepository(
    private val appContext: Context,
    private val apiService: MfApiService = NetworkClient.apiService
) {

    private val fileMutex = Mutex()

    private val portfolioAdapter: JsonAdapter<PortfolioData> by lazy {
        NetworkClient.moshi.adapter(PortfolioData::class.java).indent("  ")
    }

    private val activeFile: File
        get() = File(appContext.filesDir, ACTIVE_PORTFOLIO_FILENAME)

    /**
     * Loads the active portfolio from internal storage (`portfolio_active.json`).
     * If `portfolio_active.json` does not exist yet or is corrupted, it copies and parses
     * the bundled `assets/portfolio_seed.json` and writes it to `portfolio_active.json`.
     */
    suspend fun loadPortfolio(): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            if (activeFile.exists() && activeFile.length() > 0L) {
                runCatching {
                    val jsonString = activeFile.readText(Charsets.UTF_8)
                    portfolioAdapter.fromJson(jsonString)
                }.getOrNull()?.let { existingPortfolio ->
                    return@withLock existingPortfolio
                }
            }

            // Fallback / First-launch: read from assets/portfolio_seed.json and persist to internal storage
            val seedPortfolio = readSeedFromAssetsLocked()
            writePortfolioToDiskLocked(seedPortfolio)
            seedPortfolio
        }
    }

    /**
     * Resets the user's active portfolio back to the original bundled 20-scheme `portfolio_seed.json`.
     */
    suspend fun resetToSeedPortfolio(): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val seedPortfolio = readSeedFromAssetsLocked()
            writePortfolioToDiskLocked(seedPortfolio)
            seedPortfolio
        }
    }

    /**
     * Updates the held units and invested amount for a specific scheme and persists to `portfolio_active.json`.
     */
    suspend fun updateSchemeHolding(
        schemeCode: Int,
        newUnits: Double,
        newInvestedAmount: Double
    ): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val current = readActiveOrSeedLocked()
            val updatedSchemes = current.schemes.map { scheme ->
                if (scheme.schemeCode == schemeCode) {
                    scheme.copy(
                        units = newUnits.coerceAtLeast(0.0),
                        investedAmount = newInvestedAmount.coerceAtLeast(0.0)
                    )
                } else {
                    scheme
                }
            }
            val updatedPortfolio = current.copy(schemes = updatedSchemes)
            writePortfolioToDiskLocked(updatedPortfolio)
            updatedPortfolio
        }
    }

    /**
     * Parallel live NAV refresh across all schemes in the portfolio using `supervisorScope` + `async/awaitAll`.
     *
     * Hits `https://api.mfapi.in/mf/{schemeCode}/latest` (with automatic fallback to `/mf/{schemeCode}`)
     * for every scheme concurrently. Updates `fallbackNav` and `lastNavDate` for each successful scheme
     * and persists the updated state to `portfolio_active.json`.
     */
    suspend fun syncLiveNavs(): Pair<PortfolioData, NavSyncReport> = withContext(Dispatchers.IO) {
        val currentPortfolio = loadPortfolio()
        val schemes = currentPortfolio.schemes

        data class SingleSchemeNavResult(
            val scheme: MutualFundScheme,
            val isUpdated: Boolean,
            val errorMessage: String?
        )

        val results: List<SingleSchemeNavResult> = supervisorScope {
            schemes.map { scheme ->
                async(Dispatchers.IO) {
                    fetchLatestNavForScheme(scheme).fold(
                        onSuccess = { (latestNav, latestDate) ->
                            SingleSchemeNavResult(
                                scheme = scheme.copy(
                                    fallbackNav = latestNav,
                                    lastNavDate = latestDate
                                ),
                                isUpdated = true,
                                errorMessage = null
                            )
                        },
                        onFailure = { error ->
                            SingleSchemeNavResult(
                                scheme = scheme,
                                isUpdated = false,
                                errorMessage = "${scheme.schemeCode} (${scheme.schemeName.take(22)}...):${error.localizedMessage ?: "Network error"}"
                            )
                        }
                    )
                }
            }.awaitAll()
        }

        val updatedCount = results.count { it.isUpdated }
        val failedCount = results.size - updatedCount
        val errors = results.mapNotNull { it.errorMessage }
        val nowMs = System.currentTimeMillis()

        val updatedPortfolio = fileMutex.withLock {
            val freshPortfolio = currentPortfolio.copy(
                lastSyncedEpochMs = if (updatedCount > 0) nowMs else currentPortfolio.lastSyncedEpochMs,
                schemes = results.map { it.scheme }
            )
            writePortfolioToDiskLocked(freshPortfolio)
            freshPortfolio
        }

        val report = NavSyncReport(
            totalSchemes = schemes.size,
            updatedCount = updatedCount,
            failedCount = failedCount,
            timestampEpochMs = nowMs,
            errorMessages = errors
        )

        Pair(updatedPortfolio, report)
    }

    /**
     * Fetches full historical NAV series for a single scheme (`https://api.mfapi.in/mf/{schemeCode}`)
     * for interactive charting (1M, 6M, 1Y, 3Y, 5Y) on the Scheme Detail screen.
     */
    suspend fun fetchSchemeHistoricalNavs(schemeCode: Int): Result<List<MfApiNavEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.getSchemeNavDetails(schemeCode)
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code()} when fetching history for$schemeCode")
                }
                val body = response.body()
                    ?: throw IOException("Empty response body for scheme $schemeCode")
                val entries = body.data.orEmpty()
                if (entries.isEmpty()) {
                    throw IOException("No historical NAV rows returned for scheme $schemeCode")
                }
                entries
            }
        }

    /**
     * Computes the full institutional analytics summary (Look-Through Stock Overlap,
     * Sector Exposure, Asset Class & Market-Cap Breakdown, and Weighted Expense Ratio Drag)
     * from a given [PortfolioData].
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

        // Asset Class Breakdown (Equity, Debt, Hybrid)
        val assetClassBreakdown = schemes
            .groupBy { it.assetClass }
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

        // Look-Through Underlying Stock / Instrument Aggregation
        data class MutableStockAccumulator(
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
            for (holding in scheme.holdings) {
                val rupeeContribution = schemeVal * (holding.weightPercent / 100.0)
                allocatedHoldingsRupees += rupeeContribution

                val accumulator = stockMap.getOrPut(holding.ticker) {
                    MutableStockAccumulator(
                        ticker = holding.ticker,
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
                        weightInSchemePercent = holding.weightPercent,
                        rupeeContribution = rupeeContribution
                    )
                )

                marketCapRupeeMap[holding.marketCapBucket] =
                    (marketCapRupeeMap[holding.marketCapBucket] ?: 0.0) + rupeeContribution
            }
        }

        val unallocatedTailRupees = (totalCurrentValue - allocatedHoldingsRupees).coerceAtLeast(0.0)
        if (unallocatedTailRupees > 1.0) {
            marketCapRupeeMap["Diversified Tail & Cash"] = unallocatedTailRupees
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
            .groupBy { it.sector }
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

    private suspend fun fetchLatestNavForScheme(scheme: MutualFundScheme): Result<Pair<Double, String>> =
        runCatching {
            // Try the lightweight /latest endpoint first; fall back to full /mf/{schemeCode} if needed
            val latestResponse = apiService.getSchemeLatestNav(scheme.schemeCode)
            val responseBody = if (latestResponse.isSuccessful && !latestResponse.body()?.data.isNullOrEmpty()) {
                latestResponse.body()
            } else {
                val fallbackResponse = apiService.getSchemeNavDetails(scheme.schemeCode)
                if (!fallbackResponse.isSuccessful) {
                    throw IOException("HTTP ${fallbackResponse.code()}")
                }
                fallbackResponse.body()
            }

            val topEntry = responseBody?.data?.firstOrNull()
                ?: throw IOException("Empty NAV data array")

            val parsedNav = topEntry.nav.trim().toDoubleOrNull()
                ?: throw IOException("Invalid NAV format: '${topEntry.nav}'")

            if (parsedNav <= 0.0) {
                throw IOException("Non-positive NAV: $parsedNav")
            }

            Pair(parsedNav, topEntry.date)
        }

    private fun readActiveOrSeedLocked(): PortfolioData {
        if (activeFile.exists() && activeFile.length() > 0L) {
            runCatching {
                val json = activeFile.readText(Charsets.UTF_8)
                portfolioAdapter.fromJson(json)
            }.getOrNull()?.let { return it }
        }
        val seed = readSeedFromAssetsLocked()
        writePortfolioToDiskLocked(seed)
        return seed
    }

    private fun readSeedFromAssetsLocked(): PortfolioData {
        val seedJson = appContext.assets.open(SEED_PORTFOLIO_FILENAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return portfolioAdapter.fromJson(seedJson)
            ?: throw IOException("Failed to deserialize $SEED_PORTFOLIO_FILENAME")
    }

    private fun writePortfolioToDiskLocked(portfolio: PortfolioData) {
        val tempFile = File(appContext.filesDir, "$ACTIVE_PORTFOLIO_FILENAME.tmp")
        val jsonOutput = portfolioAdapter.toJson(portfolio)
        tempFile.writeText(jsonOutput, Charsets.UTF_8)
        if (activeFile.exists()) {
            activeFile.delete()
        }
        val renamed = tempFile.renameTo(activeFile)
        if (!renamed) {
            // Fallback direct write if rename across filesystems fails
            activeFile.writeText(jsonOutput, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    companion object {
        private const val SEED_PORTFOLIO_FILENAME = "portfolio_seed.json"
        private const val ACTIVE_PORTFOLIO_FILENAME = "portfolio_active.json"
    }
}