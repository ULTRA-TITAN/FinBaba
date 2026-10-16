package com.fin.mf.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.SectorExposure
import com.fin.mf.data.UnderlyingHolding
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.components.NavChartComponent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Institutional palette constants
private val WsBgWhite = Color(0xFFF8FAFC)
private val WsSurfaceWhite = Color(0xFFFFFFFF)
private val WsPrimaryIndigo = Color(0xFF1E3A8A)
private val WsAccentRoyal = Color(0xFF2563EB)
private val WsEmeraldPositive = Color(0xFF059669)
private val WsEmeraldSoft = Color(0xFFECFDF5)
private val WsCrimsonNegative = Color(0xFFDC2626)
private val WsCrimsonSoft = Color(0xFFFEF2F2)
private val WsAmberWarning = Color(0xFFD97706)
private val WsAmberSoft = Color(0xFFFFFBEB)
private val WsSlateTextPrimary = Color(0xFF0F172A)
private val WsSlateTextSecondary = Color(0xFF475569)
private val WsSlateBorder = Color(0xFFE2E8F0)

// Top-level cached currency formatter for 60/120 FPS recomposition performance
private val TopLevelInrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private enum class WorkspaceTab(val index: Int, val label: String) {
    OVERVIEW(0, "Overview"),
    PORTFOLIO(1, "Portfolio"),
    XRAY(2, "X-Ray"),
    MF_HUNTER(3, "MF-Hunter"),
    COMPARE(4, "Compare");

    companion object {
        fun fromIndex(index: Int): WorkspaceTab =
            entries.find { it.index == index } ?: OVERVIEW
    }
}

/**
 * Tool #1 Workspace: Mutual Fund Portfolio & Look-Through Tracker.
 * Integrates:
 * 1. Universal AMFI Search & Custom Portfolio CRUD (Add/Delete schemes, CSV import/export).
 * 2. Dedicated NavChartComponent with timeframe chips, custom range slider, 2-point delta analyzer, fullscreen mode, and zoomable charts.
 * 3. MF-Hunter (renamed from Deep-Dive) with spacious search, smart category filters, and Top 10/15/All look-through constituents.
 * 4. Multi-Fund Comparison tab (base-100 normalized chart up to 5 funds, custom calendar date-to-date picker, factor matrix).
 * 5. Settings gear icon in top right with feature toggles and data management.
 * 6. Clean 2x2 grid SchemeSummaryCard and 60fps card elevation performance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioTrackerWorkspace(
    portfolioViewModel: PortfolioViewModel,
    initialTabIndex: Int = 0,
    onNavigateBackToHub: () -> Unit
) {
    val uiState by portfolioViewModel.uiState.collectAsState()

    var activeTab by rememberSaveable(initialTabIndex) {
        mutableStateOf(WorkspaceTab.fromIndex(initialTabIndex))
    }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAmfiAddDialog by rememberSaveable { mutableStateOf(false) }
    var showCsvDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var schemeToDelete by remember { mutableStateOf<MutualFundScheme?>(null) }

    LaunchedEffect(activeTab) {
        if (activeTab == WorkspaceTab.MF_HUNTER &&
            uiState.selectedSchemeHistory.isEmpty() &&
            !uiState.isHistoryLoading
        ) {
            uiState.selectedSchemeCode?.let { code ->
                portfolioViewModel.selectSchemeForDeepDive(code)
            }
        }
    }

    Scaffold(
        containerColor = WsBgWhite,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WsSurfaceWhite,
                    titleContentColor = WsSlateTextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToHub) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Studio Hub",
                            tint = WsPrimaryIndigo
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "MF Portfolio & Look-Through",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsSlateTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.portfolioData?.schemes?.size ?: 0} Active Schemes • Live AMFI",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                },
                actions = {
                    // Privacy Mask Toggle
                    IconButton(onClick = { portfolioViewModel.togglePrivacyMask() }) {
                        Icon(
                            imageVector = if (uiState.isPrivacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Privacy Mask",
                            tint = WsSlateTextSecondary
                        )
                    }
                    // Add Scheme via AMFI Search
                    IconButton(onClick = { showAmfiAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Scheme",
                            tint = WsPrimaryIndigo
                        )
                    }
                    // Parallel Live NAV Sync
                    IconButton(
                        onClick = { portfolioViewModel.syncAllLiveNavs() },
                        enabled = !uiState.isSyncingNavs
                    ) {
                        if (uiState.isSyncingNavs) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = WsAccentRoyal
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Live NAVs",
                                tint = WsAccentRoyal
                            )
                        }
                    }
                    // Settings Gear Icon
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Controls",
                            tint = WsPrimaryIndigo
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = WsSurfaceWhite,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(WorkspaceTab.OVERVIEW, Icons.Default.PieChart, "Overview"),
                    Triple(WorkspaceTab.PORTFOLIO, Icons.Default.Layers, "Portfolio"),
                    Triple(WorkspaceTab.XRAY, Icons.Default.Analytics, "X-Ray"),
                    Triple(WorkspaceTab.MF_HUNTER, Icons.Default.AutoGraph, "MF-Hunter"),
                    Triple(WorkspaceTab.COMPARE, Icons.AutoMirrored.Filled.CompareArrows, "Compare")
                )
                navItems.forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WsPrimaryIndigo,
                            selectedTextColor = WsPrimaryIndigo,
                            indicatorColor = Color(0xFFDBEAFE),
                            unselectedIconColor = WsSlateTextSecondary,
                            unselectedTextColor = WsSlateTextSecondary
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
            AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                uiState.statusBannerMessage?.let { message ->
                    Surface(
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WsAccentRoyal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = WsPrimaryIndigo,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { portfolioViewModel.dismissStatusBanner() },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = WsSlateTextSecondary,
                                    modifier = Modifier.size(14.dp)
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
                        CircularProgressIndicator(color = WsPrimaryIndigo)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Institutional Portfolio...",
                            color = WsSlateTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                when (activeTab) {
                    WorkspaceTab.OVERVIEW -> {
                        WorkspaceOverviewTab(
                            uiState = uiState,
                            onSyncClick = { portfolioViewModel.syncAllLiveNavs() },
                            onInspectScheme = { schemeCode ->
                                portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                activeTab = WorkspaceTab.MF_HUNTER
                            },
                            onNavigateToXRay = { activeTab = WorkspaceTab.XRAY },
                            onDeleteScheme = { schemeToDelete = it }
                        )
                    }
                    WorkspaceTab.PORTFOLIO -> {
                        WorkspacePortfolioTab(
                            uiState = uiState,
                            onSearchQueryChange = { portfolioViewModel.updateSearchQuery(it) },
                            onFilterChange = { portfolioViewModel.updateAssetClassFilter(it) },
                            onSchemeSelected = { schemeCode ->
                                portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                activeTab = WorkspaceTab.MF_HUNTER
                            },
                            onDeleteScheme = { schemeToDelete = it }
                        )
                    }
                    WorkspaceTab.XRAY -> {
                        WorkspaceXRayTab(
                            analytics = uiState.analytics,
                            isMasked = uiState.isPrivacyMasked,
                            onSchemeClick = { schemeCode ->
                                portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                activeTab = WorkspaceTab.MF_HUNTER
                            }
                        )
                    }
                    WorkspaceTab.MF_HUNTER -> {
                        WorkspaceMfHunterTab(
                            portfolioViewModel = portfolioViewModel,
                            onSaveHoldingUpdate = { schemeCode, units, invested ->
                                portfolioViewModel.updateSchemePosition(schemeCode, units, invested)
                            }
                        )
                    }
                    WorkspaceTab.COMPARE -> {
                        FundComparisonTab(portfolioViewModel = portfolioViewModel)
                    }
                }
            }
        }
    }

    // Settings Modal Dialog (Gear Icon)
    if (showSettingsDialog) {
        SettingsModalDialog(
            uiState = uiState,
            portfolioViewModel = portfolioViewModel,
            onDismiss = { showSettingsDialog = false },
            onOpenCsv = {
                showSettingsDialog = false
                showCsvDialog = true
            },
            onPromptResetSeed = {
                showSettingsDialog = false
                showResetConfirmDialog = true
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(text = "Restore Factory 20-Scheme Seed?", fontWeight = FontWeight.Bold) },
            text = { Text("This will overwrite portfolio_active.json with the factory 20-scheme seed from assets and trigger a fresh AMFI NAV sync.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        portfolioViewModel.resetPortfolioToSeed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WsCrimsonNegative)
                ) {
                    Text("Reset Portfolio")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Scheme Confirmation Dialog
    schemeToDelete?.let { scheme ->
        AlertDialog(
            onDismissRequest = { schemeToDelete = null },
            title = { Text(text = "Delete Scheme Position?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${scheme.schemeName}' (AMFI #${scheme.schemeCode}) from your portfolio?") },
            confirmButton = {
                Button(
                    onClick = {
                        portfolioViewModel.deleteScheme(scheme.schemeCode)
                        schemeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WsCrimsonNegative)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { schemeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Add Scheme Dialog (AMFI Search)
    if (showAmfiAddDialog) {
        AmfiAddSchemeModal(
            portfolioViewModel = portfolioViewModel,
            onDismiss = { showAmfiAddDialog = false }
        )
    }

    // CSV Manager Dialog
    if (showCsvDialog) {
        CsvManagerModal(
            portfolioViewModel = portfolioViewModel,
            onDismiss = { showCsvDialog = false }
        )
    }
}

/**
 * Tab #1: Executive Overview with Equity Valuation, Look-Through Asset Slices, and Overlap Alerts.
 */
@Composable
private fun WorkspaceOverviewTab(
    uiState: PortfolioUiState,
    onSyncClick: () -> Unit,
    onInspectScheme: (Int) -> Unit,
    onNavigateToXRay: () -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit
) {
    val analytics = uiState.analytics ?: return
    val isMasked = uiState.isPrivacyMasked
    val schemes = uiState.portfolioData?.schemes.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WsElevatedCard {
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
                                tint = WsAccentRoyal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.portfolioData?.portfolioName ?: "Institutional Vault",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WsSlateTextSecondary
                            )
                        }
                        Surface(
                            color = if (analytics.totalGain >= 0) WsEmeraldSoft else WsCrimsonSoft,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (analytics.totalGain >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (analytics.totalGain >= 0) WsEmeraldPositive else WsCrimsonNegative,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatWsPercent(analytics.totalReturnPercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.totalGain >= 0) WsEmeraldPositive else WsCrimsonNegative
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formatWsInr(analytics.totalCurrentValue, isMasked),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
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
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = WsSlateBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WsMetricPillar(
                            label = "Invested Principal",
                            value = formatWsInr(analytics.totalInvested, isMasked),
                            valueColor = WsSlateTextPrimary
                        )
                        WsMetricPillar(
                            label = "Net Capital Gain",
                            value = formatWsInr(analytics.totalGain, isMasked, showPlus = true),
                            valueColor = if (analytics.totalGain >= 0) WsEmeraldPositive else WsCrimsonNegative
                        )
                        WsMetricPillar(
                            label = "Active Schemes",
                            value = "${schemes.size} Schemes",
                            valueColor = WsPrimaryIndigo
                        )
                    }
                }
            }
        }

        item {
            WsElevatedCard {
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
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = "View Overlap →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WsAccentRoyal,
                            modifier = Modifier.clickable { onNavigateToXRay() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    WsAllocationBar(slices = analytics.assetClassBreakdown)

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
                                        .background(wsSliceColor(index))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WsSlateTextPrimary
                                )
                            }
                            Text(
                                text = "${formatWsInr(slice.rupeeValue, isMasked)} (${String.format(Locale.US, "%.1f%%", slice.weightPercent)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WsSlateTextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WsElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weighted TER Drag",
                            fontSize = 12.sp,
                            color = WsSlateTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f%% / yr", analytics.weightedExpenseRatio),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsPrimaryIndigo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cost: ${formatWsInr(analytics.annualFeeDragRupees, isMasked)}/yr",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }

                WsElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToXRay() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stock Overlap Alert",
                                fontSize = 12.sp,
                                color = WsSlateTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WsAmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${analytics.highOverlapStocksCount} Stocks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsAmberWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Held in 3+ mutual funds",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
            }
        }

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
                    color = WsSlateTextPrimary
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
            WsSchemeSummaryCard(
                scheme = scheme,
                isMasked = isMasked,
                onClick = { onInspectScheme(scheme.schemeCode) },
                onDelete = { onDeleteScheme(scheme) }
            )
        }
    }
}

/**
 * Tab #2: Portfolio Schemes List with Asset Class Filters and Search (Renamed from Schemes).
 */
@Composable
private fun WorkspacePortfolioTab(
    uiState: PortfolioUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSchemeSelected: (Int) -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit
) {
    val categories = listOf("All", "Equity", "Hybrid", "Debt", "Small Cap", "Mid Cap", "Large Cap", "Flexi Cap")
    val schemes = uiState.filteredSchemes

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = WsSurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search fund name, AMC, code, or stock...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = WsSlateTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = WsSlateTextSecondary
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
                                selectedContainerColor = WsPrimaryIndigo,
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
                    text = "Showing ${schemes.size} of ${uiState.portfolioData?.schemes?.size ?: 0} Active Schemes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WsSlateTextSecondary
                )
            }

            items(items = schemes, key = { it.schemeCode }) { scheme ->
                WsSchemeSummaryCard(
                    scheme = scheme,
                    isMasked = uiState.isPrivacyMasked,
                    onClick = { onSchemeSelected(scheme.schemeCode) },
                    onDelete = { onDeleteScheme(scheme) }
                )
            }
        }
    }
}

/**
 * Tab #3: Institutional X-Ray (Look-Through Sector Breakdown & Stock Overlap Matrix).
 */
@Composable
private fun WorkspaceXRayTab(
    analytics: PortfolioAnalyticsSummary?,
    isMasked: Boolean,
    onSchemeClick: (Int) -> Unit
) {
    if (analytics == null) return
    var expandedTicker by rememberSaveable {
        mutableStateOf<String?>(analytics.topHoldings.firstOrNull()?.ticker)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WsElevatedCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Look-Through Sector Concentration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "True underlying sector exposure aggregated across all portfolio funds",
                        fontSize = 12.sp,
                        color = WsSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    analytics.sectorBreakdown.take(8).forEachIndexed { idx, sector ->
                        WsSectorExposureRow(
                            sector = sector,
                            isMasked = isMasked,
                            barColor = wsSliceColor(idx)
                        )
                        if (idx < 7 && idx < analytics.sectorBreakdown.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Underlying Stock & Bond Overlap Matrix",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Text(
                    text = "Tap any stock/bond below to inspect which mutual funds hold it",
                    fontSize = 12.sp,
                    color = WsSlateTextSecondary
                )
            }
        }

        items(items = analytics.topHoldings.take(25), key = { it.ticker }) { stock ->
            WsStockOverlapCard(
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
private fun WsSectorExposureRow(
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
                color = WsSlateTextPrimary
            )
            Text(
                text = "${formatWsInr(sector.totalRupeeValue, isMasked)} • ${String.format(Locale.US, "%.1f%%", sector.portfolioWeightPercent)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WsPrimaryIndigo
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
private fun WsStockOverlapCard(
    stock: AggregatedStockExposure,
    isExpanded: Boolean,
    isMasked: Boolean,
    onToggleExpand: () -> Unit,
    onSchemeClick: (Int) -> Unit
) {
    val isHighOverlap = stock.heldInSchemesCount >= 3

    WsElevatedCard(
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
                                color = WsPrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isHighOverlap) WsAmberSoft else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "In ${stock.heldInSchemesCount} Funds",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighOverlap) WsAmberWarning else WsSlateTextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stock.companyName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "${stock.sector} • ${stock.marketCapBucket}",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatWsInr(stock.totalRupeeExposure, isMasked),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f%% of vault", stock.portfolioWeightPercent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WsPrimaryIndigo
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = WsSlateBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fund Allocations Holding ${stock.ticker}:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    stock.schemeContributions.forEach { contribution ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSchemeClick(contribution.schemeCode) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${contribution.schemeName.take(28)}...",
                                fontSize = 12.sp,
                                color = WsPrimaryIndigo,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f%%", contribution.weightInSchemePercent)} (${formatWsInr(contribution.rupeeContribution, isMasked)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = WsSlateTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab #4: MF-Hunter (Renamed from Deep-Dive).
 * Features:
 * 1. Spacious AMFI Scheme Search with smart indexing and category filter chips.
 * 2. Dedicated NavChartComponent (zoomable charts, 2-point delta analyzer, fullscreen mode).
 * 3. Individual MF Holdings with Top 10 / Top 15 / All filter toggle and Full Constituents Modal.
 */
@Composable
private fun WorkspaceMfHunterTab(
    portfolioViewModel: PortfolioViewModel,
    onSaveHoldingUpdate: (Int, Double, Double) -> Unit
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    val allSchemes = uiState.portfolioData?.schemes.orEmpty()
    val scheme = uiState.selectedScheme ?: allSchemes.firstOrNull()

    var showEditModal by remember { mutableStateOf(false) }
    var showFullHoldingsModal by remember { mutableStateOf(false) }
    var holdingsCountFilter by rememberSaveable { mutableStateOf("Top 15") }
    var holdingsTypeFilter by rememberSaveable { mutableStateOf("All Types") }
    var mfHunterSearchQuery by remember { mutableStateOf("") }
    var mfHunterFilter by rememberSaveable { mutableStateOf("All") }

    if (scheme == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No scheme selected or portfolio is empty.", fontSize = 14.sp, color = WsSlateTextSecondary)
        }
        return
    }

    val isSchemeInPortfolio = allSchemes.any { it.schemeCode == scheme.schemeCode }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Universal Spacious Search bar in MF-Hunter with Smart Indexing & Filter Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MF-Hunter Scheme Finder",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "Search across Indian mutual funds with smart alias indexing",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mfHunterSearchQuery,
                        onValueChange = {
                            mfHunterSearchQuery = it
                            portfolioViewModel.searchAmfiSchemes(it)
                        },
                        placeholder = { Text("Search by fund name, AMC, alias (e.g. 'ppfas', 'quant small', 'hdfc')...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WsPrimaryIndigo) },
                        trailingIcon = {
                            if (mfHunterSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    mfHunterSearchQuery = ""
                                    portfolioViewModel.clearAmfiSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Smart filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Direct - Growth", "Large Cap", "Mid Cap", "Small Cap", "Debt").forEach { cat ->
                            val isSel = mfHunterFilter == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { mfHunterFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsPrimaryIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Display spacious search results
                    if (mfHunterSearchQuery.isNotEmpty()) {
                        val filteredResults = remember(uiState.amfiSearchResults, mfHunterFilter) {
                            val raw = uiState.amfiSearchResults
                            if (mfHunterFilter == "All") {
                                raw
                            } else if (mfHunterFilter == "Direct - Growth") {
                                raw.filter { it.schemeName.contains("Direct", ignoreCase = true) && it.schemeName.contains("Growth", ignoreCase = true) }
                            } else {
                                raw.filter { it.schemeName.contains(mfHunterFilter, ignoreCase = true) }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when {
                            uiState.isAmfiSearching -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = WsAccentRoyal)
                                }
                            }
                            filteredResults.isNotEmpty() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredResults.take(6).forEach { result ->
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    portfolioViewModel.selectSchemeForDeepDive(result.schemeCode)
                                                    mfHunterSearchQuery = ""
                                                    portfolioViewModel.clearAmfiSearch()
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = result.schemeName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = WsSlateTextPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "AMFI Code: #${result.schemeCode}",
                                                        fontSize = 11.sp,
                                                        color = WsPrimaryIndigo,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Tap to Inspect Holdings ➔",
                                                        fontSize = 11.sp,
                                                        color = WsAccentRoyal,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "No matching schemes found. Try a different query or filter.",
                                    fontSize = 12.sp,
                                    color = WsSlateTextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal chips row of active portfolio schemes (+ external inspected scheme if active)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isSchemeInPortfolio) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = {
                            Text(
                                text = "🔍 " + scheme.schemeName.take(22) + "...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WsEmeraldPositive,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                allSchemes.forEach { itemScheme ->
                    val isSelected = isSchemeInPortfolio && itemScheme.schemeCode == scheme.schemeCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { portfolioViewModel.selectSchemeForDeepDive(itemScheme.schemeCode) },
                        label = {
                            Text(
                                text = itemScheme.schemeName.take(24) + "...",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WsPrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Scheme Overview Card
        item {
            WsElevatedCard {
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
                                color = WsPrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (isSchemeInPortfolio) {
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
                        } else {
                            Button(
                                onClick = { showEditModal = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WsEmeraldPositive)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to Portfolio", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = scheme.schemeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "${scheme.amc} • Benchmark: ${scheme.benchmark}",
                        fontSize = 12.sp,
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WsMetricPillar(
                            label = "Current Valuation",
                            value = formatWsInr(scheme.currentValue, uiState.isPrivacyMasked),
                            valueColor = WsSlateTextPrimary
                        )
                        WsMetricPillar(
                            label = "Latest NAV (${scheme.lastNavDate})",
                            value = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            valueColor = WsAccentRoyal
                        )
                        WsMetricPillar(
                            label = "Total Return",
                            value = formatWsPercent(scheme.absoluteReturnPercent),
                            valueColor = if (scheme.absoluteGain >= 0) WsEmeraldPositive else WsCrimsonNegative
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = WsSlateBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WsMetricPillar(
                            label = "Allotted Units",
                            value = String.format(Locale.US, "%.3f", scheme.units),
                            valueColor = WsSlateTextPrimary
                        )
                        WsMetricPillar(
                            label = "Avg Buy NAV",
                            value = "₹${String.format(Locale.US, "%.2f", scheme.averageBuyNav)}",
                            valueColor = WsSlateTextSecondary
                        )
                        WsMetricPillar(
                            label = "Expense Ratio",
                            value = "${scheme.expenseRatio}%",
                            valueColor = WsSlateTextSecondary
                        )
                    }
                }
            }
        }

        // Dedicated Advanced NAV Chart Component with Zoom
        item {
            NavChartComponent(
                schemeName = scheme.schemeName,
                schemeCode = scheme.schemeCode,
                fullHistory = uiState.selectedSchemeHistory,
                selectedTimeframe = uiState.selectedTimeframe,
                onTimeframeSelected = { portfolioViewModel.updateChartTimeframe(it) }
            )
        }

        // Look-Through Constituents Section: Top 10 / Top 15 / All + View Full
        item {
            WsElevatedCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Look-Through Constituents (${scheme.holdings.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "Underlying equities and debt instruments",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showFullHoldingsModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Full", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Holdings Filter Chips: Top 10, Top 15, All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Top 10", "Top 15", "All").forEach { countOpt ->
                            val isSel = holdingsCountFilter == countOpt
                            FilterChip(
                                selected = isSel,
                                onClick = { holdingsCountFilter = countOpt },
                                label = { Text(countOpt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsPrimaryIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        listOf("All Types", "Equity", "Debt & Cash").forEach { typeOpt ->
                            val isSel = holdingsTypeFilter == typeOpt
                            FilterChip(
                                selected = isSel,
                                onClick = { holdingsTypeFilter = typeOpt },
                                label = { Text(typeOpt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsAccentRoyal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered Holdings Display
                    val filteredHoldings = remember(scheme.holdings, holdingsCountFilter, holdingsTypeFilter) {
                        var list = scheme.holdings
                        if (holdingsTypeFilter == "Equity") {
                            list = list.filterNot { it.sector.contains("Debt", ignoreCase = true) || it.marketCapBucket.contains("Debt", ignoreCase = true) }
                        } else if (holdingsTypeFilter == "Debt & Cash") {
                            list = list.filter { it.sector.contains("Debt", ignoreCase = true) || it.marketCapBucket.contains("Debt", ignoreCase = true) }
                        }
                        val sorted = list.sortedByDescending { it.weightPercent }
                        when (holdingsCountFilter) {
                            "Top 10" -> sorted.take(10)
                            "Top 15" -> sorted.take(15)
                            else -> sorted
                        }
                    }

                    filteredHoldings.forEachIndexed { index, holding ->
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
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = "${holding.sector} • ${holding.marketCapBucket}",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f%%", holding.weightPercent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsPrimaryIndigo
                                )
                                Text(
                                    text = formatWsInr(rupeeExposure, uiState.isPrivacyMasked),
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                        }
                        if (index < filteredHoldings.lastIndex) {
                            HorizontalDivider(color = WsSlateBorder)
                        }
                    }
                }
            }
        }
    }

    // Edit Holding Dialog
    if (showEditModal) {
        WsEditHoldingDialog(
            scheme = scheme,
            isNewScheme = !isSchemeInPortfolio,
            onDismiss = { showEditModal = false },
            onConfirm = { newUnits, newInvested ->
                showEditModal = false
                if (isSchemeInPortfolio) {
                    onSaveHoldingUpdate(scheme.schemeCode, newUnits, newInvested)
                } else {
                    portfolioViewModel.addSearchedScheme(scheme.schemeCode, newUnits, newInvested)
                }
            }
        )
    }

    // Full Constituents Sheet Dialog
    if (showFullHoldingsModal) {
        FullHoldingsModal(
            schemeName = scheme.schemeName,
            holdings = scheme.holdings,
            schemeValue = scheme.currentValue,
            isMasked = uiState.isPrivacyMasked,
            onDismiss = { showFullHoldingsModal = false }
        )
    }
}

/**
 * 2x2 Grid Scheme Summary Card:
 * Row 1: Current Value | Net Gain %
 * Row 2: Invested | Latest NAV
 * Eliminates GPU shadow rasterization lag by utilizing native CardDefaults.cardElevation(2.dp).
 */
@Composable
private fun WsSchemeSummaryCard(
    scheme: MutualFundScheme,
    isMasked: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isGain = scheme.absoluteGain >= 0

    WsElevatedCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category, Asset Class, Delete Button
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
                            color = WsPrimaryIndigo,
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
                            color = WsSlateTextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Scheme",
                        tint = WsSlateTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scheme.schemeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = WsSlateBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Clean 2x2 Grid Layout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Current Value | Net Gain / Return
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Value",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.currentValue, isMasked),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsSlateTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Gain",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isGain) WsEmeraldPositive else WsCrimsonNegative,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formatWsPercent(scheme.absoluteReturnPercent),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGain) WsEmeraldPositive else WsCrimsonNegative
                            )
                        }
                    }
                }

                // Row 2: Invested | Latest NAV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Invested Capital",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.investedAmount, isMasked),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WsSlateTextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Latest NAV (${scheme.lastNavDate})",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsAccentRoyal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WsElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
private fun WsMetricPillar(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = WsSlateTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun WsAllocationBar(slices: List<AllocationSlice>) {
    if (slices.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        slices.forEachIndexed { index, slice ->
            val weightFloat = (slice.weightPercent.toFloat() / 100f).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(weightFloat)
                    .fillMaxSize()
                    .background(wsSliceColor(index))
            )
        }
    }
}

private fun wsSliceColor(index: Int): Color {
    val palette = listOf(
        Color(0xFF1E3A8A), // Indigo
        Color(0xFF2563EB), // Royal
        Color(0xFF059669), // Emerald
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED), // Purple
        Color(0xFFDC2626), // Crimson
        Color(0xFF0D9488), // Teal
        Color(0xFF64748B)  // Slate
    )
    return palette[index % palette.size]
}

private fun formatWsInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatted = TopLevelInrFormatter.format(amount)
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

private fun formatWsPercent(pct: Double): String {
    val rounded = (pct * 100.0).roundToInt() / 100.0
    val sign = if (rounded >= 0.0) "+" else ""
    return "$sign$rounded%"
}

@Composable
private fun WsEditHoldingDialog(
    scheme: MutualFundScheme,
    isNewScheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var unitsInput by remember { mutableStateOf(scheme.units.toString()) }
    var investedInput by remember { mutableStateOf(scheme.investedAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNewScheme) "Add Position: ${scheme.schemeName.take(20)}..." else "Edit Position: ${scheme.schemeName.take(20)}...",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AMFI #${scheme.schemeCode} • Latest NAV: ₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                    fontSize = 12.sp,
                    color = WsSlateTextSecondary
                )
                OutlinedTextField(
                    value = unitsInput,
                    onValueChange = { unitsInput = it },
                    label = { Text("Holding Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = investedInput,
                    onValueChange = { investedInput = it },
                    label = { Text("Invested Capital (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = unitsInput.toDoubleOrNull() ?: scheme.units
                    val inv = investedInput.toDoubleOrNull() ?: scheme.investedAmount
                    onConfirm(u, inv)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WsPrimaryIndigo)
            ) {
                Text(if (isNewScheme) "Add Scheme" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Full Portfolio Look-Through Constituents Modal Dialog with Reactive Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullHoldingsModal(
    schemeName: String,
    holdings: List<UnderlyingHolding>,
    schemeValue: Double,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    var filterText by remember { mutableStateOf("") }

    val filtered = remember(holdings, filterText) {
        if (filterText.isBlank()) holdings else {
            val q = filterText.trim()
            holdings.filter {
                it.companyName.contains(q, ignoreCase = true) ||
                        it.ticker.contains(q, ignoreCase = true) ||
                        it.sector.contains(q, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WsBgWhite
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Full Portfolio Constituents",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = schemeName,
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = WsPrimaryIndigo)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WsSurfaceWhite)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search stock, ticker, or sector...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WsPrimaryIndigo) },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total ${filtered.size} constituent holdings (${holdings.size} total in fund)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { holding ->
                            val rupeeContribution = schemeValue * (holding.weightPercent / 100.0)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${holding.companyName} (${holding.ticker})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsSlateTextPrimary
                                        )
                                        Text(
                                            text = "${holding.sector} • ${holding.marketCapBucket}",
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f%%", holding.weightPercent),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsPrimaryIndigo
                                        )
                                        Text(
                                            text = formatWsInr(rupeeContribution, isMasked),
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
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
}

/**
 * Settings & Studio Controls Modal Dialog (Accessible via Top Right Gear Icon).
 */
@Composable
private fun SettingsModalDialog(
    uiState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
    onOpenCsv: () -> Unit,
    onPromptResetSeed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = WsPrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Studio Settings & Tools",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Feature Toggle: Privacy Mode
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Privacy Mask Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "Obscures rupee valuations (₹ ••••••)",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isPrivacyMasked,
                            onCheckedChange = { portfolioViewModel.togglePrivacyMask() },
                            colors = SwitchDefaults.colors(checkedTrackColor = WsPrimaryIndigo)
                        )
                    }
                }

                // Data Management Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data Management",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary
                    )

                    OutlinedButton(
                        onClick = onOpenCsv,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import / Export Portfolio CSV", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onPromptResetSeed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WsCrimsonNegative)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore 20-Scheme Seed", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = WsSlateBorder)

                // App Info & Diagnostic Stats
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "FinBaba Institutional v2.1.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "API: api.mfapi.in • Package: com.fin.mf",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    Text(
                        text = "${uiState.portfolioData?.schemes?.size ?: 0} Active Schemes Persisted to Internal Storage",
                        fontSize = 11.sp,
                        color = WsAccentRoyal
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WsPrimaryIndigo)
            ) {
                Text("Done")
            }
        }
    )
}

/**
 * Universal AMFI Scheme Addition Dialog with Filter Chips and Spacious UI.
 */
@Composable
private fun AmfiAddSchemeModal(
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var selectedSchemeCode by remember { mutableStateOf<Int?>(null) }
    var selectedSchemeName by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("100.0") }
    var investedInput by remember { mutableStateOf("10000.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Mutual Fund Scheme",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                if (selectedSchemeCode == null) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            portfolioViewModel.searchAmfiSchemes(it)
                        },
                        label = { Text("Search AMFI Scheme Name / Code...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WsPrimaryIndigo) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    portfolioViewModel.clearAmfiSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Smart Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Direct - Growth", "Large Cap", "Mid Cap", "Small Cap", "Debt").forEach { cat ->
                            val isSel = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsPrimaryIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredResults = remember(uiState.amfiSearchResults, selectedCategoryFilter) {
                        val raw = uiState.amfiSearchResults
                        if (selectedCategoryFilter == "All") {
                            raw
                        } else if (selectedCategoryFilter == "Direct - Growth") {
                            raw.filter { it.schemeName.contains("Direct", ignoreCase = true) && it.schemeName.contains("Growth", ignoreCase = true) }
                        } else {
                            raw.filter { it.schemeName.contains(selectedCategoryFilter, ignoreCase = true) }
                        }
                    }

                    when {
                        uiState.isAmfiSearching -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = WsAccentRoyal)
                            }
                        }
                        filteredResults.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredResults) { res ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSchemeCode = res.schemeCode
                                                selectedSchemeName = res.schemeName
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = res.schemeName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = WsSlateTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "AMFI Code: #${res.schemeCode}",
                                                fontSize = 11.sp,
                                                color = WsPrimaryIndigo,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Type any mutual fund query (e.g. 'Parag', 'Quant', 'SBI').",
                                    fontSize = 12.sp,
                                    color = WsSlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Enter units and invested amount
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedSchemeName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsPrimaryIndigo
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Code: #$selectedSchemeCode", fontSize = 11.sp, color = WsSlateTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Holding Units") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = investedInput,
                        onValueChange = { investedInput = it },
                        label = { Text("Invested Capital (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedSchemeCode = null }) {
                        Text("← Choose different scheme")
                    }
                }
            }
        },
        confirmButton = {
            if (selectedSchemeCode != null) {
                Button(
                    onClick = {
                        val units = unitsInput.toDoubleOrNull() ?: 100.0
                        val invested = investedInput.toDoubleOrNull() ?: 10000.0
                        selectedSchemeCode?.let { code ->
                            portfolioViewModel.addSearchedScheme(code, units, invested)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WsPrimaryIndigo)
                ) {
                    Text("Add to Vault")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CsvManagerModal(
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var csvText by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Portfolio CSV Import / Export", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isImportMode,
                        onClick = { isImportMode = false },
                        label = { Text("Export CSV") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WsPrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = isImportMode,
                        onClick = { isImportMode = true },
                        label = { Text("Import CSV") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WsPrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                if (!isImportMode) {
                    val exportCsv = remember { portfolioViewModel.exportPortfolioCsv() }
                    Text(
                        text = "Format: SchemeCode,SchemeName,Units,InvestedAmount",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    OutlinedTextField(
                        value = exportCsv,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FinBaba Portfolio CSV", exportCsv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied CSV to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WsPrimaryIndigo)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy CSV to Clipboard")
                    }
                } else {
                    Text(
                        text = "Paste CSV content below (header: SchemeCode,SchemeName,Units,InvestedAmount):",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = { Text("122639,\"Parag Parikh Flexi Cap\",2450.5,145000.0\n120503,\"Axis ELSS Tax Saver\",1500.0,90000.0") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        onClick = {
                            if (csvText.isNotBlank()) {
                                portfolioViewModel.importPortfolioFromCsv(csvText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WsEmeraldPositive)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import & Overwrite Vault")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}