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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsAllocationBar
import com.fin.mf.ui.workspace.components.WsCrimsonNegative
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsEmeraldPositive
import com.fin.mf.ui.workspace.components.WsMetricPillar
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSchemeSummaryCard
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.formatWsInr
import com.fin.mf.ui.workspace.components.formatWsPercent
import com.fin.mf.ui.workspace.components.wsSliceColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WsEmeraldSoft = SemanticFinanceColors.ProfitGreenBackground
private val WsCrimsonSoft = SemanticFinanceColors.LossRedBackground
private val WsAmberWarning = SemanticFinanceColors.BenchmarkAmber

/**
 * Tab #1: Executive overview showing hero valuation card, asset allocation bar,
 * fee drag, and largest scheme holdings.
 */
@Composable
fun WorkspaceOverviewTab(
    uiState: PortfolioUiState,
    onSyncClick: () -> Unit,
    onInspectScheme: (Int) -> Unit,
    onNavigateToXRay: () -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val analytics = uiState.analytics ?: return
    val isMasked = uiState.isPrivacyMasked
    val schemes = uiState.portfolioData?.schemes.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.portfolioData?.portfolioName ?: "Institutional Vault",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
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
                            color = Color.White
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
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WsMetricPillar(
                                label = "Invested Principal",
                                value = formatWsInr(analytics.totalInvested, isMasked),
                                valueColor = Color.White,
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                            WsMetricPillar(
                                label = "Net Capital Gain",
                                value = formatWsInr(analytics.totalGain, isMasked, showPlus = true),
                                valueColor = if (analytics.totalGain >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                            WsMetricPillar(
                                label = "Active Schemes",
                                value = "${schemes.size} Schemes",
                                valueColor = Color.White,
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                        }
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
