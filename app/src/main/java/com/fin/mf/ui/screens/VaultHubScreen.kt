package com.fin.mf.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.fin.mf.core.theme.AppearanceSettingsSheet
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.ui.components.MarketStatusTickerPill
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.fin.mf.ui.workspace.components.HairlineScrollProgressIndicator
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
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
import com.fin.mf.ui.components.bounceClick
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.workspace.components.LuxuryCreditCardHero
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
    onOpenMarketTerminal: () -> Unit = {},
    onOpenNewsTerminal: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    themeManager: VaultThemeManager = VaultThemeManager.getInstance(LocalContext.current)
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    val analytics = uiState.analytics
    val isMasked = uiState.isPrivacyMasked
    var showSettingsModal by rememberSaveable { mutableStateOf(false) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }

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
                            FinBabaMascotBadge(modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "FinBaba",
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Cursive,
                                    fontWeight = FontWeight.Bold,
                                    color = gradient.startColor,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = gradient.startColor.copy(alpha = 0.35f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                                Text(
                                    text = "FIN Studio",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = HubSlateSecondary,
                                    letterSpacing = 1.sp
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
                        IconButton(onClick = {
                            if (onOpenSettings != {}) onOpenSettings() else showSettingsModal = true
                        }) {
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
            val listState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Sleek Top Hairline Scroll Progress Tracker (Module 2)
                HairlineScrollProgressIndicator(listState = listState)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Market Status & NSE/BSE Holiday Ticker Overlay (Controlled via Settings toggle)
                    if (uiState.isMarketStatusTickerEnabled) {
                        item {
                            MarketStatusTickerPill()
                        }
                    }

                    // 1. Executive Luxury Credit Card Hero (Strict 1.586:1 ID-1 Proportions, Zero Clutter)
                    item {
                        val schemeCount = uiState.portfolioData?.schemes?.size ?: 0
                        val vaultName = uiState.portfolioData?.portfolioName ?: "PRIMARY WEALTH VAULT"
                        LuxuryCreditCardHero(
                            title = "EXECUTIVE VAULT",
                            vaultName = vaultName,
                            currentValue = analytics?.totalCurrentValue ?: 0.0,
                            investedValue = analytics?.totalInvested ?: 0.0,
                            gainValue = analytics?.totalGain ?: 0.0,
                            returnPercent = analytics?.totalReturnPercent ?: 0.0,
                            todayGain = analytics?.todayGain,
                            schemeCount = schemeCount,
                            isMasked = isMasked
                        )
                    }

                    // Section Title
                    item {
                        Column {
                            val toolVaultTitle = "FinStudio Tools"
                            val toolVaultSubtitle = "Explore Terminal Command Deck ->"

                            Text(
                                text = toolVaultTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HubSlatePrimary
                            )
                            Text(
                                text = toolVaultSubtitle,
                                fontSize = 11.5.sp,
                                color = HubSlateSecondary
                            )
                        }
                    }

                    // 2. TOOL #1 CARD: Look-Through Tracker (ACTIVE)
                    item {
                        StudioToolCard(
                            toolNumber = "01 // CORE LOOK-THROUGH",
                            statusLabel = "● LIVE",
                            isActive = true,
                            icon = Icons.Default.PieChart,
                            title = "Mutual Fund Terminal",
                            insightTag = "Look-Through Overlap & Portfolio Analytics",
                            accentColor = gradient.startColor,
                            onClick = { onOpenPortfolioTracker(0) }
                        )
                    }

                    // 3. TOOL #2 CARD: Global Market Terminal (ACTIVE)
                    item {
                        StudioToolCard(
                            toolNumber = "02 // GLOBAL TERMINAL",
                            statusLabel = "● LIVE",
                            isActive = true,
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = "Global Assets Terminal",
                            insightTag = "World Markets, Equities & Multi-Timeframe Charts",
                            accentColor = Color(0xFF059669),
                            onClick = onOpenMarketTerminal
                        )
                    }

                    // 4. TOOL #3 CARD: SIP Wealth Architect (COMING SOON)
                    item {
                        val context = LocalContext.current
                        StudioToolCard(
                            toolNumber = "03 // WEALTH LABS",
                            statusLabel = "◌ PRO",
                            isActive = false,
                            icon = Icons.Default.AutoGraph,
                            title = "SIP Wealth Architect",
                            insightTag = "Step-Up Compounding Simulator & Goal Engine",
                            accentColor = Color(0xFFD97706),
                            onClick = {
                                Toast.makeText(context, "Tool #3 SIP Wealth Architect preview coming in the next build!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // 5. TOOL #4 CARD: Market Intelligence Wire (ACTIVE)
                    item {
                        StudioToolCard(
                            toolNumber = "04 // WIRE & INTEL",
                            statusLabel = "● LIVE",
                            isActive = true,
                            icon = Icons.Default.Public,
                            title = "Market Intelligence Wire",
                            insightTag = "Finnhub Live News, Macro Feed & Sentiment Radar",
                            accentColor = Color(0xFF7C3AED),
                            onClick = onOpenNewsTerminal
                        )
                    }
                }
            }
        }
    }

    if (showSettingsModal) {
        HubSettingsModal(
            uiState = uiState,
            portfolioViewModel = portfolioViewModel,
            onOpenAppearanceSheet = { showAppearanceSheet = true },
            onDismiss = { showSettingsModal = false }
        )
    }

    if (showAppearanceSheet) {
        AppearanceSettingsSheet(
            themeManager = themeManager,
            onDismiss = { showAppearanceSheet = false }
        )
    }
}

/**
 * Crash-proof mascot loader: safely attempts to decode `finbaba_logo` as a bitmap.
 */
@Composable
private fun FinBabaMascotBadge(modifier: Modifier = Modifier) {
    val gradient = LocalVaultGradient.current
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
                .border(1.5.dp, gradient.startColor, CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = gradient.startColor,
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
    insightTag: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.5.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0xFF64748B).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.98f) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    accentColor.copy(alpha = 0.35f),
                    Color(0xFFE2E8F0).copy(alpha = 0.60f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Column: Compact frosted glass icon container (42.dp x 42.dp)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) accentColor else HubSlateSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Body: Monospace index tag + Main Title + Single-line description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = toolNumber,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = insightTag,
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Action Area: Telemetry Indicator + Minimalist Chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (isActive) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        0.8.dp,
                        if (isActive) Color(0xFF059669).copy(alpha = 0.35f) else Color(0xFFD97706).copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp,
                        color = if (isActive) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = HubSlateSecondary.copy(alpha = 0.50f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}


@Composable
private fun HubSettingsModal(
    uiState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    onOpenAppearanceSheet: () -> Unit,
    onDismiss: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = gradient.startColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Studio Settings & Toggles", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Visual Identity & Gradient Themes trigger
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gradient.startColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Visual Identity & Palette", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Active: ${gradient.displayName}", fontSize = 11.sp, color = HubSlateSecondary)
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenAppearanceSheet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

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
                            colors = SwitchDefaults.colors(checkedTrackColor = gradient.startColor)
                        )
                    }
                }

                // Market Status & Trading Holiday Ticker Pill Toggle
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
                            Text("Market Status & Holiday Ticker", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Live NSE/BSE trading status pill overlay", fontSize = 11.sp, color = HubSlateSecondary)
                        }
                        Switch(
                            checked = uiState.isMarketStatusTickerEnabled,
                            onCheckedChange = { portfolioViewModel.setMarketStatusTickerEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = gradient.startColor)
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
                        Text("Clear Active Vault", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = HubSlateBorder)

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("FinBaba Studio v2.1.0 Vault Edition", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Engine: Jetpack Compose Material 3 & Kotlin Coroutines", fontSize = 11.sp, color = HubSlateSecondary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)) {
                Text("Close")
            }
        }
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear Active Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all schemes from your active vault, returning it to an authentic empty state.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        portfolioViewModel.clearActivePortfolio()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubCrimsonNegative)
                ) {
                    Text("Clear Vault")
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