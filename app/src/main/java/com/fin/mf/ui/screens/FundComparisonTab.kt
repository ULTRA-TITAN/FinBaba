package com.fin.mf.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.comparison.ComparisonFundLineColors
import com.fin.mf.ui.comparison.ComparisonMatrixTable
import com.fin.mf.ui.comparison.MultiFundComparisonChart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CmpSurfaceWhite = Color(0xFFFFFFFF)
private val CmpSlatePrimary = Color(0xFF0F172A)
private val CmpSlateSecondary = Color(0xFF475569)
private val CmpSlateBorder = Color(0xFFE2E8F0)
private val CmpAccentRoyal = Color(0xFF2563EB)
private val CmpPortfolioPurple = SemanticFinanceColors.PortfolioPurple

private val ComparisonTimeframes = listOf(
    ChartTimeframe.SEVEN_DAYS,
    ChartTimeframe.ONE_MONTH,
    ChartTimeframe.ONE_YEAR,
    ChartTimeframe.THREE_YEARS,
    ChartTimeframe.FIVE_YEARS,
    ChartTimeframe.TEN_YEARS,
    ChartTimeframe.CUSTOM
)

/**
 * Dedicated Multi-Fund Comparison Tab:
 * 1. Peer selection across up to 5 mutual funds (from active portfolio or universal AMFI search).
 * 2. Custom date-to-date calendar range window with DatePicker dialogs.
 * 3. Base-100 normalized multi-line performance chart via [MultiFundComparisonChart].
 * 4. Comprehensive side-by-side factor matrix for up to 5 funds via [ComparisonMatrixTable].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundComparisonTab(
    portfolioViewModel: PortfolioViewModel,
    modifier: Modifier = Modifier
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAmfiSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomDatePickers by rememberSaveable { mutableStateOf(false) }

    val calendarDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    fun showSystemDatePicker(initialEpochMs: Long?, onDatePicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initialEpochMs != null && initialEpochMs > 0) cal.timeInMillis = initialEpochMs
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                onDatePicked(pickedCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Peer Selector Card (Up to 5 Funds)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmpSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Compare Mutual Funds (${uiState.comparisonSchemeCodes.size}/5)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmpSlatePrimary
                            )
                            Text(
                                text = "Select 2 to 5 funds to analyze normalized trajectories",
                                fontSize = 11.sp,
                                color = CmpSlateSecondary
                            )
                        }

                        if (uiState.comparisonSchemeCodes.size < 5) {
                            OutlinedButton(
                                onClick = { showAmfiSearchDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Fund", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Funds Horizontal Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.comparisonFunds.forEachIndexed { index, fund ->
                            val color = ComparisonFundLineColors.getOrElse(index) { gradient.startColor }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = color.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = fund.schemeName.take(18) + if (fund.schemeName.length > 18) "..." else "",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { portfolioViewModel.toggleComparisonScheme(fund.schemeCode) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = color,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Selection from Portfolio
                    Text(
                        text = "Quick Select from My Portfolio:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CmpSlateSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.portfolioData?.schemes.orEmpty().forEach { scheme ->
                            val isSelected = uiState.comparisonSchemeCodes.contains(scheme.schemeCode)
                            FilterChip(
                                selected = isSelected,
                                onClick = { portfolioViewModel.toggleComparisonScheme(scheme.schemeCode) },
                                label = {
                                    Text(
                                        text = scheme.schemeName.take(20) + "...",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Timeframe Selector & Custom Date Window
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmpSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Analysis Horizon",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CmpSlatePrimary
                        )

                        // Portfolio Composite Overlay Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Overlay Vault Avg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isPortfolioAvgOverlayEnabled) CmpPortfolioPurple else CmpSlateSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = uiState.isPortfolioAvgOverlayEnabled,
                                onCheckedChange = { portfolioViewModel.togglePortfolioAvgOverlay() },
                                colors = SwitchDefaults.colors(checkedTrackColor = CmpPortfolioPurple),
                                modifier = Modifier.size(width = 38.dp, height = 24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeframe Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ComparisonTimeframes.forEach { tf ->
                            val isSel = uiState.comparisonTimeframe == tf
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    portfolioViewModel.updateComparisonTimeframe(tf)
                                    showCustomDatePickers = (tf == ChartTimeframe.CUSTOM)
                                },
                                label = { Text(tf.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Custom Date to Date Window
                    AnimatedVisibility(visible = uiState.comparisonTimeframe == ChartTimeframe.CUSTOM) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Custom Calendar Date Window (Date to Date):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CmpSlateSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val startDateLabel = uiState.customComparisonStartEpochMs?.let {
                                    calendarDateFormat.format(Date(it))
                                } ?: "Pick Start Date"

                                val endDateLabel = uiState.customComparisonEndEpochMs?.let {
                                    calendarDateFormat.format(Date(it))
                                } ?: "Pick End Date"

                                OutlinedButton(
                                    onClick = {
                                        showSystemDatePicker(uiState.customComparisonStartEpochMs) { picked ->
                                            portfolioViewModel.setCustomComparisonDateRange(
                                                startEpochMs = picked,
                                                endEpochMs = uiState.customComparisonEndEpochMs
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(startDateLabel, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        showSystemDatePicker(uiState.customComparisonEndEpochMs) { picked ->
                                            portfolioViewModel.setCustomComparisonDateRange(
                                                startEpochMs = uiState.customComparisonStartEpochMs,
                                                endEpochMs = picked
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(endDateLabel, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi-Line Base-100 Performance Canvas Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmpSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Normalized Base-100 Performance Trajectory",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CmpSlatePrimary
                    )
                    Text(
                        text = "Rebased to 100.0 at horizon start • Drag crosshair to scrub",
                        fontSize = 11.sp,
                        color = CmpSlateSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    when {
                        uiState.isComparisonLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CmpAccentRoyal)
                            }
                        }
                        uiState.comparisonFunds.size < 2 -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = null,
                                        tint = CmpSlateSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select 2 to 5 mutual funds to begin normalized peer comparison.",
                                        fontSize = 12.sp,
                                        color = CmpSlateSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {
                            MultiFundComparisonChart(
                                funds = uiState.comparisonFunds,
                                timeframe = uiState.comparisonTimeframe,
                                customStartEpochMs = uiState.customComparisonStartEpochMs,
                                customEndEpochMs = uiState.customComparisonEndEpochMs,
                                isPortfolioOverlayEnabled = uiState.isPortfolioAvgOverlayEnabled,
                                portfolioSchemes = uiState.portfolioData?.schemes.orEmpty()
                            )
                        }
                    }
                }
            }
        }

        // Side-by-Side Factor Comparison Matrix Table (Supports up to 5 funds)
        if (uiState.comparisonFunds.size >= 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CmpSurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Factor & Risk Comparison Matrix",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmpSlatePrimary
                            )
                            Text(
                                text = "${uiState.comparisonFunds.size} Funds",
                                fontSize = 11.sp,
                                color = CmpAccentRoyal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ComparisonMatrixTable(funds = uiState.comparisonFunds)
                    }
                }
            }
        }
    }

    // AMFI Search Dialog
    if (showAmfiSearchDialog) {
        AmfiSearchModal(
            portfolioViewModel = portfolioViewModel,
            onDismiss = { showAmfiSearchDialog = false },
            onSelectScheme = { schemeCode ->
                portfolioViewModel.toggleComparisonScheme(schemeCode)
                showAmfiSearchDialog = false
            }
        )
    }
}

/**
 * Universal AMFI Search dialog allowing selection of any mutual fund scheme with smart filter chips.
 */
@Composable
private fun AmfiSearchModal(
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
    onSelectScheme: (Int) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Mutual Fund to Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        portfolioViewModel.searchAmfiSchemes(it)
                    },
                    placeholder = { Text("Search by name, AMC, alias (e.g. 'kot', 'ppfas')...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
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

                Spacer(modifier = Modifier.height(10.dp))

                // Smart Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Direct - Growth", "Large Cap", "Mid Cap", "Small Cap", "Debt").forEach { cat ->
                        val isSel = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gradient.startColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filteredResults = remember(uiState.amfiSearchResults, selectedCategoryFilter) {
                    val raw = uiState.amfiSearchResults
                    if (selectedCategoryFilter == "All") {
                        raw
                    } else if (selectedCategoryFilter == "Direct - Growth") {
                        raw.filter { it.schemeName.contains("Direct", ignoreCase = true) && it.schemeName.contains("Growth", ignoreCase = true) }
                    } else {
                        raw.filter { it.schemeName.contains(selectedCategoryFilter, ignoreCase = true) }
                    }
                }

                when {
                    uiState.isAmfiSearching -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CmpAccentRoyal)
                        }
                    }
                    filteredResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredResults) { res ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectScheme(res.schemeCode) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = res.schemeName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CmpSlatePrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "AMFI Code: #${res.schemeCode}",
                                            fontSize = 11.sp,
                                            color = gradient.startColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Type any fund name (e.g. 'Parag', 'Quant', 'SBI').",
                                fontSize = 12.sp,
                                color = CmpSlateSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
