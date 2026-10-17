package com.fin.mf.tools.terminal.charts

import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.tools.terminal.data.ChartDataPoint
import com.fin.mf.tools.terminal.data.TerminalChartPoint


import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// ================================================================================
// DESIGN SYSTEM & PALETTE CONSTANTS
// ================================================================================

object TerminalPalette {
    val SurfaceWhite = Color(0xFFFFFFFF)
    val BackgroundIceMist = Color(0xFFF8FAFC)
    val SlateBorder = Color(0xFFE2E8F0)
    val BullishEmerald = Color(0xFF059669)
    val BullishSoftTint = Color(0xFFECFDF5)
    val BearishCrimson = Color(0xFFDC2626)
    val BearishSoftTint = Color(0xFFFEF2F2)
    val BenchmarkAmberGold = Color(0xFFF59E0B)
    val BenchmarkAmberDeep = Color(0xFFD97706)
    val RulerPurple = Color(0xFF7C3AED)
    val RulerPurpleSoft = Color(0xFF7C3AED).copy(alpha = 0.12f)
    val TextPrimarySlate = Color(0xFF0F172A)
    val TextSecondarySlate = Color(0xFF475569)
}

@Stable
data class VaultGradientConfig(
    val startColor: Color = Color(0xFF2563EB),
    val endColor: Color = Color(0xFF1D4ED8)
)

val LocalVaultGradient = compositionLocalOf { VaultGradientConfig() }

// ================================================================================
// DATA MODELS & VIEWPORT STATE
// ================================================================================

@Stable
data class ChartDataPoint(
    val timestampMillis: Long,
    val price: Float,
    val volume: Long = 0L,
    val open: Float = price,
    val high: Float = price,
    val low: Float = price
)

@Stable
data class RulerMeasurement(
    val startIndex: Int,
    val endIndex: Int,
    val pointA: ChartDataPoint,
    val pointB: ChartDataPoint,
    val priceSpread: Float,
    val percentageReturn: Float,
    val barsElapsed: Int,
    val daysElapsed: Int,
    val annualizedCagr: Float?
)

@Stable
private data class ComputedViewportSlice(
    val visiblePoints: List<ChartDataPoint>,
    val visibleBenchmark: List<ChartDataPoint>,
    val startIndex: Int,
    val endIndex: Int,
    val minPrice: Float,
    val maxPrice: Float,
    val maxVolume: Long,
    val priceTicks: List<Pair<Float, String>>,
    val timeTicks: List<Pair<Int, String>>,
    val benchmarkNormalizedPrices: FloatArray,
    val latestAlphaSpread: Float?
)

// ================================================================================
// BACKWARD-COMPATIBLE ENTRY POINT
// ================================================================================

/**
 * Drop-in replacement and upgrade for `StandardInteractiveChart`.
 * Preserves 100% backward compatibility with existing ViewModel bindings and navigation routes.
 * Backward-compatible overload for existing Screen call sites passing List<TerminalChartPoint>.
 */
@Composable
fun StandardInteractiveChart(
    points: List<TerminalChartPoint>,
    scrubbedPoint: TerminalChartPoint?,
    isPositive: Boolean,
    currency: String = "INR",
    onScrub: (TerminalChartPoint?) -> Unit
) {
    val chartDataPoints = remember(points) {
        points.map { pt ->
            ChartDataPoint(
                timestampEpochSec = pt.epochMs / 1000L,
                open = pt.price.toFloat(),
                high = pt.price.toFloat(),
                low = pt.price.toFloat(),
                close = pt.price.toFloat(),
                volume = pt.volume
            )
        }
    }
    TradingViewCartesianChart(
        points = chartDataPoints,
        currencySymbol = if (currency.equals("USD", true)) "$" else "₹",
        dayChange = if (isPositive) 1f else -1f,
        onPointSelected = { selectedPoint ->
            if (selectedPoint != null) {
                val originalPt = points.firstOrNull { it.epochMs / 1000L == selectedPoint.timestampEpochSec }
                    ?: TerminalChartPoint(
                        epochMs = selectedPoint.timestampEpochSec * 1000L,
                        price = selectedPoint.close.toDouble(),
                        dateFormatted = "",
                        volume = selectedPoint.volume
                    )
                onScrub(originalPt)
            } else {
                onScrub(null)
            }
        }
    )
}

@Composable
fun StandardInteractiveChart(
    points: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    benchmarkPoints: List<ChartDataPoint> = emptyList(),
    currencySymbol: String = "₹",
    isIntraday: Boolean = false,
    dayChange: Float = 0f,
    initialShowBenchmark: Boolean = false,
    initialShowVolume: Boolean = true,
    chartHeight: Dp = 340.dp,
    onPointSelected: ((ChartDataPoint?) -> Unit)? = null
) {
    TradingViewCartesianChart(
        points = points,
        modifier = modifier,
        benchmarkPoints = benchmarkPoints,
        currencySymbol = currencySymbol,
        isIntraday = isIntraday,
        dayChange = dayChange,
        initialShowBenchmark = initialShowBenchmark,
        initialShowVolume = initialShowVolume,
        chartHeight = chartHeight,
        onPointSelected = onPointSelected
    )
}

// ================================================================================
// CORE INSTITUTIONAL CARTESIAN ENGINE
// ================================================================================

@Composable
fun TradingViewCartesianChart(
    points: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    benchmarkPoints: List<ChartDataPoint> = emptyList(),
    currencySymbol: String = "₹",
    isIntraday: Boolean = false,
    dayChange: Float = 0f,
    initialShowBenchmark: Boolean = false,
    initialShowVolume: Boolean = true,
    chartHeight: Dp = 340.dp,
    onPointSelected: ((ChartDataPoint?) -> Unit)? = null
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val accentColor = LocalVaultGradient.current.startColor

    // Interactive Toolbar States
    var isRulerModeActive by remember { mutableStateOf(false) }
    var showBenchmarkOverlay by remember { mutableStateOf(initialShowBenchmark && benchmarkPoints.isNotEmpty()) }
    var showVolumeSubChart by remember { mutableStateOf(initialShowVolume) }

    // Viewport Zoom & Pan States
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var scrollOffsetFraction by remember { mutableFloatStateOf(0.0f) } // 0f = oldest left, 1f = latest right

    // Crosshair Scrubber States
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbedLocalIndex by remember { mutableIntStateOf(-1) }
    var lastScrubReleaseTime by remember { mutableLongStateOf(0L) }

    // Two-Point Ruler States (indices in full `points` list)
    var rulerStartGlobalIndex by remember { mutableIntStateOf(-1) }
    var rulerEndGlobalIndex by remember { mutableIntStateOf(-1) }

    // Auto-timeout 2 seconds after finger lift for Crosshair
    LaunchedEffect(isScrubbing, lastScrubReleaseTime) {
        if (!isScrubbing && lastScrubReleaseTime > 0L && scrubbedLocalIndex != -1) {
            delay(2000L)
            scrubbedLocalIndex = -1
            onPointSelected?.invoke(null)
        }
    }

    // Formatters (remembered outside loops)
    val priceFormatter = remember(currencySymbol) {
        DecimalFormat("$currencySymbol#,##0.00")
    }
    val signedPriceFormatter = remember(currencySymbol) {
        DecimalFormat("+$currencySymbol#,##0.00;-$currencySymbol#,##0.00")
    }
    val timeFormatter = remember(isIntraday) {
        SimpleDateFormat(if (isIntraday) "HH:mm" else "dd MMM", Locale.US)
    }
    val fullDateFormatter = remember(isIntraday) {
        SimpleDateFormat(if (isIntraday) "dd MMM, HH:mm" else "dd MMM yyyy", Locale.US)
    }

    // Offload Coordinate Slicing, Min/Max Calculation, and Benchmark Normalization to Dispatchers.Default
    var viewportSlice by remember { mutableStateOf<ComputedViewportSlice?>(null) }

    LaunchedEffect(points, benchmarkPoints, zoomScale, scrollOffsetFraction, showBenchmarkOverlay, isIntraday, currencySymbol) {
        if (points.isEmpty()) {
            viewportSlice = null
            return@LaunchedEffect
        }
        val computed = withContext(Dispatchers.Default) {
            val totalCount = points.size
            val visibleCount = (totalCount / zoomScale.coerceIn(1.0f, 15.0f))
                .roundToInt()
                .coerceIn(min(2, totalCount), totalCount)
            val maxStartIndex = (totalCount - visibleCount).coerceAtLeast(0)
            val startIndex = (scrollOffsetFraction.coerceIn(0f, 1f) * maxStartIndex)
                .roundToInt()
                .coerceIn(0, maxStartIndex)
            val endIndex = (startIndex + visibleCount).coerceAtMost(totalCount)

            val slice = points.subList(startIndex, endIndex)
            val benchSlice = if (showBenchmarkOverlay && benchmarkPoints.size >= endIndex) {
                benchmarkPoints.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            var rawMin = Float.MAX_VALUE
            var rawMax = -Float.MAX_VALUE
            var maxVol = 1L

            for (i in slice.indices) {
                val p = slice[i]
                if (p.price < rawMin) rawMin = p.price
                if (p.price > rawMax) rawMax = p.price
                if (p.volume > maxVol) maxVol = p.volume
            }

            // Normalize NIFTY 50 Benchmark into Asset Price Space via Base-100 Relative Return
            val normalizedBench = FloatArray(benchSlice.size)
            var alphaSpread: Float? = null

            if (benchSlice.isNotEmpty() && slice.isNotEmpty()) {
                val assetBasePrice = slice.first().price.coerceAtLeast(0.0001f)
                val benchBasePrice = benchSlice.first().price.coerceAtLeast(0.0001f)

                for (i in benchSlice.indices) {
                    val benchRelReturn = (benchSlice[i].price - benchBasePrice) / benchBasePrice
                    // Project onto asset price scale so both curves share a common baseline at t=0
                    val projectedPrice = assetBasePrice * (1f + benchRelReturn)
                    normalizedBench[i] = projectedPrice
                    if (projectedPrice < rawMin) rawMin = projectedPrice
                    if (projectedPrice > rawMax) rawMax = projectedPrice
                }

                val latestAssetRel = ((slice.last().price - assetBasePrice) / assetBasePrice) * 100f
                val latestBenchRel = ((benchSlice.last().price - benchBasePrice) / benchBasePrice) * 100f
                alphaSpread = latestAssetRel - latestBenchRel
            }

            if (rawMax <= rawMin) {
                rawMax = rawMin + 1f
            }
            val padding = (rawMax - rawMin) * 0.08f
            val finalMin = rawMin - padding
            val finalMax = rawMax + padding

            // Precompute 5 balanced Y-axis divisions (6 ticks)
            val steps = 5
            val deltaPrice = (finalMax - finalMin) / steps
            val pTicks = ArrayList<Pair<Float, String>>(steps + 1)
            for (step in 0..steps) {
                val pVal = finalMin + step * deltaPrice
                pTicks.add(pVal to priceFormatter.format(pVal))
            }

            // Precompute 5 equidistant X-axis time divisions
            val xSteps = min(4, (slice.size - 1).coerceAtLeast(1))
            val tTicks = ArrayList<Pair<Int, String>>(xSteps + 1)
            for (step in 0..xSteps) {
                val idx = ((slice.size - 1) * (step.toFloat() / xSteps)).roundToInt().coerceIn(0, slice.lastIndex)
                val label = timeFormatter.format(Date(slice[idx].timestampMillis))
                tTicks.add(idx to label)
            }

            ComputedViewportSlice(
                visiblePoints = slice,
                visibleBenchmark = benchSlice,
                startIndex = startIndex,
                endIndex = endIndex,
                minPrice = finalMin,
                maxPrice = finalMax,
                maxVolume = maxVol,
                priceTicks = pTicks,
                timeTicks = tTicks,
                benchmarkNormalizedPrices = normalizedBench,
                latestAlphaSpread = alphaSpread
            )
        }
        viewportSlice = computed
    }

    // Derived Ruler Measurement Calculation
    val rulerMeasurement by remember(points, rulerStartGlobalIndex, rulerEndGlobalIndex) {
        derivedStateOf {
            if (!isRulerModeActive || rulerStartGlobalIndex !in points.indices || rulerEndGlobalIndex !in points.indices) {
                null
            } else {
                val idxA = min(rulerStartGlobalIndex, rulerEndGlobalIndex)
                val idxB = max(rulerStartGlobalIndex, rulerEndGlobalIndex)
                val ptA = points[idxA]
                val ptB = points[idxB]
                val spread = ptB.price - ptA.price
                val pctReturn = if (ptA.price != 0f) (spread / ptA.price) * 100f else 0f
                val bars = abs(idxB - idxA)
                val millisSpan = abs(ptB.timestampMillis - ptA.timestampMillis)
                val days = (millisSpan / (1000L * 60 * 60 * 24)).toInt()
                val cagr = if (days > 365 && ptA.price > 0f && ptB.price > 0f) {
                    val years = days / 365.25
                    (((ptB.price.toDouble() / ptA.price.toDouble()).pow(1.0 / years) - 1.0) * 100.0).toFloat()
                } else {
                    null
                }
                RulerMeasurement(
                    startIndex = idxA,
                    endIndex = idxB,
                    pointA = ptA,
                    pointB = ptB,
                    priceSpread = spread,
                    percentageReturn = pctReturn,
                    barsElapsed = bars,
                    daysElapsed = days,
                    annualizedCagr = cagr
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalPalette.SurfaceWhite, RoundedCornerShape(12.dp))
            .border(1.dp, TerminalPalette.SlateBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // ====================================================================
        // CHART CONTROL HEADER (Ruler Mode, Benchmark Toggle, Volume, Alpha)
        // ====================================================================
        ChartControlHeader(
            isRulerActive = isRulerModeActive,
            onToggleRuler = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isRulerModeActive = !isRulerModeActive
                if (!isRulerModeActive) {
                    rulerStartGlobalIndex = -1
                    rulerEndGlobalIndex = -1
                } else {
                    isScrubbing = false
                    scrubbedLocalIndex = -1
                }
            },
            hasBenchmarkData = benchmarkPoints.isNotEmpty(),
            isBenchmarkActive = showBenchmarkOverlay,
            onToggleBenchmark = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showBenchmarkOverlay = !showBenchmarkOverlay
            },
            isVolumeActive = showVolumeSubChart,
            onToggleVolume = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showVolumeSubChart = !showVolumeSubChart
            },
            zoomScale = zoomScale,
            onResetZoom = {
                zoomScale = 1.0f
                scrollOffsetFraction = 1.0f
            },
            alphaSpread = viewportSlice?.latestAlphaSpread
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ====================================================================
        // FLOATING RULER TELEMETRY BANNER
        // ====================================================================
        AnimatedVisibility(
            visible = rulerMeasurement != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            rulerMeasurement?.let { m ->
                RulerMeasurementCard(
                    measurement = m,
                    signedPriceFormatter = signedPriceFormatter
                )
            }
        }

        // ====================================================================
        // CARTESIAN TECHNICAL CHART CANVAS
        // ====================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalPalette.BackgroundIceMist)
        ) {
            val currentSlice = viewportSlice
            if (currentSlice == null || currentSlice.visiblePoints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Awaiting Institutional Market Feed...",
                        color = TerminalPalette.TextSecondarySlate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                CartesianPlotCanvas(
                    slice = currentSlice,
                    totalPointsCount = points.size,
                    dayChange = dayChange,
                    accentColor = accentColor,
                    showVolume = showVolumeSubChart,
                    showBenchmark = showBenchmarkOverlay,
                    isRulerModeActive = isRulerModeActive,
                    rulerMeasurement = rulerMeasurement,
                    zoomScale = zoomScale,
                    scrollOffsetFraction = scrollOffsetFraction,
                    scrubbedLocalIndex = scrubbedLocalIndex,
                    isScrubbing = isScrubbing,
                    priceFormatter = priceFormatter,
                    fullDateFormatter = fullDateFormatter,
                    onUpdateViewport = { newZoom, newOffset ->
                        zoomScale = newZoom
                        scrollOffsetFraction = newOffset
                    },
                    onUpdateScrub = { active, localIdx ->
                        if (active && !isScrubbing) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        isScrubbing = active
                        if (active) {
                            scrubbedLocalIndex = localIdx
                            val pt = currentSlice.visiblePoints.getOrNull(localIdx)
                            onPointSelected?.invoke(pt)
                        } else {
                            lastScrubReleaseTime = System.currentTimeMillis()
                        }
                    },
                    onUpdateRulerSelection = { globalStart, globalEnd ->
                        if (rulerStartGlobalIndex != globalStart || rulerEndGlobalIndex != globalEnd) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        rulerStartGlobalIndex = globalStart
                        rulerEndGlobalIndex = globalEnd
                    }
                )
            }
        }
    }
}

// ================================================================================
// HEADER & RULER TELEMETRY COMPOSABLES
// ================================================================================

@Composable
private fun ChartControlHeader(
    isRulerActive: Boolean,
    onToggleRuler: () -> Unit,
    hasBenchmarkData: Boolean,
    isBenchmarkActive: Boolean,
    onToggleBenchmark: () -> Unit,
    isVolumeActive: Boolean,
    onToggleVolume: () -> Unit,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    alphaSpread: Float?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Two-Point Ruler Toggle
            TerminalToggleChip(
                label = if (isRulerActive) "Ruler: Active" else "Ruler",
                isActive = isRulerActive,
                activeColor = TerminalPalette.RulerPurple,
                onClick = onToggleRuler
            )

            // NIFTY 50 Benchmark Overlay Toggle
            if (hasBenchmarkData) {
                TerminalToggleChip(
                    label = "vs NIFTY 50",
                    isActive = isBenchmarkActive,
                    activeColor = TerminalPalette.BenchmarkAmberDeep,
                    onClick = onToggleBenchmark
                )
            }

            // Volume Histogram Toggle
            TerminalToggleChip(
                label = "Vol",
                isActive = isVolumeActive,
                activeColor = TerminalPalette.TextPrimarySlate,
                onClick = onToggleVolume
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real-Time Benchmark Alpha Spread Readout
            if (isBenchmarkActive && alphaSpread != null) {
                val isPositiveAlpha = alphaSpread >= 0f
                val sign = if (isPositiveAlpha) "+" else ""
                Surface(
                    color = if (isPositiveAlpha) TerminalPalette.BullishSoftTint else TerminalPalette.BearishSoftTint,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "α $sign${String.format(Locale.US, "%.2f", alphaSpread)}%",
                        color = if (isPositiveAlpha) TerminalPalette.BullishEmerald else TerminalPalette.BearishCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Zoom Indicator Pill
            if (zoomScale > 1.05f) {
                Surface(
                    color = TerminalPalette.BackgroundIceMist,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, TerminalPalette.SlateBorder, RoundedCornerShape(6.dp))
                        .clickable { onResetZoom() }
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", zoomScale)}x ↺",
                        color = TerminalPalette.TextSecondarySlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalToggleChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.12f) else TerminalPalette.BackgroundIceMist
    val borderColor = if (isActive) activeColor else TerminalPalette.SlateBorder
    val textColor = if (isActive) activeColor else TerminalPalette.TextSecondarySlate

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(activeColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RulerMeasurementCard(
    measurement: RulerMeasurement,
    signedPriceFormatter: DecimalFormat
) {
    val isPositive = measurement.priceSpread >= 0f
    val returnSign = if (isPositive) "+" else ""
    val badgeColor = if (isPositive) TerminalPalette.BullishEmerald else TerminalPalette.BearishCrimson

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = TerminalPalette.RulerPurpleSoft,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalPalette.RulerPurple.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TWO-POINT RULER SPREAD",
                    color = TerminalPalette.RulerPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = signedPriceFormatter.format(measurement.priceSpread),
                        color = TerminalPalette.TextPrimarySlate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($returnSign${String.format(Locale.US, "%.2f", measurement.percentageReturn)}%)",
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${measurement.barsElapsed} Bars • ${measurement.daysElapsed}d Span",
                    color = TerminalPalette.TextSecondarySlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                measurement.annualizedCagr?.let { cagr ->
                    val cagrSign = if (cagr >= 0f) "+" else ""
                    Text(
                        text = "CAGR: $cagrSign${String.format(Locale.US, "%.2f", cagr)}%",
                        color = TerminalPalette.RulerPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ================================================================================
// ZERO-ALLOCATION CANVAS & UNIFIED GESTURE PIPELINE
// ================================================================================

@Composable
private fun CartesianPlotCanvas(
    slice: ComputedViewportSlice,
    totalPointsCount: Int,
    dayChange: Float,
    accentColor: Color,
    showVolume: Boolean,
    showBenchmark: Boolean,
    isRulerModeActive: Boolean,
    rulerMeasurement: RulerMeasurement?,
    zoomScale: Float,
    scrollOffsetFraction: Float,
    scrubbedLocalIndex: Int,
    isScrubbing: Boolean,
    priceFormatter: DecimalFormat,
    fullDateFormatter: SimpleDateFormat,
    onUpdateViewport: (Float, Float) -> Unit,
    onUpdateScrub: (Boolean, Int) -> Unit,
    onUpdateRulerSelection: (Int, Int) -> Unit
) {
    val density = LocalDensity.current

    // Viewport Dimension Specs (52dp Right Axis, 24dp Bottom Axis, 12dp Top Headroom)
    val rightMarginPx = remember(density) { with(density) { 52.dp.toPx() } }
    val bottomMarginPx = remember(density) { with(density) { 24.dp.toPx() } }
    val topMarginPx = remember(density) { with(density) { 12.dp.toPx() } }
    val pillCornerRadiusPx = remember(density) { with(density) { 4.dp.toPx() } }

    // Pre-allocated Graphics Objects (Zero allocations inside Canvas draw loop)
    val priceLinePath = remember { Path() }
    val priceFillPath = remember { Path() }
    val benchmarkPath = remember { Path() }

    val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }
    val benchmarkDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f) }
    val crosshairDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) }

    val priceStroke = remember(density) {
        Stroke(
            width = with(density) { 2.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val benchmarkStroke = remember(density) {
        Stroke(
            width = with(density) { 1.75.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = benchmarkDashEffect
        )
    }

    val axisTextPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TerminalPalette.TextSecondarySlate.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }

    val badgeTextPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val badgeBgPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    val sharedBadgeRect = remember { RectF() }

    // Keep state references fresh inside the single unified pointerInput block
    val currentSliceState by rememberUpdatedState(slice)
    val currentZoomState by rememberUpdatedState(zoomScale)
    val currentScrollState by rememberUpdatedState(scrollOffsetFraction)
    val currentRulerModeState by rememberUpdatedState(isRulerModeActive)
    val currentUpdateViewport by rememberUpdatedState(onUpdateViewport)
    val currentUpdateScrub by rememberUpdatedState(onUpdateScrub)
    val currentUpdateRuler by rememberUpdatedState(onUpdateRulerSelection)

    var lastTapUpTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(totalPointsCount) {
                val touchSlop = viewConfiguration.touchSlop
                val doubleTapTimeoutMs = 300L
                val longPressTimeoutMs = 150L

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val plotW = (size.width - rightMarginPx).coerceAtLeast(1f)

                    // Ignore touches originating solely inside the right-hand price scale
                    if (down.position.x > plotW) return@awaitEachGesture

                    // 1. Check for Double-Tap Reset (< 300ms)
                    if (downTime - lastTapUpTimestamp <= doubleTapTimeoutMs &&
                        (down.position - lastTapPosition).getDistance() < touchSlop * 2f
                    ) {
                        currentUpdateViewport(1.0f, 0.0f)
                        currentUpdateScrub(false, -1)
                        lastTapUpTimestamp = 0L
                        return@awaitEachGesture
                    }

                    // 2. If Ruler Mode is active, map 1-finger drag or 2-finger pinch directly to Point A & Point B
                    if (currentRulerModeState) {
                        val activeSlice = currentSliceState
                        val count = activeSlice.visiblePoints.size
                        if (count < 2) return@awaitEachGesture

                        fun xToGlobalIndex(x: Float): Int {
                            val fraction = (x / plotW).coerceIn(0f, 1f)
                            val localIdx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                            return activeSlice.startIndex + localIdx
                        }

                        val anchorGlobalIdx = xToGlobalIndex(down.position.x)
                        currentUpdateRuler(anchorGlobalIdx, anchorGlobalIdx)

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.size >= 2) {
                                val idxA = xToGlobalIndex(activePointers[0].position.x)
                                val idxB = xToGlobalIndex(activePointers[1].position.x)
                                currentUpdateRuler(idxA, idxB)
                            } else if (activePointers.size == 1) {
                                val idxB = xToGlobalIndex(activePointers[0].position.x)
                                currentUpdateRuler(anchorGlobalIdx, idxB)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } while (event.changes.any { it.pressed })

                        return@awaitEachGesture
                    }

                    // 3. Standard Unified Gesture Pipeline: Pinch-to-Zoom, Pan, and >150ms Scrubbing
                    var gestureMode = GestureMode.UNDECIDED
                    var initialTouchPos = down.position

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        val pointerCount = pressedChanges.size

                        if (pointerCount >= 2) {
                            // Transition immediately to 2-Finger Pinch-to-Zoom
                            if (gestureMode == GestureMode.SCRUBBING) {
                                currentUpdateScrub(false, -1)
                            }
                            gestureMode = GestureMode.ZOOM_AND_PAN

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            if (zoomChange != 1.0f || panChange != Offset.Zero) {
                                val oldZoom = currentZoomState
                                val newZoom = (oldZoom * zoomChange).coerceIn(1.0f, 15.0f)

                                val oldVisibleCount = totalPointsCount / oldZoom
                                val newVisibleCount = totalPointsCount / newZoom

                                val oldMaxStart = (totalPointsCount - oldVisibleCount).coerceAtLeast(0f)
                                val newMaxStart = (totalPointsCount - newVisibleCount).coerceAtLeast(0f)

                                val focalFraction = (centroid.x / plotW).coerceIn(0f, 1f)
                                val oldStartIdx = currentScrollState * oldMaxStart
                                // Anchor focal point in data-space
                                val focalDataIndex = oldStartIdx + focalFraction * oldVisibleCount
                                val panDataDelta = (panChange.x / plotW) * newVisibleCount

                                val newStartIdx = (focalDataIndex - focalFraction * newVisibleCount - panDataDelta)
                                    .coerceIn(0f, newMaxStart)

                                val newScrollFraction = if (newMaxStart > 0f) {
                                    (newStartIdx / newMaxStart).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }

                                currentUpdateViewport(newZoom, newScrollFraction)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pointerCount == 1) {
                            val change: PointerInputChange = pressedChanges.first()
                            val elapsedMs = System.currentTimeMillis() - downTime
                            val distanceMoved = (change.position - initialTouchPos).getDistance()

                            when (gestureMode) {
                                GestureMode.UNDECIDED -> {
                                    if (distanceMoved > touchSlop) {
                                        if (currentZoomState > 1.05f) {
                                            gestureMode = GestureMode.HORIZONTAL_PAN
                                        } else {
                                            gestureMode = GestureMode.SCRUBBING
                                        }
                                    } else if (elapsedMs >= longPressTimeoutMs) {
                                        gestureMode = GestureMode.SCRUBBING
                                        val count = currentSliceState.visiblePoints.size
                                        val idx = ((change.position.x / plotW).coerceIn(0f, 1f) * (count - 1))
                                            .roundToInt()
                                            .coerceIn(0, (count - 1).coerceAtLeast(0))
                                        currentUpdateScrub(true, idx)
                                    }
                                }

                                GestureMode.HORIZONTAL_PAN -> {
                                    val dx = change.position.x - change.previousPosition.x
                                    val visibleCount = totalPointsCount / currentZoomState
                                    val maxStart = (totalPointsCount - visibleCount).coerceAtLeast(1f)
                                    val deltaStart = -(dx / plotW) * visibleCount
                                    val newScroll = (currentScrollState + (deltaStart / maxStart)).coerceIn(0f, 1f)
                                    currentUpdateViewport(currentZoomState, newScroll)
                                    change.consume()
                                }

                                GestureMode.SCRUBBING -> {
                                    val count = currentSliceState.visiblePoints.size
                                    val idx = ((change.position.x / plotW).coerceIn(0f, 1f) * (count - 1))
                                        .roundToInt()
                                        .coerceIn(0, (count - 1).coerceAtLeast(0))
                                    currentUpdateScrub(true, idx)
                                    change.consume()
                                }

                                GestureMode.ZOOM_AND_PAN -> {
                                    // Avoid sudden jumps when lifting one finger after a 2-finger pinch
                                    initialTouchPos = change.position
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Finger lift cleanup
                    if (gestureMode == GestureMode.SCRUBBING) {
                        currentUpdateScrub(false, -1)
                    } else if (gestureMode == GestureMode.UNDECIDED) {
                        lastTapUpTimestamp = System.currentTimeMillis()
                        lastTapPosition = down.position
                    }
                }
            }
    ) {
        val plotWidth = (size.width - rightMarginPx).coerceAtLeast(1f)
        val plotHeight = (size.height - bottomMarginPx - topMarginPx).coerceAtLeast(1f)
        val plotBottomY = topMarginPx + plotHeight

        val visiblePoints = slice.visiblePoints
        val pointCount = visiblePoints.size
        if (pointCount < 2) return@Canvas

        val priceRange = (slice.maxPrice - slice.minPrice).coerceAtLeast(0.0001f)

        fun indexToX(idx: Int): Float {
            return (idx.toFloat() / (pointCount - 1).coerceAtLeast(1)) * plotWidth
        }

        fun priceToY(price: Float): Float {
            val normalized = (price - slice.minPrice) / priceRange
            return plotBottomY - (normalized * plotHeight)
        }

        // ====================================================================
        // 1. CARTESIAN GRIDLINES & DYNAMIC AXES (Right 52dp, Bottom 24dp)
        // ====================================================================

        // Axis Borders
        drawLine(
            color = TerminalPalette.SlateBorder,
            start = Offset(plotWidth, 0f),
            end = Offset(plotWidth, plotBottomY),
            strokeWidth = 1f
        )
        drawLine(
            color = TerminalPalette.SlateBorder,
            start = Offset(0f, plotBottomY),
            end = Offset(plotWidth, plotBottomY),
            strokeWidth = 1f
        )

        // Horizontal Price Divisions + Right-Aligned Tick Labels
        axisTextPaint.textAlign = Paint.Align.RIGHT
        val rightLabelX = size.width - 6f

        for (i in slice.priceTicks.indices) {
            val (tickPrice, tickLabel) = slice.priceTicks[i]
            val y = priceToY(tickPrice)
            if (y in topMarginPx..plotBottomY) {
                drawLine(
                    color = TerminalPalette.SlateBorder,
                    start = Offset(0f, y),
                    end = Offset(plotWidth, y),
                    strokeWidth = 1f,
                    pathEffect = gridDashEffect
                )
                drawContext.canvas.nativeCanvas.drawText(
                    tickLabel,
                    rightLabelX,
                    y + (axisTextPaint.textSize * 0.35f),
                    axisTextPaint
                )
            }
        }

        // Vertical Time Gridlines + Bottom X-Axis Timestamp Labels
        axisTextPaint.textAlign = Paint.Align.CENTER
        val bottomLabelY = size.height - 6f

        for (i in slice.timeTicks.indices) {
            val (tickIdx, timeLabel) = slice.timeTicks[i]
            val x = indexToX(tickIdx)
            drawLine(
                color = TerminalPalette.SlateBorder,
                start = Offset(x, topMarginPx),
                end = Offset(x, plotBottomY),
                strokeWidth = 1f,
                pathEffect = gridDashEffect
            )
            val clampedX = x.coerceIn(24f, plotWidth - 24f)
            drawContext.canvas.nativeCanvas.drawText(
                timeLabel,
                clampedX,
                bottomLabelY,
                axisTextPaint
            )
        }

        // ====================================================================
        // 2. VOLUME HISTOGRAM SUB-CHART (Bottom 20% of plotHeight)
        // ====================================================================
        if (showVolume && slice.maxVolume > 0L) {
            val volumeMaxHeight = plotHeight * 0.20f
            val rawBarSpacing = plotWidth / pointCount.toFloat()
            val barWidth = (rawBarSpacing * 0.65f).coerceIn(1.5f, 18f)

            for (i in 0 until pointCount) {
                val pt = visiblePoints[i]
                val prevPrice = if (i > 0) visiblePoints[i - 1].price else pt.open
                val isBullishBar = pt.price >= prevPrice
                val barColor = if (isBullishBar) {
                    TerminalPalette.BullishEmerald.copy(alpha = 0.28f)
                } else {
                    TerminalPalette.BearishCrimson.copy(alpha = 0.28f)
                }

                val volRatio = (pt.volume.toFloat() / slice.maxVolume.toFloat()).coerceIn(0f, 1f)
                val barHeight = volRatio * volumeMaxHeight
                val centerX = indexToX(i)
                val leftX = (centerX - barWidth / 2f).coerceAtLeast(0f)

                drawRect(
                    color = barColor,
                    topLeft = Offset(leftX, plotBottomY - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        // ====================================================================
        // 3. SECONDARY BENCHMARK OVERLAY (NIFTY 50 ^NSEI - Amber Gold Dashed)
        // ====================================================================
        if (showBenchmark && slice.benchmarkNormalizedPrices.size == pointCount) {
            benchmarkPath.reset()
            for (i in 0 until pointCount) {
                val x = indexToX(i)
                val y = priceToY(slice.benchmarkNormalizedPrices[i])
                if (i == 0) {
                    benchmarkPath.moveTo(x, y)
                } else {
                    benchmarkPath.lineTo(x, y)
                }
            }
            drawPath(
                path = benchmarkPath,
                color = TerminalPalette.BenchmarkAmberGold,
                style = benchmarkStroke
            )
        }

        // ====================================================================
        // 4. PRIMARY ASSET CURVE & GRADIENT UNDERFILL
        // ====================================================================
        val isPositiveTrend = if (dayChange != 0f) {
            dayChange >= 0f
        } else {
            visiblePoints.last().price >= visiblePoints.first().price
        }
        val primaryCurveColor = if (isPositiveTrend) {
            TerminalPalette.BullishEmerald
        } else {
            TerminalPalette.BearishCrimson
        }

        priceLinePath.reset()
        priceFillPath.reset()

        for (i in 0 until pointCount) {
            val x = indexToX(i)
            val y = priceToY(visiblePoints[i].price)
            if (i == 0) {
                priceLinePath.moveTo(x, y)
                priceFillPath.moveTo(x, plotBottomY)
                priceFillPath.lineTo(x, y)
            } else {
                priceLinePath.lineTo(x, y)
                priceFillPath.lineTo(x, y)
            }
        }
        priceFillPath.lineTo(indexToX(pointCount - 1), plotBottomY)
        priceFillPath.close()

        drawPath(
            path = priceFillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryCurveColor.copy(alpha = 0.20f),
                    primaryCurveColor.copy(alpha = 0.0f)
                ),
                startY = topMarginPx,
                endY = plotBottomY
            )
        )

        drawPath(
            path = priceLinePath,
            color = primaryCurveColor,
            style = priceStroke
        )

        // ====================================================================
        // 5. LIVE PRICE PILL AT RIGHT AXIS EDGE
        // ====================================================================
        val latestQuote = visiblePoints.last().price
        val latestY = priceToY(latestQuote).coerceIn(topMarginPx + 10f, plotBottomY - 10f)
        val liveLabel = priceFormatter.format(latestQuote)

        // Subtle horizontal tracker line to current quote
        drawLine(
            color = primaryCurveColor.copy(alpha = 0.45f),
            start = Offset(0f, latestY),
            end = Offset(plotWidth, latestY),
            strokeWidth = 1f,
            pathEffect = gridDashEffect
        )

        val pillHeight = 18f
        badgeBgPaint.color = primaryCurveColor.toArgb()
        sharedBadgeRect.set(
            plotWidth + 2f,
            latestY - pillHeight / 2f,
            size.width - 2f,
            latestY + pillHeight / 2f
        )
        drawContext.canvas.nativeCanvas.drawRoundRect(
            sharedBadgeRect,
            pillCornerRadiusPx,
            pillCornerRadiusPx,
            badgeBgPaint
        )
        badgeTextPaint.textAlign = Paint.Align.CENTER
        drawContext.canvas.nativeCanvas.drawText(
            liveLabel,
            plotWidth + (rightMarginPx / 2f),
            latestY + (badgeTextPaint.textSize * 0.35f),
            badgeTextPaint
        )

        // ====================================================================
        // 6. TWO-POINT RULER SHADED BOUNDING REGION & ANCHORS
        // ====================================================================
        if (isRulerModeActive && rulerMeasurement != null) {
            val localStart = (rulerMeasurement.startIndex - slice.startIndex).coerceIn(0, pointCount - 1)
            val localEnd = (rulerMeasurement.endIndex - slice.startIndex).coerceIn(0, pointCount - 1)

            val xA = indexToX(localStart)
            val xB = indexToX(localEnd)
            val yA = priceToY(visiblePoints[localStart].price)
            val yB = priceToY(visiblePoints[localEnd].price)

            val leftX = min(xA, xB)
            val rightX = max(xA, xB)

            // Shaded bounding region (alpha = 0.12f)
            drawRect(
                color = TerminalPalette.RulerPurpleSoft,
                topLeft = Offset(leftX, topMarginPx),
                size = Size((rightX - leftX).coerceAtLeast(2f), plotHeight)
            )

            // Vertical boundary markers
            drawLine(
                color = TerminalPalette.RulerPurple,
                start = Offset(xA, topMarginPx),
                end = Offset(xA, plotBottomY),
                strokeWidth = 1.5f,
                pathEffect = crosshairDashEffect
            )
            drawLine(
                color = TerminalPalette.RulerPurple,
                start = Offset(xB, topMarginPx),
                end = Offset(xB, plotBottomY),
                strokeWidth = 1.5f,
                pathEffect = crosshairDashEffect
            )

            // Diagonal vector connecting Point A and Point B
            drawLine(
                color = TerminalPalette.RulerPurple,
                start = Offset(xA, yA),
                end = Offset(xB, yB),
                strokeWidth = 2f
            )

            // Anchor Nodes
            drawCircle(
                color = TerminalPalette.SurfaceWhite,
                radius = 6f,
                center = Offset(xA, yA)
            )
            drawCircle(
                color = TerminalPalette.RulerPurple,
                radius = 4.5f,
                center = Offset(xA, yA)
            )

            drawCircle(
                color = TerminalPalette.SurfaceWhite,
                radius = 6f,
                center = Offset(xB, yB)
            )
            drawCircle(
                color = TerminalPalette.RulerPurple,
                radius = 4.5f,
                center = Offset(xB, yB)
            )
        }

        // ====================================================================
        // 7. DUAL-AXIS CROSSHAIR SCRUBBER & FLOATING AXIS BADGES
        // ====================================================================
        if (scrubbedLocalIndex in 0 until pointCount) {
            val scrubbedPt = visiblePoints[scrubbedLocalIndex]
            val scrubX = indexToX(scrubbedLocalIndex)
            val scrubY = priceToY(scrubbedPt.price)

            // Vertical Crosshair Line
            drawLine(
                color = TerminalPalette.TextPrimarySlate.copy(alpha = 0.75f),
                start = Offset(scrubX, topMarginPx),
                end = Offset(scrubX, plotBottomY),
                strokeWidth = 1.25f,
                pathEffect = crosshairDashEffect
            )

            // Horizontal Crosshair Line across to Right Price Axis
            drawLine(
                color = TerminalPalette.TextPrimarySlate.copy(alpha = 0.75f),
                start = Offset(0f, scrubY),
                end = Offset(plotWidth, scrubY),
                strokeWidth = 1.25f,
                pathEffect = crosshairDashEffect
            )

            // Pulsing Concentric Anchor Circle at Curve Intersection
            drawCircle(
                color = accentColor.copy(alpha = if (isScrubbing) 0.28f else 0.15f),
                radius = if (isScrubbing) 12f else 9f,
                center = Offset(scrubX, scrubY)
            )
            drawCircle(
                color = TerminalPalette.SurfaceWhite,
                radius = 6f,
                center = Offset(scrubX, scrubY)
            )
            drawCircle(
                color = accentColor,
                radius = 4.5f,
                center = Offset(scrubX, scrubY)
            )

            // Floating Right Y-Axis Price Badge
            val scrubPriceText = priceFormatter.format(scrubbedPt.price)
            val yBadgeCenter = scrubY.coerceIn(topMarginPx + 10f, plotBottomY - 10f)
            badgeBgPaint.color = TerminalPalette.TextPrimarySlate.toArgb()
            sharedBadgeRect.set(
                plotWidth + 2f,
                yBadgeCenter - pillHeight / 2f,
                size.width - 2f,
                yBadgeCenter + pillHeight / 2f
            )
            drawContext.canvas.nativeCanvas.drawRoundRect(
                sharedBadgeRect,
                pillCornerRadiusPx,
                pillCornerRadiusPx,
                badgeBgPaint
            )
            badgeTextPaint.textAlign = Paint.Align.CENTER
            drawContext.canvas.nativeCanvas.drawText(
                scrubPriceText,
                plotWidth + (rightMarginPx / 2f),
                yBadgeCenter + (badgeTextPaint.textSize * 0.35f),
                badgeTextPaint
            )

            // Floating Bottom X-Axis Timestamp Badge
            val scrubDateText = fullDateFormatter.format(Date(scrubbedPt.timestampMillis))
            val textWidth = badgeTextPaint.measureText(scrubDateText)
            val halfBadgeW = (textWidth / 2f) + 8f
            val clampedBadgeCenterX = scrubX.coerceIn(halfBadgeW + 2f, plotWidth - halfBadgeW - 2f)

            drawRoundRect(
                color = TerminalPalette.TextPrimarySlate,
                topLeft = Offset(clampedBadgeCenterX - halfBadgeW, plotBottomY + 3f),
                size = Size(halfBadgeW * 2f, bottomMarginPx - 6f),
                cornerRadius = CornerRadius(pillCornerRadiusPx, pillCornerRadiusPx)
            )
            drawContext.canvas.nativeCanvas.drawText(
                scrubDateText,
                clampedBadgeCenterX,
                plotBottomY + 3f + ((bottomMarginPx - 6f) / 2f) + (badgeTextPaint.textSize * 0.35f),
                badgeTextPaint
            )
        }
    }
}

private enum class GestureMode {
    UNDECIDED,
    ZOOM_AND_PAN,
    HORIZONTAL_PAN,
    SCRUBBING
}