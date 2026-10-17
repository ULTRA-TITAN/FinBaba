package com.fin.mf.tools.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.tools.terminal.data.BenchmarkComparisonPoint
import com.fin.mf.tools.terminal.data.ChartDataPoint
import com.fin.mf.tools.terminal.data.MarketTerminalRepository
import com.fin.mf.tools.terminal.data.StockFundamentalSummary
import com.fin.mf.tools.terminal.data.TerminalAssetQuote
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import com.fin.mf.tools.terminal.data.TerminalTimeframe
import com.fin.mf.tools.terminal.data.YahooSearchQuoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WatchlistStock(
    val symbol: String,
    val name: String,
    val isIndex: Boolean = false,
    val lastPrice: Double? = null,
    val dayChangePercent: Double? = null,
    val currency: String? = null
)

data class ChartIndicatorConfig(
    val showVolume: Boolean = true,
    val showSma50: Boolean = true,
    val showSma200: Boolean = false,
    val isRulerActive: Boolean = false,
    val showBenchmarkNifty50: Boolean = false
)

data class TerminalUiState(
    val selectedSymbol: String = "RELIANCE.NS",
    val selectedTimeframe: TerminalTimeframe = TerminalTimeframe.ONE_DAY,
    val activeQuote: TerminalAssetQuote? = null,
    val stockFundamentals: StockFundamentalSummary? = null,
    val isLoadingChart: Boolean = false,
    val isLoadingFundamentals: Boolean = false,
    val chartErrorMessage: String? = null,
    val isBenchmarkOverlayEnabled: Boolean = false,
    val benchmarkSymbol: String = "^NSEI",
    val benchmarkComparisonPoints: List<BenchmarkComparisonPoint> = emptyList(),
    val benchmarkPoints: List<ChartDataPoint> = emptyList(),
    val sma50Series: List<Float?> = emptyList(),
    val sma200Series: List<Float?> = emptyList(),
    val indicatorConfig: ChartIndicatorConfig = ChartIndicatorConfig(),
    val isFullscreenOpen: Boolean = false,
    val isLoadingBenchmark: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<YahooSearchQuoteItem> = emptyList(),
    val watchlist: List<WatchlistStock> = listOf(
        WatchlistStock("^NSEI", "NIFTY 50", isIndex = true, lastPrice = 24850.30, dayChangePercent = 0.45, currency = "INR"),
        WatchlistStock("^BSESN", "S&P BSE SENSEX", isIndex = true, lastPrice = 81380.15, dayChangePercent = 0.38, currency = "INR"),
        WatchlistStock("RELIANCE.NS", "Reliance Industries", lastPrice = 2945.80, dayChangePercent = 1.12, currency = "INR"),
        WatchlistStock("HDFCBANK.NS", "HDFC Bank", lastPrice = 1680.40, dayChangePercent = -0.24, currency = "INR"),
        WatchlistStock("TCS.NS", "Tata Consultancy Services", lastPrice = 4120.00, dayChangePercent = 0.85, currency = "INR"),
        WatchlistStock("GOLDBEES.NS", "Nippon Gold ETF", lastPrice = 68.25, dayChangePercent = 0.15, currency = "INR"),
        WatchlistStock("AAPL", "Apple Inc.", lastPrice = 228.50, dayChangePercent = -0.62, currency = "USD")
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

    // Individual StateFlows for high-frequency consumption
    val selectedSymbol: StateFlow<String> = _uiState.map { it.selectedSymbol }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.selectedSymbol)
    val selectedTimeframe: StateFlow<TerminalTimeframe> = _uiState.map { it.selectedTimeframe }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.selectedTimeframe)
    val activeQuote: StateFlow<TerminalAssetQuote?> = _uiState.map { it.activeQuote }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.activeQuote)
    val stockFundamentals: StateFlow<StockFundamentalSummary?> = _uiState.map { it.stockFundamentals }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.stockFundamentals)
    val isLoadingChart: StateFlow<Boolean> = _uiState.map { it.isLoadingChart }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isLoadingChart)
    val isLoadingFundamentals: StateFlow<Boolean> = _uiState.map { it.isLoadingFundamentals }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isLoadingFundamentals)
    val indicatorConfig: StateFlow<ChartIndicatorConfig> = _uiState.map { it.indicatorConfig }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.indicatorConfig)
    val isFullscreenOpen: StateFlow<Boolean> = _uiState.map { it.isFullscreenOpen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isFullscreenOpen)
    val sma50Series: StateFlow<List<Float?>> = _uiState.map { it.sma50Series }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.sma50Series)
    val sma200Series: StateFlow<List<Float?>> = _uiState.map { it.sma200Series }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.sma200Series)
    val benchmarkPoints: StateFlow<List<ChartDataPoint>> = _uiState.map { it.benchmarkPoints }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.benchmarkPoints)
    val searchQuery: StateFlow<String> = _uiState.map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.searchQuery)
    val searchResults: StateFlow<List<YahooSearchQuoteItem>> = _uiState.map { it.searchResults }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.searchResults)
    val errorMessage: StateFlow<String?> = _uiState.map { it.chartErrorMessage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.chartErrorMessage)

    val quickWatchlistSymbols = listOf(
        "RELIANCE.NS",
        "TCS.NS",
        "HDFCBANK.NS",
        "INFY.NS",
        "ICICIBANK.NS",
        "AAPL",
        "NVDA",
        "MSFT"
    )

    private var chartFetchJob: Job? = null
    private var fundamentalsJob: Job? = null
    private var benchmarkFetchJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadAssetChart(_uiState.value.selectedSymbol, _uiState.value.selectedTimeframe)
        triggerFundamentalsFetch(_uiState.value.selectedSymbol)
    }

    fun selectSymbol(symbol: String) {
        val cleanSymbol = symbol.trim().uppercase()
        if (cleanSymbol.isEmpty()) return
        if (cleanSymbol == _uiState.value.selectedSymbol && _uiState.value.activeQuote != null) return

        _uiState.update {
            it.copy(
                selectedSymbol = cleanSymbol,
                scrubbedPoint = null,
                pointerA = null,
                pointerB = null,
                searchQuery = "",
                searchResults = emptyList(),
                chartErrorMessage = null
            )
        }
        loadAssetChart(cleanSymbol, _uiState.value.selectedTimeframe)
        triggerFundamentalsFetch(cleanSymbol)
    }

    fun selectTimeframe(timeframe: TerminalTimeframe) {
        if (timeframe == _uiState.value.selectedTimeframe && _uiState.value.activeQuote != null) return
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

    fun toggleVolume() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showVolume = !it.indicatorConfig.showVolume)) }
    }

    fun toggleSma50() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showSma50 = !it.indicatorConfig.showSma50)) }
    }

    fun toggleSma200() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showSma200 = !it.indicatorConfig.showSma200)) }
    }

    fun toggleRulerTool() {
        _uiState.update {
            val nextRuler = !it.indicatorConfig.isRulerActive
            it.copy(
                isDualPointerMode = nextRuler,
                indicatorConfig = it.indicatorConfig.copy(isRulerActive = nextRuler)
            )
        }
    }

    fun toggleBenchmarkNifty50() {
        val nextState = !_uiState.value.indicatorConfig.showBenchmarkNifty50
        _uiState.update {
            it.copy(
                isBenchmarkOverlayEnabled = nextState,
                indicatorConfig = it.indicatorConfig.copy(showBenchmarkNifty50 = nextState)
            )
        }
        if (nextState) {
            loadBenchmarkComparison(
                _uiState.value.selectedSymbol,
                _uiState.value.benchmarkSymbol,
                _uiState.value.selectedTimeframe
            )
        }
    }

    fun setFullscreenOpen(open: Boolean) {
        _uiState.update { it.copy(isFullscreenOpen = open) }
    }

    fun toggleBenchmarkOverlay() {
        toggleBenchmarkNifty50()
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
        triggerFundamentalsFetch(_uiState.value.selectedSymbol)
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
        toggleRulerTool()
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

    private fun triggerFundamentalsFetch(symbol: String) {
        fundamentalsJob?.cancel()
        fundamentalsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFundamentals = true) }
            repository.fetchStockFundamentals(symbol)
                .onSuccess { summary ->
                    val currentLtp = _uiState.value.activeQuote?.ltp
                    val finalSummary = if (summary.currentPrice == null && currentLtp != null) {
                        summary.copy(currentPrice = currentLtp)
                    } else {
                        summary
                    }
                    _uiState.update { it.copy(stockFundamentals = finalSummary, isLoadingFundamentals = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingFundamentals = false) }
                }
        }
    }

    private fun loadAssetChart(symbol: String, timeframe: TerminalTimeframe) {
        chartFetchJob?.cancel()
        chartFetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true, chartErrorMessage = null) }
            val chartResult = repository.fetchChart(symbol, timeframe)

            chartResult.onSuccess { quote ->
                // Compute Moving Averages on Dispatchers.Default
                val (sma50, sma200) = withContext(Dispatchers.Default) {
                    computeMovingAverages(quote.points)
                }

                _uiState.update {
                    val defaultPtA = if (it.isDualPointerMode) {
                        quote.chartPoints.getOrNull((quote.chartPoints.size * 0.25).toInt()) ?: quote.chartPoints.firstOrNull()
                    } else null
                    val defaultPtB = if (it.isDualPointerMode) {
                        quote.chartPoints.getOrNull((quote.chartPoints.size * 0.85).toInt()) ?: quote.chartPoints.lastOrNull()
                    } else null

                    val updatedWatchlist = it.watchlist.map { stock ->
                        if (stock.symbol.equals(quote.symbol, ignoreCase = true)) {
                            stock.copy(
                                lastPrice = quote.regularMarketPrice,
                                dayChangePercent = quote.dayChangePercent,
                                currency = quote.currency
                            )
                        } else stock
                    }

                    it.copy(
                        activeQuote = quote,
                        watchlist = updatedWatchlist,
                        isLoadingChart = false,
                        chartErrorMessage = null,
                        pointerA = defaultPtA,
                        pointerB = defaultPtB,
                        sma50Series = sma50,
                        sma200Series = sma200
                    )
                }

                // Synchronize price with stock fundamentals
                _uiState.update { current ->
                    current.stockFundamentals?.let { fund ->
                        current.copy(
                            stockFundamentals = fund.copy(
                                currentPrice = quote.ltp,
                                fiftyTwoWeekHigh = fund.fiftyTwoWeekHigh ?: quote.fiftyTwoWeekHigh,
                                fiftyTwoWeekLow = fund.fiftyTwoWeekLow ?: quote.fiftyTwoWeekLow
                            )
                        )
                    } ?: current
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

    private fun computeMovingAverages(
        points: List<ChartDataPoint>
    ): Pair<List<Float?>, List<Float?>> {
        if (points.isEmpty()) return emptyList<Float?>() to emptyList()
        val n = points.size
        val sma50 = MutableList<Float?>(n) { null }
        val sma200 = MutableList<Float?>(n) { null }

        var rollingSum50 = 0.0
        var rollingSum200 = 0.0

        for (i in 0 until n) {
            val close = points[i].close.toDouble()
            rollingSum50 += close
            rollingSum200 += close

            if (i >= 50) {
                rollingSum50 -= points[i - 50].close.toDouble()
            }
            if (i >= 49) {
                sma50[i] = (rollingSum50 / 50.0).toFloat()
            }

            if (i >= 200) {
                rollingSum200 -= points[i - 200].close.toDouble()
            }
            if (i >= 199) {
                sma200[i] = (rollingSum200 / 200.0).toFloat()
            }
        }
        return sma50 to sma200
    }
}
