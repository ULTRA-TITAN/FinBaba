package com.fin.mf.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.FundComparisonItem
import com.fin.mf.data.MfApiNavEntry
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.UnderlyingHolding
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.PortfolioViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private val CmpBgLight = Color(0xFFF8FAFC)
private val CmpSurfaceWhite = Color(0xFFFFFFFF)
private val CmpPrimaryIndigo = Color(0xFF1E3A8A)
private val CmpAccentRoyal = Color(0xFF2563EB)
private val CmpEmerald = Color(0xFF059669)
private val CmpEmeraldSoft = Color(0xFFECFDF5)
private val CmpAmber = Color(0xFFD97706)
private val CmpAmberSoft = Color(0xFFFFFBEB)
private val CmpCrimson = Color(0xFFDC2626)
private val CmpCrimsonSoft = Color(0xFFFEF2F2)
private val CmpPortfolioPurple = Color(0xFF7C3AED)
private val CmpSlatePrimary = Color(0xFF0F172A)
private val CmpSlateSecondary = Color(0xFF475569)
private val CmpSlateBorder = Color(0xFFE2E8F0)

// 5 Distinct colors for up to 5 concurrent comparison funds
private val FundLineColors = listOf(
    CmpPrimaryIndigo,
    CmpEmerald,
    CmpAmber,
    CmpCrimson,
    CmpPortfolioPurple
)

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
 * 3. Base-100 normalized multi-line performance chart across 7D, 1M, 1Y, 3Y, 5Y, 10Y, and Custom.
 * 4. Comprehensive side-by-side factor matrix for up to 5 funds with horizontal scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundComparisonTab(
    portfolioViewModel: PortfolioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by portfolioViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAmfiSearchDialog by rememberSaveable { mutableStateOf(false) }

    val calendarFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CmpBgLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header & Selection Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmpSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Multi-Fund Peer Comparison",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CmpSlatePrimary
                            )
                            Text(
                                text = "Base-100 rebased trajectory & factor overlap (Compare up to 5 schemes)",
                                fontSize = 11.sp,
                                color = CmpSlateSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showAmfiSearchDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CmpPrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add AMFI", fontSize = 11.sp, color = CmpPrimaryIndigo)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Fund Badges (Up to 5)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val funds = uiState.comparisonFunds
                        funds.forEachIndexed { index, fund ->
                            val color = FundLineColors.getOrElse(index) { CmpAccentRoyal }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = color.copy(alpha = 0.09f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
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
                                        text = fund.schemeName.take(20) + "...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Fund",
                                        tint = color,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { portfolioViewModel.removeComparisonScheme(fund.schemeCode) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Portfolio Schemes Quick Add Chips
                    val allSchemes = uiState.portfolioData?.schemes.orEmpty()
                    if (allSchemes.isNotEmpty()) {
                        Text(
                            text = "Quick Select from My Portfolio (Select up to 5):",
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
                            allSchemes.forEach { scheme ->
                                val isSelected = uiState.comparisonSchemeCodes.contains(scheme.schemeCode)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { portfolioViewModel.toggleComparisonScheme(scheme.schemeCode) },
                                    label = {
                                        Text(scheme.schemeName.take(18) + "...", fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CmpPrimaryIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CmpSlateBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // "Overlay My Portfolio Average" Toggle Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F3FF), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CmpPortfolioPurple)
                            )
                            Text(
                                text = "Overlay My Portfolio Average",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmpPortfolioPurple
                            )
                        }

                        Switch(
                            checked = uiState.isPortfolioAvgOverlayEnabled,
                            onCheckedChange = { portfolioViewModel.togglePortfolioAvgOverlay() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = CmpPortfolioPurple
                            ),
                            modifier = Modifier.size(width = 44.dp, height = 24.dp)
                        )
                    }
                }
            }
        }

        // Timeframe Selector & Normalized Chart Section
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
                            text = "Normalized Growth (Rebased: 100)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CmpSlatePrimary
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ComparisonTimeframes.forEach { tf ->
                                val isSelected = uiState.comparisonTimeframe == tf
                                Surface(
                                    color = if (isSelected) CmpPrimaryIndigo else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { portfolioViewModel.updateComparisonTimeframe(tf) }
                                ) {
                                    Text(
                                        text = tf.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else CmpSlateSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Calendar Date-to-Date Selector Row
                    AnimatedVisibility(visible = uiState.comparisonTimeframe == ChartTimeframe.CUSTOM) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            val startCal = Calendar.getInstance()
                            if (uiState.customComparisonStartEpochMs != null) {
                                startCal.timeInMillis = uiState.customComparisonStartEpochMs!!
                            } else {
                                startCal.add(Calendar.MONTH, -6)
                            }

                            val endCal = Calendar.getInstance()
                            if (uiState.customComparisonEndEpochMs != null) {
                                endCal.timeInMillis = uiState.customComparisonEndEpochMs!!
                            }

                            val startDateStr = calendarFormat.format(startCal.time)
                            val endDateStr = calendarFormat.format(endCal.time)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Date Picker Trigger
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val picked = Calendar.getInstance().apply {
                                                        set(year, month, dayOfMonth, 0, 0, 0)
                                                    }
                                                    val endMs = uiState.customComparisonEndEpochMs ?: endCal.timeInMillis
                                                    portfolioViewModel.setCustomComparisonDateRange(picked.timeInMillis, endMs)
                                                },
                                                startCal.get(Calendar.YEAR),
                                                startCal.get(Calendar.MONTH),
                                                startCal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = CmpPrimaryIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text("From", fontSize = 9.sp, color = CmpSlateSecondary)
                                        Text(startDateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmpPrimaryIndigo)
                                    }
                                }

                                Text("➔", fontSize = 12.sp, color = CmpAccentRoyal)

                                // End Date Picker Trigger
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val picked = Calendar.getInstance().apply {
                                                        set(year, month, dayOfMonth, 23, 59, 59)
                                                    }
                                                    val startMs = uiState.customComparisonStartEpochMs ?: startCal.timeInMillis
                                                    portfolioViewModel.setCustomComparisonDateRange(startMs, picked.timeInMillis)
                                                },
                                                endCal.get(Calendar.YEAR),
                                                endCal.get(Calendar.MONTH),
                                                endCal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = CmpPrimaryIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text("To", fontSize = 9.sp, color = CmpSlateSecondary)
                                        Text(endDateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmpPrimaryIndigo)
                                    }
                                }
                            }
                        }
                    }

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
                            NormalizedMultiFundChart(
                                funds = uiState.comparisonFunds,
                                timeframe = uiState.comparisonTimeframe,
                                customStartEpochMs = uiState.customComparisonStartEpochMs,
                                customEndEpochMs = uiState.customComparisonEndEpochMs,
                                isPortfolioOverlay = uiState.isPortfolioAvgOverlayEnabled,
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
 * Normalized multi-line performance chart rebased to 100 at the start of the timeframe.
 */
@Composable
private fun NormalizedMultiFundChart(
    funds: List<FundComparisonItem>,
    timeframe: ChartTimeframe,
    customStartEpochMs: Long?,
    customEndEpochMs: Long?,
    isPortfolioOverlay: Boolean,
    portfolioSchemes: List<MutualFundScheme>
) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }

    // Sliced and normalized point series per fund
    val normalizedSeriesList = remember(funds, timeframe, customStartEpochMs, customEndEpochMs) {
        funds.map { fund ->
            val chronological = fund.navHistory.mapNotNull { entry ->
                val nav = entry.nav.trim().toDoubleOrNull() ?: return@mapNotNull null
                val epoch = runCatching { dateFormat.parse(entry.date)?.time }.getOrNull() ?: 0L
                Pair(epoch, nav)
            }.reversed()

            val sliced = if (chronological.isNotEmpty()) {
                if (timeframe == ChartTimeframe.CUSTOM) {
                    val start = customStartEpochMs ?: (chronological.last().first - 180L * 86_400_000L)
                    val end = customEndEpochMs ?: chronological.last().first
                    val filtered = chronological.filter { it.first in start..end }
                    if (filtered.size >= 2) filtered else chronological.takeLast(30)
                } else {
                    val latestEpoch = chronological.last().first
                    val cutoff = latestEpoch - (timeframe.calendarDays.toLong() * 86_400_000L)
                    val filtered = chronological.filter { it.first >= cutoff }
                    if (filtered.size >= 2) filtered else chronological.takeLast(timeframe.calendarDays.coerceAtLeast(2))
                }
            } else emptyList()

            if (sliced.isNotEmpty()) {
                val baseNav = sliced.first().second
                sliced.map { Pair(it.first, if (baseNav > 0) (it.second / baseNav) * 100.0 else 100.0) }
            } else emptyList()
        }
    }

    if (normalizedSeriesList.all { it.size < 2 }) {
        Text("Insufficient historical overlapping data for chosen timeframe.", fontSize = 12.sp, color = CmpSlateSecondary)
        return
    }

    // Min and Max across all normalized series
    val allNormalizedValues = normalizedSeriesList.flatten().map { it.second }
    val minVal = (allNormalizedValues.minOrNull() ?: 90.0).coerceAtMost(98.0)
    val maxVal = (allNormalizedValues.maxOrNull() ?: 110.0).coerceAtLeast(102.0)
    val valRange = (maxVal - minVal).coerceAtLeast(0.01)

    var scrubFraction by remember { mutableFloatStateOf(1f) }

    Column {
        // Legend with Current Return %
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            funds.forEachIndexed { index, fund ->
                val series = normalizedSeriesList.getOrNull(index).orEmpty()
                val currentNorm = if (series.isNotEmpty()) {
                    val idx = (scrubFraction * series.lastIndex).roundToInt().coerceIn(0, series.lastIndex)
                    series[idx].second
                } else 100.0
                val returnPct = currentNorm - 100.0
                val sign = if (returnPct >= 0) "+" else ""
                val rounded = (returnPct * 100.0).roundToInt() / 100.0
                val color = FundLineColors.getOrElse(index) { CmpAccentRoyal }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = fund.schemeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CmpSlatePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "$sign$rounded%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (returnPct >= 0) CmpEmerald else CmpCrimson
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-line Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val vPadding = 12.dp.toPx()
            val usableH = height - (vPadding * 2f)

            // Baseline at 100.0
            val norm100Y = height - vPadding - (((100.0 - minVal) / valRange).toFloat() * usableH)
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, norm100Y),
                end = Offset(width, norm100Y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            // Draw each fund's line
            normalizedSeriesList.forEachIndexed { fundIdx, series ->
                if (series.size >= 2) {
                    val lineColor = FundLineColors.getOrElse(fundIdx) { CmpAccentRoyal }
                    val path = Path()

                    series.forEachIndexed { i, pt ->
                        val x = (i.toFloat() / series.lastIndex.toFloat()) * width
                        val normY = ((pt.second - minVal) / valRange).toFloat()
                        val y = height - vPadding - (normY * usableH)

                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw scrub dot
                    val sIdx = (scrubFraction * series.lastIndex).roundToInt().coerceIn(0, series.lastIndex)
                    val sVal = series[sIdx].second
                    val sX = (sIdx.toFloat() / series.lastIndex.toFloat()) * width
                    val sNormY = ((sVal - minVal) / valRange).toFloat()
                    val sY = height - vPadding - (sNormY * usableH)

                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(sX, sY))
                    drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(sX, sY))
                }
            }

            // Vertical scrub ruler
            val scrubX = scrubFraction * width
            drawLine(
                color = CmpSlateSecondary.copy(alpha = 0.35f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, height),
                strokeWidth = 1.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Rebased Base: 100.0", fontSize = 10.sp, color = CmpSlateSecondary)
            Text(text = "Timeframe: ${timeframe.label}", fontSize = 10.sp, color = CmpSlateSecondary)
        }
    }
}

/**
 * Side-by-side factor matrix table comparing key metrics across up to 5 funds with horizontal scrolling.
 */
@Composable
private fun ComparisonMatrixTable(funds: List<FundComparisonItem>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.width((120 + funds.size * 120).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metric",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CmpSlateSecondary,
                modifier = Modifier.width(120.dp)
            )
            funds.forEachIndexed { i, fund ->
                val color = FundLineColors.getOrElse(i) { CmpAccentRoyal }
                Text(
                    text = fund.schemeName.take(15),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        HorizontalDivider(color = CmpSlateBorder)

        MatrixRow(
            label = "Latest NAV",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { "₹" + String.format(Locale.US, "%.2f", it.latestNav) }
        )

        MatrixRow(
            label = "1-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr1Y) }
        )

        MatrixRow(
            label = "3-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr3Y) }
        )

        MatrixRow(
            label = "5-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr5Y) }
        )

        MatrixRow(
            label = "Expense Ratio",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { "${it.expenseRatio}%" }
        )

        MatrixRow(
            label = "Risk Level",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { it.riskLevel }
        )

        MatrixRow(
            label = "Category",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { it.category }
        )

        // Holdings overlap compared to Fund #1
        val overlapValues = remember(funds) {
            if (funds.size >= 2) {
                val fund0Holdings = funds[0].topHoldings
                funds.mapIndexed { idx, targetFund ->
                    if (idx == 0) {
                        "100%"
                    } else {
                        val overlapPct = computeHoldingsOverlap(fund0Holdings, targetFund.topHoldings)
                        val rounded = (overlapPct * 10.0).roundToInt() / 10.0
                        "$rounded%"
                    }
                }
            } else funds.map { "N/A" }
        }

        MatrixRow(
            label = "Overlap vs Fund #1",
            totalWidthDp = 120 + funds.size * 120,
            values = overlapValues
        )
    }
}

@Composable
private fun MatrixRow(label: String, totalWidthDp: Int, values: List<String>) {
    Row(
        modifier = Modifier
            .width(totalWidthDp.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CmpSlateSecondary,
            modifier = Modifier.width(120.dp)
        )
        values.forEach { v ->
            Text(
                text = v,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CmpSlatePrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

private fun formatCagr(cagr: Double?): String {
    if (cagr == null) return "N/A"
    val sign = if (cagr >= 0) "+" else ""
    val rounded = (cagr * 100.0).roundToInt() / 100.0
    return "$sign$rounded%"
}

private fun computeHoldingsOverlap(
    listA: List<UnderlyingHolding>,
    listB: List<UnderlyingHolding>
): Double {
    if (listA.isEmpty() || listB.isEmpty()) return 0.0
    var commonWeightSum = 0.0
    val mapB = listB.associateBy { it.ticker.trim().uppercase() }
    for (holdingA in listA) {
        val tickerA = holdingA.ticker.trim().uppercase()
        val matchingB = mapB[tickerA]
        if (matchingB != null) {
            commonWeightSum += min(holdingA.weightPercent, matchingB.weightPercent)
        }
    }
    return commonWeightSum
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
    val uiState by portfolioViewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Universal AMFI Scheme Search",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CmpSlatePrimary
            )
        },
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
                    label = { Text("Search Scheme Name / Code...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CmpPrimaryIndigo)
                    },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                portfolioViewModel.clearAmfiSearch()
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
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
                                selectedContainerColor = CmpPrimaryIndigo,
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CmpAccentRoyal)
                        }
                    }
                    filteredResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredResults) { result ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CmpSlateBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectScheme(result.schemeCode) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = result.schemeName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CmpSlatePrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "AMFI Code: #${result.schemeCode}",
                                            fontSize = 11.sp,
                                            color = CmpPrimaryIndigo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    uiState.amfiSearchError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.amfiSearchError ?: "",
                                fontSize = 12.sp,
                                color = CmpCrimson,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type at least 2 characters (e.g., 'Parag', 'Nifty', 'HDFC') to search live AMFI database.",
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
