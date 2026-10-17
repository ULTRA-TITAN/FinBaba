package com.fin.mf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay
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

    val totalPoints = visiblePoints.size

    // State 1: Zoom scale clamped between 1.0f (Full view) and 10.0f (Deep zoom)
    var zoomScale by remember(visiblePoints, timeframe) { mutableFloatStateOf(1.0f) }

    // State 2: Scroll offset fraction between 0.0f and (1.0f - 1f / zoomScale)
    var scrollOffsetFraction by remember(visiblePoints, timeframe) { mutableFloatStateOf(0f) }

    // State 3: Crosshair scrub index (null when not scrubbing)
    var scrubbedIndex by remember(visiblePoints, timeframe) { mutableStateOf<Int?>(null) }
    var isScrubbing by remember(visiblePoints, timeframe) { mutableStateOf(false) }

    // Double-tap tracking
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    // State for Two-Point Analyzer (Point A and Point B)
    var markerIndexA by remember(visiblePoints) { mutableIntStateOf(0) }
    var markerIndexB by remember(visiblePoints) { mutableIntStateOf(visiblePoints.lastIndex) }

    // Boundary clamping for markers A & B
    val safeA by remember(visiblePoints, markerIndexA) {
        derivedStateOf { markerIndexA.coerceIn(0, visiblePoints.lastIndex) }
    }
    val safeB by remember(visiblePoints, markerIndexB, safeA) {
        derivedStateOf { markerIndexB.coerceIn((safeA + 1).coerceAtMost(visiblePoints.lastIndex), visiblePoints.lastIndex) }
    }

    // Viewport slicing using derivedStateOf
    val visibleLenFraction by remember(zoomScale) {
        derivedStateOf { 1.0f / zoomScale }
    }
    val visibleStartIndex by remember(scrollOffsetFraction, totalPoints) {
        derivedStateOf {
            ((scrollOffsetFraction * totalPoints).toInt()).coerceIn(0, (totalPoints - 2).coerceAtLeast(0))
        }
    }
    val visibleWindowPoints by remember(totalPoints, visibleLenFraction) {
        derivedStateOf {
            ((totalPoints * visibleLenFraction).roundToInt()).coerceAtLeast(2)
        }
    }
    val visibleEndIndex by remember(visibleStartIndex, visibleWindowPoints, totalPoints) {
        derivedStateOf {
            (visibleStartIndex + visibleWindowPoints).coerceAtMost(totalPoints - 1)
        }
    }
    val slicedPoints by remember(visibleStartIndex, visibleEndIndex, visiblePoints) {
        derivedStateOf {
            visiblePoints.subList(visibleStartIndex, visibleEndIndex + 1)
        }
    }

    // Auto-dismiss crosshair timeout (2 seconds after finger release)
    LaunchedEffect(isScrubbing) {
        if (!isScrubbing && scrubbedIndex != null) {
            delay(2000L)
            scrubbedIndex = null
        }
    }

    // Min and Max NAV dynamically computed for sliced visible window
    val sliceMinNav = remember(slicedPoints) { slicedPoints.minOf { it.nav } }
    val sliceMaxNav = remember(slicedPoints) { slicedPoints.maxOf { it.nav } }
    val sliceNavRange = (sliceMaxNav - sliceMinNav).coerceAtLeast(0.0001)

    val firstPoint = visiblePoints.first()
    val lastPoint = visiblePoints.last()

    val totalPeriodReturnPct = if (firstPoint.nav > 0) {
        ((lastPoint.nav - firstPoint.nav) / firstPoint.nav) * 100.0
    } else 0.0

    val primaryLineColor = if (totalPeriodReturnPct >= 0) ChartEmeraldPositive else ChartCrimsonNegative

    Column {
        if (!isTwoPointAnalyzer) {
            // Standard Single-Point Scrubber readout
            val activeIndex = (scrubbedIndex ?: visibleEndIndex).coerceIn(0, totalPoints - 1)
            val currentPoint = visiblePoints[activeIndex]
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NAV on ${currentPoint.dateString}: ₹${String.format(Locale.US, "%.4f", currentPoint.nav)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChartSlateTextPrimary
                        )
                        if (isScrubbing || scrubbedIndex != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = ChartAccentRoyal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isScrubbing) "SCRUB" else "LOCK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChartAccentRoyal,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (zoomScale > 1.05f) {
                            "${String.format(Locale.US, "%.1f", zoomScale)}x • Drag to pan • Hold to scrub • Double-tap reset"
                        } else {
                            "Drag/Hold to scrub • Pinch to zoom (1x-10x)"
                        },
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

        // Floating Zoom Controls & Reset Bar (1.0x to 10.0x)
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
                            val newZoom = (zoomScale - 1.0f).coerceAtLeast(1.0f)
                            zoomScale = newZoom
                            if (newZoom == 1.0f) {
                                scrollOffsetFraction = 0f
                            } else {
                                val maxOff = (1f - 1f / newZoom).coerceAtLeast(0f)
                                scrollOffsetFraction = scrollOffsetFraction.coerceIn(0f, maxOff)
                            }
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
                            val newZoom = (zoomScale + 1.0f).coerceAtMost(10.0f)
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
                            scrollOffsetFraction = 0f
                            isScrubbing = false
                            scrubbedIndex = null
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

        // Pre-allocated paths & brush (Zero Canvas allocations in draw scope)
        val linePath = remember { Path() }
        val fillPath = remember { Path() }
        val areaGradientBrush = remember(primaryLineColor) {
            Brush.verticalGradient(
                colors = listOf(primaryLineColor.copy(alpha = 0.22f), Color.Transparent)
            )
        }

        // Unified multi-touch gesture Canvas:
        // - 2 fingers: pinch-to-zoom (1.0x-10.0x) centered at focal point + 2-finger pan
        // - 1 finger: horizontal pan when zoomed in (>1.05x)
        // - Touch & hold (>150ms): activate crosshair scrubber
        // - Double tap: reset zoom & pan
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp.dp)
                .pointerInput(totalPoints, isTwoPointAnalyzer) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position

                        // Double tap detection (< 300ms, distance < 35dp)
                        val elapsedSinceLastTap = downTime - lastTapTime
                        val distanceToLastTap = (downPos - lastTapPos).getDistance()
                        if (elapsedSinceLastTap < 300L && distanceToLastTap < 35.dp.toPx()) {
                            zoomScale = 1.0f
                            scrollOffsetFraction = 0f
                            isScrubbing = false
                            scrubbedIndex = null
                            lastTapTime = 0L
                            return@awaitEachGesture
                        }

                        var isMultiTouch = false
                        var hasMovedBeyondSlop = false
                        var holdTriggered = false
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.isEmpty()) {
                                // All touches released
                                if (!isMultiTouch && !hasMovedBeyondSlop && !holdTriggered) {
                                    // Quick tap: place crosshair or nearest marker
                                    val u = (downPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val relIdx = (u * (slicedPoints.size - 1).coerceAtLeast(1)).roundToInt()
                                    val tappedIdx = (visibleStartIndex + relIdx).coerceIn(0, totalPoints - 1)

                                    if (isTwoPointAnalyzer) {
                                        val distA = abs(tappedIdx - safeA)
                                        val distB = abs(tappedIdx - safeB)
                                        if (distA <= distB) {
                                            markerIndexA = tappedIdx.coerceIn(0, safeB - 1)
                                        } else {
                                            markerIndexB = tappedIdx.coerceIn(safeA + 1, totalPoints - 1)
                                        }
                                    } else {
                                        scrubbedIndex = tappedIdx
                                    }
                                    lastTapTime = downTime
                                    lastTapPos = downPos
                                }
                                isScrubbing = false
                                break
                            }

                            if (activePointers.size >= 2) {
                                // Multi-touch: simultaneous zoom & 2-finger pan
                                isMultiTouch = true
                                isScrubbing = false

                                val p1 = activePointers[0]
                                val p2 = activePointers[1]
                                val prevP1 = p1.previousPosition
                                val prevP2 = p2.previousPosition

                                val currentDist = (p1.position - p2.position).getDistance()
                                val prevDist = (prevP1 - prevP2).getDistance()

                                val currentCenter = (p1.position + p2.position) / 2f
                                val prevCenter = (prevP1 + prevP2) / 2f
                                val panDeltaX = currentCenter.x - prevCenter.x

                                val scaleDelta = if (prevDist > 10f) currentDist / prevDist else 1f
                                val oldScale = zoomScale
                                val newScale = (oldScale * scaleDelta).coerceIn(1.0f, 10.0f)

                                val oldLen = 1f / oldScale
                                val newLen = 1f / newScale
                                val focalFraction = (currentCenter.x / size.width.toFloat()).coerceIn(0f, 1f)

                                var newOffset = scrollOffsetFraction + focalFraction * (oldLen - newLen) - (panDeltaX / size.width.toFloat()) * newLen
                                val maxOffset = (1f - newLen).coerceAtLeast(0f)
                                newOffset = newOffset.coerceIn(0f, maxOffset)

                                zoomScale = newScale
                                scrollOffsetFraction = if (newScale <= 1.01f) 0f else newOffset

                                p1.consume()
                                p2.consume()
                            } else if (activePointers.size == 1 && !isMultiTouch) {
                                val pointer = activePointers[0]
                                val currentPos = pointer.position
                                val dragDelta = currentPos - downPos
                                val elapsed = System.currentTimeMillis() - downTime

                                if (!hasMovedBeyondSlop && dragDelta.getDistance() > touchSlop) {
                                    hasMovedBeyondSlop = true
                                }

                                // Touch & hold / long-press (> 150ms) to scrub
                                if (!holdTriggered && elapsed >= 150L && !hasMovedBeyondSlop) {
                                    holdTriggered = true
                                    if (!isTwoPointAnalyzer) {
                                        isScrubbing = true
                                        val u = (currentPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val relIdx = (u * (slicedPoints.size - 1).coerceAtLeast(1)).roundToInt()
                                        scrubbedIndex = (visibleStartIndex + relIdx).coerceIn(0, totalPoints - 1)
                                    }
                                }

                                if (isScrubbing) {
                                    val u = (currentPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val relIdx = (u * (slicedPoints.size - 1).coerceAtLeast(1)).roundToInt()
                                    scrubbedIndex = (visibleStartIndex + relIdx).coerceIn(0, totalPoints - 1)
                                    pointer.consume()
                                } else if (hasMovedBeyondSlop) {
                                    if (zoomScale > 1.05f) {
                                        // 1-finger horizontal pan when zoomed in
                                        val deltaX = pointer.position.x - pointer.previousPosition.x
                                        val visibleFraction = 1f / zoomScale
                                        val offsetDelta = -(deltaX / size.width.toFloat()) * visibleFraction
                                        val maxOffset = (1f - visibleFraction).coerceAtLeast(0f)
                                        scrollOffsetFraction = (scrollOffsetFraction + offsetDelta).coerceIn(0f, maxOffset)
                                        pointer.consume()
                                    } else if (!isTwoPointAnalyzer) {
                                        // Scrub when zoomScale <= 1.05f
                                        isScrubbing = true
                                        val u = (currentPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val relIdx = (u * (slicedPoints.size - 1).coerceAtLeast(1)).roundToInt()
                                        scrubbedIndex = (visibleStartIndex + relIdx).coerceIn(0, totalPoints - 1)
                                        pointer.consume()
                                    } else {
                                        // In 2-point mode: drag nearest marker
                                        val u = (currentPos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val relIdx = (u * (slicedPoints.size - 1).coerceAtLeast(1)).roundToInt()
                                        val dragIdx = (visibleStartIndex + relIdx).coerceIn(0, totalPoints - 1)
                                        val distA = abs(dragIdx - safeA)
                                        val distB = abs(dragIdx - safeB)
                                        if (distA <= distB) {
                                            markerIndexA = dragIdx.coerceIn(0, safeB - 1)
                                        } else {
                                            markerIndexB = dragIdx.coerceIn(safeA + 1, totalPoints - 1)
                                        }
                                        pointer.consume()
                                    }
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
                linePath.reset()
                fillPath.reset()

                val sliceLastIdx = (slicedPoints.size - 1).coerceAtLeast(1)

                slicedPoints.forEachIndexed { idx, pt ->
                    val x = (idx.toFloat() / sliceLastIdx.toFloat()) * width
                    val normalizedY = ((pt.nav - sliceMinNav) / sliceNavRange).toFloat()
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

                fillPath.lineTo(width, height)
                fillPath.close()

                // Area Gradient
                drawPath(
                    path = fillPath,
                    brush = areaGradientBrush
                )

                // Primary Stroke
                drawPath(
                    path = linePath,
                    color = primaryLineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                if (!isTwoPointAnalyzer) {
                    // Single pointer crosshair indicator
                    val activeDisplayIdx = (scrubbedIndex ?: if (!isScrubbing && zoomScale <= 1.01f) visibleEndIndex else null)
                    if (activeDisplayIdx != null && activeDisplayIdx in visibleStartIndex..visibleEndIndex) {
                        val relScrubIdx = activeDisplayIdx - visibleStartIndex
                        val scrubX = (relScrubIdx.toFloat() / sliceLastIdx.toFloat()) * width
                        val scrubNormY = ((visiblePoints[activeDisplayIdx].nav - sliceMinNav) / sliceNavRange).toFloat()
                        val scrubY = height - verticalPadding - (scrubNormY * usableHeight)

                        // Vertical Crosshair Line
                        drawLine(
                            color = ChartPrimaryIndigo.copy(alpha = 0.65f),
                            start = Offset(scrubX, 0f),
                            end = Offset(scrubX, height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        // Horizontal Reference Line
                        drawLine(
                            color = ChartPrimaryIndigo.copy(alpha = 0.25f),
                            start = Offset(0f, scrubY),
                            end = Offset(width, scrubY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        // Target rings
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
                    val rawLeftX = if (safeA < visibleStartIndex) -100f else if (safeA > visibleEndIndex) width + 100f
                        else ((safeA - visibleStartIndex).toFloat() / sliceLastIdx.toFloat()) * width
                    val rawRightX = if (safeB < visibleStartIndex) -100f else if (safeB > visibleEndIndex) width + 100f
                        else ((safeB - visibleStartIndex).toFloat() / sliceLastIdx.toFloat()) * width

                    // Shaded delta zone between Marker A and Marker B
                    val rectLeft = rawLeftX.coerceAtLeast(0f)
                    val rectRight = rawRightX.coerceAtMost(width)
                    if (rectRight > rectLeft) {
                        drawRect(
                            color = ChartAccentRoyal.copy(alpha = 0.08f),
                            topLeft = Offset(rectLeft, 0f),
                            size = Size(rectRight - rectLeft, height)
                        )
                    }

                    // Marker A Line & Circles
                    if (safeA in visibleStartIndex..visibleEndIndex) {
                        val relA = safeA - visibleStartIndex
                        val xA = (relA.toFloat() / sliceLastIdx.toFloat()) * width
                        val normYA = ((visiblePoints[safeA].nav - sliceMinNav) / sliceNavRange).toFloat()
                        val yA = height - verticalPadding - (normYA * usableHeight)

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
                    if (safeB in visibleStartIndex..visibleEndIndex) {
                        val relB = safeB - visibleStartIndex
                        val xB = (relB.toFloat() / sliceLastIdx.toFloat()) * width
                        val normYB = ((visiblePoints[safeB].nav - sliceMinNav) / sliceNavRange).toFloat()
                        val yB = height - verticalPadding - (normYB * usableHeight)

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

        // Axis Bounds & Date Labels (Shows visible window dates and min/max NAV)
        val firstSlicePoint = slicedPoints.first()
        val lastSlicePoint = slicedPoints.last()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${firstSlicePoint.dateString} • Min: ₹${String.format(Locale.US, "%.2f", sliceMinNav)}",
                fontSize = 11.sp,
                color = ChartSlateTextSecondary
            )
            Text(
                text = "Max: ₹${String.format(Locale.US, "%.2f", sliceMaxNav)} • ${lastSlicePoint.dateString}",
                fontSize = 11.sp,
                color = ChartSlateTextSecondary
            )
        }
    }
}
