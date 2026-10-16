package com.fin.mf.ui.workspace.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.SectorExposure
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.formatWsInr
import com.fin.mf.ui.workspace.components.wsSliceColor
import java.util.Locale

private val WsAmberSoft = Color(0xFFFEF3C7)
private val WsAmberWarning = SemanticFinanceColors.BenchmarkAmber

/**
 * Tab #3: Institutional X-Ray (Look-Through Sector Breakdown & Stock Overlap Matrix).
 */
@Composable
fun WorkspaceXRayTab(
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
fun WsSectorExposureRow(
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
fun WsStockOverlapCard(
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
