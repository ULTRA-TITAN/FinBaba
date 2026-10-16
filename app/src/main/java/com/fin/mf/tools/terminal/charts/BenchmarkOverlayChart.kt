package com.fin.mf.tools.terminal.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.tools.terminal.data.BenchmarkComparisonPoint
import kotlin.math.max
import kotlin.math.min

/**
 * Normalized Benchmark Overlay Chart: plots relative % return of target stock vs NIFTY 50.
 * Frozen semantic constraint: Benchmark line strictly remains locked to Amber Gold (TerminalAmberBenchmark #D97706).
 */
@Composable
fun BenchmarkOverlayChart(
    comparisonPoints: List<BenchmarkComparisonPoint>,
    targetSymbol: String,
    benchmarkSymbol: String
) {
    val gradient = LocalVaultGradient.current
    val lastPoint = comparisonPoints.lastOrNull() ?: return
    val alpha = lastPoint.alphaSpread
    val isOutperforming = alpha >= 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend & Alpha Badge
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

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Normalized Curve Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
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
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                val targetPath = Path()
                val benchPath = Path()

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

                // Draw Benchmark Path (Amber)
                drawPath(
                    path = benchPath,
                    color = TerminalAmberBenchmark,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Target Path (Brand Gradient Primary)
                drawPath(
                    path = targetPath,
                    color = gradient.startColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
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
                text = "$targetSymbol: ${if (lastPoint.targetPct >= 0) "+" else ""}${roundTwo(lastPoint.targetPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = gradient.startColor
            )
            Text(
                text = "$benchmarkSymbol: ${if (lastPoint.benchmarkPct >= 0) "+" else ""}${roundTwo(lastPoint.benchmarkPct)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalAmberBenchmark
            )
        }
    }
}
