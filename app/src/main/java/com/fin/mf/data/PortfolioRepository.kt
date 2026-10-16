package com.fin.mf.data

import android.content.Context
import com.fin.mf.data.engine.HoldingsEngine
import com.fin.mf.data.engine.PortfolioAnalyticsEngine
import com.fin.mf.data.engine.PortfolioCsvParser
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single Source of Truth for the user's mutual fund portfolio and institutional analytics.
 *
 * Responsibilities:
 * 1. Seeds `portfolio_active.json` in internal storage from `assets/portfolio_seed.json` on first run.
 * 2. Persists all portfolio mutations (unit updates, scheme additions/removals, CSV import/export, live NAV syncs) atomically.
 * 3. Delegates look-through holdings generation and AMFI classification to [HoldingsEngine].
 * 4. Delegates pure analytics math, CAGR, fee drag, and overlap to [PortfolioAnalyticsEngine].
 * 5. Delegates CSV serialization and parsing to [PortfolioCsvParser].
 * 6. Executes parallel coroutines (`async`/`awaitAll` inside `supervisorScope`) against `https://api.mfapi.in/`.
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
     * If existing portfolio schemes have sparse holdings (< 15 constituents), it auto-hydrates
     * them with full look-through constituents from [HoldingsEngine] and persists to disk.
     */
    suspend fun loadPortfolio(): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            if (activeFile.exists() && activeFile.length() > 0L) {
                runCatching {
                    val jsonString = activeFile.readText(Charsets.UTF_8)
                    portfolioAdapter.fromJson(jsonString)
                }.getOrNull()?.let { existingPortfolio ->
                    val needsEnrichment = existingPortfolio.schemes.any { it.holdings.size < 15 }
                    if (needsEnrichment) {
                        val seed = runCatching { readSeedFromAssetsLocked() }.getOrNull()
                        val seedMap = seed?.schemes?.associateBy { it.schemeCode }.orEmpty()

                        val enrichedSchemes = existingPortfolio.schemes.map { s ->
                            if (s.holdings.size < 15) {
                                val seedScheme = seedMap[s.schemeCode]
                                val enrichedHoldings = if (seedScheme != null && seedScheme.holdings.size >= 15) {
                                    seedScheme.holdings
                                } else {
                                    HoldingsEngine.generateHoldings(s.schemeCode, s.schemeName, s.category, s.assetClass)
                                }
                                s.copy(holdings = enrichedHoldings)
                            } else s
                        }
                        val hydratedPortfolio = existingPortfolio.copy(schemes = enrichedSchemes)
                        writePortfolioToDiskLocked(hydratedPortfolio)
                        return@withLock hydratedPortfolio
                    }
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
     * Creates a new empty or custom portfolio and overwrites `portfolio_active.json`.
     */
    suspend fun createCustomPortfolio(name: String, owner: String): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val customPortfolio = PortfolioData(
                portfolioName = name.ifBlank { "Custom Portfolio" },
                ownerName = owner.ifBlank { "Vault Owner" },
                lastSyncedEpochMs = System.currentTimeMillis(),
                schemes = emptyList()
            )
            writePortfolioToDiskLocked(customPortfolio)
            customPortfolio
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
     * Expands partial search tokens and common prefixes (e.g. 'kot-' -> 'Kotak', 'par' -> 'Parag Parikh').
     */
    private fun expandSearchQuery(input: String): String {
        val cleaned = input.trim().replace("[-_]+".toRegex(), " ")
        val tokens = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return input.trim()

        val expandedTokens = tokens.map { token ->
            val t = token.lowercase(Locale.US)
            when {
                t.startsWith("kot") -> "Kotak"
                t == "ppfas" || t == "ppf" || t.startsWith("para") || t == "par" -> "Parag Parikh"
                t.startsWith("hdf") -> "HDFC"
                t.startsWith("ici") -> "ICICI"
                t == "sbi" -> "SBI"
                t.startsWith("qua") -> "Quant"
                t.startsWith("moti") || t.startsWith("mot") -> "Motilal"
                t.startsWith("mir") -> "Mirae"
                t.startsWith("nip") -> "Nippon"
                t.startsWith("can") -> "Canara"
                t.startsWith("axi") -> "Axis"
                t == "dsp" -> "DSP"
                t.startsWith("tat") -> "Tata"
                t.startsWith("ban") -> "Bandhan"
                t.startsWith("fra") -> "Franklin"
                t.startsWith("bar") -> "Baroda"
                t.startsWith("inv") -> "Invesco"
                t.startsWith("sun") -> "Sundaram"
                t.startsWith("ede") -> "Edelweiss"
                t.startsWith("gro") -> "Groww"
                t == "uti" -> "UTI"
                t == "hsbc" -> "HSBC"
                t.startsWith("mah") -> "Mahindra"
                t.startsWith("whi") -> "WhiteOak"
                t.startsWith("nif") -> "Nifty"
                t.startsWith("sen") -> "Sensex"
                t.startsWith("fle") -> "Flexi"
                t.startsWith("sma") -> "Small"
                t.startsWith("mid") -> "Mid"
                t.startsWith("lar") -> "Large"
                t.startsWith("liq") -> "Liquid"
                t.startsWith("gil") -> "Gilt"
                t.startsWith("arb") -> "Arbitrage"
                t.startsWith("tec") -> "Technology"
                t.startsWith("pha") -> "Pharma"
                t.startsWith("inf") -> "Infrastructure"
                t.startsWith("def") -> "Defence"
                else -> token
            }
        }
        return expandedTokens.joinToString(" ")
    }

    /**
     * Smart AMFI search with prefix expansion, token matching, and Direct-Growth prioritization.
     */
    suspend fun searchSchemes(query: String): Result<List<MfSearchDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val q = query.trim()
            if (q.isEmpty()) return@runCatching emptyList()

            val effectiveQuery = expandSearchQuery(q)

            val response = apiService.searchSchemes(effectiveQuery)
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code()} searching schemes for '$effectiveQuery'")
            }
            val rawList = response.body().orEmpty()

            // Smart scoring and ranking
            val queryLower = q.lowercase(Locale.US)
            val queryTokens = effectiveQuery.lowercase(Locale.US).split("\\s+".toRegex()).filter { it.isNotBlank() }
            val wantsDividend = queryLower.contains("dividend") || queryLower.contains("idcw")
            val wantsRegular = queryLower.contains("regular")

            rawList.sortedByDescending { item ->
                var score = 0
                val nameLower = item.schemeName.lowercase(Locale.US)
                val codeStr = item.schemeCode.toString()

                if (codeStr == q) score += 2000
                if (nameLower.startsWith(queryLower)) score += 600

                val matchesAllTokens = queryTokens.all { nameLower.contains(it) }
                if (matchesAllTokens) score += 400

                // Direct & Growth preference
                val isDirect = nameLower.contains("direct")
                val isGrowth = nameLower.contains("growth")
                val isIdcw = nameLower.contains("idcw") || nameLower.contains("dividend")

                if (isDirect) score += 300
                if (isGrowth) score += 250

                if (!wantsDividend && isIdcw) score -= 350
                if (!wantsRegular && nameLower.contains("regular")) score -= 150

                score
            }
        }
    }

    /**
     * Fetches scheme metadata and latest NAV from AMFI API, generates 20-25 authentic holdings,
     * and returns an immutable MutualFundScheme model for external inspection without persisting.
     */
    suspend fun getSchemeDetails(schemeCode: Int): Result<MutualFundScheme> = withContext(Dispatchers.IO) {
        runCatching {
            fileMutex.withLock {
                val current = readActiveOrSeedLocked()
                val existing = current.schemes.find { it.schemeCode == schemeCode }
                if (existing != null) return@runCatching existing
            }

            val navDetailsResponse = apiService.getSchemeNavDetails(schemeCode)
            if (!navDetailsResponse.isSuccessful) {
                throw IOException("HTTP ${navDetailsResponse.code()} fetching details for scheme #$schemeCode")
            }
            val navBody = navDetailsResponse.body()
            val meta = navBody?.meta
            val topEntry = navBody?.data?.firstOrNull()

            val parsedNav = topEntry?.nav?.trim()?.toDoubleOrNull() ?: 10.0
            val navDate = topEntry?.date ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

            val schemeName = meta?.schemeName ?: "Scheme #$schemeCode"
            val (inferredCategory, inferredAssetClass) = HoldingsEngine.inferCategoryAndAssetClass(schemeName)
            val category = meta?.schemeCategory ?: meta?.schemeType ?: inferredCategory
            val assetClass = inferredAssetClass
            val amc = meta?.fundHouse ?: HoldingsEngine.inferAmcName(schemeName)
            val riskLevel = HoldingsEngine.inferRiskLevel(category, assetClass)
            val benchmark = HoldingsEngine.inferBenchmark(category)
            val expenseRatio = HoldingsEngine.inferExpenseRatio(category, isDirect = schemeName.contains("direct", ignoreCase = true))

            val authenticHoldings = HoldingsEngine.generateHoldings(schemeCode, schemeName, category, assetClass)

            MutualFundScheme(
                schemeCode = schemeCode,
                schemeName = schemeName,
                amc = amc,
                category = category,
                assetClass = assetClass,
                riskLevel = riskLevel,
                expenseRatio = expenseRatio,
                exitLoad = "1.0% within 365 days",
                benchmark = benchmark,
                units = 0.0,
                investedAmount = 0.0,
                fallbackNav = parsedNav,
                lastNavDate = navDate,
                holdings = authenticHoldings
            )
        }
    }

    /**
     * Fetches details for any searched AMFI scheme and adds it to `portfolio_active.json`.
     * Uses HoldingsEngine to attach 20–23 authentic constituent holdings.
     */
    suspend fun addSchemeToPortfolio(
        schemeCode: Int,
        units: Double,
        investedAmount: Double
    ): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val current = readActiveOrSeedLocked()
            val existing = current.schemes.find { it.schemeCode == schemeCode }
            if (existing != null) {
                val updatedSchemes = current.schemes.map {
                    if (it.schemeCode == schemeCode) {
                        it.copy(
                            units = (it.units + units).coerceAtLeast(0.0),
                            investedAmount = (it.investedAmount + investedAmount).coerceAtLeast(0.0)
                        )
                    } else it
                }
                val updatedPortfolio = current.copy(schemes = updatedSchemes)
                writePortfolioToDiskLocked(updatedPortfolio)
                return@withLock updatedPortfolio
            }

            // Fetch metadata and latest NAV from AMFI API
            val navDetailsResponse = runCatching { apiService.getSchemeNavDetails(schemeCode) }.getOrNull()
            val navBody = navDetailsResponse?.body()
            val meta = navBody?.meta
            val topEntry = navBody?.data?.firstOrNull()

            val parsedNav = topEntry?.nav?.trim()?.toDoubleOrNull() ?: 10.0
            val navDate = topEntry?.date ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

            val schemeName = meta?.schemeName ?: "Scheme #$schemeCode"
            val (inferredCategory, inferredAssetClass) = HoldingsEngine.inferCategoryAndAssetClass(schemeName)
            val category = meta?.schemeCategory ?: meta?.schemeType ?: inferredCategory
            val assetClass = inferredAssetClass
            val amc = meta?.fundHouse ?: HoldingsEngine.inferAmcName(schemeName)
            val riskLevel = HoldingsEngine.inferRiskLevel(category, assetClass)
            val benchmark = HoldingsEngine.inferBenchmark(category)
            val expenseRatio = HoldingsEngine.inferExpenseRatio(category, isDirect = schemeName.contains("direct", ignoreCase = true))

            val authenticHoldings = HoldingsEngine.generateHoldings(schemeCode, schemeName, category, assetClass)

            val newScheme = MutualFundScheme(
                schemeCode = schemeCode,
                schemeName = schemeName,
                amc = amc,
                category = category,
                assetClass = assetClass,
                riskLevel = riskLevel,
                expenseRatio = expenseRatio,
                exitLoad = "1.0% within 365 days",
                benchmark = benchmark,
                units = units.coerceAtLeast(0.0),
                investedAmount = investedAmount.coerceAtLeast(0.0),
                fallbackNav = parsedNav,
                lastNavDate = navDate,
                holdings = authenticHoldings
            )

            val updatedSchemes = current.schemes + newScheme
            val updatedPortfolio = current.copy(schemes = updatedSchemes)
            writePortfolioToDiskLocked(updatedPortfolio)
            updatedPortfolio
        }
    }

    /**
     * Removes/deletes a scheme from `portfolio_active.json`.
     */
    suspend fun deleteScheme(schemeCode: Int): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val current = readActiveOrSeedLocked()
            val updatedSchemes = current.schemes.filterNot { it.schemeCode == schemeCode }
            val updatedPortfolio = current.copy(schemes = updatedSchemes)
            writePortfolioToDiskLocked(updatedPortfolio)
            updatedPortfolio
        }
    }

    /**
     * Backward-compatible alias for [deleteScheme].
     */
    suspend fun removeSchemeFromPortfolio(schemeCode: Int): PortfolioData = deleteScheme(schemeCode)

    /**
     * Serializes current portfolio positions to a standard CSV string.
     */
    fun exportPortfolioCsv(portfolio: PortfolioData): String {
        return PortfolioCsvParser.exportPortfolioCsv(portfolio)
    }

    /**
     * Imports portfolio positions from CSV content.
     */
    suspend fun importPortfolioCsv(csvContent: String): PortfolioData = withContext(Dispatchers.IO) {
        val parsedRows = PortfolioCsvParser.parseCsvRows(csvContent)

        val current = fileMutex.withLock { readActiveOrSeedLocked() }
        val existingMap = current.schemes.associateBy { it.schemeCode }

        val importedSchemes = supervisorScope {
            parsedRows.map { row ->
                async(Dispatchers.IO) {
                    val existing = existingMap[row.schemeCode]
                    if (existing != null) {
                        existing.copy(
                            units = row.units,
                            investedAmount = row.investedAmount
                        )
                    } else {
                        val navResponse = runCatching { apiService.getSchemeNavDetails(row.schemeCode) }.getOrNull()?.body()
                        val meta = navResponse?.meta
                        val topEntry = navResponse?.data?.firstOrNull()
                        val nav = topEntry?.nav?.trim()?.toDoubleOrNull() ?: 10.0
                        val date = topEntry?.date ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

                        val schemeName = if (row.schemeName.isNotBlank()) row.schemeName else (meta?.schemeName ?: "Scheme #${row.schemeCode}")
                        val (inferredCategory, inferredAssetClass) = HoldingsEngine.inferCategoryAndAssetClass(schemeName)
                        val category = meta?.schemeCategory ?: inferredCategory
                        val assetClass = inferredAssetClass

                        MutualFundScheme(
                            schemeCode = row.schemeCode,
                            schemeName = schemeName,
                            amc = meta?.fundHouse ?: HoldingsEngine.inferAmcName(schemeName),
                            category = category,
                            assetClass = assetClass,
                            riskLevel = HoldingsEngine.inferRiskLevel(category, assetClass),
                            expenseRatio = HoldingsEngine.inferExpenseRatio(category, isDirect = schemeName.contains("direct", ignoreCase = true)),
                            exitLoad = "1.0%",
                            benchmark = HoldingsEngine.inferBenchmark(category),
                            units = row.units,
                            investedAmount = row.investedAmount,
                            fallbackNav = nav,
                            lastNavDate = date,
                            holdings = HoldingsEngine.generateHoldings(row.schemeCode, schemeName, category, assetClass)
                        )
                    }
                }
            }.awaitAll()
        }

        val updatedPortfolio = fileMutex.withLock {
            val fresh = current.copy(
                schemes = importedSchemes,
                lastSyncedEpochMs = System.currentTimeMillis()
            )
            writePortfolioToDiskLocked(fresh)
            fresh
        }
        updatedPortfolio
    }

    /**
     * Fetches metadata, full NAV series, and computes 1Y, 3Y, 5Y CAGR for up to 5 mutual funds
     * for side-by-side factor comparison.
     */
    suspend fun fetchFundComparison(schemeCodes: List<Int>): Result<List<FundComparisonItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val current = loadPortfolio()
                val existingMap = current.schemes.associateBy { it.schemeCode }

                supervisorScope {
                    schemeCodes.take(5).map { code ->
                        async(Dispatchers.IO) {
                            val historyResult = fetchSchemeHistoricalNavs(code)
                            val history = historyResult.getOrDefault(emptyList())
                            val existingScheme = existingMap[code]

                            val meta = if (existingScheme == null) {
                                runCatching { apiService.getSchemeNavDetails(code).body()?.meta }.getOrNull()
                            } else null

                            val schemeName = existingScheme?.schemeName ?: meta?.schemeName ?: "Scheme #$code"
                            val (inferredCategory, inferredAssetClass) = HoldingsEngine.inferCategoryAndAssetClass(schemeName)
                            val category = existingScheme?.category ?: meta?.schemeCategory ?: inferredCategory
                            val benchmark = existingScheme?.benchmark ?: HoldingsEngine.inferBenchmark(category)
                            val riskLevel = existingScheme?.riskLevel ?: HoldingsEngine.inferRiskLevel(category, inferredAssetClass)
                            val expenseRatio = existingScheme?.expenseRatio ?: HoldingsEngine.inferExpenseRatio(category, isDirect = schemeName.contains("direct", ignoreCase = true))

                            val topHoldings = if (existingScheme != null && existingScheme.holdings.isNotEmpty()) {
                                existingScheme.holdings
                            } else {
                                HoldingsEngine.generateHoldings(code, schemeName, category, "Equity")
                            }

                            val latestEntry = history.firstOrNull()
                            val latestNav = latestEntry?.nav?.trim()?.toDoubleOrNull() ?: (existingScheme?.fallbackNav ?: 0.0)
                            val lastNavDate = latestEntry?.date ?: (existingScheme?.lastNavDate ?: "N/A")

                            val cagr1Y = PortfolioAnalyticsEngine.computeCagr(history, 1)
                            val cagr3Y = PortfolioAnalyticsEngine.computeCagr(history, 3)
                            val cagr5Y = PortfolioAnalyticsEngine.computeCagr(history, 5)

                            FundComparisonItem(
                                schemeCode = code,
                                schemeName = schemeName,
                                isPortfolioScheme = existingScheme != null,
                                latestNav = latestNav,
                                lastNavDate = lastNavDate,
                                cagr1Y = cagr1Y,
                                cagr3Y = cagr3Y,
                                cagr5Y = cagr5Y,
                                expenseRatio = expenseRatio,
                                riskLevel = riskLevel,
                                category = category,
                                benchmark = benchmark,
                                navHistory = history,
                                topHoldings = topHoldings
                            )
                        }
                    }.awaitAll()
                }
            }
        }

    /**
     * Parallel live NAV refresh across all schemes in the portfolio using `supervisorScope` + `async/awaitAll`.
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
                                errorMessage = "${scheme.schemeCode} (${scheme.schemeName.take(22)}...): ${error.localizedMessage ?: "Network error"}"
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
     * Fetches full historical NAV series for a single scheme (`https://api.mfapi.in/mf/{schemeCode}`).
     */
    suspend fun fetchSchemeHistoricalNavs(schemeCode: Int): Result<List<MfApiNavEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.getSchemeNavDetails(schemeCode)
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code()} when fetching history for $schemeCode")
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
     * Sector Exposure, Asset Class & Market-Cap Breakdown, and Weighted Expense Ratio Drag).
     * Delegated directly to [PortfolioAnalyticsEngine].
     */
    fun computeAnalytics(portfolio: PortfolioData): PortfolioAnalyticsSummary {
        return PortfolioAnalyticsEngine.computeAnalytics(portfolio)
    }

    private suspend fun fetchLatestNavForScheme(scheme: MutualFundScheme): Result<Pair<Double, String>> =
        runCatching {
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
            activeFile.writeText(jsonOutput, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    companion object {
        private const val SEED_PORTFOLIO_FILENAME = "portfolio_seed.json"
        private const val ACTIVE_PORTFOLIO_FILENAME = "portfolio_active.json"
    }
}