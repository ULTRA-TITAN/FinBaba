package com.fin.mf.tools.news.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fin.mf.tools.news.data.FinnhubArticle
import com.fin.mf.tools.news.data.NewsCategory
import com.fin.mf.tools.news.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Institutional ViewModel managing state, parallel fetching, category routing,
 * bookmark state, and look-through sentiment diagnostics for the Finnhub terminal.
 */
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        val defaultTickers = listOf("NVDA", "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA")
        val bookmarkedIds = repository.getBookmarkedArticleIds()

        _uiState.update {
            it.copy(
                portfolioTickers = defaultTickers,
                bookmarkedArticleIds = bookmarkedIds
            )
        }

        // Initial load of global macro news
        loadCategoryNews(NewsCategory.GENERAL, forceRefresh = false)

        // Asynchronously load look-through portfolio tickers and sentiment radar
        viewModelScope.launch {
            val dynamicTickers = repository.extractPortfolioTickers()
            _uiState.update { it.copy(portfolioTickers = dynamicTickers) }
            refreshSentimentRadar(dynamicTickers)
        }
    }

    /**
     * Changes current category tab and streams relevant news items.
     */
    fun selectCategory(category: NewsCategory) {
        if (_uiState.value.selectedCategory == category && !_uiState.value.isRefreshing) return

        _uiState.update { it.copy(selectedCategory = category) }

        if (category == NewsCategory.SAVED) {
            loadSavedArticles()
        } else {
            loadCategoryNews(category, forceRefresh = false)
        }
    }

    /**
     * Executes manual pull-to-refresh across active category and sentiment gauges.
     */
    fun refreshNews() {
        val currentCategory = _uiState.value.selectedCategory
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            if (currentCategory == NewsCategory.SAVED) {
                loadSavedArticles()
            } else {
                fetchCategoryData(currentCategory, forceRefresh = true)
            }
            refreshSentimentRadar(_uiState.value.portfolioTickers)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Updates full-text search query.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Toggles search mode bar visibility.
     */
    fun setSearchActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
    }

    /**
     * Toggles article bookmark and persists to disk.
     */
    fun toggleBookmark(article: FinnhubArticle) {
        viewModelScope.launch {
            repository.toggleBookmark(article)
            val updatedIds = repository.getBookmarkedArticleIds()
            _uiState.update { it.copy(bookmarkedArticleIds = updatedIds) }

            if (_uiState.value.selectedCategory == NewsCategory.SAVED) {
                loadSavedArticles()
            }
        }
    }

    /**
     * Clears error notice from UI state.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadCategoryNews(category: NewsCategory, forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            fetchCategoryData(category, forceRefresh)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchCategoryData(category: NewsCategory, forceRefresh: Boolean) {
        when {
            category.isLookThrough -> {
                val tickers = _uiState.value.portfolioTickers
                val result = repository.fetchPortfolioLookthroughNews(tickers, forceRefresh)
                result.onSuccess { articles ->
                    val activeTickers = articles.mapNotNull { it.related?.trim()?.uppercase() }.toSet()
                    _uiState.update {
                        it.copy(
                            feedArticles = articles,
                            breakingArticles = articles.take(3),
                            activeLookthroughTickersWithNews = activeTickers
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
            category.finnhubCategory != null -> {
                val result = repository.fetchMarketNews(category.finnhubCategory, forceRefresh)
                result.onSuccess { articles ->
                    _uiState.update {
                        it.copy(
                            feedArticles = articles,
                            breakingArticles = articles.take(3)
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
            else -> {
                // Default fallback
                val result = repository.fetchMarketNews("general", forceRefresh)
                result.onSuccess { articles ->
                    _uiState.update {
                        it.copy(
                            feedArticles = articles,
                            breakingArticles = articles.take(3)
                        )
                    }
                }
            }
        }
    }

    private fun loadSavedArticles() {
        val saved = repository.getBookmarkedArticles()
        _uiState.update {
            it.copy(
                feedArticles = saved,
                breakingArticles = saved.take(1)
            )
        }
    }

    private fun refreshSentimentRadar(tickers: List<String>) {
        viewModelScope.launch {
            val radar = repository.fetchSentimentRadar(tickers)
            _uiState.update { it.copy(sentimentRadar = radar) }
        }
    }
}
