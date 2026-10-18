package com.fin.mf.ui

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.core.export.MediaStoreExportManager
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.FundComparisonItem
import com.fin.mf.data.MarketCapMandate
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MfSearchDto
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.NavSyncReport
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.PortfolioRepository
import com.fin.mf.data.engine.AmfiFuzzyReconciler
import com.fin.mf.data.engine.BrokerFormatNormalizer
import com.fin.mf.data.engine.ReconciledSchemeItem
import com.fin.mf.data.engine.UniversalSpreadsheetReader
import com.fin.mf.ui.workspace.dialogs.VaultImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Legacy timeframe selector kept for backward-compatibility.
 */
enum class NavChartRange(val label: String, val approximateTradingDays: Int) {
    ONE_MONTH("1M", 22),
    SIX_MONTHS("6M", 125),
    ONE_YEAR("1Y", 250),
    THREE_YEARS("3Y", 750),
    FIVE_YEARS("5Y", 1250)
}

/**
 * Immutable UI state representing the entire FinBaba application state.
 */
data class PortfolioUiState(
    val isLoading: Boolean = true,
    val isSyncingNavs: Boolean = false,
    val isPrivacyMasked: Boolean = false,
    val portfolioData: PortfolioData? = null,
    val analytics: PortfolioAnalyticsSummary? = null,
    val searchQuery: String = "",
    val selectedAssetClassFilter: String = "All",
    val lastSyncReport: NavSyncReport? = null,
    val statusBannerMessage: String? = null,
    val selectedSchemeCode: Int? = null,
    val isHistoryLoading: Boolean = false,
    val selectedSchemeHistory: List<MfApiNavEntry> = emptyList(),
    val selectedChartRange: NavChartRange = NavChartRange.ONE_YEAR,
    val selectedTimeframe: ChartTimeframe = ChartTimeframe.ONE_YEAR,
    val historyErrorMessage: String? = null,
    val amfiSearchResults: List<MfSearchDto> = emptyList(),
    val isAmfiSearching: Boolean = false,
    val amfiSearchError: String? = null,
    val comparisonSchemeCodes: List<Int> = emptyList(),
    val isComparisonLoading: Boolean = false,
    val comparisonFunds: List<FundComparisonItem> = emptyList(),
    val comparisonErrorMessage: String? = null,
    val isPortfolioAvgOverlayEnabled: Boolean = false,
    val comparisonTimeframe: ChartTimeframe = ChartTimeframe.ONE_YEAR,
    val customComparisonStartEpochMs: Long? = null,
    val customComparisonEndEpochMs: Long? = null,
    val inspectedExternalScheme: MutualFundScheme? = null,
    val portfolioManifest: com.fin.mf.data.PortfolioManifest? = null,
    val isFloatingFabEnabled: Boolean = false,
    val isMarketStatusTickerEnabled: Boolean = true,
    val reconciliationProgress: com.fin.mf.data.ReconciliationProgress? = null,
    val reconciliationItems: List<ReconciledSchemeItem>? = null,
    val cachedReconciliationDraft: List<ReconciledSchemeItem>? = null,
    val isReconciliationSheetVisible: Boolean = false,
    val amfiDictionary: List<Pair<Int, String>> = emptyList(),
    val isReconciling: Boolean = false
) {
    /**
     * Dynamically filtered and sorted schemes based on the user's search query and asset class filter.
     */
    val filteredSchemes: List<MutualFundScheme>
        get() {
            val allSchemes = portfolioData?.schemes.orEmpty()
            return allSchemes.filter { scheme ->
                val matchesCategory = selectedAssetClassFilter == "All" ||
                        scheme.assetClass.equals(selectedAssetClassFilter, ignoreCase = true) ||
                        scheme.category.contains(selectedAssetClassFilter, ignoreCase = true)

                val q = searchQuery.trim()
                val matchesSearch = q.isEmpty() ||
                        scheme.schemeName.contains(q, ignoreCase = true) ||
                        scheme.amc.contains(q, ignoreCase = true) ||
                        scheme.category.contains(q, ignoreCase = true) ||
                        scheme.schemeCode.toString().contains(q) ||
                        scheme.holdings.any {
                            it.ticker.contains(q, ignoreCase = true) ||
                                    it.companyName.contains(q, ignoreCase = true)
                        }

                matchesCategory && matchesSearch
            }.sortedByDescending { it.currentValue }
        }

    /**
     * Returns the currently selected scheme for the deep-dive/MF-Hunter detail modal/screen.
     * Checks user's portfolio first, then falls back to any external AMFI searched scheme.
     */
    val selectedScheme: MutualFundScheme?
        get() = portfolioData?.schemes?.find { it.schemeCode == selectedSchemeCode } ?: inspectedExternalScheme
}

/**
 * Primary ViewModel coordinating local portfolio storage, live parallel NAV syncs,
 * look-through overlap analytics, AMFI scheme search, custom CRUD operations,
 * and multi-fund normalized factor comparison.
 */
class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PortfolioRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private var amfiSearchJob: Job? = null
    private var comparisonJob: Job? = null
    private var bannerDismissJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                isFloatingFabEnabled = repository.isFloatingFabEnabled(),
                isMarketStatusTickerEnabled = repository.isMarketStatusTickerEnabled()
            )
        }
        loadInitialPortfolio()
    }

    /**
     * Posts a status banner message with a 5-second auto-dismiss timer.
     */
    fun postStatusBanner(message: String) {
        bannerDismissJob?.cancel()
        _uiState.update { it.copy(statusBannerMessage = message) }
        bannerDismissJob = viewModelScope.launch {
            delay(5000L)
            _uiState.update { current ->
                if (current.statusBannerMessage == message) {
                    current.copy(statusBannerMessage = null)
                } else {
                    current
                }
            }
        }
    }

    /**
     * Loads the active portfolio and manifest, computes look-through analytics immediately,
     * and triggers a background live NAV sync.
     */
    fun loadInitialPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val portfolio = repository.loadPortfolio()
                val manifest = repository.getManifest()
                val analytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(portfolio)
                }
                Triple(portfolio, manifest, analytics)
            }.onSuccess { (portfolio, manifest, analytics) ->
                val firstSchemeCode = portfolio.schemes.firstOrNull()?.schemeCode
                val initialComparison = portfolio.schemes.take(2).map { it.schemeCode }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        portfolioManifest = manifest,
                        analytics = analytics,
                        selectedSchemeCode = it.selectedSchemeCode ?: firstSchemeCode,
                        comparisonSchemeCodes = if (it.comparisonSchemeCodes.isEmpty()) initialComparison else it.comparisonSchemeCodes
                    )
                }
                syncAllLiveNavs()
                if (initialComparison.isNotEmpty()) {
                    loadComparisonData(initialComparison)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Failed to load portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Switches the active portfolio to [portfolioId], recalculates all analytics on Dispatchers.Default,
     * and updates the UI state.
     */
    fun switchPortfolio(portfolioId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val newPortfolio = repository.switchPortfolio(portfolioId)
                val newManifest = repository.getManifest()
                val newAnalytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(newPortfolio)
                }
                Triple(newPortfolio, newManifest, newAnalytics)
            }.onSuccess { (portfolio, manifest, analytics) ->
                val firstSchemeCode = portfolio.schemes.firstOrNull()?.schemeCode
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        portfolioManifest = manifest,
                        analytics = analytics,
                        selectedSchemeCode = firstSchemeCode
                    )
                }
                postStatusBanner("Switched to '${portfolio.portfolioName}'")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Error switching portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Creates a new portfolio (empty) and sets it active.
     */
    fun createNewPortfolio(name: String, seedWithDefault: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val newPortfolio = repository.createNewPortfolio(name, seedWithDefault)
                val newManifest = repository.getManifest()
                val newAnalytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(newPortfolio)
                }
                Triple(newPortfolio, newManifest, newAnalytics)
            }.onSuccess { (portfolio, manifest, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        portfolioManifest = manifest,
                        analytics = analytics,
                        selectedSchemeCode = portfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Created portfolio '${portfolio.portfolioName}'")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Failed to create portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Renames an existing portfolio in manifest and storage.
     */
    fun renamePortfolio(portfolioId: String, newName: String) {
        viewModelScope.launch {
            runCatching {
                val updatedManifest = repository.renamePortfolio(portfolioId, newName)
                val activeData = if (_uiState.value.portfolioManifest?.activePortfolioId == portfolioId) {
                    _uiState.value.portfolioData?.copy(portfolioName = newName.ifBlank { "Portfolio" })
                } else {
                    _uiState.value.portfolioData
                }
                Pair(updatedManifest, activeData)
            }.onSuccess { (manifest, activeData) ->
                _uiState.update {
                    it.copy(
                        portfolioManifest = manifest,
                        portfolioData = activeData
                    )
                }
                postStatusBanner("Renamed portfolio to '$newName'")
            }.onFailure { error ->
                postStatusBanner("Failed to rename portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Deletes a portfolio from storage and manifest.
     */
    fun deletePortfolio(portfolioId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val updatedManifest = repository.deletePortfolio(portfolioId)
                val activePortfolio = repository.loadPortfolio()
                val analytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(activePortfolio)
                }
                Triple(updatedManifest, activePortfolio, analytics)
            }.onSuccess { (manifest, activePortfolio, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioManifest = manifest,
                        portfolioData = activePortfolio,
                        analytics = analytics,
                        selectedSchemeCode = activePortfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Portfolio deleted")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Cannot delete portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Duplicates an existing portfolio.
     */
    fun duplicatePortfolio(portfolioId: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val duplicatedPortfolio = repository.duplicatePortfolio(portfolioId, newName)
                val manifest = repository.getManifest()
                val analytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(duplicatedPortfolio)
                }
                Triple(duplicatedPortfolio, manifest, analytics)
            }.onSuccess { (portfolio, manifest, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        portfolioManifest = manifest,
                        analytics = analytics,
                        selectedSchemeCode = portfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Duplicated portfolio '${portfolio.portfolioName}'")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Failed to duplicate portfolio: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Triggers parallel `async/awaitAll` network calls to `https://api.mfapi.in/mf/{code}/latest`
     * for all schemes and updates `portfolio_active.json`.
     */
    fun syncAllLiveNavs() {
        if (_uiState.value.isSyncingNavs) return

        viewModelScope.launch {
            val totalSchemes = _uiState.value.portfolioData?.schemes?.size ?: 0
            _uiState.update { it.copy(isSyncingNavs = true) }
            postStatusBanner("Syncing live AMFI NAVs across $totalSchemes schemes...")

            runCatching {
                repository.syncLiveNavs()
            }.onSuccess { (updatedPortfolio, report) ->
                val freshAnalytics = repository.computeAnalytics(updatedPortfolio)
                val summaryMsg = if (report.failedCount == 0) {
                    "Live NAV sync complete: ${report.updatedCount}/${report.totalSchemes} schemes updated."
                } else {
                    "Synced ${report.updatedCount}/${report.totalSchemes} schemes (${report.failedCount} offline/cached)."
                }

                _uiState.update {
                    it.copy(
                        isSyncingNavs = false,
                        portfolioData = updatedPortfolio,
                        analytics = freshAnalytics,
                        lastSyncReport = report
                    )
                }
                postStatusBanner(summaryMsg)
            }.onFailure { error ->
                _uiState.update { it.copy(isSyncingNavs = false) }
                postStatusBanner("Live NAV sync error: ${error.localizedMessage ?: "Check internet connection"}")
            }
        }
    }

    /**
     * Updates a scheme's units and invested amount, persists to `portfolio_active.json`,
     * and recomputes all look-through portfolio analytics.
     */
    fun updateSchemePosition(schemeCode: Int, newUnits: Double, newInvestedAmount: Double) {
        viewModelScope.launch {
            runCatching {
                val updatedPortfolio = repository.updateSchemeHolding(
                    schemeCode = schemeCode,
                    newUnits = newUnits,
                    newInvestedAmount = newInvestedAmount
                )
                val updatedAnalytics = repository.computeAnalytics(updatedPortfolio)
                Pair(updatedPortfolio, updatedAnalytics)
            }.onSuccess { (updatedPortfolio, updatedAnalytics) ->
                _uiState.update {
                    it.copy(
                        portfolioData = updatedPortfolio,
                        analytics = updatedAnalytics
                    )
                }
                postStatusBanner("Saved position update to portfolio_active.json")
            }.onFailure { error ->
                postStatusBanner("Failed to update holding: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Searches AMFI schemes via `https://api.mfapi.in/mf/search?q={query}`.
     */
    fun searchAmfiSchemes(query: String) {
        amfiSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    amfiSearchResults = emptyList(),
                    isAmfiSearching = false,
                    amfiSearchError = null
                )
            }
            return
        }

        _uiState.update { it.copy(isAmfiSearching = true, amfiSearchError = null) }
        amfiSearchJob = viewModelScope.launch {
            repository.searchSchemes(trimmed)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            amfiSearchResults = results,
                            isAmfiSearching = false,
                            amfiSearchError = if (results.isEmpty()) "No mutual fund schemes found for '$trimmed'" else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            amfiSearchResults = emptyList(),
                            isAmfiSearching = false,
                            amfiSearchError = error.localizedMessage ?: "Network error searching AMFI"
                        )
                    }
                }
        }
    }

    fun clearAmfiSearch() {
        amfiSearchJob?.cancel()
        _uiState.update {
            it.copy(
                amfiSearchResults = emptyList(),
                isAmfiSearching = false,
                amfiSearchError = null
            )
        }
    }

    /**
     * Adds any searched AMFI scheme into the user's active portfolio (`portfolio_active.json`).
     */
    fun addSearchedScheme(schemeCode: Int, units: Double, investedAmount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val updatedPortfolio = repository.addSchemeToPortfolio(schemeCode, units, investedAmount)
                val updatedAnalytics = repository.computeAnalytics(updatedPortfolio)
                Pair(updatedPortfolio, updatedAnalytics)
            }.onSuccess { (updatedPortfolio, updatedAnalytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = updatedPortfolio,
                        analytics = updatedAnalytics,
                        selectedSchemeCode = schemeCode,
                        inspectedExternalScheme = null
                    )
                }
                postStatusBanner("Added scheme #$schemeCode to active portfolio.")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Failed to add scheme: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Deletes a scheme from `portfolio_active.json`.
     */
    fun deleteScheme(schemeCode: Int) {
        viewModelScope.launch {
            runCatching {
                val updatedPortfolio = repository.deleteScheme(schemeCode)
                val updatedAnalytics = repository.computeAnalytics(updatedPortfolio)
                Pair(updatedPortfolio, updatedAnalytics)
            }.onSuccess { (updatedPortfolio, updatedAnalytics) ->
                val nextSelectedCode = if (_uiState.value.selectedSchemeCode == schemeCode) {
                    updatedPortfolio.schemes.firstOrNull()?.schemeCode
                } else {
                    _uiState.value.selectedSchemeCode
                }
                _uiState.update {
                    it.copy(
                        portfolioData = updatedPortfolio,
                        analytics = updatedAnalytics,
                        selectedSchemeCode = nextSelectedCode
                    )
                }
                postStatusBanner("Removed scheme #$schemeCode from portfolio.")
            }.onFailure { error ->
                postStatusBanner("Failed to delete scheme: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Creates a new empty/custom portfolio.
     */
    fun createNewEmptyPortfolio(name: String, owner: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val emptyPortfolio = repository.createCustomPortfolio(name, owner)
                val analytics = repository.computeAnalytics(emptyPortfolio)
                Pair(emptyPortfolio, analytics)
            }.onSuccess { (emptyPortfolio, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = emptyPortfolio,
                        analytics = analytics,
                        selectedSchemeCode = null
                    )
                }
                postStatusBanner("Created new portfolio '${emptyPortfolio.portfolioName}'.")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Failed to create portfolio: ${error.localizedMessage}")
            }
        }
    }

    private val exportManager = MediaStoreExportManager()

    /**
     * Returns serialized CSV content for user export (clipboard/preview).
     */
    fun exportPortfolioCsv(): String {
        val currentPortfolio = _uiState.value.portfolioData ?: return "SchemeCode,SchemeName,Units,InvestedAmount\n"
        return repository.exportPortfolioCsv(currentPortfolio)
    }

    /**
     * Exports the 4-tab institutional OpenXML .xlsx audit workbook into user's Downloads/FinBaba directory.
     */
    fun exportPortfolioExcel(context: Context): Result<Uri> {
        val portfolio = _uiState.value.portfolioData ?: return Result.failure(IllegalStateException("Portfolio empty"))
        val analytics = _uiState.value.analytics ?: repository.computeAnalytics(portfolio)
        return exportManager.exportExcelFile(context, portfolio, analytics).onSuccess {
            postStatusBanner("Exported Excel workbook to Downloads/FinBaba")
        }.onFailure { error ->
            postStatusBanner("Excel export failed: ${error.localizedMessage}")
        }
    }

    /**
     * Exports the standard portfolio CSV file via MediaStore into user's Downloads/FinBaba directory.
     */
    fun exportPortfolioCsvFile(context: Context): Result<Uri> {
        val portfolio = _uiState.value.portfolioData ?: return Result.failure(IllegalStateException("Portfolio empty"))
        return exportManager.exportCsvFile(context, portfolio).onSuccess {
            postStatusBanner("Exported CSV file to Downloads/FinBaba")
        }.onFailure { error ->
            postStatusBanner("CSV export failed: ${error.localizedMessage}")
        }
    }

    /**
     * Launches a system chooser to share or open the exported file.
     */
    fun shareExportedFile(context: Context, uri: Uri, mimeType: String) {
        exportManager.dispatchShareOrOpenChooser(context, uri, mimeType)
    }

    /**
     * Opens the exported file with an external app (Excel, Sheets, etc.).
     */
    fun openExportedFile(context: Context, uri: Uri, mimeType: String) {
        exportManager.dispatchViewChooser(context, uri, mimeType)
    }

    /**
     * Imports portfolio positions from CSV content.
     */
    fun importPortfolioFromCsv(csvText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val importedPortfolio = repository.importPortfolioCsv(csvText)
                val analytics = repository.computeAnalytics(importedPortfolio)
                Pair(importedPortfolio, analytics)
            }.onSuccess { (importedPortfolio, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = importedPortfolio,
                        analytics = analytics,
                        selectedSchemeCode = importedPortfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Successfully imported ${importedPortfolio.schemes.size} schemes from CSV.")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Import failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Streams and parses an Excel (.xlsx), CSV, or TSV file from a content [Uri],
     * extracts broker holdings, and performs AMFI token-set fuzzy reconciliation.
     * Safely reads bytes inside withContext(Dispatchers.IO) to prevent premature stream closure.
     */
    fun processSpreadsheetImport(uri: Uri, contentResolver: ContentResolver, filename: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReconciling = true,
                    reconciliationProgress = com.fin.mf.data.ReconciliationProgress(
                        stageText = "Reading spreadsheet bytes...",
                        progressFraction = 0.20f
                    )
                )
            }
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Unable to open file stream for $filename")
                }
                executeReconciliationPipeline(bytes, filename)
            }.onSuccess { (reconciled, dict) ->
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconciliationProgress = null,
                        reconciliationItems = reconciled,
                        cachedReconciliationDraft = reconciled,
                        isReconciliationSheetVisible = true,
                        amfiDictionary = dict
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconciliationProgress = null
                    )
                }
                postStatusBanner("Spreadsheet parsing failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Overload accepting an in-memory [ByteArray] (for testing or cached buffers).
     */
    fun processSpreadsheetImport(bytes: ByteArray, filename: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReconciling = true,
                    reconciliationProgress = com.fin.mf.data.ReconciliationProgress(
                        stageText = "Reading spreadsheet bytes...",
                        progressFraction = 0.20f
                    )
                )
            }
            runCatching {
                executeReconciliationPipeline(bytes, filename)
            }.onSuccess { (reconciled, dict) ->
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconciliationProgress = null,
                        reconciliationItems = reconciled,
                        cachedReconciliationDraft = reconciled,
                        isReconciliationSheetVisible = true,
                        amfiDictionary = dict
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconciliationProgress = null
                    )
                }
                postStatusBanner("Spreadsheet parsing failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Overload accepting an [InputStream] for backward compatibility.
     * Reads all bytes immediately before coroutine dispatch to prevent stream closure race conditions.
     */
    fun processSpreadsheetImport(inputStream: InputStream, filename: String) {
        val bytes = inputStream.readBytes()
        processSpreadsheetImport(bytes, filename)
    }

    private suspend fun executeReconciliationPipeline(
        bytes: ByteArray,
        filename: String
    ): Pair<List<ReconciledSchemeItem>, List<Pair<Int, String>>> {
        _uiState.update {
            it.copy(
                reconciliationProgress = com.fin.mf.data.ReconciliationProgress(
                    stageText = "Filtering broker data rows...",
                    progressFraction = 0.50f
                )
            )
        }
        val rows = withContext(Dispatchers.Default) {
            UniversalSpreadsheetReader.readSpreadsheetRowsFromBytes(bytes, filename)
        }
        val records = withContext(Dispatchers.Default) {
            BrokerFormatNormalizer.normalizeRows(rows)
        }
        if (records.isEmpty()) {
            throw IllegalStateException("No valid mutual fund holding records found in $filename")
        }
        _uiState.update {
            it.copy(
                reconciliationProgress = com.fin.mf.data.ReconciliationProgress(
                    stageText = "Reconciling with AMFI registry...",
                    progressFraction = 0.80f
                )
            )
        }
        val dict = repository.getAmfiSchemeDictionary()
        val reconciled = withContext(Dispatchers.Default) {
            AmfiFuzzyReconciler.reconcileAll(records, dict)
        }
        _uiState.update {
            it.copy(
                reconciliationProgress = com.fin.mf.data.ReconciliationProgress(
                    stageText = "Preparing confirmation preview...",
                    progressFraction = 1.0f
                )
            )
        }
        delay(150L) // Visual smoothing for 100% completion
        return Pair(reconciled, dict)
    }

    /**
     * Commits reconciled schemes into the active portfolio with the specified merge strategy.
     */
    fun commitReconciledHoldings(schemes: List<MutualFundScheme>, mode: VaultImportMode) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    reconciliationItems = null,
                    cachedReconciliationDraft = null,
                    isReconciliationSheetVisible = false
                )
            }
            runCatching {
                val updatedPortfolio = repository.commitReconciledPortfolio(schemes, mode)
                val analytics = repository.computeAnalytics(updatedPortfolio)
                Pair(updatedPortfolio, analytics)
            }.onSuccess { (updatedPortfolio, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = updatedPortfolio,
                        analytics = analytics,
                        selectedSchemeCode = updatedPortfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Successfully reconciled and imported ${schemes.size} funds!")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Import commit failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Dismisses the active reconciliation preview sheet, safely persisting the draft in ViewModel.
     */
    fun dismissReconciliation(currentItems: List<ReconciledSchemeItem>? = null) {
        _uiState.update {
            it.copy(
                reconciliationItems = null,
                isReconciliationSheetVisible = false,
                cachedReconciliationDraft = currentItems ?: it.reconciliationItems ?: it.cachedReconciliationDraft
            )
        }
    }

    /**
     * Resumes reviewing the cached reconciliation draft without re-reading or re-parsing the file.
     */
    fun resumeReconciliationDraft() {
        val draft = _uiState.value.cachedReconciliationDraft
        if (!draft.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    reconciliationItems = draft,
                    isReconciliationSheetVisible = true
                )
            }
        }
    }

    /**
     * Discards the cached reconciliation draft.
     */
    fun discardReconciliationDraft() {
        _uiState.update {
            it.copy(
                reconciliationItems = null,
                cachedReconciliationDraft = null,
                isReconciliationSheetVisible = false
            )
        }
        postStatusBanner("Discarded spreadsheet draft.")
    }

    /**
     * Clears all mutual fund schemes from the active portfolio, resetting it to clean empty state.
     */
    fun clearActivePortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val clearedPortfolio = repository.clearActivePortfolio()
                val clearedAnalytics = repository.computeAnalytics(clearedPortfolio)
                val manifest = repository.getManifest()
                Triple(clearedPortfolio, manifest, clearedAnalytics)
            }.onSuccess { (portfolio, manifest, analytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        portfolioManifest = manifest,
                        analytics = analytics,
                        selectedSchemeCode = null
                    )
                }
                postStatusBanner("Cleared all schemes from active vault.")
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Clear failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Updates the SEBI Market Cap target mandate configuration and recomputes analytics immediately.
     */
    fun updateMarketCapMandate(mandate: MarketCapMandate) {
        viewModelScope.launch {
            runCatching {
                val updatedPortfolio = repository.updateMarketCapMandate(mandate)
                val updatedAnalytics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    repository.computeAnalytics(updatedPortfolio)
                }
                Pair(updatedPortfolio, updatedAnalytics)
            }.onSuccess { (portfolio, analytics) ->
                _uiState.update {
                    it.copy(
                        portfolioData = portfolio,
                        analytics = analytics
                    )
                }
                postStatusBanner("Market Cap Mandate updated.")
            }.onFailure { error ->
                postStatusBanner("Failed to update mandate: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Selects a specific scheme for deep-dive inspection and fetches its live historical NAV chart series.
     * If an external searched scheme is selected, optionally creates an immediate optimistic scheme so UI updates with 0ms lag.
     */
    fun selectSchemeForDeepDive(schemeCode: Int, initialName: String? = null) {
        val existingInPortfolio = _uiState.value.portfolioData?.schemes?.find { it.schemeCode == schemeCode }
        val fallbackScheme = existingInPortfolio ?: initialName?.let { name ->
            val (inferredCategory, inferredAssetClass) = com.fin.mf.data.engine.HoldingsEngine.inferCategoryAndAssetClass(name)
            val amc = com.fin.mf.data.engine.HoldingsEngine.inferAmcName(name)
            val holdings = com.fin.mf.data.engine.HoldingsEngine.generateHoldings(schemeCode, name, inferredCategory, inferredAssetClass)
            MutualFundScheme(
                schemeCode = schemeCode,
                schemeName = name,
                amc = amc,
                category = inferredCategory,
                assetClass = inferredAssetClass,
                riskLevel = com.fin.mf.data.engine.HoldingsEngine.inferRiskLevel(inferredCategory, inferredAssetClass),
                expenseRatio = com.fin.mf.data.engine.HoldingsEngine.inferExpenseRatio(inferredCategory, name.contains("direct", ignoreCase = true)),
                exitLoad = "1.0% within 365 days",
                benchmark = com.fin.mf.data.engine.HoldingsEngine.inferBenchmark(inferredCategory),
                units = 0.0,
                investedAmount = 0.0,
                fallbackNav = 0.0,
                lastNavDate = "",
                holdings = holdings
            )
        }

        _uiState.update {
            it.copy(
                selectedSchemeCode = schemeCode,
                inspectedExternalScheme = fallbackScheme,
                isHistoryLoading = true,
                selectedSchemeHistory = emptyList(),
                historyErrorMessage = null
            )
        }

        viewModelScope.launch {
            // If the scheme is not currently in the active portfolio, hydrate its metadata and authentic holdings
            if (existingInPortfolio == null) {
                repository.getSchemeDetails(schemeCode)
                    .onSuccess { externalScheme ->
                        _uiState.update { current ->
                            if (current.selectedSchemeCode == schemeCode) {
                                current.copy(inspectedExternalScheme = externalScheme)
                            } else current
                        }
                    }
            }

            // Fetch historical NAV series for charting
            repository.fetchSchemeHistoricalNavs(schemeCode)
                .onSuccess { navHistory ->
                    _uiState.update {
                        it.copy(
                            isHistoryLoading = false,
                            selectedSchemeHistory = navHistory,
                            historyErrorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isHistoryLoading = false,
                            selectedSchemeHistory = emptyList(),
                            historyErrorMessage = error.localizedMessage ?: "Unable to load historical NAV series"
                        )
                    }
                }
        }
    }

    /**
     * Manages multi-fund comparison scheme selection (allows up to 5 funds).
     */
    fun toggleComparisonScheme(schemeCode: Int) {
        val currentCodes = _uiState.value.comparisonSchemeCodes
        val updated = if (currentCodes.contains(schemeCode)) {
            currentCodes.filterNot { it == schemeCode }
        } else {
            if (currentCodes.size >= 5) {
                currentCodes.drop(1) + schemeCode
            } else {
                currentCodes + schemeCode
            }
        }
        _uiState.update { it.copy(comparisonSchemeCodes = updated) }
        loadComparisonData(updated)
    }

    fun removeComparisonScheme(schemeCode: Int) {
        val updated = _uiState.value.comparisonSchemeCodes.filterNot { it == schemeCode }
        _uiState.update { it.copy(comparisonSchemeCodes = updated) }
        loadComparisonData(updated)
    }

    fun togglePortfolioAvgOverlay() {
        _uiState.update { it.copy(isPortfolioAvgOverlayEnabled = !it.isPortfolioAvgOverlayEnabled) }
    }

    fun updateComparisonTimeframe(timeframe: ChartTimeframe) {
        _uiState.update { it.copy(comparisonTimeframe = timeframe) }
    }

    fun setCustomComparisonDateRange(startEpochMs: Long?, endEpochMs: Long?) {
        _uiState.update {
            it.copy(
                customComparisonStartEpochMs = startEpochMs,
                customComparisonEndEpochMs = endEpochMs
            )
        }
    }

    fun reloadComparisonData() {
        loadComparisonData(_uiState.value.comparisonSchemeCodes)
    }

    private fun loadComparisonData(schemeCodes: List<Int>) {
        comparisonJob?.cancel()
        if (schemeCodes.isEmpty()) {
            _uiState.update { it.copy(comparisonFunds = emptyList(), isComparisonLoading = false, comparisonErrorMessage = null) }
            return
        }

        _uiState.update { it.copy(isComparisonLoading = true, comparisonErrorMessage = null) }
        comparisonJob = viewModelScope.launch {
            repository.fetchFundComparison(schemeCodes)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            isComparisonLoading = false,
                            comparisonFunds = items,
                            comparisonErrorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isComparisonLoading = false,
                            comparisonFunds = emptyList(),
                            comparisonErrorMessage = error.localizedMessage ?: "Failed to load comparison data"
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateAssetClassFilter(assetClass: String) {
        _uiState.update { it.copy(selectedAssetClassFilter = assetClass) }
    }

    fun updateChartRange(range: NavChartRange) {
        _uiState.update { it.copy(selectedChartRange = range) }
    }

    fun updateChartTimeframe(timeframe: ChartTimeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun togglePrivacyMask() {
        _uiState.update { it.copy(isPrivacyMasked = !it.isPrivacyMasked) }
    }

    fun setFloatingFabEnabled(enabled: Boolean) {
        repository.setFloatingFabEnabled(enabled)
        _uiState.update { it.copy(isFloatingFabEnabled = enabled) }
    }

    fun toggleFloatingFab() {
        val next = !_uiState.value.isFloatingFabEnabled
        setFloatingFabEnabled(next)
    }

    fun setMarketStatusTickerEnabled(enabled: Boolean) {
        repository.setMarketStatusTickerEnabled(enabled)
        _uiState.update { it.copy(isMarketStatusTickerEnabled = enabled) }
    }

    fun toggleMarketStatusTicker() {
        val next = !_uiState.value.isMarketStatusTickerEnabled
        setMarketStatusTickerEnabled(next)
    }

    fun dismissStatusBanner() {
        bannerDismissJob?.cancel()
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}