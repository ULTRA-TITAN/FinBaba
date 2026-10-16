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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 2-Point Delta Analyzer Chart: allows dual marker placement and computes absolute delta & annualized CAGR.
 */
@Composable
fun DualPointerAnalyzerChart(
    points: List<TerminalChartPoint>,
    pointerA: TerminalChartPoint?,
    pointerB: TerminalChartPoint?,
    currency: String,
    onPointerAChanged: (TerminalChartPoint) -> Unit,
    onPointerBChanged: (TerminalChartPoint) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val ptA = pointerA ?: points.firstOrNull() ?: return
    val ptB = pointerB ?: points.lastOrNull() ?: return

    val idxA = points.indexOf(ptA).coerceAtLeast(0)
    val idxB = points.indexOf(ptB).coerceAtLeast(0)

    val lowerIdx = min(idxA, idxB)
    val upperIdx = max(idxA, idxB)

    val startPt = points[lowerIdx]
    val endPt = points[upperIdx]

    val deltaPrice = endPt.price - startPt.price
    val absReturnPct = if (startPt.price > 0) (deltaPrice / startPt.price) * 100.0 else 0.0

    // Compute CAGR if span >= 365 days
    val spanMs = max(0L, endPt.epochMs - startPt.epochMs)
    val spanDays = (spanMs / (1000.0 * 60.0 * 60.0 * 24.0)).roundToInt()
    val cagr = if (spanDays >= 365 && startPt.price > 0 && endPt.price > 0) {
        val years = spanDays / 365.25
        ((endPt.price / startPt.price).pow(1.0 / years) - 1.0) * 100.0
    } else null

    Column(modifier = Modifier.fillMaxWidth()) {
        // Floating comparison banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TerminalBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(gradient.startColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Point A (${startPt.dateFormatted})",
                                fontSize = 11.sp,
                                color = TerminalSlateSecondary
                            )
                        }
                        Text(
                            text = formatAssetPrice(startPt.price, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlatePrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(TerminalPurplePointer)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Point B (${endPt.dateFormatted})",
                                fontSize = 11.sp,
                                color = TerminalSlateSecondary
                            )
                        }
                        Text(
                            text = formatAssetPrice(endPt.price, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalSlatePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TerminalSlateBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDeltaPos = deltaPrice >= 0
                    Column {
                        Text("Absolute Delta", fontSize = 11.sp, color = TerminalSlateSecondary)
                        Text(
                            text = "${if (isDeltaPos) "+" else ""}${roundTwo(absReturnPct)}% (${formatAssetPrice(deltaPrice, currency, showPlus = true)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeltaPos) TerminalEmeraldPositive else TerminalCrimsonNegative
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (cagr != null) "Annualized CAGR" else "Span Duration",
                            fontSize = 11.sp,
                            color = TerminalSlateSecondary
                        )
                        Text(
                            text = if (cagr != null) {
                                "${if (cagr >= 0) "+" else ""}${roundTwo(cagr)}% p.a."
                            } else {
                                "$spanDays days"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cagr != null && cagr >= 0) TerminalEmeraldPositive else TerminalSlatePrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Pointer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val minPrice = points.minOf { it.price }
                val maxPrice = points.maxOf { it.price }
                val priceRange = max(maxPrice - minPrice, 0.01)

                val w = size.width
                val h = size.height
                val topPadding = 12f
                val bottomPadding = 16f
                val plotHeight = h - topPadding - bottomPadding

                val xA = (lowerIdx.toFloat() / (points.size - 1)) * w
                val xB = (upperIdx.toFloat() / (points.size - 1)) * w

                // Draw highlighted region between Point A and Point B
                drawRect(
                    color = gradient.startColor.copy(alpha = 0.08f),
                    topLeft = Offset(xA, topPadding),
                    size = androidx.compose.ui.geometry.Size(xB - xA, plotHeight)
                )

                // Draw base line
                val path = Path()
                points.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / (points.size - 1)) * w
                    val normalizedY = ((pt.price - minPrice) / priceRange).toFloat()
                    val y = h - bottomPadding - (normalizedY * plotHeight)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = TerminalSlateSecondary.copy(alpha = 0.45f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Vertical Marker A
                drawLine(
                    color = gradient.startColor,
                    start = Offset(xA, topPadding),
                    end = Offset(xA, h - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                // Vertical Marker B
                drawLine(
                    color = TerminalPurplePointer,
                    start = Offset(xB, topPadding),
                    end = Offset(xB, h - bottomPadding),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dual Range Slider to move Point A and Point B cleanly
        var sliderPosition by remember(lowerIdx, upperIdx) {
            mutableStateOf(lowerIdx.toFloat()..upperIdx.toFloat())
        }

        RangeSlider(
            value = sliderPosition,
            onValueChange = { range ->
                sliderPosition = range
                val newA = range.start.roundToInt().coerceIn(0, points.size - 1)
                val newB = range.endInclusive.roundToInt().coerceIn(0, points.size - 1)
                onPointerAChanged(points[newA])
                onPointerBChanged(points[newB])
            },
            valueRange = 0f..(points.size - 1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = gradient.startColor,
                activeTrackColor = gradient.startColor,
                inactiveTrackColor = TerminalSlateBorder
            )
        )
    }
}
