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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.SectorExposure
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale

/**
 * Full-height Modal Bottom Sheet providing look-through Sector exposure
 * and constituent company matrix across all portfolio schemes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorDetailSheet(
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gradient = LocalVaultGradient.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var expandedSectorName by remember { mutableStateOf<String?>(null) }

    // Map stocks to sectors
    val stocksBySector = remember(analytics.topHoldings) {
        analytics.topHoldings.groupBy { it.sector.ifBlank { "Diversified" } }
    }

    val maxSectorWeight = remember(analytics.sectorBreakdown) {
        analytics.sectorBreakdown.maxOfOrNull { it.portfolioWeightPercent } ?: 1.0
    }

    // Filter sectors and constituent stocks by search query
    val filteredSectors = remember(analytics.sectorBreakdown, searchQuery, stocksBySector) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) {
            analytics.sectorBreakdown
        } else {
            analytics.sectorBreakdown.filter { sector ->
                sector.sectorName.lowercase(Locale.ROOT).contains(q) ||
                stocksBySector[sector.sectorName].orEmpty().any {
                    it.companyName.lowercase(Locale.ROOT).contains(q) ||
                    it.ticker.lowercase(Locale.ROOT).contains(q)
                }
            }
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
                        text = "Complete Sector Exposure & Constituent Matrix",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "Cross-Fund Institutional Sector Allocation (${analytics.sectorBreakdown.size} Sectors)",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search sector, industry, or stock...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gradient.startColor,
                    unfocusedBorderColor = WsSlateBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sectors List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredSectors,
                    key = { it.sectorName }
                ) { sector ->
                    val isExpanded = expandedSectorName == sector.sectorName || searchQuery.isNotBlank()
                    val sectorStocks = stocksBySector[sector.sectorName].orEmpty()

                    SectorRowCard(
                        sector = sector,
                        stocks = sectorStocks,
                        maxWeight = maxSectorWeight,
                        isExpanded = isExpanded,
                        isMasked = isMasked,
                        onToggle = {
                            expandedSectorName = if (expandedSectorName == sector.sectorName) null else sector.sectorName
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectorRowCard(
    sector: SectorExposure,
    stocks: List<AggregatedStockExposure>,
    maxWeight: Double,
    isExpanded: Boolean,
    isMasked: Boolean,
    onToggle: () -> Unit
) {
    val relativeProgress = (sector.portfolioWeightPercent / maxWeight).toFloat().coerceIn(0.04f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WsSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
    ) {
        Column {
            // Clickable Sector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sector.sectorName,
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
                                text = "${sector.stockCount} Stocks",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar representing relative weighting
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(relativeProgress)
                                .background(WsPrimaryIndigo)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", sector.portfolioWeightPercent)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsPrimaryIndigo
                        )
                        Text(
                            text = formatWsInr(sector.totalRupeeValue, isMasked),
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = WsSlateTextSecondary
                    )
                }
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
                            text = "No constituent breakdown available.",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
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
                                            text = "${stock.ticker} • ${stock.marketCapBucket}",
                                            fontSize = 10.5.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", stock.portfolioWeightPercent)}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsPrimaryIndigo
                                        )
                                        Text(
                                            text = formatWsInr(stock.totalRupeeExposure, isMasked),
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                }

                                if (stock.schemeContributions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val contribSummary = stock.schemeContributions
                                        .take(2)
                                        .joinToString(", ") { c ->
                                            "${c.schemeName.take(18)} (${String.format(Locale.US, "%.1f", c.weightInSchemePercent)}%)"
                                        } + if (stock.schemeContributions.size > 2) " +${stock.schemeContributions.size - 2} more" else ""

                                    Text(
                                        text = "Contributing Funds: $contribSummary",
                                        fontSize = 9.5.sp,
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
