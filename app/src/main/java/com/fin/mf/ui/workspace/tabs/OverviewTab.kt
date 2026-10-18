package com.fin.mf.ui.workspace.tabs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fin.mf.ui.components.bounceClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.MarketCapMandate
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.workspace.components.HairlineScrollProgressIndicator
import com.fin.mf.ui.workspace.components.LuxuryCreditCardHero
import com.fin.mf.ui.workspace.components.OnboardingTutorialCard
import com.fin.mf.ui.workspace.sheets.AssetSplitDetailSheet
import com.fin.mf.ui.workspace.sheets.MarketCapDetailSheet
import com.fin.mf.ui.workspace.sheets.SectorDetailSheet
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSchemeSummaryCard
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.dialogs.EditMarketCapMandateDialog
import com.fin.mf.ui.workspace.components.formatWsInr
import com.fin.mf.ui.workspace.components.formatWsPercent
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val WsAmberWarning = SemanticFinanceColors.BenchmarkAmber

/**
 * Tab #1: Executive overview with Net Worth snapshot, SEBI Market Cap exposure,
 * Consolidated Asset Split, Sector breakdown, TER drag, and scheme holdings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceOverviewTab(
    uiState: PortfolioUiState,
    onSyncClick: () -> Unit,
    onInspectScheme: (Int) -> Unit,
    onNavigateToXRay: () -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit,
    onAddSchemeClick: () -> Unit = {},
    onUpdateMandate: (MarketCapMandate) -> Unit = {}
) {
    val analytics = uiState.analytics ?: return
    val isMasked = uiState.isPrivacyMasked
    val schemes = uiState.portfolioData?.schemes.orEmpty()
    val mandate = uiState.portfolioData?.marketCapMandate ?: MarketCapMandate()
    val gradient = LocalVaultGradient.current

    if (schemes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            OnboardingTutorialCard(
                onAddSchemeClick = onAddSchemeClick
            )
        }
        return
    }

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    var showMarketCapSheet by remember { mutableStateOf(false) }
    var showAssetSplitSheet by remember { mutableStateOf(false) }
    var showSectorSheet by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = uiState.isSyncingNavs,
        onRefresh = onSyncClick,
        modifier = Modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = uiState.isSyncingNavs,
                modifier = Modifier.align(Alignment.TopCenter),
                color = gradient.startColor
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Card A: Net Worth Snapshot Card
        item {
            NetWorthSnapshotCard(
                analytics = analytics,
                schemeCount = schemes.size,
                isMasked = isMasked,
                vaultName = uiState.portfolioData?.portfolioName ?: "PRIMARY WEALTH VAULT"
            )
        }

        // 2. Card B: SEBI Market Cap Exposure Card
        item {
            SebiMarketCapExposureCard(
                analytics = analytics,
                mandate = mandate,
                onUpdateMandate = onUpdateMandate,
                isMasked = isMasked,
                onCardClick = { showMarketCapSheet = true }
            )
        }

        // 3. Card C: Consolidated Asset Split Card
        item {
            ConsolidatedAssetSplitCard(
                analytics = analytics,
                schemes = schemes,
                isMasked = isMasked,
                onCardClick = { showAssetSplitSheet = true }
            )
        }

        // 4. Card D: Sector Breakdown Card
        item {
            SectorBreakdownCard(
                analytics = analytics,
                onNavigateToXRay = onNavigateToXRay,
                onCardClick = { showSectorSheet = true }
            )
        }

        // 5. Core Metrics: TER Drag & Stock Overlap Alerts
        item {
            // =========================================================================
            // DISPLAY TEXT CONFIGURATION: Core Metrics (Safe to edit below)
            // =========================================================================
            val terDragTitle = "Weighted TER Drag"
            val overlapAlertTitle = "Stock Overlap Alert"
            val overlapSubtitle = "Held in 3+ mutual funds"
            // =========================================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WsElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = terDragTitle,
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
                                text = overlapAlertTitle,
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
                            text = overlapSubtitle,
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
            }
        }

        // 6. Largest Scheme Holdings Header
        item {
            // =========================================================================
            // DISPLAY TEXT CONFIGURATION: Largest Positions (Safe to edit below)
            // =========================================================================
            val positionsTitle = "Largest Scheme Positions"
            val btnSyncText = "Sync Live NAVs"
            // =========================================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = positionsTitle,
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
                    Text(btnSyncText, fontSize = 12.sp)
                }
            }
        }

        // 7. Largest Scheme Holdings List
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

    HairlineScrollProgressIndicator(
        listState = listState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}

    if (showMarketCapSheet) {
        MarketCapDetailSheet(
            analytics = analytics,
            mandate = mandate,
            isMasked = isMasked,
            onDismiss = { showMarketCapSheet = false }
        )
    }

    if (showAssetSplitSheet) {
        AssetSplitDetailSheet(
            analytics = analytics,
            schemes = schemes,
            isMasked = isMasked,
            onDismiss = { showAssetSplitSheet = false }
        )
    }

    if (showSectorSheet) {
        SectorDetailSheet(
            analytics = analytics,
            isMasked = isMasked,
            onDismiss = { showSectorSheet = false }
        )
    }
}

/**
 * Card A: Bespoke Luxury Credit Card Hero with EMV chip, contactless waves,
 * tabular monospaced valuation, holographic return pill, and cardholder metadata.
 */
@Composable
private fun NetWorthSnapshotCard(
    analytics: PortfolioAnalyticsSummary,
    schemeCount: Int? = null,
    isMasked: Boolean,
    vaultName: String = "PRIMARY WEALTH VAULT"
) {
    LuxuryCreditCardHero(
        title = "EXECUTIVE VAULT",
        vaultName = vaultName,
        currentValue = analytics.totalCurrentValue,
        investedValue = analytics.totalInvested,
        gainValue = analytics.totalGain,
        returnPercent = analytics.totalReturnPercent,
        todayGain = analytics.todayGain,
        schemeCount = schemeCount,
        isMasked = isMasked
    )
}


/**
 * Card B: SEBI Market Cap Exposure Card with target mandate, progress bar, 4-tier grid, and Edit Mandate sheet.
 */
@Composable
private fun SebiMarketCapExposureCard(
    analytics: PortfolioAnalyticsSummary,
    mandate: MarketCapMandate,
    onUpdateMandate: (MarketCapMandate) -> Unit,
    isMasked: Boolean,
    onCardClick: () -> Unit = {}
) {
    var showEditMandateDialog by remember { mutableStateOf(false) }

    val largeSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Large", ignoreCase = true) }
    val midSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Mid", ignoreCase = true) }
    val smallSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Small", ignoreCase = true) }
    val microSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Micro", ignoreCase = true) }

    val largeWeight = largeSlice?.weightPercent ?: 0.0
    val midWeight = midSlice?.weightPercent ?: 0.0
    val smallWeight = smallSlice?.weightPercent ?: 0.0
    val nonLargeMidSmall = analytics.marketCapBreakdown.filter {
        !it.label.contains("Large", ignoreCase = true) &&
        !it.label.contains("Mid", ignoreCase = true) &&
        !it.label.contains("Small", ignoreCase = true)
    }
    val microWeight = nonLargeMidSmall.sumOf { it.weightPercent }.coerceAtLeast(microSlice?.weightPercent ?: 0.0)

    val largeVal = largeSlice?.rupeeValue ?: 0.0
    val midVal = midSlice?.rupeeValue ?: 0.0
    val smallVal = smallSlice?.rupeeValue ?: 0.0
    val microVal = nonLargeMidSmall.sumOf { it.rupeeValue }.coerceAtLeast(microSlice?.rupeeValue ?: 0.0)

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Market Cap Alignment"
    val editMandateText = "Edit Mandate"
    // =========================================================================

    WsElevatedCard(
        modifier = Modifier.bounceClick(scaleDown = 0.985f, onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cardTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WsSlateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.clickable { showEditMandateDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = editMandateText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target Mandate: ${mandate.largeCapTarget.roundToInt()}% Large / ${mandate.midCapTarget.roundToInt()}% Mid / ${mandate.smallCapTarget.roundToInt()}% Small / ${mandate.microCapTarget.roundToInt()}% Micro",
                fontSize = 11.sp,
                color = WsSlateTextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (largeWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(largeWeight.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(SemanticFinanceColors.SebiLargeCap)
                    )
                }
                if (midWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(midWeight.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(SemanticFinanceColors.SebiMidCap)
                    )
                }
                if (smallWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(smallWeight.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(SemanticFinanceColors.SebiSmallCap)
                    )
                }
                if (microWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(microWeight.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(SemanticFinanceColors.SebiMicroCap)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Large",
                    dotColor = SemanticFinanceColors.SebiLargeCap,
                    weightPct = largeWeight,
                    rupeeVal = largeVal,
                    statusChip = formatMarketCapDelta(largeWeight, mandate.largeCapTarget),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Mid",
                    dotColor = SemanticFinanceColors.SebiMidCap,
                    weightPct = midWeight,
                    rupeeVal = midVal,
                    statusChip = formatMarketCapDelta(midWeight, mandate.midCapTarget),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Small",
                    dotColor = SemanticFinanceColors.SebiSmallCap,
                    weightPct = smallWeight,
                    rupeeVal = smallVal,
                    statusChip = formatMarketCapDelta(smallWeight, mandate.smallCapTarget),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Micro",
                    dotColor = SemanticFinanceColors.SebiMicroCap,
                    weightPct = microWeight,
                    rupeeVal = microVal,
                    statusChip = formatMarketCapDelta(microWeight, mandate.microCapTarget),
                    isMasked = isMasked
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Advisory Note Addressing Largest Negative Drift Gap
            val drifts = listOf(
                Pair("Large Cap", largeWeight - mandate.largeCapTarget),
                Pair("Mid Cap", midWeight - mandate.midCapTarget),
                Pair("Small Cap", smallWeight - mandate.smallCapTarget),
                Pair("Micro Cap", microWeight - mandate.microCapTarget)
            )
            val mostUnderweight = drifts.filter { it.second < -2.0 }.minByOrNull { it.second }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (mostUnderweight != null) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (mostUnderweight != null) Color(0xFFFECACA) else Color(0xFFBBF7D0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (mostUnderweight != null) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (mostUnderweight != null) Color(0xFFDC2626) else Color(0xFF16A34A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mostUnderweight != null) {
                            "Advisory: ${mostUnderweight.first} is underweight by ${roundOne(abs(mostUnderweight.second))}%. Consider allocating your next SIP inflow here."
                        } else {
                            "Advisory: Portfolio allocation is balanced across SEBI market cap tiers within target mandate bands."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (mostUnderweight != null) Color(0xFF991B1B) else Color(0xFF166534)
                    )
                }
            }
        }
    }

    if (showEditMandateDialog) {
        EditMarketCapMandateDialog(
            currentMandate = mandate,
            onSave = {
                onUpdateMandate(it)
                showEditMandateDialog = false
            },
            onDismiss = { showEditMandateDialog = false }
        )
    }
}

@Composable
private fun MarketCapTierCell(
    modifier: Modifier = Modifier,
    tierName: String,
    dotColor: Color,
    weightPct: Double,
    rupeeVal: Double,
    statusChip: String,
    isMasked: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tierName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = String.format(Locale.US, "%.1f%%", weightPct),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    fontFeatureSettings = "tnum",
                    color = WsSlateTextPrimary
                )
            )

            Text(
                text = formatWsInr(rupeeVal, isMasked),
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    fontFeatureSettings = "tnum",
                    color = WsSlateTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = when {
                    statusChip.contains("Over", ignoreCase = true) -> Color(0xFFFFFBEB)
                    statusChip.contains("Under", ignoreCase = true) -> Color(0xFFFEF2F2)
                    else -> Color(0xFFECFDF5)
                },
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    when {
                        statusChip.contains("Over", ignoreCase = true) -> Color(0xFFFDE68A)
                        statusChip.contains("Under", ignoreCase = true) -> Color(0xFFFECACA)
                        else -> Color(0xFFA7F3D0)
                    }
                )
            ) {
                Text(
                    text = statusChip.uppercase(),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = when {
                        statusChip.contains("Over", ignoreCase = true) -> Color(0xFFD97706)
                        statusChip.contains("Under", ignoreCase = true) -> Color(0xFFDC2626)
                        else -> Color(0xFF059669)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Card C: Consolidated Asset Split Card with horizontal progress track and 2x2 grid.
 */
@Composable
private fun ConsolidatedAssetSplitCard(
    analytics: PortfolioAnalyticsSummary,
    schemes: List<MutualFundScheme>,
    isMasked: Boolean,
    onCardClick: () -> Unit = {}
) {
    val totalVal = analytics.totalCurrentValue
    val equitySchemesVal = schemes.filter { it.assetClass.equals("Equity", ignoreCase = true) }.sumOf { it.currentValue }
    val hybridSchemesVal = schemes.filter { it.assetClass.equals("Hybrid", ignoreCase = true) }.sumOf { it.currentValue }
    val debtSchemesVal = schemes.filter { it.assetClass.equals("Debt", ignoreCase = true) && !it.category.contains("Liquid", ignoreCase = true) }.sumOf { it.currentValue }
    val liquidSchemesVal = schemes.filter { it.category.contains("Liquid", ignoreCase = true) || it.assetClass.contains("Cash", ignoreCase = true) }.sumOf { it.currentValue }

    val equitiesVal = equitySchemesVal + (hybridSchemesVal * 0.65)
    val debtVal = debtSchemesVal + (hybridSchemesVal * 0.25)
    val cashVal = liquidSchemesVal + (hybridSchemesVal * 0.08)
    val reitsVal = (totalVal - equitiesVal - debtVal - cashVal).coerceAtLeast(0.0)

    val equitiesPct = if (totalVal > 0) (equitiesVal / totalVal) * 100.0 else 0.0
    val debtPct = if (totalVal > 0) (debtVal / totalVal) * 100.0 else 0.0
    val cashPct = if (totalVal > 0) (cashVal / totalVal) * 100.0 else 0.0
    val reitsPct = if (totalVal > 0) (reitsVal / totalVal) * 100.0 else 0.0

    val colorEquity = Color(0xFF1E3A8A)
    val colorDebt = Color(0xFF0891B2)
    val colorCash = Color(0xFF059669)
    val colorReits = Color(0xFFD97706)

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Consolidated Asset Split"
    val assetClassesLabel = "4 Asset Classes"
    // =========================================================================

    WsElevatedCard(
        modifier = Modifier.bounceClick(scaleDown = 0.985f, onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = assetClassesLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (equitiesPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(equitiesPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(colorEquity)
                    )
                }
                if (debtPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(debtPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(colorDebt)
                    )
                }
                if (cashPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(cashPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(colorCash)
                    )
                }
                if (reitsPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(reitsPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(colorReits)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssetSplitGridCell(
                        modifier = Modifier.weight(1f),
                        bulletColor = colorEquity,
                        label = "Equities",
                        rupeeValue = equitiesVal,
                        pct = equitiesPct,
                        isMasked = isMasked
                    )
                    AssetSplitGridCell(
                        modifier = Modifier.weight(1f),
                        bulletColor = colorDebt,
                        label = "Debt & Arbitrage",
                        rupeeValue = debtVal,
                        pct = debtPct,
                        isMasked = isMasked
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssetSplitGridCell(
                        modifier = Modifier.weight(1f),
                        bulletColor = colorCash,
                        label = "Cash & TREPS",
                        rupeeValue = cashVal,
                        pct = cashPct,
                        isMasked = isMasked
                    )
                    AssetSplitGridCell(
                        modifier = Modifier.weight(1f),
                        bulletColor = colorReits,
                        label = "REITs & InvITs",
                        rupeeValue = reitsVal,
                        pct = reitsPct,
                        isMasked = isMasked
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetSplitGridCell(
    modifier: Modifier = Modifier,
    bulletColor: Color,
    label: String,
    rupeeValue: Double,
    pct: Double,
    isMasked: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(bulletColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WsSlateTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatWsInr(rupeeValue, isMasked),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " | ${String.format(Locale.US, "%.0f%%", pct)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = bulletColor
                )
            }
        }
    }
}

/**
 * Card D: Sector Breakdown Card with ranked top 6 exposure and animated progress bars.
 */
@Composable
private fun SectorBreakdownCard(
    analytics: PortfolioAnalyticsSummary,
    onNavigateToXRay: () -> Unit,
    onCardClick: () -> Unit = {}
) {
    val gradient = LocalVaultGradient.current
    val topSectors = analytics.sectorBreakdown.take(6)
    val maxWeight = topSectors.maxOfOrNull { it.portfolioWeightPercent } ?: 1.0

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Sector Breakdown"
    val cardSubtitle = "Ranked exposure by total AUM weighting"
    val badgeLabel = "Top 6 Sectors"
    // =========================================================================

    WsElevatedCard(
        modifier = Modifier.bounceClick(scaleDown = 0.985f, onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cardTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = cardSubtitle,
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { onNavigateToXRay() }
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradient.startColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topSectors.forEachIndexed { index, sector ->
                    val fraction = if (maxWeight > 0.0) {
                        (sector.portfolioWeightPercent / maxWeight).toFloat().coerceIn(0.05f, 1f)
                    } else 0.05f

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d", index + 1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = gradient.startColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sector.sectorName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${sector.stockCount} Stocks",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", sector.portfolioWeightPercent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WsSlateTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(gradient.startColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMarketCapDelta(actual: Double, target: Double): String {
    val delta = actual - target
    return when {
        abs(delta) <= 2.0 -> "Balanced"
        delta > 0 -> "+${roundOne(delta)}% Overweight"
        else -> "-${roundOne(abs(delta))}% Underweight"
    }
}

private fun roundOne(num: Double): Double {
    return (num * 10.0).roundToInt() / 10.0
}
