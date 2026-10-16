package com.fin.mf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.NavSyncReport
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Timeframe selector for the historical NAV chart on the Scheme Detail screen.
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
    val historyErrorMessage: String? = null
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
     * Returns the currently selected scheme for the deep-dive detail modal/screen.
     */
    val selectedScheme: MutualFundScheme?
        get() = portfolioData?.schemes?.find { it.schemeCode == selectedSchemeCode }
}

/**
 * Primary ViewModel coordinating local portfolio storage, live parallel NAV syncs,
 * look-through overlap analytics, and historical NAV chart state.
 */
class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PortfolioRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        loadInitialPortfolio()
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioData = portfolio,
                        analytics = analytics,
                        selectedSchemeCode = it.selectedSchemeCode ?: portfolio.schemes.firstOrNull()?.schemeCode
                    )
                }
                // Automatically fetch fresh NAVs from api.mfapi.in in the background on boot
                syncAllLiveNavs()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusBannerMessage = "Failed to load portfolio: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Triggers parallel `async/awaitAll` network calls to `https://api.mfapi.in/mf/{code}/latest`
     * for all 20 schemes and updates `portfolio_active.json`.
     */
    fun syncAllLiveNavs() {
        if (_uiState.value.isSyncingNavs) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingNavs = true,
                    statusBannerMessage = "Syncing live AMFI NAVs across 20 schemes..."
                )
            }

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
                        lastSyncReport = report,
                        statusBannerMessage = summaryMsg
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncingNavs = false,
                        statusBannerMessage = "Live NAV sync error: ${error.localizedMessage ?: "Check internet connection"}"
                    )
                }
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
                        analytics = updatedAnalytics,
                        statusBannerMessage = "Saved position update to portfolio_active.json"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        statusBannerMessage = "Failed to update holding: ${error.localizedMessage}"
                    )
                }
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
                        statusBannerMessage = "Restored portfolio to factory 20-scheme seed."
                    )
                }
                syncAllLiveNavs()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusBannerMessage = "Reset failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Selects a specific scheme for deep-dive inspection and fetches its live historical NAV chart series
     * from `https://api.mfapi.in/mf/{schemeCode}`.
     */
    fun selectSchemeForDeepDive(schemeCode: Int) {
        _uiState.update {
            it.copy(
                selectedSchemeCode = schemeCode,
                isHistoryLoading = true,
                selectedSchemeHistory = emptyList(),
                historyErrorMessage = null
            )
        }

        viewModelScope.launch {
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateAssetClassFilter(assetClass: String) {
        _uiState.update { it.copy(selectedAssetClassFilter = assetClass) }
    }

    fun updateChartRange(range: NavChartRange) {
        _uiState.update { it.copy(selectedChartRange = range) }
    }

    fun togglePrivacyMask() {
        _uiState.update { it.copy(isPrivacyMasked = !it.isPrivacyMasked) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}