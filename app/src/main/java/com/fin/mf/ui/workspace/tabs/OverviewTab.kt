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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSchemeSummaryCard
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
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
@Composable
fun WorkspaceOverviewTab(
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
        // 1. Card A: Net Worth Snapshot Card
        item {
            NetWorthSnapshotCard(
                analytics = analytics,
                isMasked = isMasked
            )
        }

        // 2. Card B: SEBI Market Cap Exposure Card
        item {
            SebiMarketCapExposureCard(
                analytics = analytics,
                isMasked = isMasked
            )
        }

        // 3. Card C: Consolidated Asset Split Card
        item {
            ConsolidatedAssetSplitCard(
                analytics = analytics,
                schemes = schemes,
                isMasked = isMasked
            )
        }

        // 4. Card D: Sector Breakdown Card
        item {
            SectorBreakdownCard(
                analytics = analytics,
                onNavigateToXRay = onNavigateToXRay
            )
        }

        // 5. Institutional Metrics: TER Drag & Stock Overlap Alerts
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

        // 6. Largest Scheme Holdings Header
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
}

/**
 * Card A: Net Worth Snapshot Card with inline INR badge, return pill, and metrics split.
 */
@Composable
private fun NetWorthSnapshotCard(
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean
) {
    val gradient = LocalVaultGradient.current
    val isPositive = analytics.totalGain >= 0
    val returnColor = if (isPositive) Color(0xFF34D399) else Color(0xFFF87171)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, gradient.startColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient.brush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NET WORTH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.95f),
                            letterSpacing = 1.2.sp
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Live Look-Through",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatWsInr(analytics.totalCurrentValue, isMasked),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "INR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = returnColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatWsPercent(analytics.totalReturnPercent)} Return",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Invested Principal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatWsInr(analytics.totalInvested, isMasked),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Net Capital Gain",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatWsInr(analytics.totalGain, isMasked, showPlus = true),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositive) Color(0xFF34D399) else Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card B: SEBI Market Cap Exposure Card with target mandate, progress bar, and 4-tier grid.
 */
@Composable
private fun SebiMarketCapExposureCard(
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean
) {
    val largeSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Large", ignoreCase = true) }
    val midSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Mid", ignoreCase = true) }
    val smallSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Small", ignoreCase = true) }
    val microSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Micro", ignoreCase = true) }

    val largeWeight = largeSlice?.weightPercent ?: 0.0
    val midWeight = midSlice?.weightPercent ?: 0.0
    val smallWeight = smallSlice?.weightPercent ?: 0.0
    val microWeight = microSlice?.weightPercent ?: 0.0

    val largeVal = largeSlice?.rupeeValue ?: 0.0
    val midVal = midSlice?.rupeeValue ?: 0.0
    val smallVal = smallSlice?.rupeeValue ?: 0.0
    val microVal = microSlice?.rupeeValue ?: 0.0

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Market Cap Exposure",
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
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "Breakdown",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target Mandate: 50% Large / 25% Mid / 25% Small",
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
                    statusChip = formatMarketCapDelta(largeWeight, 50.0),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Mid",
                    dotColor = SemanticFinanceColors.SebiMidCap,
                    weightPct = midWeight,
                    rupeeVal = midVal,
                    statusChip = formatMarketCapDelta(midWeight, 25.0),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Small",
                    dotColor = SemanticFinanceColors.SebiSmallCap,
                    weightPct = smallWeight,
                    rupeeVal = smallVal,
                    statusChip = formatMarketCapDelta(smallWeight, 25.0),
                    isMasked = isMasked
                )
                MarketCapTierCell(
                    modifier = Modifier.weight(1f),
                    tierName = "Micro",
                    dotColor = SemanticFinanceColors.SebiMicroCap,
                    weightPct = microWeight,
                    rupeeVal = microVal,
                    statusChip = if (microWeight <= 0.5) "0% Target" else "+${roundOne(microWeight)}% Micro",
                    isMasked = isMasked
                )
            }
        }
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
        shape = RoundedCornerShape(10.dp),
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = WsSlateTextPrimary
            )

            Text(
                text = formatWsInr(rupeeVal, isMasked),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = WsSlateTextSecondary,
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = statusChip,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
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
    isMasked: Boolean
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

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consolidated Asset Split",
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
                        text = "4 Asset Classes",
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
    onNavigateToXRay: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    val topSectors = analytics.sectorBreakdown.take(6)
    val maxWeight = topSectors.maxOfOrNull { it.portfolioWeightPercent } ?: 1.0

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sector Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "Ranked exposure by total AUM weighting",
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
                        text = "Top 6 Sectors",
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
        delta > 0 -> "+${roundOne(delta)}% Over"
        else -> "-${roundOne(abs(delta))}% Under"
    }
}

private fun roundOne(num: Double): Double {
    return (num * 10.0).roundToInt() / 10.0
}
