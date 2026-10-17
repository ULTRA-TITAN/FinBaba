package com.fin.mf.tools.terminal.ui

import com.fin.mf.tools.terminal.data.TerminalTimeframe
import com.fin.mf.tools.terminal.data.ChartDataPoint
import com.fin.mf.tools.terminal.data.TerminalAssetQuote
import java.util.Date


import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.core.theme.LocalVaultGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================
// SHARED TECHNICAL CHART MODELS & STATE DEFINITIONS
// ============================================================================

// Using TerminalTimeframe from com.fin.mf.tools.terminal.data

enum class TerminalChartStyle(val label: String) {
    AREA("AREA"),
    CANDLESTICK("CANDLES")
}

@Immutable
data class TerminalCandle(
    val timestampMillis: Long,
    val formattedTime: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long,
    val benchmarkClose: Float
)

@Immutable
data class TerminalIndicatorConfig(
    val showVolume: Boolean = true,
    val showDma50: Boolean = true,
    val showDma200: Boolean = false,
    val isRulerActive: Boolean = false,
    val showBenchmarkNifty50: Boolean = false,
    val chartStyle: TerminalChartStyle = TerminalChartStyle.AREA
)

@Immutable
data class ComputedChartGeometry(
    val candles: List<TerminalCandle> = emptyList(),
    val dma50Series: FloatArray = FloatArray(0),
    val dma200Series: FloatArray = FloatArray(0),
    val normalizedBenchmarkPrices: FloatArray = FloatArray(0),
    val minPrice: Float = 0f,
    val maxPrice: Float = 1f,
    val maxVolume: Long = 1L,
    val yAxisLabels: List<String> = emptyList(),
    val benchmarkReturnPct: Double = 0.0,
    val instrumentReturnPct: Double = 0.0
)

@Immutable
data class RulerSelectionState(
    val startIndex: Int,
    val endIndex: Int,
    val startCandle: TerminalCandle,
    val endCandle: TerminalCandle,
    val priceDelta: Double,
    val pctDelta: Double,
    val barsCount: Int
)

// ============================================================================
// BACKGROUND INDICATOR & COORDINATE MATH ENGINE (DISPATCHERS.DEFAULT)
// ============================================================================

@Stable
object ChartMathProcessor {

    private const val COLOR_DMA_50_HEX = 0xFF2563EB
    private const val COLOR_DMA_200_HEX = 0xFF0D9488

    val Dma50Color = Color(COLOR_DMA_50_HEX)
    val Dma200Color = Color(COLOR_DMA_200_HEX)

    fun computeChartGeometrySync(
        candles: List<TerminalCandle>,
        config: TerminalIndicatorConfig,
        region: EquityMarketRegion
    ): ComputedChartGeometry {
        val count = candles.size
        if (count == 0) return ComputedChartGeometry()

        val dma50 = FloatArray(count) { Float.NaN }
        val dma200 = FloatArray(count) { Float.NaN }
        val normBenchmark = FloatArray(count) { Float.NaN }

        // Rolling sums for O(N) Simple Moving Average computation
        var rollingSum50 = 0f
        var rollingSum200 = 0f
        val window50 = min(50, max(5, count / 4))
        val window200 = min(200, max(12, count / 2))

        val firstClose = candles[0].close.coerceAtLeast(0.01f)
        val firstBench = candles[0].benchmarkClose.coerceAtLeast(0.01f)

        var minP = Float.MAX_VALUE
        var maxP = -Float.MAX_VALUE
        var maxVol = 1L

        for (i in 0 until count) {
            val c = candles[i]
            val closeVal = c.close
            rollingSum50 += closeVal
            rollingSum200 += closeVal

            if (i >= window50) {
                rollingSum50 -= candles[i - window50].close
            }
            if (i >= window50 - 1) {
                dma50[i] = rollingSum50 / window50.toFloat()
            }

            if (i >= window200) {
                rollingSum200 -= candles[i - window200].close
            }
            if (i >= window200 - 1) {
                dma200[i] = rollingSum200 / window200.toFloat()
            }

            // Rebase NIFTY 50 benchmark to instrument's starting price for visual relative alpha comparison
            val benchRebased = firstClose * (c.benchmarkClose / firstBench)
            normBenchmark[i] = benchRebased

            // Track price bounds across active series
            if (c.low < minP) minP = c.low
            if (c.high > maxP) maxP = c.high

            if (config.showDma50 && !dma50[i].isNaN()) {
                if (dma50[i] < minP) minP = dma50[i]
                if (dma50[i] > maxP) maxP = dma50[i]
            }
            if (config.showDma200 && !dma200[i].isNaN()) {
                if (dma200[i] < minP) minP = dma200[i]
                if (dma200[i] > maxP) maxP = dma200[i]
            }
            if (config.showBenchmarkNifty50 && !normBenchmark[i].isNaN()) {
                if (normBenchmark[i] < minP) minP = normBenchmark[i]
                if (normBenchmark[i] > maxP) maxP = normBenchmark[i]
            }

            if (c.volume > maxVol) {
                maxVol = c.volume
            }
        }

        if (minP == Float.MAX_VALUE || maxP == -Float.MAX_VALUE) {
            minP = 0f
            maxP = 100f
        }

        val rawSpan = (maxP - minP).coerceAtLeast(0.5f)
        val paddedMin = (minP - rawSpan * 0.06f).coerceAtLeast(0f)
        val paddedMax = maxP + rawSpan * 0.06f
        val finalSpan = (paddedMax - paddedMin).coerceAtLeast(1f)

        // Pre-format 5 horizontal grid price labels off the UI thread
        val labels = ArrayList<String>(5)
        for (step in 0..4) {
            val fraction = 1f - (step / 4f)
            val levelPrice = (paddedMin + finalSpan * fraction).toDouble()
            labels.add(InstitutionalFormatters.formatPrice(levelPrice, region))
        }

        val lastCandle = candles[count - 1]
        val instRetPct = ((lastCandle.close - firstClose) / firstClose) * 100.0
        val benchRetPct = ((lastCandle.benchmarkClose - firstBench) / firstBench) * 100.0

        return ComputedChartGeometry(
            candles = candles,
            dma50Series = dma50,
            dma200Series = dma200,
            normalizedBenchmarkPrices = normBenchmark,
            minPrice = paddedMin,
            maxPrice = paddedMax,
            maxVolume = maxVol,
            yAxisLabels = labels,
            benchmarkReturnPct = benchRetPct,
            instrumentReturnPct = instRetPct
        )
    }
}

// ============================================================================
// PART C: FULL-SCREEN PRO ANALYSIS TERMINAL DIALOG
// ============================================================================

/**
 * Convenience overload accepting [TerminalAssetQuote].
 */
@Composable
fun TerminalFullscreenDialog(
    quote: TerminalAssetQuote,
    selectedTimeframe: TerminalTimeframe,
    onTimeframeSelected: (TerminalTimeframe) -> Unit,
    benchmarkPoints: List<ChartDataPoint> = emptyList(),
    indicatorConfig: TerminalIndicatorConfig = TerminalIndicatorConfig(),
    onIndicatorConfigChange: (TerminalIndicatorConfig) -> Unit = {},
    onDismiss: () -> Unit
) {
    val region = remember(quote.symbol, quote.currency) {
        EquityMarketRegion.fromSymbolOrCurrency(quote.symbol, quote.currency)
    }

    val candles = remember(quote.points, benchmarkPoints) {
        val count = quote.points.size
        val hasBench = benchmarkPoints.size == count
        quote.points.mapIndexed { idx, pt ->
            val benchClose = if (hasBench) benchmarkPoints[idx].close else pt.close
            val dateStr = java.text.SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(pt.epochMs))
            TerminalCandle(
                timestampMillis = pt.epochMs,
                formattedTime = dateStr,
                open = pt.open,
                high = pt.high,
                low = pt.low,
                close = pt.close,
                volume = pt.volume,
                benchmarkClose = benchClose
            )
        }
    }

    TerminalFullscreenDialog(
        symbol = quote.symbol,
        companyName = quote.companyName,
        region = region,
        currentLtp = quote.regularMarketPrice,
        previousClose = quote.previousClose,
        selectedTimeframe = selectedTimeframe,
        onTimeframeSelected = onTimeframeSelected,
        indicatorConfig = indicatorConfig,
        onIndicatorConfigChange = onIndicatorConfigChange,
        candles = candles,
        onDismiss = onDismiss
    )
}

@Composable
fun TerminalFullscreenDialog(
    symbol: String,
    companyName: String,
    region: EquityMarketRegion,
    currentLtp: Double,
    previousClose: Double,
    selectedTimeframe: TerminalTimeframe,
    onTimeframeSelected: (TerminalTimeframe) -> Unit,
    indicatorConfig: TerminalIndicatorConfig,
    onIndicatorConfigChange: (TerminalIndicatorConfig) -> Unit,
    candles: List<TerminalCandle>,
    onDismiss: () -> Unit
) {
    val accentColor = LocalVaultGradient.current.startColor

    // Offload all indicator & coordinate math to Dispatchers.Default
    val geometry by produceState(
        initialValue = remember(candles, indicatorConfig, region) {
            ChartMathProcessor.computeChartGeometrySync(candles, indicatorConfig, region)
        },
        key1 = candles,
        key2 = indicatorConfig,
        key3 = region
    ) {
        value = withContext(Dispatchers.Default) {
            ChartMathProcessor.computeChartGeometrySync(candles, indicatorConfig, region)
        }
    }

    // Gesture-driven live inspection states
    var scrubbedIndex by remember(candles) { mutableIntStateOf(-1) }
    var rulerStartIdx by remember(candles, indicatorConfig.isRulerActive) {
        mutableIntStateOf(if (indicatorConfig.isRulerActive && candles.size >= 4) candles.size / 4 else -1)
    }
    var rulerEndIdx by remember(candles, indicatorConfig.isRulerActive) {
        mutableIntStateOf(if (indicatorConfig.isRulerActive && candles.size >= 4) (candles.size * 3) / 4 else -1)
    }

    val activeCandle: TerminalCandle? = remember(candles, scrubbedIndex) {
        if (scrubbedIndex in candles.indices) candles[scrubbedIndex] else candles.lastOrNull()
    }

    val displayLtp = activeCandle?.close?.toDouble() ?: currentLtp
    val baselineClose = if (selectedTimeframe == TerminalTimeframe.ONE_DAY && previousClose > 0.0) {
        previousClose
    } else {
        candles.firstOrNull()?.open?.toDouble() ?: previousClose
    }
    val absChange = displayLtp - baselineClose
    val pctChange = if (baselineClose > 0.0) (absChange / baselineClose) * 100.0 else 0.0
    val isBullish = absChange >= 0.0

    val activeRulerState: RulerSelectionState? = remember(candles, rulerStartIdx, rulerEndIdx, indicatorConfig.isRulerActive) {
        if (indicatorConfig.isRulerActive &&
            rulerStartIdx in candles.indices &&
            rulerEndIdx in candles.indices
        ) {
            val sIdx = min(rulerStartIdx, rulerEndIdx)
            val eIdx = max(rulerStartIdx, rulerEndIdx)
            val sCandle = candles[sIdx]
            val eCandle = candles[eIdx]
            val delta = (eCandle.close - sCandle.close).toDouble()
            val pct = if (sCandle.close > 0f) (delta / sCandle.close.toDouble()) * 100.0 else 0.0
            RulerSelectionState(
                startIndex = sIdx,
                endIndex = eIdx,
                startCandle = sCandle,
                endCandle = eCandle,
                priceDelta = delta,
                pctDelta = pct,
                barsCount = (eIdx - sIdx).coerceAtLeast(1)
            )
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TerminalThemeTokens.BackgroundIceMist
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // 1. FLOATING HUD HEADER
                FullscreenFloatingHudHeader(
                    symbol = symbol,
                    companyName = companyName,
                    region = region,
                    displayLtp = displayLtp,
                    absChange = absChange,
                    pctChange = pctChange,
                    isBullish = isBullish,
                    activeCandle = activeCandle,
                    activeCandleIndex = if (scrubbedIndex in candles.indices) scrubbedIndex else candles.lastIndex,
                    geometry = geometry,
                    rulerState = activeRulerState,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeSelected = onTimeframeSelected,
                    indicatorConfig = indicatorConfig,
                    onIndicatorConfigChange = onIndicatorConfigChange,
                    accentColor = accentColor,
                    onExitFullscreen = onDismiss
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. EXPANDED FULL-BLEED CARTESIAN CHART CANVAS
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = TerminalThemeTokens.SurfaceWhite,
                    border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate)
                ) {
                    InstitutionalCartesianChartCanvas(
                        geometry = geometry,
                        indicatorConfig = indicatorConfig,
                        region = region,
                        isBullishOverall = isBullish,
                        accentColor = accentColor,
                        scrubbedIndex = scrubbedIndex,
                        onScrubIndexChanged = { scrubbedIndex = it },
                        rulerStartIndex = rulerStartIdx,
                        rulerEndIndex = rulerEndIdx,
                        onRulerIndicesChanged = { start, end ->
                            rulerStartIdx = start
                            rulerEndIdx = end
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// FLOATING HUD HEADER (IDENTITY, LTP, TIMEFRAMES, INDICATORS & EXIT)
// ============================================================================

@Composable
private fun FullscreenFloatingHudHeader(
    symbol: String,
    companyName: String,
    region: EquityMarketRegion,
    displayLtp: Double,
    absChange: Double,
    pctChange: Double,
    isBullish: Boolean,
    activeCandle: TerminalCandle?,
    activeCandleIndex: Int,
    geometry: ComputedChartGeometry,
    rulerState: RulerSelectionState?,
    selectedTimeframe: TerminalTimeframe,
    onTimeframeSelected: (TerminalTimeframe) -> Unit,
    indicatorConfig: TerminalIndicatorConfig,
    onIndicatorConfigChange: (TerminalIndicatorConfig) -> Unit,
    accentColor: Color,
    onExitFullscreen: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TerminalThemeTokens.SurfaceWhite,
        border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Active Symbol, Company Name, Live LTP, Absolute & % Change, Exit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Symbol & Company Name
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalThemeTokens.TextPrimarySlate
                            )
                            Surface(
                                color = TerminalThemeTokens.BackgroundIceMist,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate)
                            ) {
                                Text(
                                    text = "PRO TERMINAL",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = companyName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TerminalThemeTokens.TextSecondarySlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Center-Right: Current LTP, Absolute Change & Percentage Change Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = InstitutionalFormatters.formatPrice(displayLtp, region),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalThemeTokens.TextPrimarySlate
                        )
                        val sign = if (isBullish) "+" else "-"
                        Text(
                            text = "$sign${InstitutionalFormatters.formatPrice(abs(absChange), region)} (${
                                InstitutionalFormatters.formatPercentage(pctChange, includePlusSign = true)
                            })",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBullish) {
                                TerminalThemeTokens.BullishEmerald
                            } else {
                                TerminalThemeTokens.BearishCrimson
                            }
                        )
                    }

                    // Chart Style Toggle (Area / Candlestick)
                    Surface(
                        color = TerminalThemeTokens.BackgroundIceMist,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate),
                        modifier = Modifier.clickable {
                            val nextStyle = if (indicatorConfig.chartStyle == TerminalChartStyle.AREA) {
                                TerminalChartStyle.CANDLESTICK
                            } else {
                                TerminalChartStyle.AREA
                            }
                            onIndicatorConfigChange(indicatorConfig.copy(chartStyle = nextStyle))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (indicatorConfig.chartStyle == TerminalChartStyle.AREA) {
                                    Icons.Default.CandlestickChart
                                } else {
                                    Icons.AutoMirrored.Filled.ShowChart
                                },
                                contentDescription = "Switch Chart Style",
                                tint = TerminalThemeTokens.TextPrimarySlate,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = indicatorConfig.chartStyle.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalThemeTokens.TextPrimarySlate
                            )
                        }
                    }

                    // Exit Fullscreen Button (Icons.Default.FullscreenExit)
                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalThemeTokens.BackgroundIceMist)
                            .border(
                                width = 1.dp,
                                color = TerminalThemeTokens.BorderSubtleSlate,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen Terminal",
                            tint = TerminalThemeTokens.TextPrimarySlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = TerminalThemeTokens.DividerLightSlate)

            // Middle Row: Timeframe Selector [1D, 5D, 1M, 6M, 1Y, 5Y, MAX] + Indicator Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Selector Bar
                TerminalTimeframeSelectorBar(
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeSelected = onTimeframeSelected,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Indicator Toggles: [Volume, 50-DMA, 200-DMA, Ruler Tool, Benchmark NIFTY 50]
                TerminalIndicatorToggleRow(
                    config = indicatorConfig,
                    onConfigChange = onIndicatorConfigChange,
                    accentColor = accentColor
                )
            }

            // Bottom Telemetry Strip: Live OHLCV + DMA-50/200 + Benchmark / Ruler Readout
            if (rulerState != null) {
                TerminalMeasureRulerBanner(
                    startPrice = rulerState.startCandle.close.toDouble(),
                    endPrice = rulerState.endCandle.close.toDouble(),
                    startTimestampLabel = rulerState.startCandle.formattedTime,
                    endTimestampLabel = rulerState.endCandle.formattedTime,
                    barsCount = rulerState.barsCount,
                    region = region
                )
            } else if (activeCandle != null) {
                LiveOhlcvAndOverlayTelemetryStrip(
                    candle = activeCandle,
                    candleIndex = activeCandleIndex,
                    geometry = geometry,
                    indicatorConfig = indicatorConfig,
                    region = region
                )
            }
        }
    }
}

// ============================================================================
// SHARED TIMEFRAME & INDICATOR CONTROLS
// ============================================================================

@Composable
fun TerminalTimeframeSelectorBar(
    selectedTimeframe: TerminalTimeframe,
    onTimeframeSelected: (TerminalTimeframe) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(TerminalThemeTokens.BackgroundIceMist)
            .border(1.dp, TerminalThemeTokens.BorderSubtleSlate, RoundedCornerShape(9.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timeframes = remember { TerminalTimeframe.entries }
        for (i in 0 until timeframes.size) {
            val tf = timeframes[i]
            val isSelected = tf == selectedTimeframe
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onTimeframeSelected(tf) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tf.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) {
                        TerminalThemeTokens.SurfaceWhite
                    } else {
                        TerminalThemeTokens.TextSecondarySlate
                    }
                )
            }
        }
    }
}

@Composable
fun TerminalIndicatorToggleRow(
    config: TerminalIndicatorConfig,
    onConfigChange: (TerminalIndicatorConfig) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Volume Toggle
        IndicatorToggleChip(
            label = "Volume",
            isActive = config.showVolume,
            activeColor = accentColor,
            activeTint = accentColor.copy(alpha = 0.12f),
            icon = Icons.Default.BarChart,
            onClick = { onConfigChange(config.copy(showVolume = !config.showVolume)) }
        )

        // 2. 50-DMA Toggle
        IndicatorToggleChip(
            label = "50-DMA",
            isActive = config.showDma50,
            activeColor = ChartMathProcessor.Dma50Color,
            activeTint = ChartMathProcessor.Dma50Color.copy(alpha = 0.12f),
            icon = Icons.Default.Timeline,
            onClick = { onConfigChange(config.copy(showDma50 = !config.showDma50)) }
        )

        // 3. 200-DMA Toggle
        IndicatorToggleChip(
            label = "200-DMA",
            isActive = config.showDma200,
            activeColor = ChartMathProcessor.Dma200Color,
            activeTint = ChartMathProcessor.Dma200Color.copy(alpha = 0.12f),
            icon = Icons.Default.Timeline,
            onClick = { onConfigChange(config.copy(showDma200 = !config.showDma200)) }
        )

        // 4. Ruler Tool Toggle (Purple #7C3AED)
        IndicatorToggleChip(
            label = "Ruler Tool",
            isActive = config.isRulerActive,
            activeColor = TerminalThemeTokens.MeasureRulerPurple,
            activeTint = TerminalThemeTokens.MeasureRulerTint,
            icon = Icons.Default.Straighten,
            onClick = { onConfigChange(config.copy(isRulerActive = !config.isRulerActive)) }
        )

        // 5. Benchmark NIFTY 50 Toggle (Frozen Amber Gold #F59E0B / #D97706)
        IndicatorToggleChip(
            label = "Benchmark NIFTY 50",
            isActive = config.showBenchmarkNifty50,
            activeColor = TerminalThemeTokens.BenchmarkAmberDeep,
            activeTint = TerminalThemeTokens.BenchmarkAmberTint,
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            onClick = {
                onConfigChange(config.copy(showBenchmarkNifty50 = !config.showBenchmarkNifty50))
            }
        )
    }
}

@Composable
private fun IndicatorToggleChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    activeTint: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) activeTint else TerminalThemeTokens.BackgroundIceMist
    val borderColor = if (isActive) activeColor.copy(alpha = 0.55f) else TerminalThemeTokens.BorderSubtleSlate
    val textColor = if (isActive) activeColor else TerminalThemeTokens.TextSecondarySlate

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}

@Composable
private fun LiveOhlcvAndOverlayTelemetryStrip(
    candle: TerminalCandle,
    candleIndex: Int,
    geometry: ComputedChartGeometry,
    indicatorConfig: TerminalIndicatorConfig,
    region: EquityMarketRegion
) {
    val dma50Val = if (candleIndex in geometry.dma50Series.indices) {
        geometry.dma50Series[candleIndex]
    } else Float.NaN
    val dma200Val = if (candleIndex in geometry.dma200Series.indices) {
        geometry.dma200Series[candleIndex]
    } else Float.NaN

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(7.dp))
            .background(TerminalThemeTokens.BackgroundIceMist)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = candle.formattedTime,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = TerminalThemeTokens.TextPrimarySlate
        )
        TelemetryKeyValue("O", InstitutionalFormatters.formatPrice(candle.open.toDouble(), region))
        TelemetryKeyValue("H", InstitutionalFormatters.formatPrice(candle.high.toDouble(), region), TerminalThemeTokens.BullishEmerald)
        TelemetryKeyValue("L", InstitutionalFormatters.formatPrice(candle.low.toDouble(), region), TerminalThemeTokens.BearishCrimson)
        TelemetryKeyValue("C", InstitutionalFormatters.formatPrice(candle.close.toDouble(), region))
        TelemetryKeyValue("VOL", formatCompactVolume(candle.volume))

        if (indicatorConfig.showDma50 && !dma50Val.isNaN()) {
            TelemetryKeyValue(
                key = "50-DMA",
                value = InstitutionalFormatters.formatPrice(dma50Val.toDouble(), region),
                valueColor = ChartMathProcessor.Dma50Color
            )
        }
        if (indicatorConfig.showDma200 && !dma200Val.isNaN()) {
            TelemetryKeyValue(
                key = "200-DMA",
                value = InstitutionalFormatters.formatPrice(dma200Val.toDouble(), region),
                valueColor = ChartMathProcessor.Dma200Color
            )
        }
        if (indicatorConfig.showBenchmarkNifty50) {
            TelemetryKeyValue(
                key = "NIFTY 50",
                value = String.format(Locale.US, "%,.1f", candle.benchmarkClose),
                valueColor = TerminalThemeTokens.BenchmarkAmberDeep
            )
        }
    }
}

@Composable
private fun TelemetryKeyValue(
    key: String,
    value: String,
    valueColor: Color = TerminalThemeTokens.TextPrimarySlate
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "$key:",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TerminalThemeTokens.TextSecondarySlate
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

// ============================================================================
// ZERO-ALLOCATION CARTESIAN CHART CANVAS WITH UNIFIED AWAITEACHGESTURE
// ============================================================================

@Composable
fun InstitutionalCartesianChartCanvas(
    geometry: ComputedChartGeometry,
    indicatorConfig: TerminalIndicatorConfig,
    region: EquityMarketRegion,
    isBullishOverall: Boolean,
    accentColor: Color,
    scrubbedIndex: Int,
    onScrubIndexChanged: (Int) -> Unit,
    rulerStartIndex: Int,
    rulerEndIndex: Int,
    onRulerIndicesChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val candles = geometry.candles
    val count = candles.size

    // ------------------------------------------------------------------------
    // PRE-ALLOCATED CANVAS GRAPHICS OBJECTS (ZERO ALLOCATION IN DRAW LOOP)
    // ------------------------------------------------------------------------
    val priceLinePath = remember { Path() }
    val areaFillPath = remember { Path() }
    val dma50Path = remember { Path() }
    val dma200Path = remember { Path() }
    val benchmarkPath = remember { Path() }

    val mainLineStroke = remember(density) {
        Stroke(
            width = with(density) { 2.2.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val dmaStroke = remember(density) {
        Stroke(
            width = with(density) { 1.6.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val benchmarkDashStroke = remember(density) {
        Stroke(
            width = with(density) { 1.8.dp.toPx() },
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
    val crosshairDashStroke = remember(density) {
        Stroke(
            width = with(density) { 1.2.dp.toPx() },
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
        )
    }
    val rulerDashStroke = remember(density) {
        Stroke(
            width = with(density) { 1.6.dp.toPx() },
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
        )
    }

    val yAxisLabelPaint = remember(density) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = TerminalThemeTokens.TextSecondarySlate.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            typeface = Typeface.MONOSPACE
            textAlign = NativePaint.Align.RIGHT
        }
    }

    val hudBadgeTextPaint = remember(density) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = TerminalThemeTokens.SurfaceWhite.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = NativePaint.Align.CENTER
        }
    }

    val primarySeriesColor = if (isBullishOverall) {
        TerminalThemeTokens.BullishEmerald
    } else {
        TerminalThemeTokens.BearishCrimson
    }

    val areaGradientBrush = remember(primarySeriesColor) {
        Brush.verticalGradient(
            colors = listOf(
                primarySeriesColor.copy(alpha = 0.26f),
                primarySeriesColor.copy(alpha = 0.04f),
                Color.Transparent
            )
        )
    }

    // ------------------------------------------------------------------------
    // SINGLE UNIFIED LOW-LEVEL AWAITEACHGESTURE TRACKING (60/120 FPS LOCKED)
    // ------------------------------------------------------------------------
    Canvas(
        modifier = modifier.pointerInput(count, indicatorConfig.isRulerActive) {
            if (count <= 1) return@pointerInput

            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val chartWidth = size.width.toFloat().coerceAtLeast(1f)

                fun xToCandleIndex(x: Float): Int {
                    val normalized = (x / chartWidth).coerceIn(0f, 1f)
                    return (normalized * (count - 1)).roundToInt().coerceIn(0, count - 1)
                }

                val initialIdx = xToCandleIndex(firstDown.position.x)

                if (indicatorConfig.isRulerActive) {
                    onRulerIndicesChanged(initialIdx, initialIdx)
                    onScrubIndexChanged(initialIdx)
                } else {
                    onScrubIndexChanged(initialIdx)
                }

                var isTracking = true
                while (isTracking) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val activePointers = event.changes.filter { it.pressed }

                    when {
                        activePointers.isEmpty() -> {
                            if (!indicatorConfig.isRulerActive) {
                                onScrubIndexChanged(-1)
                            }
                            isTracking = false
                        }
                        activePointers.size >= 2 -> {
                            // 2-Finger Instant Measure / Dual-Anchor Ruler Tracking
                            val idxA = xToCandleIndex(activePointers[0].position.x)
                            val idxB = xToCandleIndex(activePointers[1].position.x)
                            onRulerIndicesChanged(idxA, idxB)
                            onScrubIndexChanged(idxB)
                            activePointers[0].consume()
                            activePointers[1].consume()
                        }
                        else -> {
                            // 1-Finger High-Frequency Scrub or Ruler Drag
                            val pointer = activePointers[0]
                            val currentIdx = xToCandleIndex(pointer.position.x)
                            if (indicatorConfig.isRulerActive) {
                                onRulerIndicesChanged(initialIdx, currentIdx)
                            }
                            onScrubIndexChanged(currentIdx)
                            pointer.consume()
                        }
                    }
                }
            }
        }
    ) {
        if (count < 2) return@Canvas

        val canvasW = size.width
        val canvasH = size.height
        val priceChartBottom = if (indicatorConfig.showVolume) canvasH * 0.78f else canvasH * 0.92f
        val priceChartTop = 8.dp.toPx()
        val priceChartHeight = (priceChartBottom - priceChartTop).coerceAtLeast(1f)

        val volumeTop = canvasH * 0.82f
        val volumeBottom = canvasH * 0.96f
        val volumeHeight = (volumeBottom - volumeTop).coerceAtLeast(1f)

        val minP = geometry.minPrice
        val maxP = geometry.maxPrice
        val priceSpan = (maxP - minP).coerceAtLeast(0.0001f)
        val stepX = canvasW / (count - 1).toFloat()

        fun priceToY(price: Float): Float {
            val normalized = (price - minP) / priceSpan
            return priceChartBottom - (normalized * priceChartHeight)
        }

        // 1. HORIZONTAL & VERTICAL CARTESIAN GRIDLINES (#E2E8F0)
        val gridRows = 4
        for (r in 0..gridRows) {
            val y = priceChartTop + (priceChartHeight * r / gridRows.toFloat())
            drawLine(
                color = TerminalThemeTokens.BorderSubtleSlate,
                start = Offset(0f, y),
                end = Offset(canvasW, y),
                strokeWidth = 1.dp.toPx()
            )
            if (r < geometry.yAxisLabels.size) {
                drawContext.canvas.nativeCanvas.drawText(
                    geometry.yAxisLabels[r],
                    canvasW - 4.dp.toPx(),
                    y - 4.dp.toPx(),
                    yAxisLabelPaint
                )
            }
        }

        val gridCols = 4
        for (c in 1 until gridCols) {
            val x = canvasW * (c / gridCols.toFloat())
            drawLine(
                color = TerminalThemeTokens.BorderSubtleSlate.copy(alpha = 0.65f),
                start = Offset(x, priceChartTop),
                end = Offset(x, priceChartBottom),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. VOLUME HISTOGRAM BARS (IF ENABLED)
        if (indicatorConfig.showVolume) {
            val maxVol = geometry.maxVolume.coerceAtLeast(1L).toFloat()
            val barWidth = (stepX * 0.62f).coerceIn(1.5f, 14f)
            for (i in 0 until count) {
                val candle = candles[i]
                val x = i * stepX
                val volFraction = (candle.volume.toFloat() / maxVol).coerceIn(0.04f, 1f)
                val barH = volFraction * volumeHeight
                val isCandleBullish = candle.close >= candle.open
                val barColor = if (isCandleBullish) {
                    TerminalThemeTokens.BullishEmerald.copy(alpha = 0.32f)
                } else {
                    TerminalThemeTokens.BearishCrimson.copy(alpha = 0.32f)
                }
                drawRect(
                    color = barColor,
                    topLeft = Offset(x - barWidth / 2f, volumeBottom - barH),
                    size = Size(barWidth, barH)
                )
            }
        }

        // 3. BENCHMARK NIFTY 50 NORMALIZED OVERLAY LINE (#F59E0B / #D97706)
        if (indicatorConfig.showBenchmarkNifty50 && geometry.normalizedBenchmarkPrices.size == count) {
            benchmarkPath.reset()
            var started = false
            for (i in 0 until count) {
                val bVal = geometry.normalizedBenchmarkPrices[i]
                if (!bVal.isNaN()) {
                    val x = i * stepX
                    val y = priceToY(bVal)
                    if (!started) {
                        benchmarkPath.moveTo(x, y)
                        started = true
                    } else {
                        benchmarkPath.lineTo(x, y)
                    }
                }
            }
            if (started) {
                drawPath(
                    path = benchmarkPath,
                    color = TerminalThemeTokens.BenchmarkAmberGold,
                    style = benchmarkDashStroke
                )
            }
        }

        // 4. 200-DMA & 50-DMA OVERLAYS
        if (indicatorConfig.showDma200 && geometry.dma200Series.size == count) {
            dma200Path.reset()
            var started = false
            for (i in 0 until count) {
                val v = geometry.dma200Series[i]
                if (!v.isNaN()) {
                    val x = i * stepX
                    val y = priceToY(v)
                    if (!started) {
                        dma200Path.moveTo(x, y)
                        started = true
                    } else {
                        dma200Path.lineTo(x, y)
                    }
                }
            }
            if (started) {
                drawPath(
                    path = dma200Path,
                    color = ChartMathProcessor.Dma200Color,
                    style = dmaStroke
                )
            }
        }

        if (indicatorConfig.showDma50 && geometry.dma50Series.size == count) {
            dma50Path.reset()
            var started = false
            for (i in 0 until count) {
                val v = geometry.dma50Series[i]
                if (!v.isNaN()) {
                    val x = i * stepX
                    val y = priceToY(v)
                    if (!started) {
                        dma50Path.moveTo(x, y)
                        started = true
                    } else {
                        dma50Path.lineTo(x, y)
                    }
                }
            }
            if (started) {
                drawPath(
                    path = dma50Path,
                    color = ChartMathProcessor.Dma50Color,
                    style = dmaStroke
                )
            }
        }

        // 5. PRIMARY PRICE SERIES (AREA OR CANDLESTICK)
        if (indicatorConfig.chartStyle == TerminalChartStyle.AREA) {
            priceLinePath.reset()
            areaFillPath.reset()

            for (i in 0 until count) {
                val x = i * stepX
                val y = priceToY(candles[i].close)
                if (i == 0) {
                    priceLinePath.moveTo(x, y)
                    areaFillPath.moveTo(x, priceChartBottom)
                    areaFillPath.lineTo(x, y)
                } else {
                    priceLinePath.lineTo(x, y)
                    areaFillPath.lineTo(x, y)
                }
            }
            areaFillPath.lineTo((count - 1) * stepX, priceChartBottom)
            areaFillPath.close()

            drawPath(
                path = areaFillPath,
                brush = areaGradientBrush,
                style = Fill
            )
            drawPath(
                path = priceLinePath,
                color = primarySeriesColor,
                style = mainLineStroke
            )
        } else {
            // Institutional Candlestick Rendering
            val bodyWidth = (stepX * 0.68f).coerceIn(2.5f, 18f)
            val wickStrokeWidth = 1.3.dp.toPx()

            for (i in 0 until count) {
                val c = candles[i]
                val x = i * stepX
                val highY = priceToY(c.high)
                val lowY = priceToY(c.low)
                val openY = priceToY(c.open)
                val closeY = priceToY(c.close)

                val isBull = c.close >= c.open
                val candleColor = if (isBull) {
                    TerminalThemeTokens.BullishEmerald
                } else {
                    TerminalThemeTokens.BearishCrimson
                }

                // High-Low Wick
                drawLine(
                    color = candleColor,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = wickStrokeWidth
                )

                // Open-Close Body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyH = (bodyBottom - bodyTop).coerceAtLeast(2.dp.toPx())
                drawRect(
                    color = candleColor,
                    topLeft = Offset(x - bodyWidth / 2f, bodyTop),
                    size = Size(bodyWidth, bodyH)
                )
            }
        }

        // 6. RULER / MEASURE OVERLAY (PURPLE #7C3AED)
        if (indicatorConfig.isRulerActive &&
            rulerStartIndex in 0 until count &&
            rulerEndIndex in 0 until count
        ) {
            val sIdx = min(rulerStartIndex, rulerEndIndex)
            val eIdx = max(rulerStartIndex, rulerEndIndex)
            val startX = sIdx * stepX
            val endX = eIdx * stepX
            val startY = priceToY(candles[sIdx].close)
            val endY = priceToY(candles[eIdx].close)

            // Shaded Purple Measure Zone
            drawRect(
                color = TerminalThemeTokens.MeasureRulerPurple.copy(alpha = 0.10f),
                topLeft = Offset(startX, priceChartTop),
                size = Size((endX - startX).coerceAtLeast(2f), priceChartHeight)
            )

            // Vertical Anchor Lines
            drawLine(
                color = TerminalThemeTokens.MeasureRulerPurple,
                start = Offset(startX, priceChartTop),
                end = Offset(startX, priceChartBottom),
                strokeWidth = rulerDashStroke.width,
                pathEffect = rulerDashStroke.pathEffect
            )
            drawLine(
                color = TerminalThemeTokens.MeasureRulerPurple,
                start = Offset(endX, priceChartTop),
                end = Offset(endX, priceChartBottom),
                strokeWidth = rulerDashStroke.width,
                pathEffect = rulerDashStroke.pathEffect
            )

            // Diagonal Price Vector Line & Anchor Nodes
            drawLine(
                color = TerminalThemeTokens.MeasureRulerPurple,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = TerminalThemeTokens.SurfaceWhite,
                radius = 5.5.dp.toPx(),
                center = Offset(startX, startY)
            )
            drawCircle(
                color = TerminalThemeTokens.MeasureRulerPurple,
                radius = 4.dp.toPx(),
                center = Offset(startX, startY)
            )
            drawCircle(
                color = TerminalThemeTokens.SurfaceWhite,
                radius = 5.5.dp.toPx(),
                center = Offset(endX, endY)
            )
            drawCircle(
                color = TerminalThemeTokens.MeasureRulerPurple,
                radius = 4.dp.toPx(),
                center = Offset(endX, endY)
            )
        }

        // 7. SINGLE-FINGER CROSSHAIR SCRUB POINTER & FLOATING AXIS PILL
        if (!indicatorConfig.isRulerActive && scrubbedIndex in 0 until count) {
            val activeCandle = candles[scrubbedIndex]
            val cx = scrubbedIndex * stepX
            val cy = priceToY(activeCandle.close)

            // Vertical & Horizontal Dashed Crosshairs
            drawLine(
                color = TerminalThemeTokens.TextSecondarySlate,
                start = Offset(cx, priceChartTop),
                end = Offset(cx, canvasH),
                strokeWidth = crosshairDashStroke.width,
                pathEffect = crosshairDashStroke.pathEffect
            )
            drawLine(
                color = TerminalThemeTokens.TextSecondarySlate,
                start = Offset(0f, cy),
                end = Offset(canvasW, cy),
                strokeWidth = crosshairDashStroke.width,
                pathEffect = crosshairDashStroke.pathEffect
            )

            // Highlight Halo + Spot Dot
            drawCircle(
                color = TerminalThemeTokens.SurfaceWhite,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accentColor,
                radius = 4.2.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Floating Timestamp Pill at Top of Crosshair
            val pillW = 104.dp.toPx()
            val pillH = 20.dp.toPx()
            val pillX = (cx - pillW / 2f).coerceIn(4.dp.toPx(), canvasW - pillW - 4.dp.toPx())
            drawRoundRect(
                color = TerminalThemeTokens.TextPrimarySlate,
                topLeft = Offset(pillX, priceChartTop),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                activeCandle.formattedTime,
                pillX + pillW / 2f,
                priceChartTop + pillH * 0.72f,
                hudBadgeTextPaint
            )
        }
    }
}