package com.fin.mf.tools.terminal.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Institutional Interactive Chart Engine:
 * - Real-time smooth Bezier-like polyline with area gradient fill.
 * - Volume Histogram sub-chart (bottom 20% canvas height, green/red bars matching price ticks).
 * - Multi-touch pinch-to-zoom (up to 6x) & horizontal panning.
 * - Single-finger scrubbing crosshair ruler with two-way anchor linking to Y-axis price label pill & date pill.
 * - Top floating HUD tooltip displaying live price, date, and % return from start.
 */
@Composable
fun StandardInteractiveChart(
    points: List<TerminalChartPoint>,
    scrubbedPoint: TerminalChartPoint?,
    isPositive: Boolean,
    currency: String = "INR",
    onScrub: (TerminalChartPoint?) -> Unit
) {
    if (points.isEmpty()) return

    val density = LocalDensity.current
    val lineColor = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
    val gradientTop = if (isPositive) TerminalEmeraldPositive.copy(alpha = 0.28f) else TerminalCrimsonNegative.copy(alpha = 0.28f)
    val gradientBottom = Color.Transparent

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetRatio by remember { mutableFloatStateOf(0f) }
    var touchX by remember { mutableFloatStateOf(-1f) }

    // Compute visible slice based on zoom and pan
    val activePoints = remember(points, zoomScale, panOffsetRatio) {
        if (zoomScale <= 1.05f) {
            points
        } else {
            val visibleCount = max(8, (points.size / zoomScale).roundToInt())
            val maxStart = max(0, points.size - visibleCount)
            val startIndex = (panOffsetRatio * maxStart).roundToInt().coerceIn(0, maxStart)
            points.subList(startIndex, min(points.size, startIndex + visibleCount))
        }
    }

    val minPrice = remember(activePoints) { activePoints.minOfOrNull { it.price } ?: 0.0 }
    val maxPrice = remember(activePoints) { activePoints.maxOfOrNull { it.price } ?: 0.0 }
    val priceRange = max(maxPrice - minPrice, 0.01)
    val maxVolume = remember(activePoints) { activePoints.maxOfOrNull { it.volume }?.toDouble() ?: 0.0 }

    // Start price for relative delta HUD
    val initialPrice = activePoints.firstOrNull()?.price ?: minPrice

    Column(modifier = Modifier.fillMaxWidth()) {
        // Floating Scrubber HUD Tooltip
        val currentScrub = scrubbedPoint
        if (currentScrub != null) {
            val deltaPct = if (initialPrice > 0) ((currentScrub.price - initialPrice) / initialPrice) * 100.0 else 0.0
            val isDeltaPos = deltaPct >= 0.0

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TerminalSlatePrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentScrub.dateFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = formatAssetPrice(currentScrub.price, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Period Delta",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${if (isDeltaPos) "+" else ""}${roundTwo(deltaPct)}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeltaPos) SemanticFinanceColors.ProfitGreen else SemanticFinanceColors.LossRed
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .pointerInput(activePoints) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isMultiTouch = false
                        touchX = down.position.x
                        val fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                        val idx = (fraction * (activePoints.size - 1)).roundToInt().coerceIn(0, activePoints.size - 1)
                        onScrub(activePoints[idx])

                        do {
                            val event = awaitPointerEvent()
                            val downPointers = event.changes.filter { it.pressed }

                            if (downPointers.size >= 2) {
                                isMultiTouch = true
                                touchX = -1f
                                onScrub(null)
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newZoom = (zoomScale * zoom).coerceIn(1f, 6f)
                                zoomScale = newZoom
                                if (newZoom > 1f) {
                                    val deltaRatio = -pan.x / size.width
                                    panOffsetRatio = (panOffsetRatio + deltaRatio).coerceIn(0f, 1f)
                                } else {
                                    panOffsetRatio = 0f
                                }
                                event.changes.forEach { it.consume() }
                            } else if (downPointers.size == 1 && !isMultiTouch) {
                                val change = downPointers.first()
                                touchX = change.position.x
                                val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                val pIdx = (frac * (activePoints.size - 1)).roundToInt().coerceIn(0, activePoints.size - 1)
                                onScrub(activePoints[pIdx])
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        touchX = -1f
                        onScrub(null)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (activePoints.size < 2) return@Canvas

                val w = size.width
                val h = size.height
                val topPadding = 16f
                val bottomPadding = 24f
                val plotHeight = h - topPadding - bottomPadding

                // Volume Histogram Sub-chart (bottom 22% of plot area)
                if (maxVolume > 0.0) {
                    val volumeSubHeight = plotHeight * 0.22f
                    val volBaseY = h - bottomPadding
                    val barWidth = max(2f, (w / activePoints.size) * 0.65f)

                    activePoints.forEachIndexed { i, pt ->
                        if (pt.volume > 0) {
                            val x = (i.toFloat() / (activePoints.size - 1)) * w
                            val barH = ((pt.volume / maxVolume) * volumeSubHeight).toFloat().coerceAtLeast(2f)
                            val isBarGreen = if (i > 0) pt.price >= activePoints[i - 1].price else isPositive
                            val barColor = if (isBarGreen) {
                                TerminalEmeraldPositive.copy(alpha = 0.45f)
                            } else {
                                TerminalCrimsonNegative.copy(alpha = 0.45f)
                            }
                            drawRect(
                                color = barColor,
                                topLeft = Offset(x - (barWidth / 2f), volBaseY - barH),
                                size = Size(barWidth, barH)
                            )
                        }
                    }
                }

                // Price Paths
                val path = Path()
                val fillPath = Path()

                activePoints.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (activePoints.size - 1)) * w
                    val normalizedY = ((pt.price - minPrice) / priceRange).toFloat()
                    val y = h - bottomPadding - (normalizedY * plotHeight)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h - bottomPadding)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(w, h - bottomPadding)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientTop, gradientBottom),
                        startY = topPadding,
                        endY = h - bottomPadding
                    )
                )

                // Draw Price Stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Crosshair & Two-Way Anchor to Y-axis
                if (touchX >= 0f && currentScrub != null) {
                    val idx = activePoints.indexOf(currentScrub)
                    if (idx != -1) {
                        val ptX = (idx.toFloat() / (activePoints.size - 1)) * w
                        val normY = ((currentScrub.price - minPrice) / priceRange).toFloat()
                        val ptY = h - bottomPadding - (normY * plotHeight)

                        // Vertical Crosshair Line
                        drawLine(
                            color = TerminalSlateSecondary.copy(alpha = 0.5f),
                            start = Offset(ptX, topPadding),
                            end = Offset(ptX, h - bottomPadding),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )

                        // Two-Way Anchor horizontal line to Y-axis
                        drawLine(
                            color = TerminalSlateSecondary.copy(alpha = 0.4f),
                            start = Offset(ptX, ptY),
                            end = Offset(w, ptY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Concentric Pulsing Anchor Circle
                        drawCircle(
                            color = lineColor.copy(alpha = 0.3f),
                            radius = 9.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                    }
                }
            }

            // Zoom Reset Floating Pill
            if (zoomScale > 1.05f) {
                Surface(
                    onClick = {
                        zoomScale = 1f
                        panOffsetRatio = 0f
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Zoom",
                            tint = TerminalSlateSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", zoomScale)}x • Reset",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlatePrimary
                        )
                    }
                }
            }

            // Crosshair Two-Way Anchor Y-axis Label Pill
            if (touchX >= 0f && currentScrub != null) {
                val idx = activePoints.indexOf(currentScrub)
                if (idx != -1) {
                    val normY = ((currentScrub.price - minPrice) / priceRange).toFloat()
                    val pxY = 260.dp * (1f - normY) // Approximate Y
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = with(density) {
                                        val topPaddingPx = 16.dp.toPx()
                                        val bottomPaddingPx = 24.dp.toPx()
                                        val plotH = (260.dp.toPx() - topPaddingPx - bottomPaddingPx)
                                        val y = 260.dp.toPx() - bottomPaddingPx - (normY * plotH)
                                        (y - 10.dp.toPx()).roundToInt()
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                            .background(lineColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatAssetPrice(currentScrub.price, currency),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Timeline extremes & Volume Indicator footnote
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activePoints.firstOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
            if (maxVolume > 0.0) {
                Text(
                    text = "VOL HISTOGRAM (20%)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalSlateSecondary.copy(alpha = 0.7f)
                )
            }
            Text(
                text = activePoints.lastOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
        }
    }
}
