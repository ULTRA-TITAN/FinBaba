package com.fin.mf.tools.terminal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fin.mf.core.theme.AppearanceSettingsSheet
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.tools.terminal.charts.BenchmarkOverlayChart
import com.fin.mf.tools.terminal.charts.DualPointerAnalyzerChart
import com.fin.mf.tools.terminal.charts.StandardInteractiveChart
import com.fin.mf.tools.terminal.data.TerminalTimeframe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketTerminalScreen(
    onNavigateBackToHub: () -> Unit,
    terminalViewModel: MarketTerminalViewModel = viewModel(),
    themeManager: VaultThemeManager = VaultThemeManager.getInstance(LocalContext.current)
) {
    val gradient = LocalVaultGradient.current
    val uiState by terminalViewModel.uiState.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colorScheme = TerminalColorScheme) {
        Scaffold(
            containerColor = TerminalBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TerminalSurface,
                        titleContentColor = TerminalSlatePrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBackToHub) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Vault Hub",
                                tint = gradient.startColor
                            )
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Market Terminal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TerminalSlatePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = gradient.startColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "TOOL #2",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gradient.startColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.activeQuote?.exchange ?: "GLOBAL"} • Real-Time Yahoo Feed",
                                fontSize = 11.sp,
                                color = TerminalSlateSecondary
                            )
                        }
                    },
                    actions = {
                        // Studio Settings & Appearance
                        IconButton(onClick = { showAppearanceSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Terminal Settings",
                                tint = gradient.startColor
                            )
                        }
                        val isWatchlisted = uiState.watchlist.any {
                            it.symbol.equals(uiState.selectedSymbol, ignoreCase = true)
                        }
                        IconButton(
                            onClick = {
                                val name = uiState.activeQuote?.shortName ?: uiState.selectedSymbol
                                terminalViewModel.toggleWatchlistSymbol(uiState.selectedSymbol, name)
                            }
                        ) {
                            Icon(
                                imageVector = if (isWatchlisted) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Watchlist",
                                tint = if (isWatchlisted) TerminalAmberBenchmark else TerminalSlateSecondary
                            )
                        }
                        IconButton(
                            onClick = { terminalViewModel.refreshActiveQuote() },
                            enabled = !uiState.isLoadingChart
                        ) {
                            if (uiState.isLoadingChart) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = gradient.startColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = gradient.startColor
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Search Bar & Instant Autocomplete Dropdown
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = {
                                terminalViewModel.onSearchQueryChanged(it)
                                isSearchExpanded = it.isNotBlank()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Search NSE/BSE stocks, ETFs, indices (e.g. TATAMOTORS, AAPL)",
                                    fontSize = 12.sp,
                                    color = TerminalSlateSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TerminalSlateSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        terminalViewModel.clearSearch()
                                        isSearchExpanded = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            tint = TerminalSlateSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = TerminalSurface,
                                unfocusedContainerColor = TerminalSurface,
                                focusedBorderColor = gradient.startColor,
                                unfocusedBorderColor = TerminalSlateBorder
                            )
                        )

                        AnimatedVisibility(
                            visible = isSearchExpanded && (uiState.isSearching || uiState.searchResults.isNotEmpty()),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    if (uiState.isSearching) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = gradient.startColor
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Searching global exchanges...",
                                                fontSize = 12.sp,
                                                color = TerminalSlateSecondary
                                            )
                                        }
                                    } else {
                                        uiState.searchResults.take(6).forEach { quoteItem ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        terminalViewModel.selectSymbol(quoteItem.symbol)
                                                        isSearchExpanded = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = quoteItem.symbol,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TerminalSlatePrimary
                                                    )
                                                    Text(
                                                        text = quoteItem.displayName,
                                                        fontSize = 11.sp,
                                                        color = TerminalSlateSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = TerminalBg,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder)
                                                ) {
                                                    Text(
                                                        text = quoteItem.exchange ?: "NSE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TerminalSlateSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = TerminalSlateBorder.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Watchlist & Benchmark Quick-Picker (2-Column Grid)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "WATCHLIST & ASSET PICKER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlateSecondary,
                            letterSpacing = 0.5.sp
                        )

                        uiState.watchlist.chunked(2).forEach { rowStocks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStocks.forEach { stock ->
                                    val isSelected = stock.symbol.equals(uiState.selectedSymbol, ignoreCase = true)
                                    val exchangeBadge = when {
                                        stock.symbol.startsWith("^") -> "INDEX"
                                        stock.symbol.endsWith(".NS") -> "NSE"
                                        stock.symbol.endsWith(".BO") -> "BSE"
                                        else -> "US"
                                    }
                                    val activeQuote = uiState.activeQuote
                                    val currentPrice = if (isSelected && activeQuote != null) {
                                        activeQuote.regularMarketPrice
                                    } else stock.lastPrice

                                    val currentPct = if (isSelected && activeQuote != null) {
                                        activeQuote.dayChangePercent
                                    } else stock.dayChangePercent

                                    val currencyCode = if (isSelected && activeQuote != null) {
                                        activeQuote.currency
                                    } else (stock.currency ?: "INR")

                                    Surface(
                                        onClick = { terminalViewModel.selectSymbol(stock.symbol) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) gradient.startColor.copy(alpha = 0.08f) else Color.White,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) gradient.startColor else TerminalSlateBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val cleanTicker = stock.symbol.removeSuffix(".NS").removeSuffix(".BO")
                                                Text(
                                                    text = cleanTicker,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) gradient.startColor else TerminalSlatePrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (isSelected) gradient.startColor.copy(alpha = 0.15f)
                                                            else Color(0xFFF1F5F9)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = exchangeBadge,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) gradient.startColor else TerminalSlateSecondary
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val priceText = if (currentPrice != null) {
                                                    val prefix = if (currencyCode.equals("USD", ignoreCase = true)) "$" else "₹"
                                                    "$prefix${String.format(java.util.Locale.US, "%,.2f", currentPrice)}"
                                                } else "--"

                                                Text(
                                                    text = priceText,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TerminalSlatePrimary
                                                )

                                                if (currentPct != null) {
                                                    val isPositive = currentPct >= 0.0
                                                    val pctColor = if (isPositive) SemanticFinanceColors.ProfitGreen else SemanticFinanceColors.LossRed
                                                    val sign = if (isPositive) "+" else ""
                                                    Text(
                                                        text = "$sign${String.format(java.util.Locale.US, "%.2f", currentPct)}%",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pctColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (rowStocks.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 3. Hero Quote & Day Range Needle Meter
                item {
                    HeroQuoteCard(
                        quote = uiState.activeQuote,
                        selectedSymbol = uiState.selectedSymbol,
                        scrubbedPoint = uiState.scrubbedPoint,
                        selectedTimeframe = uiState.selectedTimeframe
                    )
                }

                // 4. Timeframe Selector & Feature Toggles Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Timeframe chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TerminalTimeframe.entries) { tf ->
                                val isSelected = tf == uiState.selectedTimeframe
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) gradient.startColor else TerminalSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) gradient.startColor else TerminalSlateBorder
                                    ),
                                    modifier = Modifier.clickable { terminalViewModel.selectTimeframe(tf) }
                                ) {
                                    Text(
                                        text = tf.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TerminalSlatePrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Feature Toggles: 2-Point Analyzer & NIFTY 50 Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.isDualPointerMode,
                                onClick = { terminalViewModel.toggleDualPointerMode() },
                                label = {
                                    Text(
                                        text = "Measure / Ruler",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TerminalPurplePointer,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = uiState.isBenchmarkOverlayEnabled,
                                onClick = { terminalViewModel.toggleBenchmarkOverlay() },
                                label = {
                                    Text(
                                        text = "vs NIFTY 50",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TerminalAmberBenchmark,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 5. Interactive Chart Canvas & Floating Analytics Pill
                item {
                    val points = uiState.activeQuote?.chartPoints ?: emptyList()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (uiState.isLoadingChart) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = TerminalAccentRoyal,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Streaming live ticks...",
                                            fontSize = 12.sp,
                                            color = TerminalSlateSecondary
                                        )
                                    }
                                }
                            } else if (uiState.chartErrorMessage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = uiState.chartErrorMessage ?: "Failed to load chart",
                                            fontSize = 13.sp,
                                            color = TerminalCrimsonNegative,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else if (points.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No trading series available for this timeframe.",
                                        fontSize = 13.sp,
                                        color = TerminalSlateSecondary
                                    )
                                }
                            } else {
                                // RENDER MODES
                                if (uiState.isBenchmarkOverlayEnabled && uiState.benchmarkComparisonPoints.isNotEmpty()) {
                                    BenchmarkOverlayChart(
                                        comparisonPoints = uiState.benchmarkComparisonPoints,
                                        targetSymbol = uiState.selectedSymbol,
                                        benchmarkSymbol = uiState.benchmarkSymbol
                                    )
                                } else if (uiState.isDualPointerMode) {
                                    DualPointerAnalyzerChart(
                                        points = points,
                                        pointerA = uiState.pointerA,
                                        pointerB = uiState.pointerB,
                                        currency = uiState.activeQuote?.currency ?: "INR",
                                        onPointerAChanged = { terminalViewModel.setPointerA(it) },
                                        onPointerBChanged = { terminalViewModel.setPointerB(it) },
                                        onExitRulerMode = { terminalViewModel.toggleDualPointerMode() }
                                    )
                                } else {
                                    StandardInteractiveChart(
                                        points = points,
                                        scrubbedPoint = uiState.scrubbedPoint,
                                        isPositive = (uiState.activeQuote?.periodReturnPercent ?: 0.0) >= 0.0,
                                        currency = uiState.activeQuote?.currency ?: "INR",
                                        onScrub = { terminalViewModel.setScrubbedPoint(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Institutional Stats Matrix Grid
                item {
                    val quote = uiState.activeQuote
                    if (quote != null) {
                        InstitutionalStatsCard(quote = quote)
                    }
                }
            }
        }
    }

    if (showAppearanceSheet) {
        AppearanceSettingsSheet(
            themeManager = themeManager,
            onDismiss = { showAppearanceSheet = false }
        )
    }
}
