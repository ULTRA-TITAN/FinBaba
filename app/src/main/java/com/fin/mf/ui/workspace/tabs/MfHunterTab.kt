package com.fin.mf.ui.workspace.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * 3. Individual MF Holdings with Top 10 / Top 15 / All filter toggle and Full Constituents Modal.
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
    var holdingsCountFilter by rememberSaveable { mutableStateOf("Top 15") }
    var holdingsTypeFilter by rememberSaveable { mutableStateOf("All Types") }
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

    LazyColumn(
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
                            FilterChip(
                                selected = isSel,
                                onClick = { mfHunterFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
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
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = {
                            Text(
                                text = "🔍 " + scheme.schemeName.take(22) + "...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SemanticFinanceColors.ProfitGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                allSchemes.forEach { itemScheme ->
                    val isSelected = isSchemeInPortfolio && itemScheme.schemeCode == scheme.schemeCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { portfolioViewModel.selectSchemeForDeepDive(itemScheme.schemeCode) },
                        label = {
                            Text(
                                text = itemScheme.schemeName.take(24) + "...",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = gradient.startColor,
                            selectedLabelColor = Color.White
                        )
                    )
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "AMFI #${scheme.schemeCode} • ${scheme.category}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradient.startColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

        // Look-Through Constituents Section: Top 10 / Top 15 / All + View Full
        item {
            WsElevatedCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Look-Through Constituents (${scheme.holdings.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "Underlying equities and debt instruments",
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
                            Text("View Full", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Holdings Filter Chips: Top 10, Top 15, All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Top 10", "Top 15", "All").forEach { countOpt ->
                            val isSel = holdingsCountFilter == countOpt
                            FilterChip(
                                selected = isSel,
                                onClick = { holdingsCountFilter = countOpt },
                                label = { Text(countOpt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        listOf("All Types", "Equity", "Debt & Cash").forEach { typeOpt ->
                            val isSel = holdingsTypeFilter == typeOpt
                            FilterChip(
                                selected = isSel,
                                onClick = { holdingsTypeFilter = typeOpt },
                                label = { Text(typeOpt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsAccentRoyal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered Holdings Display
                    val filteredHoldings = remember(scheme.holdings, holdingsCountFilter, holdingsTypeFilter) {
                        var list = scheme.holdings
                        if (holdingsTypeFilter == "Equity") {
                            list = list.filterNot { it.sector.contains("Debt", ignoreCase = true) || it.marketCapBucket.contains("Debt", ignoreCase = true) }
                        } else if (holdingsTypeFilter == "Debt & Cash") {
                            list = list.filter { it.sector.contains("Debt", ignoreCase = true) || it.marketCapBucket.contains("Debt", ignoreCase = true) }
                        }
                        val sorted = list.sortedByDescending { it.weightPercent }
                        when (holdingsCountFilter) {
                            "Top 10" -> sorted.take(10)
                            "Top 15" -> sorted.take(15)
                            else -> sorted
                        }
                    }

                    filteredHoldings.forEachIndexed { index, holding ->
                        val rupeeExposure = scheme.currentValue * (holding.weightPercent / 100.0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${holding.companyName} (${holding.ticker})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = "${holding.sector} • ${holding.marketCapBucket}",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
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
