package com.fin.mf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.FundComparisonItem
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MfSearchDto
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.NavSyncReport
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.PortfolioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val inspectedExternalScheme: MutualFundScheme? = null
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
     * Loads `portfolio_active.json` (or seeds it from `assets/portfolio_seed.json` on first boot),
     * computes look-through analytics immediately, and triggers a background live NAV sync.
     */
    fun loadInitialPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val portfolio = repository.loadPortfolio()
                val analytics = repository.computeAnalytics(portfolio)
                Pair(portfolio, analytics)
            }.onSuccess { (portfolio, analytics) ->
                val firstSchemeCode = portfolio.schemes.firstOrNull()?.schemeCode
                val initialComparison = portfolio.schemes.take(2).map { it.schemeCode }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
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

    /**
     * Returns serialized CSV content for user export.
     */
    fun exportPortfolioCsv(): String {
        val currentPortfolio = _uiState.value.portfolioData ?: return "SchemeCode,SchemeName,Units,InvestedAmount\n"
        return repository.exportPortfolioCsv(currentPortfolio)
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
     * Resets `portfolio_active.json` back to the factory 20-scheme `portfolio_seed.json` state.
     */
    fun resetPortfolioToSeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val seedPortfolio = repository.resetToSeedPortfolio()
                val seedAnalytics = repository.computeAnalytics(seedPortfolio)
                Pair(seedPortfolio, seedAnalytics)
            }.onSuccess { (seedPortfolio, seedAnalytics) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = seedPortfolio,
                        analytics = seedAnalytics,
                        selectedSchemeCode = seedPortfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                postStatusBanner("Restored portfolio to factory 20-scheme seed.")
                syncAllLiveNavs()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                postStatusBanner("Reset failed: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Selects a specific scheme for deep-dive inspection and fetches its live historical NAV chart series.
     */
    fun selectSchemeForDeepDive(schemeCode: Int) {
        val existingInPortfolio = _uiState.value.portfolioData?.schemes?.find { it.schemeCode == schemeCode }
        _uiState.update {
            it.copy(
                selectedSchemeCode = schemeCode,
                inspectedExternalScheme = existingInPortfolio,
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

    fun dismissStatusBanner() {
        bannerDismissJob?.cancel()
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}