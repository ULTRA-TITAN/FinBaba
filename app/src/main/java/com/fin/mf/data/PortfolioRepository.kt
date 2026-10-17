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
 * 1. Initializes an authentic empty `Primary Vault` in internal storage on first run.
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

    private val manifestAdapter: JsonAdapter<PortfolioManifest> by lazy {
        NetworkClient.moshi.adapter(PortfolioManifest::class.java).indent("  ")
    }

    private val manifestFile: File
        get() = File(appContext.filesDir, MANIFEST_FILENAME)

    private fun portfolioFile(id: String): File {
        val sanitized = id.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return File(appContext.filesDir, "portfolio_$sanitized.json")
    }

    /**
     * Retrieves the current portfolios manifest tracking all saved portfolios.
     */
    suspend fun getManifest(): PortfolioManifest = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            readManifestLocked()
        }
    }

    /**
     * Loads the active portfolio according to `portfolios_manifest.json`.
     * If existing portfolio schemes have sparse holdings (< 15 constituents), it auto-hydrates
     * them with full look-through constituents from [HoldingsEngine] and persists to disk.
     */
    suspend fun loadPortfolio(): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            val activePortfolio = readActiveOrDefaultLocked()

            val needsEnrichment = activePortfolio.schemes.any { it.holdings.size < 35 }
            if (needsEnrichment) {
                val enrichedSchemes = activePortfolio.schemes.map { s ->
                    if (s.holdings.size < 35) {
                        val enrichedHoldings = HoldingsEngine.generateHoldings(s.schemeCode, s.schemeName, s.category, s.assetClass)
                        s.copy(holdings = enrichedHoldings)
                    } else s
                }
                val hydratedPortfolio = activePortfolio.copy(schemes = enrichedSchemes)
                writePortfolioToDiskLocked(hydratedPortfolio, manifest.activePortfolioId)
                return@withLock hydratedPortfolio
            }
            activePortfolio
        }
    }

    /**
     * Switches the active portfolio to [portfolioId] and returns its loaded [PortfolioData].
     */
    suspend fun switchPortfolio(portfolioId: String): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            val targetMeta = manifest.portfolios.firstOrNull { it.id == portfolioId }
                ?: throw IllegalArgumentException("Portfolio with id '$portfolioId' does not exist")

            val updatedManifest = manifest.copy(activePortfolioId = targetMeta.id)
            writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))

            readActiveOrDefaultLocked()
        }
    }

    /**
     * Creates a new portfolio and makes it active.
     */
    suspend fun createNewPortfolio(name: String, seedWithDefault: Boolean = false): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            val newId = "p_${System.currentTimeMillis()}"
            val newPortfolioName = name.ifBlank { "New Vault" }

            val initialPortfolio = PortfolioData(
                portfolioName = newPortfolioName,
                ownerName = "Investor",
                lastSyncedEpochMs = 0L,
                schemes = emptyList(),
                marketCapMandate = MarketCapMandate()
            )

            // Save new portfolio file atomically
            writePortfolioToDiskLocked(initialPortfolio, newId)

            // Register in manifest as active
            val newMeta = PortfolioMeta(
                id = newId,
                name = newPortfolioName,
                createdAtEpochMs = System.currentTimeMillis(),
                schemeCount = 0,
                totalValue = 0.0
            )
            val updatedManifest = manifest.copy(
                activePortfolioId = newId,
                portfolios = manifest.portfolios + newMeta
            )
            writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))

            initialPortfolio
        }
    }

    /**
     * Renames an existing portfolio in the manifest and on disk.
     */
    suspend fun renamePortfolio(portfolioId: String, newName: String): PortfolioManifest = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            val cleanName = newName.ifBlank { "Portfolio" }

            val file = portfolioFile(portfolioId)
            if (file.exists()) {
                runCatching {
                    val p = readPortfolioFromFileLocked(file, portfolioId)
                    writePortfolioToDiskLocked(p.copy(portfolioName = cleanName), portfolioId)
                }
            }

            val updatedPortfolios = manifest.portfolios.map { meta ->
                if (meta.id == portfolioId) meta.copy(name = cleanName) else meta
            }
            val updatedManifest = manifest.copy(portfolios = updatedPortfolios)
            writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))
            updatedManifest
        }
    }

    /**
     * Deletes a portfolio from disk and manifest. Fails if it is the only remaining portfolio.
     */
    suspend fun deletePortfolio(portfolioId: String): PortfolioManifest = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            if (manifest.portfolios.size <= 1) {
                throw IllegalStateException("Cannot delete the only remaining portfolio.")
            }

            // Delete portfolio file and any backup
            val file = portfolioFile(portfolioId)
            if (file.exists()) {
                file.delete()
            }
            val bak = File(file.parentFile, "${file.name}.bak")
            if (bak.exists()) {
                bak.delete()
            }

            val remainingPortfolios = manifest.portfolios.filter { it.id != portfolioId }
            val nextActiveId = if (manifest.activePortfolioId == portfolioId) {
                remainingPortfolios.first().id
            } else {
                manifest.activePortfolioId
            }

            val updatedManifest = manifest.copy(
                activePortfolioId = nextActiveId,
                portfolios = remainingPortfolios
            )
            writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))
            updatedManifest
        }
    }

    /**
     * Duplicates an existing portfolio and marks the copy as active.
     */
    suspend fun duplicatePortfolio(portfolioId: String, newName: String): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val manifest = readManifestLocked()
            val srcFile = portfolioFile(portfolioId)
            val sourcePortfolio = if (srcFile.exists()) {
                readPortfolioFromFileLocked(srcFile, portfolioId)
            } else {
                readActiveOrDefaultLocked()
            }

            val newId = "p_${System.currentTimeMillis()}"
            val duplicateName = newName.ifBlank { "${sourcePortfolio.portfolioName} (Copy)" }
            val duplicatedData = sourcePortfolio.copy(
                portfolioName = duplicateName,
                lastSyncedEpochMs = System.currentTimeMillis()
            )

            writePortfolioToDiskLocked(duplicatedData, newId)

            val newMeta = PortfolioMeta(
                id = newId,
                name = duplicateName,
                createdAtEpochMs = System.currentTimeMillis(),
                schemeCount = duplicatedData.schemes.size,
                totalValue = duplicatedData.schemes.sumOf { it.currentValue }
            )
            val updatedManifest = manifest.copy(
                activePortfolioId = newId,
                portfolios = manifest.portfolios + newMeta
            )
            writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))

            duplicatedData
        }
    }

    /**
     * Clears all mutual fund schemes from the active portfolio, resetting to clean empty state.
     */
    suspend fun clearActivePortfolio(): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val current = readActiveOrDefaultLocked()
            val cleared = current.copy(
                schemes = emptyList(),
                lastSyncedEpochMs = 0L
            )
            writePortfolioToDiskLocked(cleared)
            cleared
        }
    }

    /**
     * Updates the SEBI Market Cap target mandate configuration.
     */
    suspend fun updateMarketCapMandate(mandate: MarketCapMandate): PortfolioData = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val current = readActiveOrDefaultLocked()
            val updated = current.copy(marketCapMandate = mandate)
            writePortfolioToDiskLocked(updated)
            updated
        }
    }

    /**
     * Creates a new empty or custom portfolio and overwrites the active portfolio.
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
            val current = readActiveOrDefaultLocked()
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
                val current = readActiveOrDefaultLocked()
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
            val current = readActiveOrDefaultLocked()
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
            val secondEntry = navBody?.data?.getOrNull(1)

            val parsedNav = topEntry?.nav?.trim()?.toDoubleOrNull() ?: 10.0
            val prevNav = secondEntry?.nav?.trim()?.toDoubleOrNull()
            val dayDiff = if (prevNav != null && prevNav > 0.0) parsedNav - prevNav else 0.0
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
                holdings = authenticHoldings,
                dayChange = dayDiff
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
            val current = readActiveOrDefaultLocked()
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

        val current = fileMutex.withLock { readActiveOrDefaultLocked() }
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
                        onSuccess = { (latestNav, latestDate, dayDiff) ->
                            val resolvedDayChange = if (dayDiff != null) {
                                dayDiff
                            } else if (scheme.fallbackNav > 0.0 && latestNav != scheme.fallbackNav) {
                                latestNav - scheme.fallbackNav
                            } else {
                                scheme.dayChange
                            }
                            SingleSchemeNavResult(
                                scheme = scheme.copy(
                                    fallbackNav = latestNav,
                                    lastNavDate = latestDate,
                                    dayChange = resolvedDayChange
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

    private suspend fun fetchLatestNavForScheme(scheme: MutualFundScheme): Result<Triple<Double, String, Double?>> =
        runCatching {
            val detailsResponse = apiService.getSchemeNavDetails(scheme.schemeCode)
            val responseBody = if (detailsResponse.isSuccessful && !detailsResponse.body()?.data.isNullOrEmpty()) {
                detailsResponse.body()
            } else {
                val fallbackResponse = apiService.getSchemeLatestNav(scheme.schemeCode)
                if (!fallbackResponse.isSuccessful) {
                    throw IOException("HTTP ${fallbackResponse.code()}")
                }
                fallbackResponse.body()
            }

            val data = responseBody?.data.orEmpty()
            val topEntry = data.firstOrNull()
                ?: throw IOException("Empty NAV data array")

            val parsedNav = topEntry.nav.trim().toDoubleOrNull()
                ?: throw IOException("Invalid NAV format: '${topEntry.nav}'")

            if (parsedNav <= 0.0) {
                throw IOException("Non-positive NAV: $parsedNav")
            }

            val secondEntry = data.getOrNull(1)
            val prevNav = secondEntry?.nav?.trim()?.toDoubleOrNull()
            val dayDiff = if (prevNav != null && prevNav > 0.0) parsedNav - prevNav else null

            Triple(parsedNav, topEntry.date, dayDiff)
        }

    private fun readManifestLocked(): PortfolioManifest {
        if (manifestFile.exists() && manifestFile.length() > 0L) {
            runCatching {
                val json = manifestFile.readText(Charsets.UTF_8)
                manifestAdapter.fromJson(json)
            }.getOrNull()?.let { return it }
        }

        // Initialize / Migrate manifest
        val defaultFile = portfolioFile(DEFAULT_PORTFOLIO_ID)
        val legacyFile = File(appContext.filesDir, LEGACY_ACTIVE_PORTFOLIO_FILENAME)

        val initialData: PortfolioData = if (defaultFile.exists() && defaultFile.length() > 0L) {
            runCatching {
                readPortfolioFromFileLocked(defaultFile, DEFAULT_PORTFOLIO_ID)
            }.getOrNull() ?: createDefaultEmptyPortfolio()
        } else if (legacyFile.exists() && legacyFile.length() > 0L) {
            val legacyData = runCatching {
                val json = legacyFile.readText(Charsets.UTF_8)
                portfolioAdapter.fromJson(json)
            }.getOrNull() ?: createDefaultEmptyPortfolio()
            writeTextAtomically(defaultFile, portfolioAdapter.toJson(legacyData))
            runCatching { legacyFile.delete() }
            legacyData
        } else {
            val emptyPortfolio = createDefaultEmptyPortfolio()
            writeTextAtomically(defaultFile, portfolioAdapter.toJson(emptyPortfolio))
            emptyPortfolio
        }

        val initialManifest = PortfolioManifest(
            activePortfolioId = DEFAULT_PORTFOLIO_ID,
            portfolios = listOf(
                PortfolioMeta(
                    id = DEFAULT_PORTFOLIO_ID,
                    name = initialData.portfolioName.ifBlank { "Primary Vault" },
                    createdAtEpochMs = System.currentTimeMillis(),
                    schemeCount = initialData.schemes.size,
                    totalValue = initialData.schemes.sumOf { it.currentValue }
                )
            )
        )
        writeTextAtomically(manifestFile, manifestAdapter.toJson(initialManifest))
        return initialManifest
    }

    private fun createDefaultEmptyPortfolio(): PortfolioData {
        return PortfolioData(
            portfolioName = "Primary Vault",
            ownerName = "Investor",
            lastSyncedEpochMs = 0L,
            schemes = emptyList(),
            marketCapMandate = MarketCapMandate()
        )
    }

    private fun readActiveOrDefaultLocked(): PortfolioData {
        val manifest = readManifestLocked()
        val targetFile = portfolioFile(manifest.activePortfolioId)
        if (targetFile.exists() && targetFile.length() > 0L) {
            try {
                return readPortfolioFromFileLocked(targetFile, manifest.activePortfolioId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to default portfolio
        val defaultFile = portfolioFile(DEFAULT_PORTFOLIO_ID)
        if (defaultFile.exists() && defaultFile.length() > 0L) {
            try {
                return readPortfolioFromFileLocked(defaultFile, DEFAULT_PORTFOLIO_ID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Clean empty default fallback
        val defaultPortfolio = createDefaultEmptyPortfolio()
        writePortfolioToDiskLocked(defaultPortfolio, DEFAULT_PORTFOLIO_ID)
        return defaultPortfolio
    }

    private fun readPortfolioFromFileLocked(file: File, portfolioId: String): PortfolioData {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("File $file not found or empty")
        }
        try {
            val json = file.readText(Charsets.UTF_8)
            val parsed = portfolioAdapter.fromJson(json)
            if (parsed != null) return parsed
            throw IOException("Null parsed from JSON in $file")
        } catch (e: Exception) {
            e.printStackTrace()
            val bak = File(file.parentFile, "${file.name}.bak")
            runCatching { file.renameTo(bak) }
            throw e
        }
    }

    private fun writePortfolioToDiskLocked(portfolio: PortfolioData, portfolioId: String? = null) {
        val manifest = readManifestLocked()
        val targetId = portfolioId ?: manifest.activePortfolioId
        val targetFile = portfolioFile(targetId)

        // Write portfolio atomically
        writeTextAtomically(targetFile, portfolioAdapter.toJson(portfolio))

        // Update manifest
        val updatedPortfolios = manifest.portfolios.map { meta ->
            if (meta.id == targetId) {
                meta.copy(
                    name = portfolio.portfolioName.ifBlank { meta.name },
                    schemeCount = portfolio.schemes.size,
                    totalValue = portfolio.schemes.sumOf { it.currentValue }
                )
            } else meta
        }
        val updatedManifest = manifest.copy(portfolios = updatedPortfolios)
        writeTextAtomically(manifestFile, manifestAdapter.toJson(updatedManifest))
    }

    private fun writeTextAtomically(targetFile: File, content: String) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        tempFile.writeText(content, Charsets.UTF_8)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val renamed = tempFile.renameTo(targetFile)
        if (!renamed) {
            targetFile.writeText(content, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    fun isFloatingFabEnabled(): Boolean {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLE_FLOATING_FAB, false)
    }

    fun setFloatingFabEnabled(enabled: Boolean) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLE_FLOATING_FAB, enabled).apply()
    }

    fun isMarketStatusTickerEnabled(): Boolean {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLE_MARKET_STATUS_TICKER, true)
    }

    fun setMarketStatusTickerEnabled(enabled: Boolean) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLE_MARKET_STATUS_TICKER, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "finbaba_prefs"
        const val KEY_ENABLE_FLOATING_FAB = "KEY_ENABLE_FLOATING_FAB"
        const val KEY_ENABLE_MARKET_STATUS_TICKER = "KEY_ENABLE_MARKET_STATUS_TICKER"
        private const val LEGACY_ACTIVE_PORTFOLIO_FILENAME = "portfolio_active.json"
        private const val MANIFEST_FILENAME = "portfolios_manifest.json"
        const val DEFAULT_PORTFOLIO_ID = "default"
    }
}