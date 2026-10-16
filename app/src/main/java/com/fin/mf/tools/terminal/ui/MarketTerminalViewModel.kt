package com.fin.mf.tools.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.tools.terminal.data.BenchmarkComparisonPoint
import com.fin.mf.tools.terminal.data.MarketTerminalRepository
import com.fin.mf.tools.terminal.data.TerminalAssetQuote
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import com.fin.mf.tools.terminal.data.TerminalTimeframe
import com.fin.mf.tools.terminal.data.YahooSearchQuoteItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchlistStock(
    val symbol: String,
    val name: String,
    val isIndex: Boolean = false
)

data class TerminalUiState(
    val selectedSymbol: String = "RELIANCE.NS",
    val selectedTimeframe: TerminalTimeframe = TerminalTimeframe.ONE_DAY,
    val activeQuote: TerminalAssetQuote? = null,
    val isLoadingChart: Boolean = false,
    val chartErrorMessage: String? = null,
    val isBenchmarkOverlayEnabled: Boolean = false,
    val benchmarkSymbol: String = "^NSEI",
    val benchmarkComparisonPoints: List<BenchmarkComparisonPoint> = emptyList(),
    val isLoadingBenchmark: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<YahooSearchQuoteItem> = emptyList(),
    val watchlist: List<WatchlistStock> = listOf(
        WatchlistStock("^NSEI", "NIFTY 50", isIndex = true),
        WatchlistStock("^BSESN", "S&P BSE SENSEX", isIndex = true),
        WatchlistStock("RELIANCE.NS", "Reliance Industries Ltd"),
        WatchlistStock("HDFCBANK.NS", "HDFC Bank Ltd"),
        WatchlistStock("TCS.NS", "Tata Consultancy Services"),
        WatchlistStock("GOLDBEES.NS", "Nippon India ETF Gold BeES"),
        WatchlistStock("AAPL", "Apple Inc.")
    ),
    val scrubbedPoint: TerminalChartPoint? = null,
    val isDualPointerMode: Boolean = false,
    val pointerA: TerminalChartPoint? = null,
    val pointerB: TerminalChartPoint? = null
)

class MarketTerminalViewModel(
    private val repository: MarketTerminalRepository = MarketTerminalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var chartFetchJob: Job? = null
    private var benchmarkFetchJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadAssetChart(_uiState.value.selectedSymbol, _uiState.value.selectedTimeframe)
    }

    fun selectSymbol(symbol: String) {
        val cleanSymbol = symbol.trim().uppercase()
        if (cleanSymbol == _uiState.value.selectedSymbol && _uiState.value.activeQuote != null) return

        _uiState.update {
            it.copy(
                selectedSymbol = cleanSymbol,
                scrubbedPoint = null,
                pointerA = null,
                pointerB = null,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadAssetChart(cleanSymbol, _uiState.value.selectedTimeframe)
    }

    fun selectTimeframe(timeframe: TerminalTimeframe) {
        if (timeframe == _uiState.value.selectedTimeframe) return
        _uiState.update {
            it.copy(
                selectedTimeframe = timeframe,
                scrubbedPoint = null,
                pointerA = null,
                pointerB = null
            )
        }
        loadAssetChart(_uiState.value.selectedSymbol, timeframe)
    }

    fun toggleBenchmarkOverlay() {
        val willEnable = !_uiState.value.isBenchmarkOverlayEnabled
        _uiState.update { it.copy(isBenchmarkOverlayEnabled = willEnable) }
        if (willEnable) {
            loadBenchmarkComparison(
                _uiState.value.selectedSymbol,
                _uiState.value.benchmarkSymbol,
                _uiState.value.selectedTimeframe
            )
        }
    }

    fun setBenchmarkSymbol(benchmarkSymbol: String) {
        _uiState.update { it.copy(benchmarkSymbol = benchmarkSymbol) }
        if (_uiState.value.isBenchmarkOverlayEnabled) {
            loadBenchmarkComparison(
                _uiState.value.selectedSymbol,
                benchmarkSymbol,
                _uiState.value.selectedTimeframe
            )
        }
    }

    fun refreshActiveQuote() {
        loadAssetChart(_uiState.value.selectedSymbol, _uiState.value.selectedTimeframe)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(280) // Debounce typing
            _uiState.update { it.copy(isSearching = true) }
            val result = repository.searchAssets(query)
            result.onSuccess { quotes ->
                _uiState.update { it.copy(isSearching = false, searchResults = quotes) }
            }.onFailure {
                _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun setScrubbedPoint(point: TerminalChartPoint?) {
        _uiState.update { it.copy(scrubbedPoint = point) }
    }

    fun toggleDualPointerMode() {
        val willBeDual = !_uiState.value.isDualPointerMode
        _uiState.update { current ->
            if (willBeDual) {
                val points = current.activeQuote?.chartPoints ?: emptyList()
                val ptA = points.getOrNull((points.size * 0.25).toInt()) ?: points.firstOrNull()
                val ptB = points.getOrNull((points.size * 0.85).toInt()) ?: points.lastOrNull()
                current.copy(
                    isDualPointerMode = true,
                    pointerA = ptA,
                    pointerB = ptB,
                    scrubbedPoint = null
                )
            } else {
                current.copy(
                    isDualPointerMode = false,
                    pointerA = null,
                    pointerB = null
                )
            }
        }
    }

    fun setPointerA(point: TerminalChartPoint?) {
        _uiState.update { it.copy(pointerA = point) }
    }

    fun setPointerB(point: TerminalChartPoint?) {
        _uiState.update { it.copy(pointerB = point) }
    }

    fun toggleWatchlistSymbol(symbol: String, name: String) {
        _uiState.update { current ->
            val existing = current.watchlist.find { it.symbol.equals(symbol, ignoreCase = true) }
            val updated = if (existing != null) {
                current.watchlist.filterNot { it.symbol.equals(symbol, ignoreCase = true) }
            } else {
                current.watchlist + WatchlistStock(symbol, name)
            }
            current.copy(watchlist = updated)
        }
    }

    private fun loadAssetChart(symbol: String, timeframe: TerminalTimeframe) {
        chartFetchJob?.cancel()
        chartFetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, chartErrorMessage = null) }
            val chartResult = repository.fetchChart(symbol, timeframe)

            chartResult.onSuccess { quote ->
                _uiState.update {
                    val defaultPtA = if (it.isDualPointerMode) {
                        quote.chartPoints.getOrNull((quote.chartPoints.size * 0.25).toInt()) ?: quote.chartPoints.firstOrNull()
                    } else null
                    val defaultPtB = if (it.isDualPointerMode) {
                        quote.chartPoints.getOrNull((quote.chartPoints.size * 0.85).toInt()) ?: quote.chartPoints.lastOrNull()
                    } else null

                    it.copy(
                        activeQuote = quote,
                        isLoadingChart = false,
                        chartErrorMessage = null,
                        pointerA = defaultPtA,
                        pointerB = defaultPtB
                    )
                }

                if (_uiState.value.isBenchmarkOverlayEnabled) {
                    loadBenchmarkComparison(symbol, _uiState.value.benchmarkSymbol, timeframe)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingChart = false,
                        chartErrorMessage = err.localizedMessage ?: "Failed to load live chart for $symbol"
                    )
                }
            }
        }
    }

    private fun loadBenchmarkComparison(
        targetSymbol: String,
        benchmarkSymbol: String,
        timeframe: TerminalTimeframe
    ) {
        benchmarkFetchJob?.cancel()
        benchmarkFetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBenchmark = true) }
            val compResult = repository.fetchBenchmarkComparison(targetSymbol, benchmarkSymbol, timeframe)

            compResult.onSuccess { points ->
                _uiState.update {
                    it.copy(
                        benchmarkComparisonPoints = points,
                        isLoadingBenchmark = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        benchmarkComparisonPoints = emptyList(),
                        isLoadingBenchmark = false
                    )
                }
            }
        }
    }
}
