package com.fin.mf.tools.terminal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fin.mf.tools.terminal.data.BenchmarkComparisonPoint
import com.fin.mf.tools.terminal.data.TerminalAssetQuote
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import com.fin.mf.tools.terminal.data.TerminalTimeframe
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// Institutional Design System Palette
private val TerminalBg = Color(0xFFF8FAFC)
private val TerminalSurface = Color(0xFFFFFFFF)
private val TerminalPrimaryIndigo = Color(0xFF1E3A8A)
private val TerminalAccentRoyal = Color(0xFF2563EB)
private val TerminalEmeraldPositive = Color(0xFF059669)
private val TerminalEmeraldSoft = Color(0xFFECFDF5)
private val TerminalCrimsonNegative = Color(0xFFDC2626)
private val TerminalCrimsonSoft = Color(0xFFFEF2F2)
private val TerminalAmberBenchmark = Color(0xFFD97706)
private val TerminalAmberSoft = Color(0xFFFFFBEB)
private val TerminalPurplePointer = Color(0xFF7C3AED)
private val TerminalSlatePrimary = Color(0xFF0F172A)
private val TerminalSlateSecondary = Color(0xFF475569)
private val TerminalSlateBorder = Color(0xFFE2E8F0)

private val TopLevelInrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}
private val TopLevelUsdFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 2
}

private val TerminalColorScheme = lightColorScheme(
    primary = TerminalPrimaryIndigo,
    onPrimary = Color.White,
    secondary = TerminalAccentRoyal,
    onSecondary = Color.White,
    background = TerminalBg,
    onBackground = TerminalSlatePrimary,
    surface = TerminalSurface,
    onSurface = TerminalSlatePrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketTerminalScreen(
    onNavigateBackToHub: () -> Unit,
    terminalViewModel: MarketTerminalViewModel = viewModel()
) {
    val uiState by terminalViewModel.uiState.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

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
                                tint = TerminalSlatePrimary
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
                                    color = TerminalAccentRoyal.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "TOOL #2",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerminalAccentRoyal,
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
                                    color = TerminalAccentRoyal
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = TerminalAccentRoyal
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
                                focusedBorderColor = TerminalAccentRoyal,
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
                                                color = TerminalAccentRoyal
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

                // 2. Watchlist & Benchmark Quick-Picker Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(uiState.watchlist, key = { it.symbol }) { stock ->
                            val isSelected = stock.symbol.equals(uiState.selectedSymbol, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { terminalViewModel.selectSymbol(stock.symbol) },
                                label = {
                                    Text(
                                        text = stock.name.take(16),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TerminalPrimaryIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = TerminalSurface,
                                    labelColor = TerminalSlatePrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) TerminalPrimaryIndigo else TerminalSlateBorder
                                )
                            )
                        }
                    }
                }

                // 3. Hero Quote & Day Range Needle Meter
                item {
                    val quote = uiState.activeQuote
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header Symbol + Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = quote?.longName ?: uiState.selectedSymbol,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TerminalSlatePrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${quote?.symbol ?: uiState.selectedSymbol} • ${quote?.currency ?: "INR"}",
                                        fontSize = 12.sp,
                                        color = TerminalSlateSecondary
                                    )
                                }

                                if (quote != null) {
                                    val isPositive = quote.dayChange >= 0
                                    Surface(
                                        color = if (isPositive) TerminalEmeraldSoft else TerminalCrimsonSoft,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${if (isPositive) "+" else ""}${roundTwo(quote.dayChangePercent)}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Big Price display (or scrubbed price)
                            val displayPrice = uiState.scrubbedPoint?.price
                                ?: quote?.regularMarketPrice
                                ?: 0.0
                            val currency = quote?.currency ?: "INR"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = formatAssetPrice(displayPrice, currency),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TerminalSlatePrimary
                                    )
                                    if (uiState.scrubbedPoint != null) {
                                        Text(
                                            text = "Scrubbed at ${uiState.scrubbedPoint!!.dateFormatted}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TerminalAccentRoyal
                                        )
                                    } else if (quote != null) {
                                        val isPositive = quote.dayChange >= 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${formatAssetPrice(quote.dayChange, currency)} Today",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
                                        )
                                    }
                                }

                                if (quote != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${uiState.selectedTimeframe.label} Return",
                                            fontSize = 11.sp,
                                            color = TerminalSlateSecondary
                                        )
                                        val periodReturn = quote.periodReturnPercent
                                        val isPeriodPos = periodReturn >= 0
                                        Text(
                                            text = "${if (isPeriodPos) "+" else ""}${roundTwo(periodReturn)}%",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPeriodPos) TerminalEmeraldPositive else TerminalCrimsonNegative
                                        )
                                    }
                                }
                            }

                            if (quote != null && quote.dayHigh > quote.dayLow) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = TerminalSlateBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Intraday Range Meter with marker needle
                                RangeMeter(
                                    label = "Today's Range",
                                    current = quote.regularMarketPrice,
                                    low = quote.dayLow,
                                    high = quote.dayHigh,
                                    currency = currency
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 52-Week Range Meter
                                if (quote.fiftyTwoWeekHigh > quote.fiftyTwoWeekLow) {
                                    RangeMeter(
                                        label = "52-Week Range",
                                        current = quote.regularMarketPrice,
                                        low = quote.fiftyTwoWeekLow,
                                        high = quote.fiftyTwoWeekHigh,
                                        currency = currency
                                    )
                                }
                            }
                        }
                    }
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
                                    color = if (isSelected) TerminalPrimaryIndigo else TerminalSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) TerminalPrimaryIndigo else TerminalSlateBorder
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
                                        text = "2-Point Analyzer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
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
                                    // MODE A: Normalized Benchmark Overlay
                                    BenchmarkOverlayChart(
                                        comparisonPoints = uiState.benchmarkComparisonPoints,
                                        targetSymbol = uiState.selectedSymbol,
                                        benchmarkSymbol = uiState.benchmarkSymbol
                                    )
                                } else if (uiState.isDualPointerMode) {
                                    // MODE B: 2-Point Analyzer
                                    DualPointerAnalyzerChart(
                                        points = points,
                                        pointerA = uiState.pointerA,
                                        pointerB = uiState.pointerB,
                                        currency = uiState.activeQuote?.currency ?: "INR",
                                        onPointerAChanged = { terminalViewModel.setPointerA(it) },
                                        onPointerBChanged = { terminalViewModel.setPointerB(it) }
                                    )
                                } else {
                                    // MODE C: Standard Scrubbable Single-Series Chart
                                    StandardInteractiveChart(
                                        points = points,
                                        scrubbedPoint = uiState.scrubbedPoint,
                                        isPositive = (uiState.activeQuote?.periodReturnPercent ?: 0.0) >= 0.0,
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Trading & Valuation Metrics",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerminalSlatePrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Prev Close",
                                        value = formatAssetPrice(quote.previousClose, quote.currency)
                                    )
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Open",
                                        value = formatAssetPrice(quote.open, quote.currency)
                                    )
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Volume",
                                        value = formatCompactVolume(quote.volume)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = TerminalSlateBorder.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Day High",
                                        value = formatAssetPrice(quote.dayHigh, quote.currency)
                                    )
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Day Low",
                                        value = formatAssetPrice(quote.dayLow, quote.currency)
                                    )
                                    StatCell(
                                        modifier = Modifier.weight(1f),
                                        label = "Exchange",
                                        value = quote.exchange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard Interactive Chart with smooth line, gradient fill, and drag scrub crosshair.
 */
@Composable
private fun StandardInteractiveChart(
    points: List<TerminalChartPoint>,
    scrubbedPoint: TerminalChartPoint?,
    isPositive: Boolean,
    onScrub: (TerminalChartPoint?) -> Unit
) {
    val lineColor = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
    val gradientTop = if (isPositive) TerminalEmeraldPositive.copy(alpha = 0.28f) else TerminalCrimsonNegative.copy(alpha = 0.28f)
    val gradientBottom = Color.Transparent

    var touchX by remember { mutableFloatStateOf(-1f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        },
                        onDragEnd = {
                            touchX = -1f
                            onScrub(null)
                        },
                        onDragCancel = {
                            touchX = -1f
                            onScrub(null)
                        }
                    )
                }
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val minPrice = points.minOf { it.price }
                val maxPrice = points.maxOf { it.price }
                val priceRange = max(maxPrice - minPrice, 0.01)

                val w = size.width
                val h = size.height
                val topPadding = 16f
                val bottomPadding = 20f
                val plotHeight = h - topPadding - bottomPadding

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (points.size - 1)) * w
                    val normalizedY = ((pt.price - minPrice) / priceRange).toFloat()
                    val y = h - bottomPadding - (normalizedY * plotHeight)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h - bottomPadding)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(w, h - bottomPadding)
                fillPath.close()

                // Draw Area Gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientTop, gradientBottom),
                        startY = topPadding,
                        endY = h - bottomPadding
                    )
                )

                // Draw Primary Stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Crosshair Scrubber if active
                if (touchX >= 0f && scrubbedPoint != null) {
                    val clampedX = touchX.coerceIn(0f, w)
                    drawLine(
                        color = TerminalSlateSecondary.copy(alpha = 0.5f),
                        start = Offset(clampedX, topPadding),
                        end = Offset(clampedX, h - bottomPadding),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )

                    // Find point coordinate
                    val idx = points.indexOf(scrubbedPoint)
                    if (idx != -1) {
                        val ptX = (idx.toFloat() / (points.size - 1)) * w
                        val normY = ((scrubbedPoint.price - minPrice) / priceRange).toFloat()
                        val ptY = h - bottomPadding - (normY * plotHeight)

                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                    }
                }
            }
        }

        // Timeline extremes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.firstOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
            Text(
                text = points.lastOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
        }
    }
}

/**
 * 2-Point Delta Analyzer Chart: allows dual marker placement and computes absolute delta & annualized CAGR.
 */
@Composable
private fun DualPointerAnalyzerChart(
    points: List<TerminalChartPoint>,
    pointerA: TerminalChartPoint?,
    pointerB: TerminalChartPoint?,
    currency: String,
    onPointerAChanged: (TerminalChartPoint) -> Unit,
    onPointerBChanged: (TerminalChartPoint) -> Unit
) {
    val ptA = pointerA ?: points.firstOrNull() ?: return
    val ptB = pointerB ?: points.lastOrNull() ?: return

    val idxA = points.indexOf(ptA).coerceAtLeast(0)
    val idxB = points.indexOf(ptB).coerceAtLeast(0)

    val lowerIdx = min(idxA, idxB)
    val upperIdx = max(idxA, idxB)

    val startPt = points[lowerIdx]
    val endPt = points[upperIdx]

    val deltaPrice = endPt.price - startPt.price
    val absReturnPct = if (startPt.price > 0) (deltaPrice / startPt.price) * 100.0 else 0.0

    // Compute CAGR if span >= 365 days
    val spanMs = max(0L, endPt.epochMs - startPt.epochMs)
    val spanDays = (spanMs / (1000.0 * 60.0 * 60.0 * 24.0)).roundToInt()
    val cagr = if (spanDays >= 365 && startPt.price > 0 && endPt.price > 0) {
        val years = spanDays / 365.25
        ((endPt.price / startPt.price).pow(1.0 / years) - 1.0) * 100.0
    } else null

    Column(modifier = Modifier.fillMaxWidth()) {
        // Floating comparison banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TerminalBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(TerminalAccentRoyal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Point A (${startPt.dateFormatted})",
                                fontSize = 11.sp,
                                color = TerminalSlateSecondary
                            )
                        }
                        Text(
                            text = formatAssetPrice(startPt.price, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlatePrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(TerminalPurplePointer)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Point B (${endPt.dateFormatted})",
                                fontSize = 11.sp,
                                color = TerminalSlateSecondary
                            )
                        }
                        Text(
                            text = formatAssetPrice(endPt.price, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlatePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TerminalSlateBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDeltaPos = deltaPrice >= 0
                    Column {
                        Text("Absolute Delta", fontSize = 11.sp, color = TerminalSlateSecondary)
                        Text(
                            text = "${if (isDeltaPos) "+" else ""}${roundTwo(absReturnPct)}% (${formatAssetPrice(deltaPrice, currency, showPlus = true)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeltaPos) TerminalEmeraldPositive else TerminalCrimsonNegative
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (cagr != null) "Annualized CAGR" else "Span Duration",
                            fontSize = 11.sp,
                            color = TerminalSlateSecondary
                        )
                        Text(
                            text = if (cagr != null) {
                                "${if (cagr >= 0) "+" else ""}${roundTwo(cagr)}% p.a."
                            } else {
                                "$spanDays days"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cagr != null && cagr >= 0) TerminalEmeraldPositive else TerminalSlatePrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Pointer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val minPrice = points.minOf { it.price }
                val maxPrice = points.maxOf { it.price }
                val priceRange = max(maxPrice - minPrice, 0.01)

                val w = size.width
                val h = size.height
                val topPadding = 12f
                val bottomPadding = 16f
                val plotHeight = h - topPadding - bottomPadding

                val xA = (lowerIdx.toFloat() / (points.size - 1)) * w
                val xB = (upperIdx.toFloat() / (points.size - 1)) * w

                // Draw highlighted region between Point A and Point B
                drawRect(
                    color = TerminalAccentRoyal.copy(alpha = 0.08f),
                    topLeft = Offset(xA, topPadding),
                    size = androidx.compose.ui.geometry.Size(xB - xA, plotHeight)
                )

                // Draw base line
                val path = Path()
                points.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (points.size - 1)) * w
                    val normalizedY = ((pt.price - minPrice) / priceRange).toFloat()
                    val y = h - bottomPadding - (normalizedY * plotHeight)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = TerminalSlateSecondary.copy(alpha = 0.45f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Vertical Marker A
                drawLine(
                    color = TerminalAccentRoyal,
                    start = Offset(xA, topPadding),
                    end = Offset(xA, h - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                // Vertical Marker B
                drawLine(
                    color = TerminalPurplePointer,
                    start = Offset(xB, topPadding),
                    end = Offset(xB, h - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dual Range Slider to move Point A and Point B cleanly
        var sliderPosition by remember(lowerIdx, upperIdx) {
            mutableStateOf(lowerIdx.toFloat()..upperIdx.toFloat())
        }

        RangeSlider(
            value = sliderPosition,
            onValueChange = { range ->
                sliderPosition = range
                val newA = range.start.roundToInt().coerceIn(0, points.size - 1)
                val newB = range.endInclusive.roundToInt().coerceIn(0, points.size - 1)
                onPointerAChanged(points[newA])
                onPointerBChanged(points[newB])
            },
            valueRange = 0f..(points.size - 1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = TerminalAccentRoyal,
                activeTrackColor = TerminalAccentRoyal,
                inactiveTrackColor = TerminalSlateBorder
            )
        )
    }
}

/**
 * Normalized Benchmark Overlay Chart: plots relative % return of target stock vs NIFTY 50.
 */
@Composable
private fun BenchmarkOverlayChart(
    comparisonPoints: List<BenchmarkComparisonPoint>,
    targetSymbol: String,
    benchmarkSymbol: String
) {
    val lastPoint = comparisonPoints.lastOrNull() ?: return
    val alpha = lastPoint.alphaSpread
    val isOutperforming = alpha >= 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend & Alpha Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TerminalAccentRoyal)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = targetSymbol.take(12),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalSlatePrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TerminalAmberBenchmark)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = benchmarkSymbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalSlatePrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOutperforming) TerminalEmeraldSoft else TerminalCrimsonSoft
            ) {
                Text(
                    text = "Alpha: ${if (isOutperforming) "+" else ""}${roundTwo(alpha)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOutperforming) TerminalEmeraldPositive else TerminalCrimsonNegative,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Normalized Curve Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (comparisonPoints.size < 2) return@Canvas

                val minPct = min(
                    comparisonPoints.minOf { it.targetPct },
                    comparisonPoints.minOf { it.benchmarkPct }
                )
                val maxPct = max(
                    comparisonPoints.maxOf { it.targetPct },
                    comparisonPoints.maxOf { it.benchmarkPct }
                )
                val rangePct = max(maxPct - minPct, 0.5)

                val w = size.width
                val h = size.height
                val topPadding = 12f
                val bottomPadding = 16f
                val plotHeight = h - topPadding - bottomPadding

                // Draw 0.0% Baseline
                val zeroNorm = ((0.0 - minPct) / rangePct).toFloat()
                if (zeroNorm in 0f..1f) {
                    val zeroY = h - bottomPadding - (zeroNorm * plotHeight)
                    drawLine(
                        color = TerminalSlateSecondary.copy(alpha = 0.35f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                val targetPath = Path()
                val benchPath = Path()

                comparisonPoints.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (comparisonPoints.size - 1)) * w

                    val normTargetY = ((pt.targetPct - minPct) / rangePct).toFloat()
                    val targetY = h - bottomPadding - (normTargetY * plotHeight)

                    val normBenchY = ((pt.benchmarkPct - minPct) / rangePct).toFloat()
                    val benchY = h - bottomPadding - (normBenchY * plotHeight)

                    if (i == 0) {
                        targetPath.moveTo(x, targetY)
                        benchPath.moveTo(x, benchY)
                    } else {
                        targetPath.lineTo(x, targetY)
                        benchPath.lineTo(x, benchY)
                    }
                }

                // Draw Benchmark Path (Amber)
                drawPath(
                    path = benchPath,
                    color = TerminalAmberBenchmark,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Target Path (Royal Blue)
                drawPath(
                    path = targetPath,
                    color = TerminalAccentRoyal,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Return summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$targetSymbol: ${if (lastPoint.targetPct >= 0) "+" else ""}${roundTwo(lastPoint.targetPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalAccentRoyal
            )
            Text(
                text = "$benchmarkSymbol: ${if (lastPoint.benchmarkPct >= 0) "+" else ""}${roundTwo(lastPoint.benchmarkPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalAmberBenchmark
            )
        }
    }
}

@Composable
private fun RangeMeter(
    label: String,
    current: Double,
    low: Double,
    high: Double,
    currency: String
) {
    val fraction = if (high > low) {
        ((current - low) / (high - low)).toFloat().coerceIn(0f, 1f)
    } else 0.5f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = TerminalSlateSecondary)
            Text(
                text = "Current: ${formatAssetPrice(current, currency)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TerminalSlatePrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TerminalSlateBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TerminalAccentRoyal)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "L: ${formatAssetPrice(low, currency)}",
                fontSize = 10.sp,
                color = TerminalSlateSecondary
            )
            Text(
                text = "H: ${formatAssetPrice(high, currency)}",
                fontSize = 10.sp,
                color = TerminalSlateSecondary
            )
        }
    }
}

@Composable
private fun StatCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = TerminalSlateSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalSlatePrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatAssetPrice(amount: Double, currency: String, showPlus: Boolean = false): String {
    val formatted = if (currency.equals("USD", ignoreCase = true)) {
        TopLevelUsdFormatter.format(amount)
    } else {
        TopLevelInrFormatter.format(amount)
    }
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

private fun formatCompactVolume(volume: Long): String {
    return when {
        volume >= 10_000_000 -> "${roundTwo(volume / 10_000_000.0)} Cr"
        volume >= 100_000 -> "${roundTwo(volume / 100_000.0)} L"
        volume >= 1_000 -> "${roundTwo(volume / 1_000.0)} K"
        else -> volume.toString()
    }
}

private fun roundTwo(num: Double): Double {
    return (num * 100.0).roundToInt() / 100.0
}
