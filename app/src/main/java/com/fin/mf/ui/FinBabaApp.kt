package com.fin.mf.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.SectorExposure
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

// --- Elevated White & Royal Indigo Institutional Palette ---
private val FinBgWhite = Color(0xFFF8FAFC)
private val FinSurfaceWhite = Color(0xFFFFFFFF)
private val FinPrimaryIndigo = Color(0xFF1E3A8A)
private val FinAccentRoyal = Color(0xFF2563EB)
private val FinEmeraldPositive = Color(0xFF059669)
private val FinEmeraldSoft = Color(0xFFECFDF5)
private val FinCrimsonNegative = Color(0xFFDC2626)
private val FinCrimsonSoft = Color(0xFFFEF2F2)
private val FinAmberWarning = Color(0xFFD97706)
private val FinAmberSoft = Color(0xFFFFFBEB)
private val FinSlateTextPrimary = Color(0xFF0F172A)
private val FinSlateTextSecondary = Color(0xFF475569)
private val FinSlateBorder = Color(0xFFE2E8F0)

private val FinBabaLightColorScheme = lightColorScheme(
    primary = FinPrimaryIndigo,
    onPrimary = Color.White,
    secondary = FinAccentRoyal,
    onSecondary = Color.White,
    background = FinBgWhite,
    onBackground = FinSlateTextPrimary,
    surface = FinSurfaceWhite,
    onSurface = FinSlateTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = FinSlateTextSecondary
)

private enum class FinBabaTab(val title: String) {
    VAULT("Vault"),
    FUNDS("20 Schemes"),
    XRAY("X-Ray Overlap"),
    DEEP_DIVE("NAV & Edit"),
    SIP_ARCHITECT("SIP Lab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinBabaApp(
    portfolioViewModel: PortfolioViewModel = viewModel()
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    var activeTab by rememberSaveable { mutableStateOf(FinBabaTab.VAULT) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = FinBabaLightColorScheme) {
        Scaffold(
            containerColor = FinBgWhite,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FinSurfaceWhite,
                        titleContentColor = FinSlateTextPrimary
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FinPrimaryIndigo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "FinBaba Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FinBaba Vault",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FinSlateTextPrimary
                                )
                                Text(
                                    text = "Institutional MF Analytics • AMFI Live",
                                    fontSize = 11.sp,
                                    color = FinSlateTextSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { portfolioViewModel.togglePrivacyMask() }) {
                            Icon(
                                imageVector = if (uiState.isPrivacyMasked) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "Toggle Privacy Mask",
                                tint = FinSlateTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { portfolioViewModel.syncAllLiveNavs() },
                            enabled = !uiState.isSyncingNavs
                        ) {
                            if (uiState.isSyncingNavs) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = FinAccentRoyal
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Live NAVs",
                                    tint = FinAccentRoyal
                                )
                            }
                        }
                        IconButton(onClick = { showResetConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Reset to Seed",
                                tint = FinSlateTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.shadow(elevation = 4.dp, spotColor = Color(0x1A0F172A))
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = FinSurfaceWhite,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(elevation = 12.dp, spotColor = Color(0x260F172A))
                ) {
                    val tabs = listOf(
                        Triple(FinBabaTab.VAULT, Icons.Default.PieChart, "Vault"),
                        Triple(FinBabaTab.FUNDS, Icons.Default.AccountBalance, "Schemes"),
                        Triple(FinBabaTab.XRAY, Icons.Default.Layers, "X-Ray"),
                        Triple(FinBabaTab.DEEP_DIVE, Icons.Default.AutoGraph, "Deep-Dive"),
                        Triple(FinBabaTab.SIP_ARCHITECT, Icons.Default.Analytics, "SIP Lab")
                    )
                    tabs.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = {
                                activeTab = tab
                                if (tab == FinBabaTab.DEEP_DIVE && uiState.selectedSchemeHistory.isEmpty() && !uiState.isHistoryLoading) {
                                    uiState.selectedSchemeCode?.let { code ->
                                        portfolioViewModel.selectSchemeForDeepDive(code)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FinPrimaryIndigo,
                                selectedTextColor = FinPrimaryIndigo,
                                indicatorColor = Color(0xFFDBEAFE),
                                unselectedIconColor = FinSlateTextSecondary,
                                unselectedTextColor = FinSlateTextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Live Sync Status Banner
                AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                    uiState.statusBannerMessage?.let { message ->
                        Surface(
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = FinAccentRoyal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    fontSize = 12.sp,
                                    color = FinPrimaryIndigo,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { portfolioViewModel.dismissStatusBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = FinSlateTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = FinPrimaryIndigo)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Initializing 20-Scheme Institutional Vault...",
                                color = FinSlateTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    when (activeTab) {
                        FinBabaTab.VAULT -> {
                            VaultOverviewScreen(
                                uiState = uiState,
                                onSyncClick = { portfolioViewModel.syncAllLiveNavs() },
                                onInspectScheme = { schemeCode ->
                                    portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                    activeTab = FinBabaTab.DEEP_DIVE
                                },
                                onNavigateToXRay = { activeTab = FinBabaTab.XRAY }
                            )
                        }
                        FinBabaTab.FUNDS -> {
                            SchemesExplorerScreen(
                                uiState = uiState,
                                onSearchQueryChange = { portfolioViewModel.updateSearchQuery(it) },
                                onFilterChange = { portfolioViewModel.updateAssetClassFilter(it) },
                                onSchemeSelected = { schemeCode ->
                                    portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                    activeTab = FinBabaTab.DEEP_DIVE
                                }
                            )
                        }
                        FinBabaTab.XRAY -> {
                            LookThroughXRayScreen(
                                analytics = uiState.analytics,
                                isMasked = uiState.isPrivacyMasked,
                                onSchemeClick = { schemeCode ->
                                    portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                    activeTab = FinBabaTab.DEEP_DIVE
                                }
                            )
                        }
                        FinBabaTab.DEEP_DIVE -> {
                            SchemeDeepDiveScreen(
                                uiState = uiState,
                                onSelectScheme = { portfolioViewModel.selectSchemeForDeepDive(it) },
                                onRangeSelected = { portfolioViewModel.updateChartRange(it) },
                                onSaveHoldingUpdate = { schemeCode, units, invested ->
                                    portfolioViewModel.updateSchemePosition(schemeCode, units, invested)
                                }
                            )
                        }
                        FinBabaTab.SIP_ARCHITECT -> {
                            SipWealthArchitectScreen(
                                currentPortfolioValue = uiState.analytics?.totalCurrentValue ?: 0.0,
                                isMasked = uiState.isPrivacyMasked
                            )
                        }
                    }
                }
            }
        }

        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text(
                        text = "Restore Factory 20-Scheme Seed?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "This will overwrite portfolio_active.json with the default 20-scheme institutional seed from assets/portfolio_seed.json and re-sync live AMFI NAVs."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            portfolioViewModel.resetPortfolioToSeed()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinCrimsonNegative)
                    ) {
                        Text("Reset Portfolio")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ============================================================================
// SCREEN 1: VAULT OVERVIEW (HERO CARD, SYMMETRY BARS, EXPENSE DRAG, TOP FUNDS)
// ============================================================================
@Composable
private fun VaultOverviewScreen(
    uiState: PortfolioUiState,
    onSyncClick: () -> Unit,
    onInspectScheme: (Int) -> Unit,
    onNavigateToXRay: () -> Unit
) {
    val analytics = uiState.analytics ?: return
    val isMasked = uiState.isPrivacyMasked
    val schemes = uiState.portfolioData?.schemes.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Elevated Net Worth Hero Card
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FinAccentRoyal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.portfolioData?.portfolioName ?: "Institutional Vault",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinSlateTextSecondary
                            )
                        }
                        Surface(
                            color = if (analytics.totalGain >= 0) FinEmeraldSoft else FinCrimsonSoft,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (analytics.totalGain >= 0) {
                                        Icons.Default.TrendingUp
                                    } else {
                                        Icons.Default.TrendingDown
                                    },
                                    contentDescription = null,
                                    tint = if (analytics.totalGain >= 0) FinEmeraldPositive else FinCrimsonNegative,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatSignedPercent(analytics.totalReturnPercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.totalGain >= 0) FinEmeraldPositive else FinCrimsonNegative
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formatInr(analytics.totalCurrentValue, isMasked),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinSlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val lastSyncFormatted = remember(uiState.portfolioData?.lastSyncedEpochMs) {
                        val ms = uiState.portfolioData?.lastSyncedEpochMs ?: 0L
                        if (ms > 0L) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(ms))
                        } else {
                            "Bundled Seed"
                        }
                    }
                    Text(
                        text = "Last AMFI Sync: $lastSyncFormatted",
                        fontSize = 11.sp,
                        color = FinSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = FinSlateBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Symmetrical 3-Column Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPillar(
                            label = "Invested Principal",
                            value = formatInr(analytics.totalInvested, isMasked),
                            valueColor = FinSlateTextPrimary
                        )
                        MetricPillar(
                            label = "Net Capital Gain",
                            value = formatInr(analytics.totalGain, isMasked, showPlus = true),
                            valueColor = if (analytics.totalGain >= 0) FinEmeraldPositive else FinCrimsonNegative
                        )
                        MetricPillar(
                            label = "Active Schemes",
                            value = "${schemes.size} Direct",
                            valueColor = FinPrimaryIndigo
                        )
                    }
                }
            }
        }

        // 2. Asset Class & Market-Cap Symmetry Card
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Asset & Market-Cap Symmetry",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinSlateTextPrimary
                        )
                        Text(
                            text = "View Overlap →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinAccentRoyal,
                            modifier = Modifier.clickable { onNavigateToXRay() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AllocationProportionBar(slices = analytics.assetClassBreakdown)

                    Spacer(modifier = Modifier.height(12.dp))

                    analytics.assetClassBreakdown.forEachIndexed { index, slice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(sliceColorForIndex(index))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FinSlateTextPrimary
                                )
                            }
                            Text(
                                text = "${formatInr(slice.rupeeValue, isMasked)} (${String.format(Locale.US, "%.1f%%", slice.weightPercent)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinSlateTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Institutional Fee Drag & Overlap Diagnostic Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedWhiteCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weighted TER Drag",
                            fontSize = 12.sp,
                            color = FinSlateTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f%% / yr", analytics.weightedExpenseRatio),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FinPrimaryIndigo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cost: ${formatInr(analytics.annualFeeDragRupees, isMasked)}/yr",
                            fontSize = 11.sp,
                            color = FinSlateTextSecondary
                        )
                    }
                }

                ElevatedWhiteCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToXRay() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stock Overlap Alert",
                                fontSize = 12.sp,
                                color = FinSlateTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = FinAmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${analytics.highOverlapStocksCount} Stocks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FinAmberWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Held in 3+ mutual funds",
                            fontSize = 11.sp,
                            color = FinSlateTextSecondary
                        )
                    }
                }
            }
        }

        // 4. Top 5 Holdings by Portfolio Valuation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Largest Scheme Positions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinSlateTextPrimary
                )
                OutlinedButton(
                    onClick = onSyncClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Live NAVs", fontSize = 12.sp)
                }
            }
        }

        items(
            items = schemes.sortedByDescending { it.currentValue }.take(6),
            key = { it.schemeCode }
        ) { scheme ->
            SchemeSummaryCard(
                scheme = scheme,
                isMasked = isMasked,
                onClick = { onInspectScheme(scheme.schemeCode) }
            )
        }
    }
}

// ============================================================================
// SCREEN 2: 20 SCHEMES EXPLORER (SEARCH BY FUND OR LOOK-THROUGH STOCK TICKER)
// ============================================================================
@Composable
private fun SchemesExplorerScreen(
    uiState: PortfolioUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSchemeSelected: (Int) -> Unit
) {
    val categories = listOf("All", "Equity", "Hybrid", "Debt", "Small Cap", "Mid Cap", "Large Cap", "Flexi Cap")
    val schemes = uiState.filteredSchemes

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = FinSurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp, spotColor = Color(0x120F172A))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search fund name, AMC, code, or stock (e.g. HDFCBANK)...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = FinSlateTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = FinSlateTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val selected = uiState.selectedAssetClassFilter.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinPrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Showing ${schemes.size} of ${uiState.portfolioData?.schemes?.size ?: 0} Institutional Schemes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinSlateTextSecondary
                )
            }

            items(items = schemes, key = { it.schemeCode }) { scheme ->
                SchemeSummaryCard(
                    scheme = scheme,
                    isMasked = uiState.isPrivacyMasked,
                    onClick = { onSchemeSelected(scheme.schemeCode) }
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 3: LOOK-THROUGH STOCK & SECTOR OVERLAP X-RAY
// ============================================================================
@Composable
private fun LookThroughXRayScreen(
    analytics: PortfolioAnalyticsSummary?,
    isMasked: Boolean,
    onSchemeClick: (Int) -> Unit
) {
    if (analytics == null) return
    var expandedTicker by rememberSaveable { mutableStateOf<String?>(analytics.topHoldings.firstOrNull()?.ticker) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sector Concentration Card
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Look-Through Sector Concentration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinSlateTextPrimary
                    )
                    Text(
                        text = "True underlying sector exposure aggregated across all 20 mutual funds",
                        fontSize = 12.sp,
                        color = FinSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    analytics.sectorBreakdown.take(8).forEachIndexed { idx, sector ->
                        SectorExposureRow(
                            sector = sector,
                            isMasked = isMasked,
                            barColor = sliceColorForIndex(idx)
                        )
                        if (idx < 7 && idx < analytics.sectorBreakdown.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // 2. Underlying Equity & Debt Overlap Header
        item {
            Column {
                Text(
                    text = "Underlying Stock & Bond Overlap Matrix",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinSlateTextPrimary
                )
                Text(
                    text = "Tap any stock/bond below to inspect which mutual funds hold it",
                    fontSize = 12.sp,
                    color = FinSlateTextSecondary
                )
            }
        }

        items(items = analytics.topHoldings.take(25), key = { it.ticker }) { stock ->
            StockOverlapCard(
                stock = stock,
                isExpanded = expandedTicker == stock.ticker,
                isMasked = isMasked,
                onToggleExpand = {
                    expandedTicker = if (expandedTicker == stock.ticker) null else stock.ticker
                },
                onSchemeClick = onSchemeClick
            )
        }
    }
}

@Composable
private fun SectorExposureRow(
    sector: SectorExposure,
    isMasked: Boolean,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${sector.sectorName} (${sector.stockCount} instruments)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinSlateTextPrimary
            )
            Text(
                text = "${formatInr(sector.totalRupeeValue, isMasked)} • ${String.format(Locale.US, "%.1f%%", sector.portfolioWeightPercent)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FinPrimaryIndigo
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { (sector.portfolioWeightPercent.toFloat() / 35f).coerceIn(0.04f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun StockOverlapCard(
    stock: AggregatedStockExposure,
    isExpanded: Boolean,
    isMasked: Boolean,
    onToggleExpand: () -> Unit,
    onSchemeClick: (Int) -> Unit
) {
    val isHighOverlap = stock.heldInSchemesCount >= 3

    ElevatedWhiteCard(
        modifier = Modifier.clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stock.ticker,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FinPrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isHighOverlap) FinAmberSoft else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "In ${stock.heldInSchemesCount} Funds",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighOverlap) FinAmberWarning else FinSlateTextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stock.companyName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinSlateTextPrimary
                    )
                    Text(
                        text = "${stock.sector} • ${stock.marketCapBucket}",
                        fontSize = 11.sp,
                        color = FinSlateTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatInr(stock.totalRupeeExposure, isMasked),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinSlateTextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f%% of Vault", stock.portfolioWeightPercent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinAccentRoyal
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = FinSlateBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Contributing Mutual Fund Schemes:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    stock.schemeContributions.forEach { contrib ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSchemeClick(contrib.schemeCode) }
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = contrib.schemeName,
                                fontSize = 12.sp,
                                color = FinPrimaryIndigo,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f%%", contrib.weightInSchemePercent)} (${formatInr(contrib.rupeeContribution, isMasked)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinSlateTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 4: SCHEME DEEP-DIVE (LIVE API.MFAPI.IN NAV CHART + POSITION EDITOR)
// ============================================================================
@Composable
private fun SchemeDeepDiveScreen(
    uiState: PortfolioUiState,
    onSelectScheme: (Int) -> Unit,
    onRangeSelected: (NavChartRange) -> Unit,
    onSaveHoldingUpdate: (Int, Double, Double) -> Unit
) {
    val allSchemes = uiState.portfolioData?.schemes.orEmpty()
    val scheme = uiState.selectedScheme ?: allSchemes.firstOrNull() ?: return
    var showEditModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontal Quick-Switcher across the 20 schemes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSchemes.forEach { itemScheme ->
                    val isSelected = itemScheme.schemeCode == scheme.schemeCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectScheme(itemScheme.schemeCode) },
                        label = {
                            Text(
                                text = itemScheme.schemeName.take(24) + "...",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FinPrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Scheme Header & Valuation Card
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "AMFI #${scheme.schemeCode} • ${scheme.category}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinPrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { showEditModal = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Position",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Units", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = scheme.schemeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinSlateTextPrimary
                    )
                    Text(
                        text = "${scheme.amc} • Benchmark: ${scheme.benchmark}",
                        fontSize = 12.sp,
                        color = FinSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPillar(
                            label = "Current Valuation",
                            value = formatInr(scheme.currentValue, uiState.isPrivacyMasked),
                            valueColor = FinSlateTextPrimary
                        )
                        MetricPillar(
                            label = "Latest NAV (${scheme.lastNavDate})",
                            value = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            valueColor = FinAccentRoyal
                        )
                        MetricPillar(
                            label = "Total Return",
                            value = formatSignedPercent(scheme.absoluteReturnPercent),
                            valueColor = if (scheme.absoluteGain >= 0) FinEmeraldPositive else FinCrimsonNegative
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = FinSlateBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPillar(
                            label = "Allotted Units",
                            value = String.format(Locale.US, "%.3f", scheme.units),
                            valueColor = FinSlateTextPrimary
                        )
                        MetricPillar(
                            label = "Avg Buy NAV",
                            value = "₹${String.format(Locale.US, "%.2f", scheme.averageBuyNav)}",
                            valueColor = FinSlateTextSecondary
                        )
                        MetricPillar(
                            label = "Expense Ratio",
                            value = "${scheme.expenseRatio}%",
                            valueColor = FinSlateTextSecondary
                        )
                    }
                }
            }
        }

        // Interactive Historical NAV Chart Card (Live from api.mfapi.in)
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Historical NAV Trajectory",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinSlateTextPrimary
                            )
                            Text(
                                text = "Live series from api.mfapi.in/mf/${scheme.schemeCode}",
                                fontSize = 11.sp,
                                color = FinSlateTextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NavChartRange.entries.forEach { range ->
                                val selected = uiState.selectedChartRange == range
                                Surface(
                                    color = if (selected) FinPrimaryIndigo else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { onRangeSelected(range) }
                                ) {
                                    Text(
                                        text = range.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else FinSlateTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        uiState.isHistoryLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = FinAccentRoyal)
                            }
                        }
                        uiState.historyErrorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = uiState.historyErrorMessage,
                                        fontSize = 12.sp,
                                        color = FinCrimsonNegative
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = { onSelectScheme(scheme.schemeCode) }) {
                                        Text("Retry Live History Fetch")
                                    }
                                }
                            }
                        }
                        uiState.selectedSchemeHistory.isNotEmpty() -> {
                            InteractiveNavHistoryChart(
                                fullHistory = uiState.selectedSchemeHistory,
                                range = uiState.selectedChartRange
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = { onSelectScheme(scheme.schemeCode) }) {
                                    Text("Load Live Historical NAV Chart")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Underlying Portfolio Holdings of this Scheme
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Scheme Look-Through Constituents (${scheme.holdings.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinSlateTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    scheme.holdings.forEachIndexed { index, holding ->
                        val rupeeExposure = scheme.currentValue * (holding.weightPercent / 100.0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${holding.companyName} (${holding.ticker})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinSlateTextPrimary
                                )
                                Text(
                                    text = "${holding.sector} • ${holding.marketCapBucket}",
                                    fontSize = 11.sp,
                                    color = FinSlateTextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f%%", holding.weightPercent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinPrimaryIndigo
                                )
                                Text(
                                    text = formatInr(rupeeExposure, uiState.isPrivacyMasked),
                                    fontSize = 11.sp,
                                    color = FinSlateTextSecondary
                                )
                            }
                        }
                        if (index < scheme.holdings.lastIndex) {
                            HorizontalDivider(color = FinSlateBorder)
                        }
                    }
                }
            }
        }
    }

    if (showEditModal) {
        EditSchemeHoldingDialog(
            scheme = scheme,
            onDismiss = { showEditModal = false },
            onConfirm = { newUnits, newInvested ->
                showEditModal = false
                onSaveHoldingUpdate(scheme.schemeCode, newUnits, newInvested)
            }
        )
    }
}

@Composable
private fun InteractiveNavHistoryChart(
    fullHistory: List<MfApiNavEntry>,
    range: NavChartRange
) {
    // api.mfapi.in returns newest first (index 0); reverse slice so chart flows chronologically left-to-right
    val chronologicalPoints = remember(fullHistory, range) {
        fullHistory
            .take(range.approximateTradingDays)
            .mapNotNull { entry ->
                entry.nav.toDoubleOrNull()?.let { navVal -> Pair(entry.date, navVal) }
            }
            .reversed()
    }

    if (chronologicalPoints.size < 2) {
        Text("Insufficient historical data points.", fontSize = 12.sp, color = FinSlateTextSecondary)
        return
    }

    var scrubbedIndex by remember(chronologicalPoints) {
        mutableIntStateOf(chronologicalPoints.lastIndex)
    }

    val safeIndex = scrubbedIndex.coerceIn(0, chronologicalPoints.lastIndex)
    val selectedPoint = chronologicalPoints[safeIndex]
    val firstPoint = chronologicalPoints.first()
    val periodReturnPct = if (firstPoint.second > 0) {
        ((selectedPoint.second - firstPoint.second) / firstPoint.second) * 100.0
    } else {
        0.0
    }
    val isPositive = periodReturnPct >= 0.0
    val lineColor = if (isPositive) FinEmeraldPositive else FinCrimsonNegative

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NAV on ${selectedPoint.first}: ₹${String.format(Locale.US, "%.4f", selectedPoint.second)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FinSlateTextPrimary
                )
                Text(
                    text = "Drag across chart to scrub historical dates",
                    fontSize = 11.sp,
                    color = FinSlateTextSecondary
                )
            }
            Surface(
                color = if (isPositive) FinEmeraldSoft else FinCrimsonSoft,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${range.label}: ${formatSignedPercent(periodReturnPct)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val minNav = remember(chronologicalPoints) { chronologicalPoints.minOf { it.second } }
        val maxNav = remember(chronologicalPoints) { chronologicalPoints.maxOf { it.second } }
        val navRange = (maxNav - minNav).coerceAtLeast(0.0001)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(chronologicalPoints) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scrubbedIndex = (fraction * chronologicalPoints.lastIndex).roundToInt()
                    }
                }
                .pointerInput(chronologicalPoints) {
                    detectHorizontalDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scrubbedIndex = (fraction * chronologicalPoints.lastIndex).roundToInt()
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val verticalPadding = 12.dp.toPx()
            val usableHeight = height - (verticalPadding * 2f)

            val linePath = Path()
            val fillPath = Path()

            chronologicalPoints.forEachIndexed { idx, (_, navVal) ->
                val x = (idx.toFloat() / chronologicalPoints.lastIndex.toFloat()) * width
                val normalizedY = ((navVal - minNav) / navRange).toFloat()
                val y = height - verticalPadding - (normalizedY * usableHeight)

                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Scrubber Crosshair
            val scrubX = (safeIndex.toFloat() / chronologicalPoints.lastIndex.toFloat()) * width
            val scrubNormY = ((selectedPoint.second - minNav) / navRange).toFloat()
            val scrubY = height - verticalPadding - (scrubNormY * usableHeight)

            drawLine(
                color = FinPrimaryIndigo.copy(alpha = 0.45f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, height),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(scrubX, scrubY)
            )
            drawCircle(
                color = FinPrimaryIndigo,
                radius = 5.dp.toPx(),
                center = Offset(scrubX, scrubY)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Low: ₹${String.format(Locale.US, "%.2f", minNav)}",
                fontSize = 11.sp,
                color = FinSlateTextSecondary
            )
            Text(
                text = "High: ₹${String.format(Locale.US, "%.2f", maxNav)}",
                fontSize = 11.sp,
                color = FinSlateTextSecondary
            )
        }
    }
}

@Composable
private fun EditSchemeHoldingDialog(
    scheme: MutualFundScheme,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var unitsInput by remember { mutableStateOf(scheme.units.toString()) }
    var investedInput by remember { mutableStateOf(scheme.investedAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Position",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = scheme.schemeName,
                    fontSize = 13.sp,
                    color = FinSlateTextSecondary
                )
                OutlinedTextField(
                    value = unitsInput,
                    onValueChange = { unitsInput = it },
                    label = { Text("Allotted Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = investedInput,
                    onValueChange = { investedInput = it },
                    label = { Text("Total Invested Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text(
                    text = "Persists directly to internal storage (portfolio_active.json).",
                    fontSize = 11.sp,
                    color = FinSlateTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedUnits = unitsInput.toDoubleOrNull() ?: scheme.units
                    val parsedInvested = investedInput.toDoubleOrNull() ?: scheme.investedAmount
                    onConfirm(parsedUnits, parsedInvested)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinPrimaryIndigo)
            ) {
                Text("Save to Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// SCREEN 5: SIP WEALTH ARCHITECT (STEP-UP COMPOUNDING SIMULATOR)
// ============================================================================
@Composable
private fun SipWealthArchitectScreen(
    currentPortfolioValue: Double,
    isMasked: Boolean
) {
    var monthlySip by rememberSaveable { mutableFloatStateOf(25000f) }
    var expectedCagr by rememberSaveable { mutableFloatStateOf(14.0f) }
    var horizonYears by rememberSaveable { mutableFloatStateOf(15f) }
    var annualStepUpPct by rememberSaveable { mutableFloatStateOf(10f) }

    val years = horizonYears.roundToInt()
    val monthlyRate = (expectedCagr / 100.0) / 12.0

    // Calculate future value of existing Vault + Step-Up Monthly SIP
    val existingCorpusFutureValue = currentPortfolioValue * (1.0 + (expectedCagr / 100.0)).pow(years.toDouble())

    var sipTotalInvested = 0.0
    var sipFutureValue = 0.0
    var currentMonthlyInstallment = monthlySip.toDouble()

    for (year in 1..years) {
        for (month in 1..12) {
            sipTotalInvested += currentMonthlyInstallment
            sipFutureValue = (sipFutureValue + currentMonthlyInstallment) * (1.0 + monthlyRate)
        }
        currentMonthlyInstallment *= (1.0 + (annualStepUpPct / 100.0))
    }

    val combinedProjectedWealth = existingCorpusFutureValue + sipFutureValue
    val totalPrincipal = currentPortfolioValue + sipTotalInvested
    val totalCompoundingGain = combinedProjectedWealth - totalPrincipal

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedWhiteCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Projected Institutional Corpus ($years Years)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatInr(combinedProjectedWealth, isMasked),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinPrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val principalWeight = if (combinedProjectedWealth > 0) {
                        ((totalPrincipal / combinedProjectedWealth) * 100.0).coerceIn(5.0, 95.0)
                    } else {
                        50.0
                    }

                    AllocationProportionBar(
                        slices = listOf(
                            AllocationSlice("Principal", totalPrincipal, principalWeight),
                            AllocationSlice("Compounding Gain", totalCompoundingGain, 100.0 - principalWeight)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPillar(
                            label = "Existing Vault FV",
                            value = formatInr(existingCorpusFutureValue, isMasked),
                            valueColor = FinSlateTextPrimary
                        )
                        MetricPillar(
                            label = "Fresh SIP Principal",
                            value = formatInr(sipTotalInvested, isMasked),
                            valueColor = FinAccentRoyal
                        )
                        MetricPillar(
                            label = "Compounding Gain",
                            value = formatInr(totalCompoundingGain, isMasked),
                            valueColor = FinEmeraldPositive
                        )
                    }
                }
            }
        }

        item {
            ElevatedWhiteCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Compounding Parameters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinSlateTextPrimary
                    )

                    SliderParameterControl(
                        label = "Monthly SIP Installment",
                        valueDisplay = formatInr(monthlySip.toDouble(), false),
                        value = monthlySip,
                        valueRange = 5000f..250000f,
                        onValueChange = { monthlySip = (it / 1000f).roundToInt() * 1000f }
                    )

                    SliderParameterControl(
                        label = "Annual SIP Step-Up %",
                        valueDisplay = "${annualStepUpPct.roundToInt()}% / yr",
                        value = annualStepUpPct,
                        valueRange = 0f..25f,
                        onValueChange = { annualStepUpPct = it.roundToInt().toFloat() }
                    )

                    SliderParameterControl(
                        label = "Expected Portfolio CAGR",
                        valueDisplay = String.format(Locale.US, "%.1f%%", expectedCagr),
                        value = expectedCagr,
                        valueRange = 8f..22f,
                        onValueChange = { expectedCagr = (it * 2f).roundToInt() / 2f }
                    )

                    SliderParameterControl(
                        label = "Investment Horizon",
                        valueDisplay = "${horizonYears.roundToInt()} Years",
                        value = horizonYears,
                        valueRange = 3f..30f,
                        onValueChange = { horizonYears = it.roundToInt().toFloat() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderParameterControl(
    label: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FinSlateTextSecondary
            )
            Text(
                text = valueDisplay,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FinPrimaryIndigo
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = FinPrimaryIndigo,
                activeTrackColor = FinAccentRoyal
            )
        )
    }
}

// ============================================================================
// REUSABLE ELEVATED WHITE CARDS & FORMATTING HELPERS
// ============================================================================
@Composable
private fun SchemeSummaryCard(
    scheme: MutualFundScheme,
    isMasked: Boolean,
    onClick: () -> Unit
) {
    val isGain = scheme.absoluteGain >= 0
    ElevatedWhiteCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = scheme.assetClass,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinPrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = scheme.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinSlateTextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "NAV ₹${String.format(Locale.US, "%.2f", scheme.fallbackNav)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinSlateTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scheme.schemeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = FinSlateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FinSlateBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Value",
                        fontSize = 11.sp,
                        color = FinSlateTextSecondary
                    )
                    Text(
                        text = formatInr(scheme.currentValue, isMasked),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinSlateTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Invested",
                        fontSize = 11.sp,
                        color = FinSlateTextSecondary
                    )
                    Text(
                        text = formatInr(scheme.investedAmount, isMasked),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinSlateTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Gain / Return",
                        fontSize = 11.sp,
                        color = FinSlateTextSecondary
                    )
                    Text(
                        text = "${formatInr(scheme.absoluteGain, isMasked, showPlus = true)} (${formatSignedPercent(scheme.absoluteReturnPercent)})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGain) FinEmeraldPositive else FinCrimsonNegative
                    )
                }
            }
        }
    }
}

@Composable
private fun ElevatedWhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x1F0F172A),
                ambientColor = Color(0x140F172A)
            )
            .border(1.dp, FinSlateBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun MetricPillar(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = FinSlateTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun AllocationProportionBar(slices: List<AllocationSlice>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE2E8F0)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        slices.forEachIndexed { index, slice ->
            val weightFraction by animateFloatAsState(
                targetValue = (slice.weightPercent.toFloat() / 100f).coerceAtLeast(0.03f),
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "sliceWeight"
            )
            Box(
                modifier = Modifier
                    .weight(weightFraction)
                    .height(12.dp)
                    .background(sliceColorForIndex(index))
            )
        }
    }
}

private fun sliceColorForIndex(index: Int): Color {
    val palette = listOf(
        Color(0xFF1E3A8A),
        Color(0xFF2563EB),
        Color(0xFF059669),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFF0891B2),
        Color(0xFFDB2777),
        Color(0xFF475569)
    )
    return palette[index % palette.size]
}

private fun formatInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val formatted = formatter.format(amount)
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

private fun formatSignedPercent(pct: Double): String {
    val sign = if (pct >= 0.0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, pct)
}