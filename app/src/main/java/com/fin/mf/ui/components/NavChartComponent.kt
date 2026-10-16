package com.fin.mf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.MfApiNavEntry
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Institutional Historical NAV Chart with:
 * 1. Timeframe selector (7D, 15D, 1M, 3M, YTD, 1Y, 3Y, 5Y, 7Y, 10Y, Custom).
 * 2. Custom date range slider to slice any arbitrary interval.
 * 3. Two-Point Graph Delta Analyzer with dual draggable markers and annualized CAGR math.
 * 4. Full Screen modal dialog mode with expanded height and detailed axis markings.
 * 5. Dynamic Zoomable Charts: Pinch-to-zoom (1.0x–5.0x), pan gestures, and one-tap Reset Zoom pill.
 */
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
        FullscreenNavChartDialog(
            schemeName = schemeName,
            schemeCode = schemeCode,
            visiblePoints = visiblePoints,
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = onTimeframeSelected,
            customSliceRange = customSliceRange,
            onCustomSliceRangeChanged = { customSliceRange = it },
            isTwoPointAnalyzerEnabled = isTwoPointAnalyzerEnabled,
            onTwoPointAnalyzerToggled = { isTwoPointAnalyzerEnabled = it },
            onDismissRequest = { isFullScreenOpen = false }
        )
    }
}

@Composable
internal fun TimeframeChipsRow(
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
