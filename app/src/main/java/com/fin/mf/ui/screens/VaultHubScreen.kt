package com.fin.mf.ui.screens

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Root Destination: FinBaba Financial Studio & Tool Vault Home Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHubScreen(
    portfolioViewModel: PortfolioViewModel,
    onOpenPortfolioTracker: (initialTabIndex: Int) -> Unit
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    val analytics = uiState.analytics
    val isMasked = uiState.isPrivacyMasked

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
                },
                modifier = Modifier.shadow(elevation = 4.dp, spotColor = Color(0x1A0F172A))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0x220F172A)
                        )
                        .border(1.dp, HubSlateBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = HubSurfaceWhite)
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
                                        text = "$schemeCount Direct-Growth Schemes • AMFI Connected",
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
                                                Icons.Default.TrendingUp
                                            } else {
                                                Icons.Default.TrendingDown
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

                        if (analytics == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = HubPrimaryIndigo
                                )
                                Text(
                                    text = "Computing portfolio valuation...",
                                    fontSize = 13.sp,
                                    color = HubSlateSecondary
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Portfolio Equity",
                                        fontSize = 11.sp,
                                        color = HubSlateSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatHubInr(analytics.totalCurrentValue, isMasked),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HubSlatePrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Net Unrealized Gain",
                                        fontSize = 11.sp,
                                        color = HubSlateSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatHubInr(analytics.totalGain, isMasked, showPlus = true),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
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
                val terMetricsText = if (analytics != null) {
                    "20 Schemes • Weighted TER: ${formatHubPlainPercent(analytics.weightedExpenseRatio)} • ${analytics.topHoldings.size} Look-Through Holdings"
                } else {
                    "20 Schemes • Real-Time AMFI NAV Engine"
                }

                StudioToolCard(
                    toolNumber = "TOOL #1",
                    statusLabel = "ACTIVE",
                    isActive = true,
                    icon = Icons.Default.PieChart,
                    title = "Mutual Fund Portfolio & Look-Through Tracker",
                    description = "Full 20-scheme institutional workspace with live parallel AMFI NAV syncing, position editing (units & cost), and constituent holdings inspection.",
                    metricsHighlight = terMetricsText,
                    primaryButtonText = "Open Tool",
                    onActionClick = { onOpenPortfolioTracker(0) }
                )
            }

            // 3. TOOL #2 CARD: Fund Overlap Matrix (Coming Soon / Preview)
            item {
                val overlapMetricsText = if (analytics != null) {
                    "${analytics.highOverlapStocksCount} high-overlap stocks detected across 3+ funds"
                } else {
                    "Cross-scheme stock & sector concentration diagnostics"
                }

                StudioToolCard(
                    toolNumber = "TOOL #2",
                    statusLabel = "COMING SOON • PREVIEW READY",
                    isActive = false,
                    icon = Icons.Default.Layers,
                    title = "Fund Overlap Matrix",
                    description = "Cross-scheme equity and debt overlap matrix identifying redundant stock exposure and sector concentration across your mutual fund vault.",
                    metricsHighlight = overlapMetricsText,
                    primaryButtonText = "Preview Overlap X-Ray",
                    onActionClick = { onOpenPortfolioTracker(2) }
                )
            }

            // 4. TOOL #3 CARD: CAGR & Step-Up SIP Simulator (Coming Soon / Shortcut)
            item {
                StudioToolCard(
                    toolNumber = "TOOL #3",
                    statusLabel = "COMING SOON • SHORTCUT READY",
                    isActive = false,
                    icon = Icons.Default.Analytics,
                    title = "CAGR & Step-Up SIP Simulator",
                    description = "Model multi-decade wealth compounding combining your current portfolio equity with annual step-up SIP contributions and custom CAGR assumptions.",
                    metricsHighlight = "Interactive 3–30 Year Step-Up Compounding Architect",
                    primaryButtonText = "Launch SIP Simulator",
                    onActionClick = { onOpenPortfolioTracker(4) }
                )
            }
        }
    }
}

@Composable
private fun FinBabaMascotBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logoResId = remember(context) {
        context.resources.getIdentifier("finbaba_logo", "drawable", context.packageName)
    }

    if (logoResId != 0) {
        Image(
            painter = painterResource(id = logoResId),
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
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive) 10.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x1F0F172A)
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) HubAccentRoyal.copy(alpha = 0.45f) else HubSlateBorder,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurfaceWhite)
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
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HubSlatePrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = HubSlateSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HubSlateBorder, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = metricsHighlight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubPrimaryIndigo,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isActive) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HubPrimaryIndigo),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = primaryButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = primaryButtonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubPrimaryIndigo
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HubPrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatHubInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val formatted = formatter.format(amount)
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