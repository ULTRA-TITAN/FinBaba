package com.fin.mf.tools.terminal.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Standard Interactive Chart with smooth line, gradient fill, and drag scrub crosshair.
 */
@Composable
fun StandardInteractiveChart(
    points: List<TerminalChartPoint>,
    scrubbedPoint: TerminalChartPoint?,
    isPositive: Boolean,
    onScrub: (TerminalChartPoint?) -> Unit
) {
    val lineColor = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
    val gradientTop = if (isPositive) TerminalEmeraldPositive.copy(alpha = 0.28f) else TerminalCrimsonNegative.copy(alpha = 0.28f)
    val gradientBottom = Color.Transparent

    var touchX by remember { mutableFloatStateOf(-1f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        },
                        onDragEnd = {
                            touchX = -1f
                            onScrub(null)
                        },
                        onDragCancel = {
                            touchX = -1f
                            onScrub(null)
                        }
                    )
                }
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(points[idx])
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val minPrice = points.minOf { it.price }
                val maxPrice = points.maxOf { it.price }
                val priceRange = max(maxPrice - minPrice, 0.01)

                val w = size.width
                val h = size.height
                val topPadding = 16f
                val bottomPadding = 20f
                val plotHeight = h - topPadding - bottomPadding

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (points.size - 1)) * w
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

                // Draw Area Gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientTop, gradientBottom),
                        startY = topPadding,
                        endY = h - bottomPadding
                    )
                )

                // Draw Primary Stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Crosshair Scrubber if active
                if (touchX >= 0f && scrubbedPoint != null) {
                    val clampedX = touchX.coerceIn(0f, w)
                    drawLine(
                        color = TerminalSlateSecondary.copy(alpha = 0.5f),
                        start = Offset(clampedX, topPadding),
                        end = Offset(clampedX, h - bottomPadding),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )

                    // Find point coordinate
                    val idx = points.indexOf(scrubbedPoint)
                    if (idx != -1) {
                        val ptX = (idx.toFloat() / (points.size - 1)) * w
                        val normY = ((scrubbedPoint.price - minPrice) / priceRange).toFloat()
                        val ptY = h - bottomPadding - (normY * plotHeight)

                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(ptX, ptY)
                        )
                    }
                }
            }
        }

        // Timeline extremes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.firstOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
            Text(
                text = points.lastOrNull()?.dateFormatted ?: "",
                fontSize = 11.sp,
                color = TerminalSlateSecondary
            )
        }
    }
}
