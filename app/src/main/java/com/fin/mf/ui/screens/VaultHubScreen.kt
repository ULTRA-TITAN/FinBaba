package com.fin.mf.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.PortfolioViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val HubBgWhite = Color(0xFFF8FAFC)
private val HubSurfaceWhite = Color(0xFFFFFFFF)
private val HubPrimaryIndigo = Color(0xFF1E3A8A)
private val HubAccentRoyal = Color(0xFF2563EB)
private val HubEmeraldPositive = Color(0xFF059669)
private val HubEmeraldSoft = Color(0xFFECFDF5)
private val HubCrimsonNegative = Color(0xFFDC2626)
private val HubCrimsonSoft = Color(0xFFFEF2F2)
private val HubAmberBadge = Color(0xFFD97706)
private val HubAmberSoft = Color(0xFFFFFBEB)
private val HubSlatePrimary = Color(0xFF0F172A)
private val HubSlateSecondary = Color(0xFF475569)
private val HubSlateBorder = Color(0xFFE2E8F0)

// Top-level cached NumberFormat for 60/120 FPS performance
private val TopLevelHubInrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private val HubColorScheme = lightColorScheme(
    primary = HubPrimaryIndigo,
    onPrimary = Color.White,
    secondary = HubAccentRoyal,
    onSecondary = Color.White,
    background = HubBgWhite,
    onBackground = HubSlatePrimary,
    surface = HubSurfaceWhite,
    onSurface = HubSlatePrimary
)

/**
 * Root Destination: FinBaba Financial Studio & Tool Vault Home Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHubScreen(
    portfolioViewModel: PortfolioViewModel,
    onOpenPortfolioTracker: (initialTabIndex: Int) -> Unit,
    onOpenMarketTerminal: () -> Unit = {}
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    val analytics = uiState.analytics
    val isMasked = uiState.isPrivacyMasked
    var showSettingsModal by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colorScheme = HubColorScheme) {
        Scaffold(
            containerColor = HubBgWhite,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HubSurfaceWhite,
                        titleContentColor = HubSlatePrimary
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FinBabaMascotBadge(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FinBaba Studio",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HubSlatePrimary
                                )
                                Text(
                                    text = "Personal Financial Studio & Tool Vault",
                                    fontSize = 11.sp,
                                    color = HubSlateSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { portfolioViewModel.togglePrivacyMask() }) {
                            Icon(
                                imageVector = if (isMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Privacy Mask",
                                tint = HubSlateSecondary
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
                                    color = HubAccentRoyal
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Live NAVs",
                                    tint = HubAccentRoyal
                                )
                            }
                        }
                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Studio Settings",
                                tint = HubPrimaryIndigo
                            )
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Studio Mascot & Executive Portfolio Summary Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = HubSurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HubSlateBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FinBabaMascotBadge(modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = uiState.portfolioData?.portfolioName ?: "FinBaba Institutional Core",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HubSlatePrimary
                                        )
                                        val schemeCount = uiState.portfolioData?.schemes?.size ?: 20
                                        Text(
                                            text = "$schemeCount Schemes • AMFI Connected",
                                            fontSize = 12.sp,
                                            color = HubSlateSecondary
                                        )
                                    }
                                }

                                if (analytics != null) {
                                    val isPositive = analytics.totalGain >= 0
                                    Surface(
                                        color = if (isPositive) HubEmeraldSoft else HubCrimsonSoft,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPositive) {
                                                    Icons.AutoMirrored.Filled.TrendingUp
                                                } else {
                                                    Icons.AutoMirrored.Filled.TrendingDown
                                                },
                                                contentDescription = null,
                                                tint = if (isPositive) HubEmeraldPositive else HubCrimsonNegative,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = formatHubSignedPercent(analytics.totalReturnPercent),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) HubEmeraldPositive else HubCrimsonNegative
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = HubSlateBorder)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (analytics != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Portfolio Value",
                                            fontSize = 12.sp,
                                            color = HubSlateSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formatHubInr(analytics.totalCurrentValue, isMasked),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = HubSlatePrimary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Total Gain / Loss",
                                            fontSize = 12.sp,
                                            color = HubSlateSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formatHubInr(analytics.totalGain, isMasked, showPlus = true),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (analytics.totalGain >= 0) HubEmeraldPositive else HubCrimsonNegative
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Invested Principal: ${formatHubInr(analytics.totalInvested, isMasked)}",
                                        fontSize = 12.sp,
                                        color = HubSlateSecondary
                                    )
                                    val syncTimeText = remember(uiState.portfolioData?.lastSyncedEpochMs) {
                                        val ms = uiState.portfolioData?.lastSyncedEpochMs ?: 0L
                                        if (ms > 0L) {
                                            SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(ms))
                                        } else {
                                            "Seed Ready"
                                        }
                                    }
                                    Text(
                                        text = "Synced: $syncTimeText",
                                        fontSize = 11.sp,
                                        color = HubAccentRoyal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Title
                item {
                    Column {
                        Text(
                            text = "Financial Studio Tool Vault",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HubSlatePrimary
                        )
                        Text(
                            text = "Select an institutional analytics module below",
                            fontSize = 12.sp,
                            color = HubSlateSecondary
                        )
                    }
                }

                // 2. TOOL #1 CARD: Mutual Fund Portfolio & Look-Through Tracker (ACTIVE)
                item {
                    val schemeCount = uiState.portfolioData?.schemes?.size ?: 20
                    val terMetricsText = if (analytics != null) {
                        "$schemeCount Schemes • TER: ${formatHubPlainPercent(analytics.weightedExpenseRatio)} • ${analytics.topHoldings.size} Holdings"
                    } else {
                        "$schemeCount Schemes • Real-Time AMFI Engine"
                    }

                    StudioToolCard(
                        toolNumber = "TOOL #1",
                        statusLabel = "ACTIVE",
                        isActive = true,
                        icon = Icons.Default.PieChart,
                        title = "Mutual Fund Portfolio & Look-Through Tracker",
                        description = "Institutional workspace with live parallel AMFI NAV syncing, position CRUD, CSV import/export, and look-through constituent inspection.",
                        metricsHighlight = terMetricsText,
                        primaryButtonText = "Open Workspace",
                        onActionClick = { onOpenPortfolioTracker(0) }
                    )
                }

                // 3. TOOL #2 CARD: Global Market Terminal & Broker Studio (TOOL #2 ACTIVE)
                item {
                    StudioToolCard(
                        toolNumber = "TOOL #2",
                        statusLabel = "TOOL #2 ACTIVE",
                        isActive = true,
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        title = "Global Market Terminal & Broker Studio",
                        description = "Real-time quotes, multi-timeframe charts, dual-pointer scrubbing, and Nifty 50 benchmark comparison for any Indian or global asset.",
                        metricsHighlight = "NSE/BSE Equities & Global Assets • Benchmark Alpha",
                        primaryButtonText = "Launch Terminal",
                        onActionClick = onOpenMarketTerminal
                    )
                }

                // 4. TOOL #3 CARD: CAGR & Step-Up SIP Simulator (UPCOMING / PREVIEW)
                item {
                    val context = LocalContext.current
                    StudioToolCard(
                        toolNumber = "TOOL #3",
                        statusLabel = "UPCOMING",
                        isActive = false,
                        icon = Icons.Default.Analytics,
                        title = "CAGR & Step-Up SIP Simulator",
                        description = "Model multi-decade wealth compounding combining your active portfolio equity with annual step-up SIP contributions and custom CAGR projections.",
                        metricsHighlight = "Interactive 3–30 Year Wealth Compounding Architect",
                        primaryButtonText = "Preview Coming Soon",
                        onActionClick = {
                            Toast.makeText(context, "Tool #3 CAGR & Step-Up SIP Simulator preview coming in the next build!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showSettingsModal) {
        HubSettingsModal(
            uiState = uiState,
            portfolioViewModel = portfolioViewModel,
            onDismiss = { showSettingsModal = false }
        )
    }
}

/**
 * Crash-proof mascot loader: safely attempts to decode `finbaba_logo` as a bitmap.
 */
@Composable
private fun FinBabaMascotBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val decodedBitmap = remember(context) {
        runCatching {
            val resId = context.resources.getIdentifier("finbaba_logo", "drawable", context.packageName)
            if (resId != 0) {
                BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = "FinBaba Mascot",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(CircleShape)
                .border(1.5.dp, HubPrimaryIndigo, CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = HubPrimaryIndigo,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "FinBaba Mascot Fallback",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioToolCard(
    toolNumber: String,
    statusLabel: String,
    isActive: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    metricsHighlight: String,
    primaryButtonText: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) HubAccentRoyal.copy(alpha = 0.45f) else HubSlateBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) HubPrimaryIndigo else Color(0xFFF1F5F9),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isActive) Color.White else HubSlateSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = toolNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HubAccentRoyal
                        )
                        Surface(
                            color = if (isActive) HubEmeraldSoft else HubAmberSoft,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = HubEmeraldPositive,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = statusLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) HubEmeraldPositive else HubAmberBadge
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HubSlatePrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = HubSlateSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, HubSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = HubAccentRoyal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = metricsHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSlatePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HubPrimaryIndigo),
                contentPadding = PaddingValues(vertical = 11.dp)
            ) {
                Text(
                    text = primaryButtonText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HubSettingsModal(
    uiState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = HubPrimaryIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Studio Settings & Toggles", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Privacy toggle
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HubSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Mask Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Mask rupee numbers on all screens", fontSize = 11.sp, color = HubSlateSecondary)
                        }
                        Switch(
                            checked = uiState.isPrivacyMasked,
                            onCheckedChange = { portfolioViewModel.togglePrivacyMask() },
                            colors = SwitchDefaults.colors(checkedTrackColor = HubPrimaryIndigo)
                        )
                    }
                }

                // Data actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vault Operations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HubSlateSecondary)

                    OutlinedButton(
                        onClick = {
                            val exportCsv = portfolioViewModel.exportPortfolioCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FinBaba Portfolio CSV", exportCsv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied CSV to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Portfolio CSV", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HubCrimsonNegative)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore 20-Scheme Seed", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = HubSlateBorder)

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("FinBaba Studio v2.1.0 Institutional Edition", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Engine: Jetpack Compose Material 3 & Kotlin Coroutines", fontSize = 11.sp, color = HubSlateSecondary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = HubPrimaryIndigo)) {
                Text("Close")
            }
        }
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restore Factory 20-Scheme Seed?", fontWeight = FontWeight.Bold) },
            text = { Text("This will restore the factory 20-scheme seed from assets and trigger a fresh AMFI NAV sync.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        portfolioViewModel.resetPortfolioToSeed()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubCrimsonNegative)
                ) {
                    Text("Restore Seed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatHubInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatted = TopLevelHubInrFormatter.format(amount)
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

private fun formatHubSignedPercent(pct: Double): String {
    val rounded = (pct * 100.0).roundToInt() / 100.0
    val sign = if (rounded >= 0.0) "+" else ""
    return "$sign$rounded%"
}

private fun formatHubPlainPercent(pct: Double): String {
    val rounded = (pct * 100.0).roundToInt() / 100.0
    return "$rounded%"
}