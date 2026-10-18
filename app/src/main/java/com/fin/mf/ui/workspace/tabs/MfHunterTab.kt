package com.fin.mf.ui.workspace.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.fin.mf.ui.workspace.components.HairlineScrollProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.UnderlyingHolding
import com.fin.mf.data.engine.HoldingsEngine
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.components.NavChartComponent
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsMetricPillar
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import com.fin.mf.ui.workspace.components.formatWsPercent
import com.fin.mf.ui.workspace.dialogs.FullHoldingsModal
import com.fin.mf.ui.workspace.dialogs.WsEditHoldingDialog
import java.util.Locale

/**
 * Tab #4: MF-Hunter (Renamed from Deep-Dive).
 * Features:
 * 1. Spacious AMFI Scheme Search with smart indexing and category filter chips.
 * 2. Dedicated NavChartComponent (zoomable charts, 2-point delta analyzer, fullscreen mode).
 * 3. Uncapped Multi-Asset Holdings with Analytical Summary Bar and Full Audit Modal.
 */
@Composable
fun WorkspaceMfHunterTab(
    portfolioViewModel: PortfolioViewModel,
    onSaveHoldingUpdate: (Int, Double, Double) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    val allSchemes = uiState.portfolioData?.schemes.orEmpty()
    val scheme = uiState.selectedScheme ?: allSchemes.firstOrNull()

    var showEditModal by remember { mutableStateOf(false) }
    var showFullHoldingsModal by remember { mutableStateOf(false) }
    var selectedHoldingCategory by rememberSaveable { mutableStateOf("All") }
    var mfHunterSearchQuery by remember { mutableStateOf("") }
    var mfHunterFilter by rememberSaveable { mutableStateOf("All") }

    if (scheme == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No scheme selected or portfolio is empty.", fontSize = 14.sp, color = WsSlateTextSecondary)
        }
        return
    }

    val isSchemeInPortfolio = allSchemes.any { it.schemeCode == scheme.schemeCode }
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        HairlineScrollProgressIndicator(listState = listState)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Universal Spacious Search bar in MF-Hunter with Smart Indexing & Filter Chips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MF-Hunter Scheme Finder",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = "Search across Indian mutual funds with smart alias indexing",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = mfHunterSearchQuery,
                            onValueChange = {
                                mfHunterSearchQuery = it
                                portfolioViewModel.searchAmfiSchemes(it)
                            },
                            placeholder = { Text("Search by fund name, AMC, alias (e.g. 'ppfas', 'quant small', 'hdfc')...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                            trailingIcon = {
                                if (mfHunterSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        mfHunterSearchQuery = ""
                                        portfolioViewModel.clearAmfiSearch()
                                    }) {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Smart filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Direct - Growth", "Large Cap", "Mid Cap", "Small Cap", "Debt").forEach { cat ->
                            val isSel = mfHunterFilter == cat
                            Surface(
                                onClick = { mfHunterFilter = cat },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSel) gradient.startColor else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSel) gradient.startColor else WsSlateBorder
                                ),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.uppercase(),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.8.sp,
                                        color = if (isSel) Color.White else WsSlateTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Display spacious search results
                    if (mfHunterSearchQuery.isNotEmpty()) {
                        val filteredResults = remember(uiState.amfiSearchResults, mfHunterFilter) {
                            val raw = uiState.amfiSearchResults
                            if (mfHunterFilter == "All") {
                                raw
                            } else if (mfHunterFilter == "Direct - Growth") {
                                raw.filter { it.schemeName.contains("Direct", ignoreCase = true) && it.schemeName.contains("Growth", ignoreCase = true) }
                            } else {
                                raw.filter { it.schemeName.contains(mfHunterFilter, ignoreCase = true) }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when {
                            uiState.isAmfiSearching -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = WsAccentRoyal)
                                }
                            }
                            filteredResults.isNotEmpty() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredResults.take(6).forEach { result ->
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    portfolioViewModel.selectSchemeForDeepDive(result.schemeCode, result.schemeName)
                                                    mfHunterSearchQuery = ""
                                                    portfolioViewModel.clearAmfiSearch()
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = result.schemeName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = WsSlateTextPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "AMFI Code: #${result.schemeCode}",
                                                        fontSize = 11.sp,
                                                        color = gradient.startColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Tap to Inspect Holdings ➔",
                                                        fontSize = 11.sp,
                                                        color = WsAccentRoyal,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "No matching schemes found. Try a different query or filter.",
                                    fontSize = 12.sp,
                                    color = WsSlateTextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal chips row of active portfolio schemes (+ external inspected scheme if active)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isSchemeInPortfolio) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SemanticFinanceColors.ProfitGreen,
                        border = BorderStroke(1.dp, SemanticFinanceColors.ProfitGreen),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔍 " + scheme.schemeName.take(22).uppercase() + "...",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                allSchemes.forEach { itemScheme ->
                    val isSelected = isSchemeInPortfolio && itemScheme.schemeCode == scheme.schemeCode
                    Surface(
                        onClick = { portfolioViewModel.selectSchemeForDeepDive(itemScheme.schemeCode) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) gradient.startColor else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) gradient.startColor else WsSlateBorder
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemScheme.schemeName.take(24).uppercase() + "...",
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp,
                                color = if (isSelected) Color.White else WsSlateTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Scheme Overview Card
        item {
            WsElevatedCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = "AMFI #${scheme.schemeCode} // ${scheme.category.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp,
                                color = gradient.startColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (isSchemeInPortfolio) {
                            OutlinedButton(
                                onClick = { showEditModal = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Position",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Units", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { showEditModal = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SemanticFinanceColors.ProfitGreen)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to Portfolio", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = scheme.schemeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "${scheme.amc} • Benchmark: ${scheme.benchmark}",
                        fontSize = 12.sp,
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WsMetricPillar(
                            label = "Current Valuation",
                            value = formatWsInr(scheme.currentValue, uiState.isPrivacyMasked),
                            valueColor = WsSlateTextPrimary
                        )
                        WsMetricPillar(
                            label = "Latest NAV (${scheme.lastNavDate})",
                            value = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            valueColor = WsAccentRoyal
                        )
                        WsMetricPillar(
                            label = "Total Return",
                            value = formatWsPercent(scheme.absoluteReturnPercent),
                            valueColor = if (scheme.absoluteGain >= 0) SemanticFinanceColors.ProfitGreen else SemanticFinanceColors.LossRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = WsSlateBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WsMetricPillar(
                            label = "Allotted Units",
                            value = String.format(Locale.US, "%.3f", scheme.units),
                            valueColor = WsSlateTextPrimary
                        )
                        WsMetricPillar(
                            label = "Avg Buy NAV",
                            value = "₹${String.format(Locale.US, "%.2f", scheme.averageBuyNav)}",
                            valueColor = WsSlateTextSecondary
                        )
                        WsMetricPillar(
                            label = "Expense Ratio",
                            value = "${scheme.expenseRatio}%",
                            valueColor = WsSlateTextSecondary
                        )
                    }
                }
            }
        }

        // Dedicated Advanced NAV Chart Component with Zoom
        item {
            NavChartComponent(
                schemeName = scheme.schemeName,
                schemeCode = scheme.schemeCode,
                fullHistory = uiState.selectedSchemeHistory,
                selectedTimeframe = uiState.selectedTimeframe,
                onTimeframeSelected = { portfolioViewModel.updateChartTimeframe(it) }
            )
        }

        // Look-Through Constituents Section: Uncapped Multi-Asset Holdings & Analytical Summary
        item {
            WsElevatedCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    val allHoldings = scheme.holdings
                    val totalHoldingsCount = allHoldings.size
                    val sortedHoldings = remember(allHoldings) { allHoldings.sortedByDescending { it.weightPercent } }

                    val top5Weight = remember(sortedHoldings) { sortedHoldings.take(5).sumOf { it.weightPercent } }
                    val top10Weight = remember(sortedHoldings) { sortedHoldings.take(10).sumOf { it.weightPercent } }

                    val equityHoldings = remember(allHoldings) { allHoldings.filter { HoldingsEngine.isEquityHolding(it) } }
                    val debtHoldings = remember(allHoldings) { allHoldings.filter { HoldingsEngine.isDebtHolding(it) } }
                    val cashHoldings = remember(allHoldings) { allHoldings.filter { HoldingsEngine.isCashHolding(it) } }

                    val equityWeight = remember(equityHoldings) { equityHoldings.sumOf { it.weightPercent } }
                    val debtWeight = remember(debtHoldings) { debtHoldings.sumOf { it.weightPercent } }
                    val cashWeight = remember(cashHoldings, equityWeight, debtWeight) {
                        val rawCash = cashHoldings.sumOf { it.weightPercent }
                        if (rawCash > 0.0) rawCash else (100.0 - equityWeight - debtWeight).coerceAtLeast(0.0)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Look-Through Constituents ($totalHoldingsCount)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "100% uncapped holdings • Full institutional breakdown",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showFullHoldingsModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Audit", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fund Analytical Summary Bar
                    Surface(
                        color = Color(0xFFF8FAFC),
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
                                    text = "$totalHoldingsCount Instruments",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-Asset Categorization Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "All" to "All Holdings ($totalHoldingsCount)",
                            "Equity" to "Equity (${equityHoldings.size})",
                            "Debt" to "Debt & Money Market (${debtHoldings.size})",
                            "Cash" to "Cash & Net Assets (${cashHoldings.size})"
                        ).forEach { (catKey, catLabel) ->
                            val isSel = selectedHoldingCategory == catKey
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedHoldingCategory = catKey },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredHoldings = remember(selectedHoldingCategory, allHoldings, equityHoldings, debtHoldings, cashHoldings) {
                        when (selectedHoldingCategory) {
                            "Equity" -> equityHoldings.sortedByDescending { it.weightPercent }
                            "Debt" -> debtHoldings.sortedByDescending { it.weightPercent }
                            "Cash" -> cashHoldings.sortedByDescending { it.weightPercent }
                            else -> sortedHoldings
                        }
                    }

                    filteredHoldings.forEachIndexed { index, holding ->
                        val rupeeExposure = scheme.currentValue * (holding.weightPercent / 100.0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                            fontWeight = FontWeight.SemiBold,
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
                                        HoldingAssetClassBadge(holding)
                                        HoldingTierBadge(holding)
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
                                    text = formatWsInr(rupeeExposure, uiState.isPrivacyMasked),
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                        }
                        if (index < filteredHoldings.lastIndex) {
                            HorizontalDivider(color = WsSlateBorder)
                        }
                    }
                }
            }
        }
    }
}

    // Edit Holding Dialog
    if (showEditModal) {
        WsEditHoldingDialog(
            scheme = scheme,
            isNewScheme = !isSchemeInPortfolio,
            onDismiss = { showEditModal = false },
            onConfirm = { newUnits, newInvested ->
                showEditModal = false
                if (isSchemeInPortfolio) {
                    onSaveHoldingUpdate(scheme.schemeCode, newUnits, newInvested)
                } else {
                    portfolioViewModel.addSearchedScheme(scheme.schemeCode, newUnits, newInvested)
                }
            }
        )
    }

    // Full Constituents Sheet Dialog
    if (showFullHoldingsModal) {
        FullHoldingsModal(
            schemeName = scheme.schemeName,
            holdings = scheme.holdings,
            schemeValue = scheme.currentValue,
            isMasked = uiState.isPrivacyMasked,
            onDismiss = { showFullHoldingsModal = false }
        )
    }
}

@Composable
internal fun HoldingAssetClassBadge(holding: UnderlyingHolding) {
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
internal fun HoldingTierBadge(holding: UnderlyingHolding) {
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
