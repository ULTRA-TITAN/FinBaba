package com.fin.mf.tools.news.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import com.fin.mf.tools.news.data.FinnhubArticle

// High-Density Editorial Color Palette
val NewsBgIce = Color(0xFFF8FAFC)
val NewsSurfaceWhite = Color(0xFFFFFFFF)
val NewsSlateBorder = Color(0xFFE2E8F0)
val NewsSlatePrimary = Color(0xFF0F172A)
val NewsSlateSecondary = Color(0xFF475569)
val NewsSlateMuted = Color(0xFF94A3B8)
val NewsEmerald = Color(0xFF059669)
val NewsEmeraldSoft = Color(0xFFECFDF5)
val NewsCrimson = Color(0xFFDC2626)
val NewsCrimsonSoft = Color(0xFFFEF2F2)
val NewsAmber = Color(0xFFD97706)
val NewsAmberSoft = Color(0xFFFFFBEB)
val NewsBlueTag = Color(0xFF2563EB)
val NewsBlueTagSoft = Color(0xFFEFF6FF)

/**
 * Launches Chrome Custom Tabs for instant, sandboxed, ad-blocked institutional reading.
 */
fun launchArticleCustomTab(context: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "No article link available", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = Uri.parse(url)
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
        customTabsIntent.launchUrl(context, uri)
    } catch (e: Exception) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Could not open article link", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Dispatches an Android Sharesheet Intent with the headline, source, and URL.
 */
fun shareNewsArticle(context: Context, article: FinnhubArticle) {
    val shareBody = buildString {
        append(article.headline)
        append("\n\n")
        if (article.summary.isNotBlank()) {
            append(article.summary)
            append("\n\n")
        }
        append("Source: ${article.displaySource}\n")
        append("Read full story: ${article.url}\n\n")
        append("Shared via FinBaba Market Intelligence Wire")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.headline)
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Share Financial News"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share article", Toast.LENGTH_SHORT).show()
    }
}
