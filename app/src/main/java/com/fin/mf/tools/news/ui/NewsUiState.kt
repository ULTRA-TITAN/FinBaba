package com.fin.mf.tools.news.ui

import com.fin.mf.tools.news.data.FinnhubArticle
import com.fin.mf.tools.news.data.NewsCategory
import com.fin.mf.tools.news.data.SentimentScore
import java.util.Locale

/**
 * Immutable UI State representation for the Market Intelligence & Sentiment Wire terminal.
 */
data class NewsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedCategory: NewsCategory = NewsCategory.GENERAL,
    val breakingArticles: List<FinnhubArticle> = emptyList(),
    val feedArticles: List<FinnhubArticle> = emptyList(),
    val bookmarkedArticleIds: Set<Long> = emptySet(),
    val sentimentRadar: Map<String, SentimentScore> = emptyMap(),
    val portfolioTickers: List<String> = emptyList(),
    val activeLookthroughTickersWithNews: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Top featured breaking story (first item of breaking articles or feed).
     */
    val heroArticle: FinnhubArticle?
        get() = breakingArticles.firstOrNull() ?: feedArticles.firstOrNull()

    /**
     * Feed list excluding the hero article when displayed at top.
     */
    val displayFeedArticles: List<FinnhubArticle>
        get() {
            val hero = heroArticle
            val baseList = if (hero != null) {
                feedArticles.filter { it.id != hero.id && it.headline != hero.headline }
            } else {
                feedArticles
            }

            if (searchQuery.isBlank()) return baseList

            val q = searchQuery.trim().lowercase(Locale.ROOT)
            return baseList.filter { article ->
                article.headline.lowercase(Locale.ROOT).contains(q) ||
                article.summary.lowercase(Locale.ROOT).contains(q) ||
                article.source.lowercase(Locale.ROOT).contains(q) ||
                article.related?.lowercase(Locale.ROOT)?.contains(q) == true
            }
        }
}
