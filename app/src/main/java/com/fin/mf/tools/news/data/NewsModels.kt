package com.fin.mf.tools.news.data

import android.text.format.DateUtils
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

/**
 * High-performance Finnhub News Article DTO mapping `/api/v1/news` and `/api/v1/company-news`.
 */
@JsonClass(generateAdapter = true)
data class FinnhubArticle(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "category") val category: String = "",
    @Json(name = "datetime") val datetime: Long = 0L,
    @Json(name = "headline") val headline: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "related") val related: String? = null,
    @Json(name = "source") val source: String = "",
    @Json(name = "summary") val summary: String = "",
    @Json(name = "url") val url: String = ""
) {
    /**
     * Human-friendly relative time string (e.g. "12m ago", "3h ago", "Yesterday").
     */
    val relativeTime: String
        get() {
            if (datetime <= 0L) return "Recent"
            val now = System.currentTimeMillis()
            val timeMs = datetime * 1000L
            return try {
                DateUtils.getRelativeTimeSpanString(
                    timeMs,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            } catch (e: Exception) {
                "Recent"
            }
        }

    /**
     * Clean display source tag.
     */
    val displaySource: String
        get() = source.trim().ifBlank { "Market Wire" }

    /**
     * Clean related ticker tag or category fallback.
     */
    val displayTag: String
        get() {
            val rel = related?.trim()
            return if (!rel.isNullOrBlank()) {
                rel.uppercase(Locale.US)
            } else {
                category.trim().uppercase(Locale.US).ifBlank { "MACRO" }
            }
        }
}

/**
 * Response model for `/api/v1/news-sentiment`.
 */
@JsonClass(generateAdapter = true)
data class SentimentResponse(
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "buzz") val buzz: BuzzScore? = null,
    @Json(name = "sentiment") val sentiment: SentimentScore? = null
)

@JsonClass(generateAdapter = true)
data class BuzzScore(
    @Json(name = "articlesInLastWeek") val articlesInLastWeek: Int = 0,
    @Json(name = "buzz") val buzz: Double = 0.0,
    @Json(name = "weeklyAverage") val weeklyAverage: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SentimentScore(
    @Json(name = "bearishPercent") val bearishPercent: Double = 0.5,
    @Json(name = "bullishPercent") val bullishPercent: Double = 0.5
) {
    val isBullish: Boolean
        get() = bullishPercent >= bearishPercent

    val formattedBullish: String
        get() = "${(bullishPercent * 100).toInt()}%"

    val formattedBearish: String
        get() = "${(bearishPercent * 100).toInt()}%"
}

/**
 * Category filters available on the Market Intelligence Wire terminal.
 */
enum class NewsCategory(
    val displayName: String,
    val finnhubCategory: String?,
    val isLookThrough: Boolean = false
) {
    GENERAL("Global Macro", "general"),
    PORTFOLIO_LOOKTHROUGH("★ My Portfolio Wire", null, isLookThrough = true),
    MERGERS_ACQUISITIONS("Mergers & Deals", "merger"),
    TECH_MACRO("Tech & Crypto", "crypto"),
    SAVED("Saved", null)
}

/**
 * Disk cache container for offline news persistence (`news_cache.json`).
 */
@JsonClass(generateAdapter = true)
data class CachedNewsData(
    @Json(name = "savedEpochMs") val savedEpochMs: Long = 0L,
    @Json(name = "articlesByCategory") val articlesByCategory: Map<String, List<FinnhubArticle>> = emptyMap(),
    @Json(name = "companyArticles") val companyArticles: Map<String, List<FinnhubArticle>> = emptyMap(),
    @Json(name = "sentimentMap") val sentimentMap: Map<String, SentimentScore> = emptyMap()
)
