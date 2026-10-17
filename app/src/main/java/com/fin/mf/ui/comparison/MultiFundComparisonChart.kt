package com.fin.mf.ui.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.ChartTimeframe
import com.fin.mf.data.FundComparisonItem
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.engine.PortfolioAnalyticsEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- Frozen Semantic & Categorical Colors (Non-Negotiable Invariants) ---
private val SemanticProfitGreen = Color(0xFF059669)
private val SemanticLossRed = Color(0xFFDC2626)
private val SemanticBenchmarkAmber = Color(0xFFD97706)
private val SemanticPortfolioPurple = Color(0xFF7C3AED)
private val ChartSlatePrimary = Color(0xFF0F172A)
private val ChartSlateSecondary = Color(0xFF475569)

/**
 * 5 distinct institutional line colors for up to 5 concurrent peer comparison funds.
 * Note: Benchmark Amber (#D97706) is reserved for index overlays; Fund #3 uses Deep Cyan (#0891B2).
 */
val ComparisonFundLineColors = listOf(
    Color(0xFF1E3A8A), // Fund 1: Royal Navy
    Color(0xFF059669), // Fund 2: Institutional Emerald
    Color(0xFF0891B2), // Fund 3: Deep Cyan
    Color(0xFFDB2777), // Fund 4: Magenta Rose
    Color(0xFF4F46E5)  // Fund 5: Electric Indigo
)

/**
 * High-performance Base-100 normalized multi-line Canvas chart for comparing 2 to 5 mutual funds,
 * with optional "My Portfolio Average" overlay and interactive touch-scrubbing crosshair.
 */
@Composable
fun MultiFundComparisonChart(
    funds: List<FundComparisonItem>,
    timeframe: ChartTimeframe,
    customStartEpochMs: Long?,
    customEndEpochMs: Long?,
    isPortfolioOverlayEnabled: Boolean,
    portfolioSchemes: List<MutualFundScheme>,
    modifier: Modifier = Modifier
) {
    val gradient = LocalVaultGradient.current
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // 1. Slice & Rebase each fund's NAV series to 100.0 at the start of the timeframe
    val normalizedSeriesList: List<List<Pair<Long, Double>>> = remember(
        funds,
        timeframe,
        customStartEpochMs,
        customEndEpochMs
    ) {
        funds.map { fund ->
            val chronological = fund.navHistory.mapNotNull { entry ->
                val nav = entry.nav.trim().toDoubleOrNull() ?: return@mapNotNull null
                val epoch = PortfolioAnalyticsEngine.parseAmfiDateEpochMs(entry.date)
                if (epoch > 0L && nav > 0.0) Pair(epoch, nav) else null
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
                    if (filtered.size >= 2) {
                        filtered
                    } else {
                        chronological.takeLast(timeframe.calendarDays.coerceAtLeast(2))
                    }
                }
            } else {
                emptyList()
            }

            if (sliced.size >= 2) {
                val baseNav = sliced.first().second
                sliced.map { (epoch, nav) ->
                    Pair(epoch, if (baseNav > 0.0) (nav / baseNav) * 100.0 else 100.0)
                }
            } else {
                emptyList()
            }
        }
    }

    if (normalizedSeriesList.all { it.size < 2 }) {
        Text(
            text = "Insufficient overlapping historical NAV data for the selected timeframe.",
            fontSize = 12.sp,
            color = ChartSlateSecondary
        )
        return
    }

    // 2. Compute optional "My Portfolio Average" Base-100 trajectory
    val referenceTimestamps = remember(normalizedSeriesList) {
        normalizedSeriesList.maxByOrNull { it.size }?.map { it.first }.orEmpty()
    }

    val portfolioOverlaySeries: List<Pair<Long, Double>> = remember(
        isPortfolioOverlayEnabled,
        referenceTimestamps,
        normalizedSeriesList,
        portfolioSchemes
    ) {
        if (!isPortfolioOverlayEnabled || referenceTimestamps.size < 2) {
            emptyList()
        } else {
            PortfolioAnalyticsEngine.buildPortfolioOverlaySeries(
                referenceTimestamps = referenceTimestamps,
                comparisonSeries = normalizedSeriesList
            )
        }
    }

    // 3. Compute global Y-axis bounds across all active series
    val allNormalizedValues = remember(normalizedSeriesList, portfolioOverlaySeries) {
        normalizedSeriesList.flatten().map { it.second } + portfolioOverlaySeries.map { it.second }
    }
    val minVal = (allNormalizedValues.minOrNull() ?: 92.0).coerceAtMost(98.5)
    val maxVal = (allNormalizedValues.maxOrNull() ?: 108.0).coerceAtLeast(101.5)
    val valRange = (maxVal - minVal).coerceAtLeast(0.5)

    var scrubFraction by remember { mutableFloatStateOf(1f) }

    val scrubbedDateLabel = remember(referenceTimestamps, scrubFraction) {
        if (referenceTimestamps.isNotEmpty()) {
            val idx = (scrubFraction * referenceTimestamps.lastIndex)
                .roundToInt()
                .coerceIn(0, referenceTimestamps.lastIndex)
            displayDateFormat.format(Date(referenceTimestamps[idx]))
        } else {
            timeframe.label
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active Scrubber Date Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = gradient.startColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Cursor Date: $scrubbedDateLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradient.startColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                text = "Base = 100.0",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChartSlateSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Legend with Frozen Semantic Profit Green / Loss Red Return %
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            funds.forEachIndexed { index, fund ->
                val series = normalizedSeriesList.getOrNull(index).orEmpty()
                val currentNorm = if (series.isNotEmpty()) {
                    val idx = (scrubFraction * series.lastIndex).roundToInt().coerceIn(0, series.lastIndex)
                    series[idx].second
                } else {
                    100.0
                }
                val returnPct = currentNorm - 100.0
                val isPositive = returnPct >= 0.0
                val sign = if (isPositive) "+" else ""
                val roundedPct = (returnPct * 100.0).roundToInt() / 100.0
                val lineColor = ComparisonFundLineColors.getOrElse(index) { gradient.startColor }

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
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(lineColor)
                        )
                        Text(
                            text = fund.schemeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChartSlatePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$sign$roundedPct% (${((currentNorm * 10.0).roundToInt() / 10.0)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) SemanticProfitGreen else SemanticLossRed
                    )
                }
            }

            if (portfolioOverlaySeries.size >= 2) {
                val pIdx = (scrubFraction * portfolioOverlaySeries.lastIndex)
                    .roundToInt()
                    .coerceIn(0, portfolioOverlaySeries.lastIndex)
                val pNorm = portfolioOverlaySeries[pIdx].second
                val pReturn = pNorm - 100.0
                val pSign = if (pReturn >= 0.0) "+" else ""
                val pRounded = (pReturn * 100.0).roundToInt() / 100.0

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
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(SemanticPortfolioPurple)
                        )
                        Text(
                            text = "My Portfolio Composite Overlay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SemanticPortfolioPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$pSign$pRounded% (${((pNorm * 10.0).roundToInt() / 10.0)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pReturn >= 0.0) SemanticProfitGreen else SemanticLossRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val linePath = remember { Path() }
        val overlayPath = remember { Path() }
        val referenceDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) }
        val overlayDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f) }

        // Multi-Line Normalized Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(normalizedSeriesList) {
                    detectTapGestures { offset ->
                        scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(normalizedSeriesList) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val vPadding = 12.dp.toPx()
            val usableH = height - (vPadding * 2f)

            // 1. Horizontal Base-100 Reference Line (Amber Gold / Slate Dash)
            val norm100Y = height - vPadding - (((100.0 - minVal) / valRange).toFloat() * usableH)
            drawLine(
                color = SemanticBenchmarkAmber.copy(alpha = 0.55f),
                start = Offset(0f, norm100Y),
                end = Offset(width, norm100Y),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = referenceDashEffect
            )

            // 2. Draw each fund's trajectory
            normalizedSeriesList.forEachIndexed { fundIdx, series ->
                if (series.size >= 2) {
                    val lineColor = ComparisonFundLineColors.getOrElse(fundIdx) { gradient.startColor }
                    linePath.reset()
                    series.forEachIndexed { i, pt ->
                        val x = (i.toFloat() / series.lastIndex.toFloat()) * width
                        val normY = ((pt.second - minVal) / valRange).toFloat()
                        val y = height - vPadding - (normY * usableH)
                        if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val sIdx = (scrubFraction * series.lastIndex).roundToInt().coerceIn(0, series.lastIndex)
                    val sVal = series[sIdx].second
                    val sX = (sIdx.toFloat() / series.lastIndex.toFloat()) * width
                    val sNormY = ((sVal - minVal) / valRange).toFloat()
                    val sY = height - vPadding - (sNormY * usableH)

                    drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = Offset(sX, sY))
                    drawCircle(color = lineColor, radius = 3.8.dp.toPx(), center = Offset(sX, sY))
                }
            }

            // 3. Draw optional Portfolio Composite Overlay (Dashed Purple Curve)
            if (portfolioOverlaySeries.size >= 2) {
                overlayPath.reset()
                portfolioOverlaySeries.forEachIndexed { i, pt ->
                    val x = (i.toFloat() / portfolioOverlaySeries.lastIndex.toFloat()) * width
                    val normY = ((pt.second - minVal) / valRange).toFloat()
                    val y = height - vPadding - (normY * usableH)
                    if (i == 0) overlayPath.moveTo(x, y) else overlayPath.lineTo(x, y)
                }
                drawPath(
                    path = overlayPath,
                    color = SemanticPortfolioPurple,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = overlayDashEffect
                    )
                )
            }

            // 4. Vertical Scrubber Ruler
            val scrubX = scrubFraction * width
            drawLine(
                color = gradient.startColor.copy(alpha = 0.45f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, height),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min Index: ${((minVal * 10.0).roundToInt() / 10.0)}",
                fontSize = 10.sp,
                color = ChartSlateSecondary
            )
            Text(
                text = "Max Index: ${((maxVal * 10.0).roundToInt() / 10.0)}",
                fontSize = 10.sp,
                color = ChartSlateSecondary
            )
        }
    }
}