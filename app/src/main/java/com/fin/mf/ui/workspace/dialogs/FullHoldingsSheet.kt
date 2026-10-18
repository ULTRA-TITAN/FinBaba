package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.UnderlyingHolding
import com.fin.mf.data.engine.HoldingsEngine
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale

private val WsBgWhite = Color(0xFFF8FAFC)

/**
 * Full Portfolio Look-Through Constituents Modal Dialog with:
 * 1. 100% Uncapped instrument listing.
 * 2. Multi-Asset categorization tabs: All Holdings, Equity, Debt & Money Market, Cash & Net Assets.
 * 3. Fund Analytical Summary Bar with concentration metrics and asset split.
 * 4. High-precision rank indexing (#1 to #N), asset class tags, and tier pills.
 * 5. Reactive multi-field search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHoldingsModal(
    schemeName: String,
    holdings: List<UnderlyingHolding>,
    schemeValue: Double,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    var filterText by remember { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val sortedHoldings = remember(holdings) { holdings.sortedByDescending { it.weightPercent } }
    val totalCount = sortedHoldings.size
    val top5Weight = remember(sortedHoldings) { sortedHoldings.take(5).sumOf { it.weightPercent } }
    val top10Weight = remember(sortedHoldings) { sortedHoldings.take(10).sumOf { it.weightPercent } }

    val equityHoldings = remember(sortedHoldings) { sortedHoldings.filter { HoldingsEngine.isEquityHolding(it) } }
    val debtHoldings = remember(sortedHoldings) { sortedHoldings.filter { HoldingsEngine.isDebtHolding(it) } }
    val cashHoldings = remember(sortedHoldings) { sortedHoldings.filter { HoldingsEngine.isCashHolding(it) } }

    val equityWeight = remember(equityHoldings) { equityHoldings.sumOf { it.weightPercent } }
    val debtWeight = remember(debtHoldings) { debtHoldings.sumOf { it.weightPercent } }
    val cashWeight = remember(cashHoldings, equityWeight, debtWeight) {
        val rawCash = cashHoldings.sumOf { it.weightPercent }
        if (rawCash > 0.0) rawCash else (100.0 - equityWeight - debtWeight).coerceAtLeast(0.0)
    }

    val categoryFiltered = remember(selectedCategory, sortedHoldings, equityHoldings, debtHoldings, cashHoldings) {
        when (selectedCategory) {
            "Equity" -> equityHoldings
            "Debt" -> debtHoldings
            "Cash" -> cashHoldings
            else -> sortedHoldings
        }
    }

    val finalFiltered = remember(categoryFiltered, filterText) {
        if (filterText.isBlank()) categoryFiltered else {
            val q = filterText.trim()
            categoryFiltered.filter {
                it.companyName.contains(q, ignoreCase = true) ||
                        it.ticker.contains(q, ignoreCase = true) ||
                        it.sector.contains(q, ignoreCase = true) ||
                        it.marketCapBucket.contains(q, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WsBgWhite
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Full Portfolio Constituents",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$schemeName • $totalCount Holdings",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = gradient.startColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WsSurfaceWhite)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search stock, issuer, ticker, or sector...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            color = WsSlateTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gradient.startColor,
                            unfocusedBorderColor = WsSlateBorder,
                            cursorColor = gradient.startColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fund Analytical Summary Bar
                    Surface(
                        color = WsSurfaceWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "TOTAL HOLDINGS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextSecondary
                                )
                                Text(
                                    text = "$totalCount Instruments",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WsSlateTextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CONCENTRATION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextSecondary
                                )
                                Text(
                                    text = "Top 5: ${String.format(Locale.US, "%.1f", top5Weight)}% • Top 10: ${String.format(Locale.US, "%.1f", top10Weight)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = gradient.startColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ASSET SPLIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextSecondary
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", equityWeight)}% Eq • ${String.format(Locale.US, "%.1f", debtWeight + cashWeight)}% D/C",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsAccentRoyal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multi-Asset Categorization Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "All" to "All Holdings ($totalCount)",
                            "Equity" to "Equity (${equityHoldings.size})",
                            "Debt" to "Debt & Money Market (${debtHoldings.size})",
                            "Cash" to "Cash & Net Assets (${cashHoldings.size})"
                        ).forEach { (catKey, catLabel) ->
                            val isSel = selectedCategory == catKey
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = catKey },
                                label = {
                                    Text(
                                        text = catLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Displaying ${finalFiltered.size} of $totalCount constituents",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items = finalFiltered, key = { _, it -> it.ticker }) { index, holding ->
                            val rupeeContribution = schemeValue * (holding.weightPercent / 100.0)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WsSlateTextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = holding.companyName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WsSlateTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(${holding.ticker})",
                                                    fontSize = 10.sp,
                                                    color = WsSlateTextSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ModalHoldingAssetBadge(holding)
                                                ModalHoldingTierBadge(holding)
                                                Text(
                                                    text = "• ${holding.sector}",
                                                    fontSize = 10.sp,
                                                    color = WsSlateTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f%%", holding.weightPercent),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = gradient.startColor
                                        )
                                        Text(
                                            text = formatWsInr(rupeeContribution, isMasked),
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
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
private fun ModalHoldingAssetBadge(holding: UnderlyingHolding) {
    val (label, bg, fg) = when {
        HoldingsEngine.isCashHolding(holding) -> Triple("Cash", Color(0xFFF1F5F9), Color(0xFF475569))
        HoldingsEngine.isDebtHolding(holding) -> Triple("Debt", Color(0xFFECFDF5), Color(0xFF047857))
        else -> Triple("Equity", Color(0xFFEEF2FF), Color(0xFF4338CA))
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun ModalHoldingTierBadge(holding: UnderlyingHolding) {
    val (label, bg, fg) = when {
        HoldingsEngine.isCashHolding(holding) -> Triple("Liquid/TREPS", Color(0xFFF8FAFC), Color(0xFF64748B))
        HoldingsEngine.isDebtHolding(holding) -> {
            val tier = when {
                holding.companyName.contains("Sovereign", ignoreCase = true) || holding.sector.contains("Sovereign", ignoreCase = true) -> "Sovereign G-Sec"
                holding.ticker.contains("AAA", ignoreCase = true) || holding.companyName.contains("AAA", ignoreCase = true) -> "AAA Rated"
                holding.ticker.contains("CP", ignoreCase = true) || holding.ticker.contains("CD", ignoreCase = true) -> "A1+ CP"
                holding.companyName.contains("Treasury", ignoreCase = true) -> "T-Bill"
                else -> "Debt Note"
            }
            Triple(tier, Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        }
        else -> {
            when (holding.marketCapBucket) {
                "Large Cap" -> Triple("Large Cap", Color(0xFFEFF6FF), Color(0xFF2563EB))
                "Mid Cap" -> Triple("Mid Cap", Color(0xFFF0FDF4), Color(0xFF15803D))
                "Small Cap" -> Triple("Small Cap", Color(0xFFFFFBEB), Color(0xFFB45309))
                "Micro Cap" -> Triple("Micro Cap", Color(0xFFFAF5FF), Color(0xFF7E22CE))
                else -> Triple(holding.marketCapBucket.ifBlank { "Equity" }, Color(0xFFF1F5F9), Color(0xFF475569))
            }
        }
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
