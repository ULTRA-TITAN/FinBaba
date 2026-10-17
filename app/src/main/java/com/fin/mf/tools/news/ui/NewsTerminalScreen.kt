package com.fin.mf.tools.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.remember
import com.fin.mf.data.PortfolioRepository
import com.fin.mf.ui.components.MarketStatusTickerPill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.tools.news.data.NewsCategory
import com.fin.mf.ui.components.bounceClick

/**
 * World-Class Tool #4: Market Intelligence Wire & Sentiment Radar Terminal Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsTerminalScreen(
    themeManager: VaultThemeManager,
    onNavigateBackToHub: () -> Unit,
    newsViewModel: NewsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by newsViewModel.uiState.collectAsState()
    val gradient = LocalVaultGradient.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val pullToRefreshState = rememberPullToRefreshState()
    val isTickerEnabled = remember { PortfolioRepository(context).isMarketStatusTickerEnabled() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NewsBgIce,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NewsSurfaceWhite,
                    titleContentColor = NewsSlatePrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToHub) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Vault Hub",
                            tint = NewsSlatePrimary
                        )
                    }
                },
                title = {
                    if (uiState.isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { newsViewModel.setSearchQuery(it) },
                            placeholder = { Text("Search headlines, tickers...", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewsBlueTag,
                                unfocusedBorderColor = NewsSlateBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { newsViewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Market Intelligence Wire",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NewsSlatePrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = NewsBlueTagSoft,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "FINNHUB",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NewsBlueTag,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Real-Time Finnhub Feed • Global Sentiments",
                                fontSize = 11.sp,
                                color = NewsSlateSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { newsViewModel.setSearchActive(!uiState.isSearchActive) }) {
                        Icon(
                            imageVector = if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Wire",
                            tint = NewsSlatePrimary
                        )
                    }

                    IconButton(onClick = {
                        val newCat = if (uiState.selectedCategory == NewsCategory.SAVED) {
                            NewsCategory.GENERAL
                        } else {
                            NewsCategory.SAVED
                        }
                        newsViewModel.selectCategory(newCat)
                    }) {
                        Icon(
                            imageVector = if (uiState.selectedCategory == NewsCategory.SAVED) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = if (uiState.selectedCategory == NewsCategory.SAVED) NewsAmber else NewsSlatePrimary
                        )
                    }

                    IconButton(onClick = { newsViewModel.refreshNews() }) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NewsBlueTag
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Feed",
                                tint = NewsSlatePrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = { newsViewModel.refreshNews() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Live Market Status & NSE/BSE Holiday Ticker Pill
                if (isTickerEnabled) {
                    item {
                        MarketStatusTickerPill(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Category Filter Strip
                item {
                    CategoryFilterStrip(
                        selectedCategory = uiState.selectedCategory,
                        onSelectCategory = { newsViewModel.selectCategory(it) }
                    )
                }

                // Error Notice Banner if any
                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = NewsCrimsonSoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NewsCrimson.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "Network issue",
                                    fontSize = 12.sp,
                                    color = NewsCrimson,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { newsViewModel.refreshNews() }) {
                                    Text("Retry", fontWeight = FontWeight.Bold, color = NewsCrimson)
                                }
                            }
                        }
                    }
                }

                // Look-Through Sentiment Radar (Visible on Lookthrough or General Tab)
                if (uiState.selectedCategory == NewsCategory.PORTFOLIO_LOOKTHROUGH ||
                    uiState.selectedCategory == NewsCategory.GENERAL
                ) {
                    item {
                        SentimentRadarCard(
                            portfolioTickers = uiState.portfolioTickers,
                            sentimentRadar = uiState.sentimentRadar,
                            activeLookthroughTickersWithNews = uiState.activeLookthroughTickersWithNews,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Breaking News Hero Card (Top Headline)
                val hero = uiState.heroArticle
                if (hero != null && uiState.searchQuery.isBlank()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            NewsHeroCard(
                                article = hero,
                                isBookmarked = uiState.bookmarkedArticleIds.contains(hero.id),
                                onToggleBookmark = { newsViewModel.toggleBookmark(hero) }
                            )
                        }
                    }
                }

                // Feed Section Header
                item {
                    val feedCount = uiState.displayFeedArticles.size
                    val title = when (uiState.selectedCategory) {
                        NewsCategory.SAVED -> "SAVED ARTICLES ($feedCount)"
                        NewsCategory.PORTFOLIO_LOOKTHROUGH -> "PORTFOLIO LOOK-THROUGH WIRE ($feedCount)"
                        else -> "LATEST MARKET WIRE ($feedCount)"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = NewsSlateSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Initial loading state
                if (uiState.isLoading && uiState.feedArticles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NewsBlueTag, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to Finnhub Financial Wire...",
                                    fontSize = 13.sp,
                                    color = NewsSlateSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Empty State
                if (!uiState.isLoading && uiState.displayFeedArticles.isEmpty() && hero == null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = NewsSurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NewsSlateBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = NewsSlateMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (uiState.selectedCategory == NewsCategory.SAVED) {
                                        "No Saved Articles Yet"
                                    } else {
                                        "No Articles Found"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NewsSlatePrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.selectedCategory == NewsCategory.SAVED) {
                                        "Bookmark stories from the market wire for permanent offline reading."
                                    } else {
                                        "Pull down to refresh or try another category filter."
                                    },
                                    fontSize = 12.sp,
                                    color = NewsSlateSecondary
                                )
                            }
                        }
                    }
                }

                // Ranked Feed Stream
                items(
                    items = uiState.displayFeedArticles,
                    key = { it.id.takeIf { id -> id != 0L } ?: it.hashCode() }
                ) { article ->
                    NewsArticleRow(
                        article = article,
                        isBookmarked = uiState.bookmarkedArticleIds.contains(article.id),
                        onToggleBookmark = { newsViewModel.toggleBookmark(article) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Thumb-friendly horizontal category filter strip.
 */
@Composable
private fun CategoryFilterStrip(
    selectedCategory: NewsCategory,
    onSelectCategory: (NewsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = LocalVaultGradient.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NewsSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, NewsSlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NewsCategory.values().forEach { category ->
                val isSelected = category == selectedCategory

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .bounceClick(scaleDown = 0.96f) { onSelectCategory(category) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) NewsSlatePrimary else NewsBgIce,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) NewsSlatePrimary else NewsSlateBorder
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = category.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else NewsSlatePrimary
                        )
                    }
                }
            }
        }
    }
}
