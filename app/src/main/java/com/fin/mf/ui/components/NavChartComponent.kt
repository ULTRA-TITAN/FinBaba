package com.fin.mf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.MfApiNavEntry
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

// Top-level cached colors and formatters for 60/120 FPS performance
private val ChartPrimaryIndigo = Color(0xFF1E3A8A)
private val ChartAccentRoyal = Color(0xFF2563EB)
private val ChartEmeraldPositive = Color(0xFF059669)
private val ChartEmeraldSoft = Color(0xFFECFDF5)
private val ChartCrimsonNegative = Color(0xFFDC2626)
private val ChartCrimsonSoft = Color(0xFFFEF2F2)
private val ChartAmberAccent = Color(0xFFD97706)
private val ChartSlateTextPrimary = Color(0xFF0F172A)
private val ChartSlateTextSecondary = Color(0xFF475569)
private val ChartSlateBorder = Color(0xFFE2E8F0)
private val ChartSurfaceWhite = Color(0xFFFFFFFF)
private val ChartBgLight = Color(0xFFF8FAFC)

private val TopLevelCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}

data class NavChartPoint(
    val dateString: String,
    val epochMs: Long,
    val nav: Double
)

/**
 * Institutional Historical NAV Chart with:
 * 1. Timeframe selector (7D, 15D, 1M, 3M, YTD, 1Y, 3Y, 5Y, 7Y, 10Y, Custom).
 * 2. Custom date range slider to slice any arbitrary interval.
 * 3. Two-Point Graph Delta Analyzer with dual draggable markers and annualized CAGR math.
 * 4. Full Screen modal dialog mode with expanded height and detailed axis markings.
 * 5. Dynamic Zoomable Charts: Pinch-to-zoom (1.0x–5.0x), pan gestures, and one-tap Reset Zoom pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavChartComponent(
    schemeName: String,
    schemeCode: Int,
    fullHistory: List<MfApiNavEntry>,
    selectedTimeframe: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFullScreenOpen by rememberSaveable { mutableStateOf(false) }
    var isTwoPointAnalyzerEnabled by rememberSaveable { mutableStateOf(false) }
    var customSliceRange by remember { mutableStateOf(0f..1f) }

    // Parse raw entries into chronologically sorted data points (oldest to newest)
    val chronologicalPoints = remember(fullHistory) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        fullHistory.mapNotNull { entry ->
            val navVal = entry.nav.trim().toDoubleOrNull() ?: return@mapNotNull null
            val parsedDate = runCatching { dateFormat.parse(entry.date) }.getOrNull()
            val epoch = parsedDate?.time ?: 0L
            NavChartPoint(dateString = entry.date, epochMs = epoch, nav = navVal)
        }.reversed()
    }

    if (chronologicalPoints.size < 2) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChartSurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChartSlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Insufficient historical NAV data points for charting.",
                    fontSize = 13.sp,
                    color = ChartSlateTextSecondary
                )
            }
        }
        return
    }

    // Filter points based on selected timeframe
    val visiblePoints = remember(chronologicalPoints, selectedTimeframe, customSliceRange) {
        filterPointsByTimeframe(chronologicalPoints, selectedTimeframe, customSliceRange)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChartSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChartSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar: Title, Subtitle, Fullscreen Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historical NAV Trajectory",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChartSlateTextPrimary
                    )
                    Text(
                        text = "Live series • AMFI #$schemeCode",
                        fontSize = 11.sp,
                        color = ChartSlateTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isFullScreenOpen = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand Full Screen Chart",
                            tint = ChartPrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timeframe Selector Chips
            TimeframeChipsRow(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected
            )

            // Custom Range Slider if CUSTOM timeframe selected
            AnimatedVisibility(visible = selectedTimeframe == ChartTimeframe.CUSTOM) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom Slice Interval",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChartPrimaryIndigo
                        )
                        Text(
                            text = "${visiblePoints.firstOrNull()?.dateString ?: ""} → ${visiblePoints.lastOrNull()?.dateString ?: ""}",
                            fontSize = 11.sp,
                            color = ChartSlateTextSecondary
                        )
                    }
                    RangeSlider(
                        value = customSliceRange,
                        onValueChange = { customSliceRange = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = ChartPrimaryIndigo,
                            activeTrackColor = ChartAccentRoyal,
                            inactiveTrackColor = ChartSlateBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Point Analyzer Toggle Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChartBgLight, RoundedCornerShape(10.dp))
                    .border(1.dp, ChartSlateBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = if (isTwoPointAnalyzerEnabled) ChartAccentRoyal else ChartSlateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "2-Point Analyzer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTwoPointAnalyzerEnabled) ChartPrimaryIndigo else ChartSlateTextSecondary
                    )
                }

                Switch(
                    checked = isTwoPointAnalyzerEnabled,
                    onCheckedChange = { isTwoPointAnalyzerEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ChartPrimaryIndigo,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ChartSlateBorder
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Chart Canvas with Pinch-to-Zoom & Pan
            ChartCanvasWithInteractivity(
                visiblePoints = visiblePoints,
                timeframe = selectedTimeframe,
                isTwoPointAnalyzer = isTwoPointAnalyzerEnabled,
                chartHeightDp = 210,
                isFullScreen = false
            )
        }
    }

    // Full Screen Chart Modal Dialog
    if (isFullScreenOpen) {
        Dialog(
            onDismissRequest = { isFullScreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ChartBgLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ChartSurfaceWhite,
                            titleContentColor = ChartSlateTextPrimary
                        ),
                        title = {
                            Column {
                                Text(
                                    text = schemeName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Full Screen Chart Studio • AMFI #$schemeCode",
                                    fontSize = 11.sp,
                                    color = ChartSlateTextSecondary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { isFullScreenOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Full Screen",
                                    tint = ChartPrimaryIndigo
                                )
                            }
                        }
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        TimeframeChipsRow(
                            selectedTimeframe = selectedTimeframe,
                            onTimeframeSelected = onTimeframeSelected
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Range Slider in Full Screen
                        AnimatedVisibility(visible = selectedTimeframe == ChartTimeframe.CUSTOM) {
                            Column {
                                RangeSlider(
                                    value = customSliceRange,
                                    onValueChange = { customSliceRange = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ChartPrimaryIndigo,
                                        activeTrackColor = ChartAccentRoyal
                                    )
                                )
                            }
                        }

                        // 2-Point Analyzer Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ChartSurfaceWhite, RoundedCornerShape(10.dp))
                                .border(1.dp, ChartSlateBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Two-Point Return & Annualized CAGR Analyzer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChartPrimaryIndigo
                            )
                            Switch(
                                checked = isTwoPointAnalyzerEnabled,
                                onCheckedChange = { isTwoPointAnalyzerEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = ChartPrimaryIndigo
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ChartSurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChartSlateBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ChartCanvasWithInteractivity(
                                    visiblePoints = visiblePoints,
                                    timeframe = selectedTimeframe,
                                    isTwoPointAnalyzer = isTwoPointAnalyzerEnabled,
                                    chartHeightDp = 380,
                                    isFullScreen = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeframeChipsRow(
    selectedTimeframe: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChartTimeframe.entries.forEach { timeframe ->
            val isSelected = selectedTimeframe == timeframe
            Surface(
                color = if (isSelected) ChartPrimaryIndigo else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTimeframeSelected(timeframe) }
            ) {
                Text(
                    text = timeframe.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else ChartSlateTextSecondary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartCanvasWithInteractivity(
    visiblePoints: List<NavChartPoint>,
    timeframe: ChartTimeframe,
    isTwoPointAnalyzer: Boolean,
    chartHeightDp: Int,
    isFullScreen: Boolean
) {
    if (visiblePoints.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Insufficient data points for this timeframe.", fontSize = 12.sp, color = ChartSlateTextSecondary)
        }
        return
    }

    // State for pinch-to-zoom (1.0x to 5.0x) and horizontal pan offset
    var zoomScale by remember(visiblePoints, timeframe) { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember(visiblePoints, timeframe) { mutableFloatStateOf(0f) }

    // State for standard single-scrubber
    var singleScrubIndex by remember(visiblePoints) {
        mutableIntStateOf(visiblePoints.lastIndex)
    }

    // State for Two-Point Analyzer (Point A and Point B)
    var markerIndexA by remember(visiblePoints) {
        mutableIntStateOf(0)
    }
    var markerIndexB by remember(visiblePoints) {
        mutableIntStateOf(visiblePoints.lastIndex)
    }

    // Boundary clamping
    val safeSingleIndex = singleScrubIndex.coerceIn(0, visiblePoints.lastIndex)
    val safeA = markerIndexA.coerceIn(0, visiblePoints.lastIndex)
    val safeB = markerIndexB.coerceIn(safeA + 1, visiblePoints.lastIndex).coerceAtMost(visiblePoints.lastIndex)

    val minNav = remember(visiblePoints) { visiblePoints.minOf { it.nav } }
    val maxNav = remember(visiblePoints) { visiblePoints.maxOf { it.nav } }
    val navRange = (maxNav - minNav).coerceAtLeast(0.0001)

    val firstPoint = visiblePoints.first()
    val lastPoint = visiblePoints.last()

    val totalPeriodReturnPct = if (firstPoint.nav > 0) {
        ((lastPoint.nav - firstPoint.nav) / firstPoint.nav) * 100.0
    } else 0.0

    val primaryLineColor = if (totalPeriodReturnPct >= 0) ChartEmeraldPositive else ChartCrimsonNegative

    Column {
        if (!isTwoPointAnalyzer) {
            // Standard Single-Point Scrubber readout
            val currentPoint = visiblePoints[safeSingleIndex]
            val scrubReturnPct = if (firstPoint.nav > 0) {
                ((currentPoint.nav - firstPoint.nav) / firstPoint.nav) * 100.0
            } else 0.0
            val isScrubPositive = scrubReturnPct >= 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NAV on ${currentPoint.dateString}: ₹${String.format(Locale.US, "%.4f", currentPoint.nav)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChartSlateTextPrimary
                    )
                    Text(
                        text = if (zoomScale > 1.05f) "Pinch to zoom • Drag to pan • Tap to scrub" else "Drag to scrub • Pinch to zoom",
                        fontSize = 11.sp,
                        color = ChartSlateTextSecondary
                    )
                }

                Surface(
                    color = if (isScrubPositive) ChartEmeraldSoft else ChartCrimsonSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val sign = if (scrubReturnPct >= 0) "+" else ""
                    val rounded = (scrubReturnPct * 100.0).roundToInt() / 100.0
                    Text(
                        text = "${timeframe.label}: $sign$rounded%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isScrubPositive) ChartEmeraldPositive else ChartCrimsonNegative,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // Two-Point Graph Delta Analyzer Floating Pill
            val pointA = visiblePoints[safeA]
            val pointB = visiblePoints[safeB]

            val deltaReturnPct = if (pointA.nav > 0) {
                ((pointB.nav - pointA.nav) / pointA.nav) * 100.0
            } else 0.0

            val daysSpan = ((pointB.epochMs - pointA.epochMs) / 86_400_000L).coerceAtLeast(1L)
            val cagrPct: Double? = if (daysSpan >= 365L && pointA.nav > 0 && pointB.nav > 0) {
                val ratio = pointB.nav / pointA.nav
                if (ratio > 0.0) {
                    ((ratio.pow(365.0 / daysSpan.toDouble())) - 1.0) * 100.0
                } else null
            } else null

            val isDeltaPositive = deltaReturnPct >= 0
            val deltaColor = if (isDeltaPositive) ChartEmeraldPositive else ChartCrimsonNegative

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Point A (${pointA.dateString}): ₹${String.format(Locale.US, "%.2f", pointA.nav)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChartPrimaryIndigo
                        )
                        Text(
                            text = "➔",
                            fontSize = 12.sp,
                            color = ChartAccentRoyal
                        )
                        Text(
                            text = "Point B (${pointB.dateString}): ₹${String.format(Locale.US, "%.2f", pointB.nav)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChartPrimaryIndigo
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val returnSign = if (deltaReturnPct >= 0) "+" else ""
                        val roundedReturn = (deltaReturnPct * 100.0).roundToInt() / 100.0
                        Text(
                            text = "Delta: $returnSign$roundedReturn% ($daysSpan days)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = deltaColor
                        )

                        if (cagrPct != null) {
                            val cagrSign = if (cagrPct >= 0) "+" else ""
                            val roundedCagr = (cagrPct * 100.0).roundToInt() / 100.0
                            val yearsSpan = (daysSpan / 365.0 * 10.0).roundToInt() / 10.0
                            Surface(
                                color = if (cagrPct >= 0) ChartEmeraldSoft else ChartCrimsonSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "CAGR: $cagrSign$roundedCagr% ($yearsSpan yrs)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cagrPct >= 0) ChartEmeraldPositive else ChartCrimsonNegative,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dual Slider for fine-grained marker scrubbing on mobile
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ChartPrimaryIndigo,
                    modifier = Modifier.size(16.dp)
                )
                RangeSlider(
                    value = safeA.toFloat()..safeB.toFloat(),
                    onValueChange = { range ->
                        markerIndexA = range.start.roundToInt().coerceIn(0, visiblePoints.lastIndex - 1)
                        markerIndexB = range.endInclusive.roundToInt().coerceIn(markerIndexA + 1, visiblePoints.lastIndex)
                    },
                    valueRange = 0f..visiblePoints.lastIndex.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ChartPrimaryIndigo,
                        activeTrackColor = ChartAccentRoyal,
                        inactiveTrackColor = ChartSlateBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Floating Zoom Controls & Reset Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val newZoom = (zoomScale - 0.5f).coerceAtLeast(1.0f)
                            zoomScale = newZoom
                            if (newZoom == 1.0f) panOffsetX = 0f
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = ChartSlateTextSecondary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }

                Surface(
                    color = if (zoomScale > 1.01f) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", zoomScale)}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (zoomScale > 1.01f) ChartAccentRoyal else ChartSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val newZoom = (zoomScale + 0.5f).coerceAtMost(5.0f)
                            zoomScale = newZoom
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = ChartSlateTextSecondary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }

            if (zoomScale > 1.01f) {
                Surface(
                    color = ChartPrimaryIndigo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset Zoom",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Drawing Canvas with Zoom & Pan Transformations
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp.dp)
                .pointerInput(visiblePoints, zoomScale, panOffsetX) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(1.0f, 5.0f)
                        zoomScale = newScale
                        val maxPan = (newScale - 1f) * size.width
                        panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPan.coerceAtLeast(0f))
                    }
                }
                .pointerInput(visiblePoints, isTwoPointAnalyzer, zoomScale, panOffsetX) {
                    detectTapGestures { offset ->
                        val rawTappedX = (offset.x + panOffsetX) / zoomScale
                        val fraction = (rawTappedX / size.width.toFloat()).coerceIn(0f, 1f)
                        val tappedIdx = (fraction * visiblePoints.lastIndex).roundToInt()

                        if (!isTwoPointAnalyzer) {
                            singleScrubIndex = tappedIdx
                        } else {
                            val distA = abs(tappedIdx - safeA)
                            val distB = abs(tappedIdx - safeB)
                            if (distA <= distB) {
                                markerIndexA = tappedIdx.coerceIn(0, safeB - 1)
                            } else {
                                markerIndexB = tappedIdx.coerceIn(safeA + 1, visiblePoints.lastIndex)
                            }
                        }
                    }
                }
                .pointerInput(visiblePoints, isTwoPointAnalyzer, safeA, safeB, zoomScale, panOffsetX) {
                    detectDragGestures { change, _ ->
                        if (zoomScale <= 1.05f) {
                            val rawDragX = (change.position.x + panOffsetX) / zoomScale
                            val fraction = (rawDragX / size.width.toFloat()).coerceIn(0f, 1f)
                            val dragIdx = (fraction * visiblePoints.lastIndex).roundToInt()
                            if (!isTwoPointAnalyzer) {
                                singleScrubIndex = dragIdx
                            } else {
                                val distA = abs(dragIdx - safeA)
                                val distB = abs(dragIdx - safeB)
                                if (distA <= distB) {
                                    markerIndexA = dragIdx.coerceIn(0, safeB - 1)
                                } else {
                                    markerIndexB = dragIdx.coerceIn(safeA + 1, visiblePoints.lastIndex)
                                }
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val verticalPadding = 16.dp.toPx()
            val usableHeight = height - (verticalPadding * 2f)

            // Draw horizontal grid lines if in full screen
            if (isFullScreen) {
                val gridLevels = 4
                for (i in 0..gridLevels) {
                    val y = height - verticalPadding - (i.toFloat() / gridLevels.toFloat() * usableHeight)
                    drawLine(
                        color = ChartSlateBorder.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }

            clipRect(0f, 0f, width, height) {
                val linePath = Path()
                val fillPath = Path()

                visiblePoints.forEachIndexed { idx, pt ->
                    val rawX = (idx.toFloat() / visiblePoints.lastIndex.toFloat()) * width
                    val x = rawX * zoomScale - panOffsetX
                    val normalizedY = ((pt.nav - minNav) / navRange).toFloat()
                    val y = height - verticalPadding - (normalizedY * usableHeight)

                    if (idx == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                val lastRawX = width
                val lastX = lastRawX * zoomScale - panOffsetX
                fillPath.lineTo(lastX, height)
                fillPath.close()

                // Area Gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryLineColor.copy(alpha = 0.22f), Color.Transparent)
                    )
                )

                // Primary Stroke
                drawPath(
                    path = linePath,
                    color = primaryLineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                if (!isTwoPointAnalyzer) {
                    // Single pointer scrub indicator
                    val rawScrubX = (safeSingleIndex.toFloat() / visiblePoints.lastIndex.toFloat()) * width
                    val scrubX = rawScrubX * zoomScale - panOffsetX
                    val scrubNormY = ((visiblePoints[safeSingleIndex].nav - minNav) / navRange).toFloat()
                    val scrubY = height - verticalPadding - (scrubNormY * usableHeight)

                    if (scrubX in -10f..(width + 10f)) {
                        drawLine(
                            color = ChartPrimaryIndigo.copy(alpha = 0.45f),
                            start = Offset(scrubX, 0f),
                            end = Offset(scrubX, height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = Offset(scrubX, scrubY)
                        )
                        drawCircle(
                            color = ChartPrimaryIndigo,
                            radius = 5.dp.toPx(),
                            center = Offset(scrubX, scrubY)
                        )
                    }
                } else {
                    // Two-Point Analyzer Markers (Point A and Point B)
                    val rawXA = (safeA.toFloat() / visiblePoints.lastIndex.toFloat()) * width
                    val xA = rawXA * zoomScale - panOffsetX
                    val normYA = ((visiblePoints[safeA].nav - minNav) / navRange).toFloat()
                    val yA = height - verticalPadding - (normYA * usableHeight)

                    val rawXB = (safeB.toFloat() / visiblePoints.lastIndex.toFloat()) * width
                    val xB = rawXB * zoomScale - panOffsetX
                    val normYB = ((visiblePoints[safeB].nav - minNav) / navRange).toFloat()
                    val yB = height - verticalPadding - (normYB * usableHeight)

                    // Shaded delta zone between Marker A and Marker B
                    val rectLeft = xA.coerceAtLeast(0f)
                    val rectRight = xB.coerceAtMost(width)
                    if (rectRight > rectLeft) {
                        drawRect(
                            color = ChartAccentRoyal.copy(alpha = 0.08f),
                            topLeft = Offset(rectLeft, 0f),
                            size = Size(rectRight - rectLeft, height)
                        )
                    }

                    // Marker A Line & Circles
                    if (xA in -10f..(width + 10f)) {
                        drawLine(
                            color = ChartPrimaryIndigo,
                            start = Offset(xA, 0f),
                            end = Offset(xA, height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(xA, yA))
                        drawCircle(color = ChartPrimaryIndigo, radius = 5.5.dp.toPx(), center = Offset(xA, yA))
                    }

                    // Marker B Line & Circles
                    if (xB in -10f..(width + 10f)) {
                        drawLine(
                            color = ChartEmeraldPositive,
                            start = Offset(xB, 0f),
                            end = Offset(xB, height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(xB, yB))
                        drawCircle(color = ChartEmeraldPositive, radius = 5.5.dp.toPx(), center = Offset(xB, yB))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Axis Bounds & Date Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ₹${String.format(Locale.US, "%.2f", minNav)} (${firstPoint.dateString})",
                fontSize = 11.sp,
                color = ChartSlateTextSecondary
            )
            Text(
                text = "Max: ₹${String.format(Locale.US, "%.2f", maxNav)} (${lastPoint.dateString})",
                fontSize = 11.sp,
                color = ChartSlateTextSecondary
            )
        }
    }
}

private fun filterPointsByTimeframe(
    allPoints: List<NavChartPoint>,
    timeframe: ChartTimeframe,
    customRange: ClosedFloatingPointRange<Float>
): List<NavChartPoint> {
    if (allPoints.size < 2) return allPoints

    return when (timeframe) {
        ChartTimeframe.CUSTOM -> {
            val startIdx = (customRange.start * allPoints.lastIndex).roundToInt().coerceIn(0, allPoints.lastIndex)
            val endIdx = (customRange.endInclusive * allPoints.lastIndex).roundToInt().coerceIn(startIdx + 1, allPoints.lastIndex)
            allPoints.subList(startIdx, (endIdx + 1).coerceAtMost(allPoints.size))
        }
        ChartTimeframe.YTD -> {
            val calendar = Calendar.getInstance()
            val latestEpoch = allPoints.last().epochMs
            calendar.timeInMillis = latestEpoch
            val currentYear = calendar.get(Calendar.YEAR)

            val ytdPoints = allPoints.filter { pt ->
                val c = Calendar.getInstance()
                c.timeInMillis = pt.epochMs
                c.get(Calendar.YEAR) == currentYear
            }
            if (ytdPoints.size >= 2) ytdPoints else allPoints.takeLast(30)
        }
        else -> {
            val days = timeframe.calendarDays
            val msThreshold = days.toLong() * 86_400_000L
            val latestEpoch = allPoints.last().epochMs
            val cutoff = latestEpoch - msThreshold
            val filtered = allPoints.filter { it.epochMs >= cutoff }
            if (filtered.size >= 2) {
                filtered
            } else {
                allPoints.takeLast(days.coerceAtLeast(2))
            }
        }
    }
}
