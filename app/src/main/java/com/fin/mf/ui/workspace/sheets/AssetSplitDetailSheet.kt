package com.fin.mf.ui.workspace.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale

/**
 * Full-height Modal Bottom Sheet providing complete multi-asset decomposition
 * across Equities, Debt, Cash & TREPS, and Alternative instruments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSplitDetailSheet(
    analytics: PortfolioAnalyticsSummary,
    schemes: List<MutualFundScheme>,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategoryTab by remember { mutableStateOf("Equities") }

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

    // Filter instruments from topHoldings
    val equityHoldings = remember(analytics.topHoldings) {
        analytics.topHoldings.filter { !it.marketCapBucket.contains("Debt", ignoreCase = true) }
    }
    val debtHoldings = remember(analytics.topHoldings) {
        analytics.topHoldings.filter {
            it.marketCapBucket.contains("Debt", ignoreCase = true) ||
            it.sector.contains("Debt", ignoreCase = true) ||
            it.companyName.contains("Bond", ignoreCase = true) ||
            it.companyName.contains("G-Sec", ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Comprehensive Asset Allocation Breakdown",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "Cross-Fund Institutional Asset Class Decomposition",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment asset bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (equitiesPct > 0) {
                    Box(modifier = Modifier.weight(equitiesPct.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(colorEquity))
                }
                if (debtPct > 0) {
                    Box(modifier = Modifier.weight(debtPct.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(colorDebt))
                }
                if (cashPct > 0) {
                    Box(modifier = Modifier.weight(cashPct.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(colorCash))
                }
                if (reitsPct > 0) {
                    Box(modifier = Modifier.weight(reitsPct.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(colorReits))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Asset class selection tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssetClassTabItem(
                    modifier = Modifier.weight(1f),
                    label = "Equities",
                    pct = equitiesPct,
                    color = colorEquity,
                    isSelected = selectedCategoryTab == "Equities",
                    onClick = { selectedCategoryTab = "Equities" }
                )
                AssetClassTabItem(
                    modifier = Modifier.weight(1f),
                    label = "Debt",
                    pct = debtPct,
                    color = colorDebt,
                    isSelected = selectedCategoryTab == "Debt",
                    onClick = { selectedCategoryTab = "Debt" }
                )
                AssetClassTabItem(
                    modifier = Modifier.weight(1f),
                    label = "Cash & TREPS",
                    pct = cashPct,
                    color = colorCash,
                    isSelected = selectedCategoryTab == "Cash & TREPS",
                    onClick = { selectedCategoryTab = "Cash & TREPS" }
                )
                AssetClassTabItem(
                    modifier = Modifier.weight(1f),
                    label = "Alternatives",
                    pct = reitsPct,
                    color = colorReits,
                    isSelected = selectedCategoryTab == "Alternatives",
                    onClick = { selectedCategoryTab = "Alternatives" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Class Detailed Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedCategoryTab) {
                    "Equities" -> {
                        item {
                            AssetClassSummaryCard(
                                title = "Equities & Business Stakes",
                                rupeeValue = equitiesVal,
                                weightPct = equitiesPct,
                                instrumentCount = equityHoldings.size,
                                color = colorEquity,
                                isMasked = isMasked,
                                description = "Direct equities, diversified equity mutual funds, and international tech exposure."
                            )
                        }
                        item {
                            Text(
                                text = "TOP EQUITY HOLDINGS (${equityHoldings.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(equityHoldings.size, key = { equityHoldings[it].ticker }) { idx ->
                            val item = equityHoldings[idx]
                            InstrumentDetailRow(
                                rank = idx + 1,
                                name = item.companyName,
                                ticker = item.ticker,
                                tag = "${item.marketCapBucket} • ${item.sector}",
                                weightPct = item.portfolioWeightPercent,
                                rupeeVal = item.totalRupeeExposure,
                                accentColor = colorEquity,
                                isMasked = isMasked,
                                contributingSchemes = item.schemeContributions.map { it.schemeName }
                            )
                        }
                    }
                    "Debt" -> {
                        item {
                            AssetClassSummaryCard(
                                title = "Debt, Arbitrage & Fixed Income",
                                rupeeValue = debtVal,
                                weightPct = debtPct,
                                instrumentCount = debtHoldings.size.coerceAtLeast(schemes.count { it.assetClass.contains("Debt") }),
                                color = colorDebt,
                                isMasked = isMasked,
                                description = "Government Sovereign G-Secs, Treasury Bills, AAA Corporate NCDs, and Arbitrage positions."
                            )
                        }
                        item {
                            Text(
                                text = "FIXED INCOME & SOVEREIGN EXPOSURE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        if (debtHoldings.isNotEmpty()) {
                            items(debtHoldings.size, key = { debtHoldings[it].ticker }) { idx ->
                                val item = debtHoldings[idx]
                                InstrumentDetailRow(
                                    rank = idx + 1,
                                    name = item.companyName,
                                    ticker = item.ticker,
                                    tag = "Fixed Income • Sovereign / AAA",
                                    weightPct = item.portfolioWeightPercent,
                                    rupeeVal = item.totalRupeeExposure,
                                    accentColor = colorDebt,
                                    isMasked = isMasked,
                                    contributingSchemes = item.schemeContributions.map { it.schemeName }
                                )
                            }
                        } else {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = WsSurfaceWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Fixed Income Allocations via Hybrid/Debt Funds", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Held across funds with conservative duration profile and sovereign G-Sec collateral.", fontSize = 11.sp, color = WsSlateTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                    "Cash & TREPS" -> {
                        item {
                            AssetClassSummaryCard(
                                title = "Cash, Margin & Overnight TREPS",
                                rupeeValue = cashVal,
                                weightPct = cashPct,
                                instrumentCount = 1,
                                color = colorCash,
                                isMasked = isMasked,
                                description = "CCIL Tri-Party Repo (TREPS), overnight settlement collateral, and liquid mutual fund balances."
                            )
                        }
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = WsSurfaceWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CCIL Tri-Party Repo & Liquid Cash", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Overnight Sovereign Collateral • Daily Liquidity", fontSize = 11.sp, color = WsSlateTextSecondary)
                                        }
                                        Text(formatWsInr(cashVal, isMasked), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colorCash)
                                    }
                                }
                            }
                        }
                    }
                    "Alternatives" -> {
                        item {
                            AssetClassSummaryCard(
                                title = "Real Estate, InvITs & Commodities",
                                rupeeValue = reitsVal,
                                weightPct = reitsPct,
                                instrumentCount = 1,
                                color = colorReits,
                                isMasked = isMasked,
                                description = "Infrastructure Investment Trusts (InvITs), Real Estate Investment Trusts (REITs), and Gold ETF hedges."
                            )
                        }
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = WsSurfaceWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Institutional Hybrid Residual & Commodity Exposure", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Alternative Inflation Hedge Assets", fontSize = 11.sp, color = WsSlateTextSecondary)
                                        }
                                        Text(formatWsInr(reitsVal, isMasked), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colorReits)
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

@Composable
private fun AssetClassTabItem(
    modifier: Modifier = Modifier,
    label: String,
    pct: Double,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else WsSlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else WsSlateTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${String.format(Locale.US, "%.0f", pct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

@Composable
private fun AssetClassSummaryCard(
    title: String,
    rupeeValue: Double,
    weightPct: Double,
    instrumentCount: Int,
    color: Color,
    isMasked: Boolean,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WsSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$instrumentCount Instruments",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatWsInr(rupeeValue, isMasked),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${String.format(Locale.US, "%.2f", weightPct)}% of total)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = WsSlateTextSecondary
            )
        }
    }
}

@Composable
private fun InstrumentDetailRow(
    rank: Int,
    name: String,
    ticker: String,
    tag: String,
    weightPct: Double,
    rupeeVal: Double,
    accentColor: Color,
    isMasked: Boolean,
    contributingSchemes: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = WsSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, WsSlateBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        text = "#$rank",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.width(28.dp)
                    )
                    Column {
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WsSlateTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$ticker • $tag",
                            fontSize = 10.5.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", weightPct)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = formatWsInr(rupeeVal, isMasked),
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
            }

            if (contributingSchemes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val schemeNames = contributingSchemes.take(2).joinToString(", ") { it.take(18) } +
                        if (contributingSchemes.size > 2) " +${contributingSchemes.size - 2} more" else ""
                Text(
                    text = "Held in: $schemeNames",
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
