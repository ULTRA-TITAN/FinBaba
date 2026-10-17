package com.fin.mf.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.fin.mf.data.PortfolioMeta
import com.fin.mf.ui.workspace.dialogs.PortfolioSwitcherBottomSheet
import com.fin.mf.ui.workspace.dialogs.CreatePortfolioDialog
import com.fin.mf.ui.workspace.dialogs.RenamePortfolioDialog
import com.fin.mf.ui.workspace.dialogs.DeletePortfolioDialog
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fin.mf.ui.components.bounceClick
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.AppearanceSettingsSheet
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsCrimsonNegative
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.dialogs.AmfiAddSchemeModal
import com.fin.mf.ui.workspace.dialogs.CsvManagerModal
import com.fin.mf.ui.workspace.dialogs.SettingsModalDialog
import com.fin.mf.ui.workspace.tabs.WorkspaceMfHunterTab
import com.fin.mf.ui.workspace.tabs.WorkspaceOverviewTab
import com.fin.mf.ui.workspace.tabs.WorkspacePortfolioTab
import com.fin.mf.ui.workspace.tabs.WorkspaceXRayTab

private val WsBgWhite = Color(0xFFF8FAFC)

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
    themeManager: VaultThemeManager = VaultThemeManager.getInstance(LocalContext.current),
    initialTabIndex: Int = 0,
    onNavigateBackToHub: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()

    var activeTab by rememberSaveable(initialTabIndex) {
        mutableStateOf(WorkspaceTab.fromIndex(initialTabIndex))
    }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAmfiAddDialog by rememberSaveable { mutableStateOf(false) }
    var showCsvDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }
    var schemeToDelete by remember { mutableStateOf<MutualFundScheme?>(null) }
    var showPortfolioSwitcherSheet by rememberSaveable { mutableStateOf(false) }
    var showCreatePortfolioDialog by rememberSaveable { mutableStateOf(false) }
    var portfolioToRename by remember { mutableStateOf<PortfolioMeta?>(null) }
    var portfolioToDelete by remember { mutableStateOf<PortfolioMeta?>(null) }

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
                            tint = gradient.startColor
                        )
                    }
                },
                title = {
                    val activeVaultName = uiState.portfolioData?.portfolioName ?: "Primary Vault"
                    val schemeCount = uiState.portfolioData?.schemes?.size ?: 0

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPortfolioSwitcherSheet = true }
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = activeVaultName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Portfolio",
                                tint = WsSlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        val subtitleText = when (activeTab) {
                            WorkspaceTab.XRAY -> {
                                val holdingsCount = uiState.analytics?.topHoldings?.size ?: 0
                                "DEEP DIAGNOSTICS • $schemeCount FUNDS • $holdingsCount STOCKS"
                            }
                            WorkspaceTab.MF_HUNTER -> "Constituent Look-Through & Search"
                            WorkspaceTab.COMPARE -> "Base-100 Performance & Factor Matrix"
                            else -> "$schemeCount Active Schemes • Live AMFI"
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
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
                    // Settings Gear Icon
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Controls",
                            tint = gradient.startColor
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isFloatingFabEnabled,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAmfiAddDialog = true },
                    containerColor = gradient.startColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Scheme"
                        )
                        Text(
                            text = "Add Scheme",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
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
                            selectedIconColor = gradient.startColor,
                            selectedTextColor = gradient.startColor,
                            indicatorColor = gradient.startColor.copy(alpha = 0.15f),
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
                        // UI TEXT (Safe to edit):
                        Text(
                            text = "Loading Portfolio Vault...",
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
                            onDeleteScheme = { schemeToDelete = it },
                            onAddSchemeClick = { showAmfiAddDialog = true },
                            onUpdateMandate = { portfolioViewModel.updateMarketCapMandate(it) }
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
                            onDeleteScheme = { schemeToDelete = it },
                            onAddSchemeClick = { showAmfiAddDialog = true },
                            onRefresh = { portfolioViewModel.syncAllLiveNavs() }
                        )
                    }
                    WorkspaceTab.XRAY -> {
                        WorkspaceXRayTab(
                            analytics = uiState.analytics,
                            isMasked = uiState.isPrivacyMasked,
                            onSchemeClick = { schemeCode ->
                                portfolioViewModel.selectSchemeForDeepDive(schemeCode)
                                activeTab = WorkspaceTab.MF_HUNTER
                            },
                            portfolioData = uiState.portfolioData
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
            onPromptClearPortfolio = {
                showSettingsDialog = false
                showClearConfirmDialog = true
            },
            onOpenAppearanceSheet = {
                showSettingsDialog = false
                showAppearanceSheet = true
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "Clear Active Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all schemes from your active vault, returning it to an authentic empty state.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        portfolioViewModel.clearActivePortfolio()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WsCrimsonNegative)
                ) {
                    Text("Clear Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
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

    // Appearance Studio Palette Bottom Sheet
    if (showAppearanceSheet) {
        AppearanceSettingsSheet(
            themeManager = themeManager,
            onDismiss = { showAppearanceSheet = false }
        )
    }

    // Portfolio Switcher Bottom Sheet
    if (showPortfolioSwitcherSheet) {
        val manifest = uiState.portfolioManifest ?: com.fin.mf.data.PortfolioManifest(
            activePortfolioId = "default",
            portfolios = listOf(
                PortfolioMeta(
                    id = "default",
                    name = uiState.portfolioData?.portfolioName ?: "Primary Vault",
                    createdAtEpochMs = System.currentTimeMillis(),
                    schemeCount = uiState.portfolioData?.schemes?.size ?: 0,
                    totalValue = uiState.analytics?.totalCurrentValue ?: 0.0
                )
            )
        )
        PortfolioSwitcherBottomSheet(
            manifest = manifest,
            isMasked = uiState.isPrivacyMasked,
            onSelectPortfolio = { id ->
                portfolioViewModel.switchPortfolio(id)
                showPortfolioSwitcherSheet = false
            },
            onRequestCreate = {
                showPortfolioSwitcherSheet = false
                showCreatePortfolioDialog = true
            },
            onRequestRename = { meta ->
                portfolioToRename = meta
            },
            onRequestDuplicate = { meta ->
                portfolioViewModel.duplicatePortfolio(meta.id, "${meta.name} (Copy)")
            },
            onRequestDelete = { meta ->
                portfolioToDelete = meta
            },
            onDismiss = { showPortfolioSwitcherSheet = false }
        )
    }

    // Create Portfolio Dialog
    if (showCreatePortfolioDialog) {
        CreatePortfolioDialog(
            onDismiss = { showCreatePortfolioDialog = false },
            onConfirm = { name, seedWithDefault ->
                portfolioViewModel.createNewPortfolio(name, seedWithDefault)
                showCreatePortfolioDialog = false
            }
        )
    }

    // Rename Portfolio Dialog
    portfolioToRename?.let { meta ->
        RenamePortfolioDialog(
            currentName = meta.name,
            onDismiss = { portfolioToRename = null },
            onConfirm = { newName ->
                portfolioViewModel.renamePortfolio(meta.id, newName)
                portfolioToRename = null
            }
        )
    }

    // Delete Portfolio Dialog
    portfolioToDelete?.let { meta ->
        DeletePortfolioDialog(
            portfolioName = meta.name,
            onDismiss = { portfolioToDelete = null },
            onConfirm = {
                portfolioViewModel.deletePortfolio(meta.id)
                portfolioToDelete = null
            }
        )
    }
}