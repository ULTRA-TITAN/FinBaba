package com.fin.mf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.data.ChartTimeframe
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
internal fun ChartCanvasWithInteractivity(
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
