package com.fin.mf.tools.terminal.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.tools.terminal.data.BenchmarkComparisonPoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Normalized Benchmark Overlay Chart: plots relative % return of target stock vs NIFTY 50.
 * Frozen semantic constraint: Benchmark line strictly remains locked to Amber Gold (TerminalAmberBenchmark #D97706) with dashed curve.
 * Includes interactive drag/scrubbing to inspect live alpha spread across any date.
 */
@Composable
fun BenchmarkOverlayChart(
    comparisonPoints: List<BenchmarkComparisonPoint>,
    targetSymbol: String,
    benchmarkSymbol: String
) {
    val gradient = LocalVaultGradient.current
    if (comparisonPoints.isEmpty()) return

    var scrubbedPoint by remember { mutableStateOf<BenchmarkComparisonPoint?>(null) }
    var touchX by remember { mutableFloatStateOf(-1f) }

    val activePoint = scrubbedPoint ?: comparisonPoints.lastOrNull() ?: return
    val alpha = activePoint.alphaSpread
    val isOutperforming = alpha >= 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend & Live Alpha Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(gradient.startColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = targetSymbol.take(12),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalSlatePrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TerminalAmberBenchmark)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = benchmarkSymbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalSlatePrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOutperforming) TerminalEmeraldSoft else TerminalCrimsonSoft
            ) {
                Text(
                    text = "Alpha: ${if (isOutperforming) "+" else ""}${roundTwo(alpha)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOutperforming) TerminalEmeraldPositive else TerminalCrimsonNegative,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dual Normalized Curve Canvas with Drag Scrubbing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(comparisonPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (comparisonPoints.size - 1)).roundToInt().coerceIn(0, comparisonPoints.size - 1)
                            scrubbedPoint = comparisonPoints[idx]
                        },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (comparisonPoints.size - 1)).roundToInt().coerceIn(0, comparisonPoints.size - 1)
                            scrubbedPoint = comparisonPoints[idx]
                        },
                        onDragEnd = {
                            touchX = -1f
                            scrubbedPoint = null
                        },
                        onDragCancel = {
                            touchX = -1f
                            scrubbedPoint = null
                        }
                    )
                }
                .pointerInput(comparisonPoints) {
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (fraction * (comparisonPoints.size - 1)).roundToInt().coerceIn(0, comparisonPoints.size - 1)
                            scrubbedPoint = comparisonPoints[idx]
                        }
                    )
                }
        ) {
            val targetPath = remember { Path() }
            val benchPath = remember { Path() }
            val zeroDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) }
            val benchDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (comparisonPoints.size < 2) return@Canvas

                val minPct = min(
                    comparisonPoints.minOf { it.targetPct },
                    comparisonPoints.minOf { it.benchmarkPct }
                )
                val maxPct = max(
                    comparisonPoints.maxOf { it.targetPct },
                    comparisonPoints.maxOf { it.benchmarkPct }
                )
                val rangePct = max(maxPct - minPct, 0.5)

                val w = size.width
                val h = size.height
                val topPadding = 12f
                val bottomPadding = 16f
                val plotHeight = h - topPadding - bottomPadding

                // Draw 0.0% Baseline
                val zeroNorm = ((0.0 - minPct) / rangePct).toFloat()
                if (zeroNorm in 0f..1f) {
                    val zeroY = h - bottomPadding - (zeroNorm * plotHeight)
                    drawLine(
                        color = TerminalSlateSecondary.copy(alpha = 0.35f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = zeroDashEffect
                    )
                }

                targetPath.reset()
                benchPath.reset()

                comparisonPoints.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (comparisonPoints.size - 1)) * w

                    val normTargetY = ((pt.targetPct - minPct) / rangePct).toFloat()
                    val targetY = h - bottomPadding - (normTargetY * plotHeight)

                    val normBenchY = ((pt.benchmarkPct - minPct) / rangePct).toFloat()
                    val benchY = h - bottomPadding - (normBenchY * plotHeight)

                    if (i == 0) {
                        targetPath.moveTo(x, targetY)
                        benchPath.moveTo(x, benchY)
                    } else {
                        targetPath.lineTo(x, targetY)
                        benchPath.lineTo(x, benchY)
                    }
                }

                // Draw Benchmark Path (Amber Gold DASHED curve)
                drawPath(
                    path = benchPath,
                    color = TerminalAmberBenchmark,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = benchDashEffect
                    )
                )

                // Draw Target Path (Brand Gradient Primary solid curve)
                drawPath(
                    path = targetPath,
                    color = gradient.startColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Crosshair Scrubber if active
                if (touchX >= 0f && scrubbedPoint != null) {
                    val idx = comparisonPoints.indexOf(scrubbedPoint)
                    if (idx != -1) {
                        val ptX = (idx.toFloat() / (comparisonPoints.size - 1)) * w
                        val normTargetY = ((scrubbedPoint!!.targetPct - minPct) / rangePct).toFloat()
                        val targetY = h - bottomPadding - (normTargetY * plotHeight)

                        val normBenchY = ((scrubbedPoint!!.benchmarkPct - minPct) / rangePct).toFloat()
                        val benchY = h - bottomPadding - (normBenchY * plotHeight)

                        drawLine(
                            color = TerminalSlateSecondary.copy(alpha = 0.5f),
                            start = Offset(ptX, topPadding),
                            end = Offset(ptX, h - bottomPadding),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )

                        // Target anchor circle
                        drawCircle(color = gradient.startColor, radius = 5.dp.toPx(), center = Offset(ptX, targetY))
                        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(ptX, targetY))

                        // Benchmark anchor circle
                        drawCircle(color = TerminalAmberBenchmark, radius = 5.dp.toPx(), center = Offset(ptX, benchY))
                        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(ptX, benchY))
                    }
                }
            }
        }

        // Return summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$targetSymbol: ${if (activePoint.targetPct >= 0) "+" else ""}${roundTwo(activePoint.targetPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = gradient.startColor
            )
            if (scrubbedPoint != null) {
                Text(
                    text = scrubbedPoint!!.dateFormatted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalSlateSecondary
                )
            }
            Text(
                text = "$benchmarkSymbol: ${if (activePoint.benchmarkPct >= 0) "+" else ""}${roundTwo(activePoint.benchmarkPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalAmberBenchmark
            )
        }
    }
}
