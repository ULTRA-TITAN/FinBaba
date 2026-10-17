package com.fin.mf.ui.workspace.sheets

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.MarketCapMandate
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.components.bounceClick
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-height Modal Bottom Sheet providing deep-dive look-through breakdown across SEBI Market Cap tiers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketCapDetailSheet(
    analytics: PortfolioAnalyticsSummary,
    mandate: MarketCapMandate,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedTier by remember { mutableStateOf<String?>("Large Cap") }

    val largeSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Large", ignoreCase = true) }
    val midSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Mid", ignoreCase = true) }
    val smallSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Small", ignoreCase = true) }
    val microSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Micro", ignoreCase = true) }

    val largeWeight = largeSlice?.weightPercent ?: 0.0
    val midWeight = midSlice?.weightPercent ?: 0.0
    val smallWeight = smallSlice?.weightPercent ?: 0.0
    val nonLargeMidSmallSlices = analytics.marketCapBreakdown.filter {
        !it.label.contains("Large", ignoreCase = true) &&
        !it.label.contains("Mid", ignoreCase = true) &&
        !it.label.contains("Small", ignoreCase = true)
    }
    val microWeight = nonLargeMidSmallSlices.sumOf { it.weightPercent }.coerceAtLeast(microSlice?.weightPercent ?: 0.0)
    val microRupeeVal = nonLargeMidSmallSlices.sumOf { it.rupeeValue }.coerceAtLeast(microSlice?.rupeeValue ?: 0.0)

    // Group stocks by market cap bucket
    val stocksByTier = remember(analytics.topHoldings) {
        val large = mutableListOf<AggregatedStockExposure>()
        val mid = mutableListOf<AggregatedStockExposure>()
        val small = mutableListOf<AggregatedStockExposure>()
        val micro = mutableListOf<AggregatedStockExposure>()

        for (stock in analytics.topHoldings) {
            when {
                stock.marketCapBucket.contains("Large", ignoreCase = true) -> large.add(stock)
                stock.marketCapBucket.contains("Mid", ignoreCase = true) -> mid.add(stock)
                stock.marketCapBucket.contains("Small", ignoreCase = true) -> small.add(stock)
                else -> micro.add(stock)
            }
        }
        mapOf(
            "Large Cap" to large.sortedByDescending { it.totalRupeeExposure },
            "Mid Cap" to mid.sortedByDescending { it.totalRupeeExposure },
            "Small Cap" to small.sortedByDescending { it.totalRupeeExposure },
            "Micro & Cash" to micro.sortedByDescending { it.totalRupeeExposure }
        )
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
                        text = "Market Cap Deep Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "SEBI Mandate Alignment & Look-Through Stock Matrix",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mandate Comparison Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = WsSurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MANDATE DRIFT SUMMARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = WsSlateTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MandateDriftChip(
                            modifier = Modifier.weight(1f),
                            tier = "Large",
                            actual = largeWeight,
                            target = mandate.largeCapTarget,
                            color = SemanticFinanceColors.SebiLargeCap
                        )
                        MandateDriftChip(
                            modifier = Modifier.weight(1f),
                            tier = "Mid",
                            actual = midWeight,
                            target = mandate.midCapTarget,
                            color = SemanticFinanceColors.SebiMidCap
                        )
                        MandateDriftChip(
                            modifier = Modifier.weight(1f),
                            tier = "Small",
                            actual = smallWeight,
                            target = mandate.smallCapTarget,
                            color = SemanticFinanceColors.SebiSmallCap
                        )
                        MandateDriftChip(
                            modifier = Modifier.weight(1f),
                            tier = "Micro",
                            actual = microWeight,
                            target = mandate.microCapTarget,
                            color = SemanticFinanceColors.SebiMicroCap
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Tier Accordions
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    TierAccordionCard(
                        tierName = "Large Cap",
                        tierColor = SemanticFinanceColors.SebiLargeCap,
                        stocks = stocksByTier["Large Cap"].orEmpty(),
                        weightPct = largeWeight,
                        rupeeVal = largeSlice?.rupeeValue ?: 0.0,
                        isExpanded = expandedTier == "Large Cap",
                        isMasked = isMasked,
                        onToggle = {
                            expandedTier = if (expandedTier == "Large Cap") null else "Large Cap"
                        }
                    )
                }

                item {
                    TierAccordionCard(
                        tierName = "Mid Cap",
                        tierColor = SemanticFinanceColors.SebiMidCap,
                        stocks = stocksByTier["Mid Cap"].orEmpty(),
                        weightPct = midWeight,
                        rupeeVal = midSlice?.rupeeValue ?: 0.0,
                        isExpanded = expandedTier == "Mid Cap",
                        isMasked = isMasked,
                        onToggle = {
                            expandedTier = if (expandedTier == "Mid Cap") null else "Mid Cap"
                        }
                    )
                }

                item {
                    TierAccordionCard(
                        tierName = "Small Cap",
                        tierColor = SemanticFinanceColors.SebiSmallCap,
                        stocks = stocksByTier["Small Cap"].orEmpty(),
                        weightPct = smallWeight,
                        rupeeVal = smallSlice?.rupeeValue ?: 0.0,
                        isExpanded = expandedTier == "Small Cap",
                        isMasked = isMasked,
                        onToggle = {
                            expandedTier = if (expandedTier == "Small Cap") null else "Small Cap"
                        }
                    )
                }

                item {
                    TierAccordionCard(
                        tierName = "Micro & Cash",
                        tierColor = SemanticFinanceColors.SebiMicroCap,
                        stocks = stocksByTier["Micro & Cash"].orEmpty(),
                        weightPct = microWeight,
                        rupeeVal = microRupeeVal,
                        isExpanded = expandedTier == "Micro & Cash",
                        isMasked = isMasked,
                        onToggle = {
                            expandedTier = if (expandedTier == "Micro & Cash") null else "Micro & Cash"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MandateDriftChip(
    modifier: Modifier = Modifier,
    tier: String,
    actual: Double,
    target: Double,
    color: Color
) {
    val drift = actual - target
    val driftColor = when {
        drift > 2.0 -> Color(0xFFD97706)
        drift < -2.0 -> Color(0xFFDC2626)
        else -> Color(0xFF059669)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, WsSlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = tier,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${actual.roundToInt()}% vs ${target.roundToInt()}%",
                fontSize = 9.sp,
                color = WsSlateTextSecondary
            )
            Text(
                text = if (drift >= 0) "+${String.format(Locale.US, "%.1f", drift)}%" else "${String.format(Locale.US, "%.1f", drift)}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = driftColor
            )
        }
    }
}

@Composable
private fun TierAccordionCard(
    tierName: String,
    tierColor: Color,
    stocks: List<AggregatedStockExposure>,
    weightPct: Double,
    rupeeVal: Double,
    isExpanded: Boolean,
    isMasked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WsSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
    ) {
        Column {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(tierColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tierName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${stocks.size} Stocks",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.2f", weightPct)}% • ${formatWsInr(rupeeVal, isMasked)}",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = WsSlateTextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(color = WsSlateBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (stocks.isEmpty()) {
                        Text(
                            text = "No underlying stocks in this tier.",
                            fontSize = 12.sp,
                            color = WsSlateTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        stocks.forEachIndexed { index, stock ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stock.companyName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WsSlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stock.ticker} • ${stock.sector}",
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", stock.portfolioWeightPercent)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tierColor
                                        )
                                        Text(
                                            text = formatWsInr(stock.totalRupeeExposure, isMasked),
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                }

                                // Contributing schemes pill summary
                                if (stock.schemeContributions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val contribSummary = stock.schemeContributions
                                        .take(2)
                                        .joinToString(", ") { c ->
                                            "${c.schemeName.take(18)} (${String.format(Locale.US, "%.1f", c.weightInSchemePercent)}%)"
                                        } + if (stock.schemeContributions.size > 2) " +${stock.schemeContributions.size - 2} more" else ""

                                    Text(
                                        text = "Held in: $contribSummary",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (index < stocks.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
    }
}
