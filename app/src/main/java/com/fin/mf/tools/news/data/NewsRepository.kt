package com.fin.mf.tools.news.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fin.mf.data.PortfolioRepository
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Institutional Repository orchestrating Finnhub News, look-through equity monitoring,
 * sentiment computation, offline disk caching (`news_cache.json`), and bookmark persistence.
 */
class NewsRepository(
    private val context: Context,
    private val apiService: FinnhubNewsApiService = FinnhubClient.service,
    private val portfolioRepository: PortfolioRepository = PortfolioRepository(context)
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("finbaba_prefs", Context.MODE_PRIVATE)
    }

    private val cacheFile: File by lazy {
        File(context.filesDir, "news_cache.json")
    }

    private val savedArticlesFile: File by lazy {
        File(context.filesDir, "saved_articles.json")
    }

    private val moshi = FinnhubClient.moshi

    private val cachedNewsAdapter by lazy {
        moshi.adapter(CachedNewsData::class.java)
    }

    private val articleListAdapter by lazy {
        val listType = Types.newParameterizedType(List::class.java, FinnhubArticle::class.java)
        moshi.adapter<List<FinnhubArticle>>(listType)
    }

    // In-memory cache structures
    private val memoryNewsCache = mutableMapOf<String, List<FinnhubArticle>>()
    private val memoryCompanyNewsCache = mutableMapOf<String, List<FinnhubArticle>>()
    private val memorySentimentCache = mutableMapOf<String, SentimentScore>()

    private var isCacheLoaded = false

    private fun ensureDiskCacheLoaded() {
        if (isCacheLoaded) return
        synchronized(this) {
            if (isCacheLoaded) return
            try {
                if (cacheFile.exists()) {
                    val json = cacheFile.readText()
                    val cached = cachedNewsAdapter.fromJson(json)
                    if (cached != null) {
                        memoryNewsCache.putAll(cached.articlesByCategory)
                        memoryCompanyNewsCache.putAll(cached.companyArticles)
                        memorySentimentCache.putAll(cached.sentimentMap)
                    }
                }
            } catch (e: Exception) {
                Log.e("NewsRepository", "Failed to load news_cache.json", e)
            } finally {
                isCacheLoaded = true
            }
        }
    }

    /**
     * Persists current memory cache state to `news_cache.json`.
     */
    private suspend fun saveDiskCache() = withContext(Dispatchers.IO) {
        try {
            val cacheData = CachedNewsData(
                savedEpochMs = System.currentTimeMillis(),
                articlesByCategory = memoryNewsCache,
                companyArticles = memoryCompanyNewsCache,
                sentimentMap = memorySentimentCache
            )
            val json = cachedNewsAdapter.toJson(cacheData)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to save news_cache.json", e)
        }
    }

    /**
     * Fetches categorized market news (e.g. general, merger, crypto) with offline cache fallback.
     */
    suspend fun fetchMarketNews(
        category: String,
        forceRefresh: Boolean = false
    ): Result<List<FinnhubArticle>> = withContext(Dispatchers.IO) {
        ensureDiskCacheLoaded()
        if (!forceRefresh && memoryNewsCache.containsKey(category) && !memoryNewsCache[category].isNullOrEmpty()) {
            return@withContext Result.success(memoryNewsCache[category]!!)
        }

        try {
            val response = apiService.getMarketNews(category = category)
            if (response.isSuccessful && response.body() != null) {
                val articles = response.body()!!.filter { it.headline.isNotBlank() }
                if (articles.isNotEmpty()) {
                    memoryNewsCache[category] = articles
                    saveDiskCache()
                    return@withContext Result.success(articles)
                }
            }
        } catch (e: Exception) {
            Log.w("NewsRepository", "Market news network error for category: $category", e)
        }

        // Fallback to cached articles
        val cached = memoryNewsCache[category]
        if (!cached.isNullOrEmpty()) {
            return@withContext Result.success(cached)
        }
        Result.failure(Exception("Unable to fetch market news for category: $category"))
    }

    /**
     * Extracts look-through equity tickers from the user's active portfolio.
     */
    suspend fun extractPortfolioTickers(): List<String> = withContext(Dispatchers.IO) {
        val extracted = mutableSetOf<String>()
        try {
            val activePortfolio = portfolioRepository.loadPortfolio()
            activePortfolio.schemes.forEach { scheme ->
                scheme.holdings.forEach { holding ->
                    val raw = holding.ticker.trim()
                    if (raw.isNotBlank()) {
                        val clean = raw
                            .replace(".NS", "", ignoreCase = true)
                            .replace(".BO", "", ignoreCase = true)
                            .trim()
                            .uppercase(Locale.US)
                        if (clean.isNotBlank() && clean.length in 1..8) {
                            extracted.add(clean)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error loading portfolio tickers", e)
        }

        // If list is small or empty, supplement with world-class institutional equities
        val defaultTickers = listOf("NVDA", "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA")
        extracted.addAll(defaultTickers)

        extracted.take(12).toList()
    }

    /**
     * Queries look-through company news wires across the extracted portfolio tickers.
     */
    suspend fun fetchPortfolioLookthroughNews(
        tickers: List<String>,
        forceRefresh: Boolean = false
    ): Result<List<FinnhubArticle>> = withContext(Dispatchers.IO) {
        ensureDiskCacheLoaded()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        val toDate = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -10)
        val fromDate = sdf.format(calendar.time)

        val aggregatedArticles = mutableListOf<FinnhubArticle>()

        for (ticker in tickers.take(6)) {
            if (!forceRefresh && memoryCompanyNewsCache.containsKey(ticker)) {
                aggregatedArticles.addAll(memoryCompanyNewsCache[ticker].orEmpty())
                continue
            }

            try {
                val response = apiService.getCompanyNews(symbol = ticker, fromDate = fromDate, toDate = toDate)
                if (response.isSuccessful && response.body() != null) {
                    val articles = response.body()!!.filter { it.headline.isNotBlank() }
                    if (articles.isNotEmpty()) {
                        memoryCompanyNewsCache[ticker] = articles
                        aggregatedArticles.addAll(articles)
                    }
                }
            } catch (e: Exception) {
                Log.w("NewsRepository", "Company news network error for: $ticker", e)
            }
        }

        if (aggregatedArticles.isNotEmpty()) {
            val distinctSorted = aggregatedArticles
                .distinctBy { it.id.takeIf { id -> id != 0L } ?: it.headline }
                .sortedByDescending { it.datetime }
            saveDiskCache()
            return@withContext Result.success(distinctSorted)
        }

        // Fallback to memoryCompanyNewsCache
        val allCached = memoryCompanyNewsCache.values.flatten()
            .distinctBy { it.id.takeIf { id -> id != 0L } ?: it.headline }
            .sortedByDescending { it.datetime }

        if (allCached.isNotEmpty()) {
            return@withContext Result.success(allCached)
        }

        // Fallback to general market news if no company news available
        fetchMarketNews("general", forceRefresh = false)
    }

    /**
     * Fetches or computes Sentiment Radar metrics for the top tickers.
     * Uses natural keyword analysis if the Finnhub news-sentiment endpoint is restricted on the free tier.
     */
    suspend fun fetchSentimentRadar(tickers: List<String>): Map<String, SentimentScore> = withContext(Dispatchers.IO) {
        ensureDiskCacheLoaded()
        val sentimentMap = mutableMapOf<String, SentimentScore>()

        for (ticker in tickers.take(6)) {
            // Check memory cache first
            if (memorySentimentCache.containsKey(ticker)) {
                sentimentMap[ticker] = memorySentimentCache[ticker]!!
                continue
            }

            var score: SentimentScore? = null
            try {
                val res = apiService.getNewsSentiment(symbol = ticker)
                if (res.isSuccessful && res.body()?.sentiment != null) {
                    score = res.body()!!.sentiment
                }
            } catch (e: Exception) {
                Log.d("NewsRepository", "Finnhub sentiment endpoint fallback for $ticker: ${e.message}")
            }

            // If API didn't return sentiment, compute from recent article headlines
            if (score == null) {
                val articles = memoryCompanyNewsCache[ticker] ?: emptyList()
                score = computeSentimentFromArticles(articles)
            }

            sentimentMap[ticker] = score
            memorySentimentCache[ticker] = score
        }

        saveDiskCache()
        sentimentMap
    }

    /**
     * Dynamic linguistic sentiment heuristic based on financial keyword polarity.
     */
    private fun computeSentimentFromArticles(articles: List<FinnhubArticle>): SentimentScore {
        if (articles.isEmpty()) {
            return SentimentScore(bearishPercent = 0.42, bullishPercent = 0.58)
        }

        val bullishKeywords = listOf(
            "record", "surge", "beat", "rally", "profit", "gain", "soar", "jump",
            "upgrade", "growth", "high", "buy", "expansion", "dividend", "bull", "success", "milestone"
        )
        val bearishKeywords = listOf(
            "drop", "plunge", "loss", "slump", "miss", "fall", "decline", "warning",
            "probe", "downgrade", "cut", "antitrust", "suit", "bear", "risk", "debt", "tariff"
        )

        var bullCount = 0
        var bearCount = 0

        articles.take(15).forEach { article ->
            val text = "${article.headline} ${article.summary}".lowercase(Locale.US)
            bullishKeywords.forEach { word -> if (text.contains(word)) bullCount++ }
            bearishKeywords.forEach { word -> if (text.contains(word)) bearCount++ }
        }

        val total = bullCount + bearCount
        return if (total > 0) {
            val bullRatio = (bullCount.toDouble() / total).coerceIn(0.20, 0.85)
            val bearRatio = (1.0 - bullRatio)
            SentimentScore(bearishPercent = bearRatio, bullishPercent = bullRatio)
        } else {
            SentimentScore(bearishPercent = 0.40, bullishPercent = 0.60)
        }
    }

    /**
     * Retrieves bookmarked article IDs from SharedPreferences.
     */
    fun getBookmarkedArticleIds(): Set<Long> {
        val stringSet = prefs.getStringSet("saved_article_ids", emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * Loads full bookmarked articles from `saved_articles.json`.
     */
    fun getBookmarkedArticles(): List<FinnhubArticle> {
        return try {
            if (savedArticlesFile.exists()) {
                val json = savedArticlesFile.readText()
                articleListAdapter.fromJson(json) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to read saved_articles.json", e)
            emptyList()
        }
    }

    /**
     * Toggles bookmark state for an article and saves both IDs and the article payload offline.
     */
    suspend fun toggleBookmark(article: FinnhubArticle): Boolean = withContext(Dispatchers.IO) {
        val currentIds = getBookmarkedArticleIds().toMutableSet()
        val currentArticles = getBookmarkedArticles().toMutableList()

        val isBookmarked: Boolean
        if (currentIds.contains(article.id)) {
            currentIds.remove(article.id)
            currentArticles.removeAll { it.id == article.id }
            isBookmarked = false
        } else {
            currentIds.add(article.id)
            currentArticles.removeAll { it.id == article.id }
            currentArticles.add(0, article)
            isBookmarked = true
        }

        // Save ID set in SharedPreferences
        prefs.edit().putStringSet("saved_article_ids", currentIds.map { it.toString() }.toSet()).apply()

        // Persist full article models in saved_articles.json
        try {
            val json = articleListAdapter.toJson(currentArticles)
            savedArticlesFile.writeText(json)
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to persist saved_articles.json", e)
        }

        isBookmarked
    }
}
