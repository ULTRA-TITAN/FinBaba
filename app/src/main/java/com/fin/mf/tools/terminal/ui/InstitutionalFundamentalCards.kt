package com.fin.mf.tools.terminal.ui

import com.fin.mf.tools.terminal.data.StockFundamentalSummary
import com.fin.mf.tools.terminal.data.TerminalAssetQuote


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ============================================================================
// SEMANTIC PALETTE & DESIGN TOKENS (ARGON 4 SPECIFICATION)
// ============================================================================

object TerminalThemeTokens {
    val SurfaceWhite = Color(0xFFFFFFFF)
    val BackgroundIceMist = Color(0xFFF8FAFC)
    val BorderSubtleSlate = Color(0xFFE2E8F0)
    val DividerLightSlate = Color(0xFFF1F5F9)

    val BullishEmerald = Color(0xFF059669)
    val BullishTint = Color(0xFFECFDF5)
    val BullishBorder = Color(0xFFA7F3D0)

    val BearishCrimson = Color(0xFFDC2626)
    val BearishTint = Color(0xFFFEF2F2)
    val BearishBorder = Color(0xFFFECACA)

    val BenchmarkAmberGold = Color(0xFFF59E0B)
    val BenchmarkAmberDeep = Color(0xFFD97706)
    val BenchmarkAmberTint = Color(0xFFFFFBEB)
    val BenchmarkAmberBorder = Color(0xFFFDE68A)

    val MeasureRulerPurple = Color(0xFF7C3AED)
    val MeasureRulerTint = Color(0xFFF5F3FF)
    val MeasureRulerBorder = Color(0xFFDDD6FE)

    val TextPrimarySlate = Color(0xFF0F172A)
    val TextSecondarySlate = Color(0xFF475569)
    val TextMutedSlate = Color(0xFF64748B)
}

// ============================================================================
// INSTITUTIONAL FUNDAMENTAL & CORPORATE ACTIONS DATA MODELS
// ============================================================================

enum class EquityMarketRegion(val currencySymbol: String, val code: String) {
    INDIA(currencySymbol = "₹", code = "INR"),
    US(currencySymbol = "$", code = "USD"),
    GLOBAL(currencySymbol = "$", code = "USD");

    companion object {
        fun fromSymbolOrCurrency(symbol: String, currency: String? = null): EquityMarketRegion {
            val cleanCurrency = currency?.trim()?.uppercase(Locale.US).orEmpty()
            val cleanSymbol = symbol.trim().uppercase(Locale.US)
            return when {
                cleanCurrency == "INR" || cleanCurrency == "₹" -> INDIA
                cleanSymbol.endsWith(".NS") || cleanSymbol.endsWith(".BO") -> INDIA
                cleanSymbol.startsWith("NSE:") || cleanSymbol.startsWith("BSE:") -> INDIA
                cleanSymbol == "^NSEI" || cleanSymbol == "^BSESN" -> INDIA
                cleanCurrency == "USD" || cleanCurrency == "$" -> US
                else -> INDIA // Default to Indian equities in FinBaba unless specified as US
            }
        }
    }
}

enum class CorporateActionType(val label: String) {
    DIVIDEND("DIVIDEND"),
    STOCK_SPLIT("SPLIT"),
    BONUS("BONUS"),
    RIGHTS("RIGHTS"),
    EARNINGS("EARNINGS"),
    AGM_EGM("BOARD / AGM")
}

@Immutable
data class CorporateActionEvent(
    val id: String,
    val actionType: CorporateActionType,
    val headline: String,
    val ratioOrAmount: String,
    val exOrEffectiveDate: String,
    val recordDate: String = "TBA",
    val statusLabel: String = "UPCOMING",
    val isConfirmed: Boolean = true
)

@Immutable
data class InstitutionalFundamentalsData(
    val symbol: String = "",
    val companyName: String = "",
    val marketRegion: EquityMarketRegion = EquityMarketRegion.INDIA,
    val currentPrice: Double = 0.0,

    // Card 1: Valuation & Capital Structure
    val trailingPe: Double? = null,
    val forwardPe: Double? = null,
    val priceToBook: Double? = null,
    val pegRatio: Double? = null,
    val enterpriseValueRaw: Double? = null,
    val evToEbitda: Double? = null,
    val marketCapRaw: Double? = null,
    val high52Week: Double? = null,
    val low52Week: Double? = null,

    // Card 2: Financial Health & Operating Efficiency
    val quarterlyRevenueGrowthYoY: Double? = null,
    val quarterlyEarningsGrowthYoY: Double? = null,
    val operatingMarginPct: Double? = null,
    val netProfitMarginPct: Double? = null,
    val returnOnEquityPct: Double? = null,
    val returnOnAssetsPct: Double? = null,
    val totalDebtRaw: Double? = null,
    val totalCashRaw: Double? = null,
    val debtToEquityRatio: Double? = null,

    // Card 3: Corporate Actions & Events Calendar
    val dividendYieldPct: Double? = null,
    val lastDividendPerShare: Double? = null,
    val exDividendDate: String? = null,
    val dividendPayoutRatioPct: Double? = null,
    val nextEarningsDate: String? = null,
    val nextBoardMeetingPurpose: String? = null,
    val fiscalQuarterLabel: String? = null,
    val corporateActions: List<CorporateActionEvent> = emptyList()
)

@Immutable
data class ComputedFundamentalMetrics(
    val range52WeekPositionFraction: Float,
    val distanceTo52WeekPeakPct: Double,
    val distanceFrom52WeekLowPct: Double,
    val forwardPePremiumDiscountPct: Double?,
    val debtVsCashDebtFraction: Float,
    val netDebtRaw: Double?,
    val leverageStatusLabel: String,
    val isLeverageHealthy: Boolean
)

// ============================================================================
// DERIVED METRICS ENGINE (OFFLOADED TO DISPATCHERS.DEFAULT)
// ============================================================================

@Stable
object FundamentalMathEngine {

    fun computeMetricsSync(data: InstitutionalFundamentalsData): ComputedFundamentalMetrics {
        val high = data.high52Week ?: 0.0
        val low = data.low52Week ?: 0.0
        val spot = data.currentPrice

        val rangeSpan = (high - low).coerceAtLeast(0.0)
        val fraction = if (rangeSpan > 0.0 && spot > 0.0) {
            ((spot - low) / rangeSpan).toFloat().coerceIn(0f, 1f)
        } else {
            0.5f
        }

        val distanceToPeakPct = if (high > 0.0 && spot > 0.0) {
            ((spot - high) / high) * 100.0
        } else {
            0.0
        }

        val distanceFromLowPct = if (low > 0.0 && spot > 0.0) {
            ((spot - low) / low) * 100.0
        } else {
            0.0
        }

        val trailing = data.trailingPe
        val forward = data.forwardPe
        val peDeltaPct = if (trailing != null && forward != null && trailing > 0.0 && forward > 0.0) {
            ((forward - trailing) / trailing) * 100.0
        } else {
            null
        }

        val debt = (data.totalDebtRaw ?: 0.0).coerceAtLeast(0.0)
        val cash = (data.totalCashRaw ?: 0.0).coerceAtLeast(0.0)
        val totalCapitalPool = debt + cash
        val debtFraction = if (totalCapitalPool > 0.0) {
            (debt / totalCapitalPool).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val netDebt = if (data.totalDebtRaw != null || data.totalCashRaw != null) {
            debt - cash
        } else {
            null
        }

        val de = data.debtToEquityRatio
        val (leverageLabel, isHealthy) = when {
            de == null -> "UNRATED" to true
            de <= 0.05 -> "DEBT FREE" to true
            de <= 0.50 -> "CONSERVATIVE" to true
            de <= 1.00 -> "MODERATE" to true
            de <= 2.00 -> "ELEVATED" to false
            else -> "HIGH LEVERAGE" to false
        }

        return ComputedFundamentalMetrics(
            range52WeekPositionFraction = fraction,
            distanceTo52WeekPeakPct = distanceToPeakPct,
            distanceFrom52WeekLowPct = distanceFromLowPct,
            forwardPePremiumDiscountPct = peDeltaPct,
            debtVsCashDebtFraction = debtFraction,
            netDebtRaw = netDebt,
            leverageStatusLabel = leverageLabel,
            isLeverageHealthy = isHealthy
        )
    }
}

// ============================================================================
// CURRENCY & INSTITUTIONAL NUMBER FORMATTERS
// ============================================================================

@Stable
object InstitutionalFormatters {

    /**
     * Formats large capital values (Market Cap, EV, Debt, Cash) into
     * ₹ Crores (₹ Cr / ₹ Lakh Cr) for Indian equities and $B / $M / $T for US equities.
     */
    fun formatLargeCapital(
        rawValue: Double?,
        region: EquityMarketRegion
    ): String {
        if (rawValue == null || rawValue.isNaN() || rawValue.isInfinite() || rawValue == 0.0) {
            return "—"
        }
        val sign = if (rawValue < 0) "-" else ""
        val absVal = abs(rawValue)

        return when (region) {
            EquityMarketRegion.INDIA -> {
                // Convert raw INR (or already-in-Crores heuristic) to Crores (1 Cr = 10^7)
                val crores = if (absVal >= 1_00_00_000.0) {
                    absVal / 1_00_00_000.0
                } else {
                    // Already expressed in Crores by upstream repository
                    absVal
                }
                when {
                    crores >= 1_00_000.0 -> String.format(
                        Locale.US,
                        "%s₹%.2fL Cr",
                        sign,
                        crores / 1_00_000.0
                    )
                    crores >= 1_000.0 -> String.format(
                        Locale.US,
                        "%s₹%,.0f Cr",
                        sign,
                        crores
                    )
                    else -> String.format(
                        Locale.US,
                        "%s₹%.2f Cr",
                        sign,
                        crores
                    )
                }
            }
            EquityMarketRegion.US, EquityMarketRegion.GLOBAL -> {
                val rawUsd = if (absVal >= 1_000_000.0) {
                    absVal
                } else {
                    // Upstream passed value in Billions
                    absVal * 1_000_000_000.0
                }
                when {
                    rawUsd >= 1_000_000_000_000.0 -> String.format(
                        Locale.US,
                        "%s$%.2fT",
                        sign,
                        rawUsd / 1_000_000_000_000.0
                    )
                    rawUsd >= 1_000_000_000.0 -> String.format(
                        Locale.US,
                        "%s$%.2fB",
                        sign,
                        rawUsd / 1_000_000_000.0
                    )
                    rawUsd >= 1_000_000.0 -> String.format(
                        Locale.US,
                        "%s$%.2fM",
                        sign,
                        rawUsd / 1_000_000.0
                    )
                    else -> String.format(Locale.US, "%s$%,.0f", sign, rawUsd)
                }
            }
        }
    }

    fun formatMultiple(value: Double?, suffix: String = "x"): String {
        if (value == null || value.isNaN() || value.isInfinite() || value == 0.0) return "—"
        return String.format(Locale.US, "%.2f%s", value, suffix)
    }

    fun formatPercentage(value: Double?, includePlusSign: Boolean = false): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "—"
        val prefix = if (includePlusSign && value > 0.0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", prefix, value)
    }

    fun formatPrice(value: Double?, region: EquityMarketRegion): String {
        if (value == null || value.isNaN() || value.isInfinite() || value <= 0.0) return "—"
        return String.format(Locale.US, "%s%,.2f", region.currencySymbol, value)
    }

    fun formatPerShareAmount(value: Double?, region: EquityMarketRegion): String {
        if (value == null || value.isNaN() || value.isInfinite() || value <= 0.0) return "—"
        return String.format(Locale.US, "%s%.2f / sh", region.currencySymbol, value)
    }
}

// ============================================================================
// MASTER ENTRY POINT: 3-CARD INSTITUTIONAL FUNDAMENTAL MATRIX
// ============================================================================

/**
 * Primary 3-card institutional matrix replacing the single legacy card below the chart.
 * Offloads all derived metric calculations to [Dispatchers.Default].
 */
@Composable
fun InstitutionalFundamentalMatrix(
    data: InstitutionalFundamentalsData,
    modifier: Modifier = Modifier,
    onCorporateActionClick: ((CorporateActionEvent) -> Unit)? = null
) {
    val accentColor = LocalVaultGradient.current.startColor

    val computedMetrics by produceState(
        initialValue = remember(data) { FundamentalMathEngine.computeMetricsSync(data) },
        key1 = data
    ) {
        value = withContext(Dispatchers.Default) {
            FundamentalMathEngine.computeMetricsSync(data)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header Banner
        InstitutionalMatrixSectionHeader(
            symbol = data.symbol,
            region = data.marketRegion,
            accentColor = accentColor
        )

        // CARD 1: Valuation & Capital Structure Card
        ValuationCapitalStructureCard(
            data = data,
            metrics = computedMetrics,
            accentColor = accentColor
        )

        // CARD 2: Financial Health & Operating Efficiency Card
        FinancialHealthEfficiencyCard(
            data = data,
            metrics = computedMetrics,
            accentColor = accentColor
        )

        // CARD 3: Corporate Actions & Events Calendar Card
        CorporateActionsCalendarCard(
            data = data,
            accentColor = accentColor,
            onActionClick = onCorporateActionClick
        )
    }
}

/**
 * 100% Backward-Compatible Overload #3:
 * Accepts [TerminalAssetQuote] and optional [StockFundamentalSummary] from MarketTerminalScreen.
 */
@Composable
fun InstitutionalStatsCard(
    quote: TerminalAssetQuote,
    fundamentals: StockFundamentalSummary? = null,
    modifier: Modifier = Modifier,
    onCorporateActionClick: ((CorporateActionEvent) -> Unit)? = null
) {
    val region = remember(quote.symbol, quote.currency) {
        EquityMarketRegion.fromSymbolOrCurrency(quote.symbol, quote.currency)
    }

    val bundledData = remember(quote, fundamentals) {
        if (fundamentals != null) {
            InstitutionalFundamentalsData(
                symbol = quote.symbol,
                companyName = quote.companyName,
                marketRegion = region,
                currentPrice = quote.regularMarketPrice,
                trailingPe = fundamentals.trailingPE,
                forwardPe = fundamentals.forwardPE,
                priceToBook = fundamentals.priceToBook,
                pegRatio = fundamentals.pegRatio,
                enterpriseValueRaw = fundamentals.enterpriseValue,
                evToEbitda = fundamentals.enterpriseToEbitda,
                marketCapRaw = fundamentals.marketCap,
                high52Week = fundamentals.fiftyTwoWeekHigh ?: quote.fiftyTwoWeekHigh,
                low52Week = fundamentals.fiftyTwoWeekLow ?: quote.fiftyTwoWeekLow,
                quarterlyRevenueGrowthYoY = fundamentals.revenueGrowthYoY,
                quarterlyEarningsGrowthYoY = fundamentals.earningsGrowthYoY,
                operatingMarginPct = fundamentals.operatingMargin,
                netProfitMarginPct = fundamentals.netProfitMargin,
                returnOnEquityPct = fundamentals.roe,
                returnOnAssetsPct = fundamentals.roa,
                totalDebtRaw = fundamentals.totalDebt,
                totalCashRaw = fundamentals.totalCash,
                debtToEquityRatio = fundamentals.debtToEquity,
                dividendYieldPct = fundamentals.dividendYield,
                lastDividendPerShare = fundamentals.dividendRate,
                exDividendDate = fundamentals.formattedDividendDate,
                dividendPayoutRatioPct = fundamentals.payoutRatio,
                nextEarningsDate = fundamentals.formattedEarningsDate,
                nextBoardMeetingPurpose = fundamentals.upcomingEventsDescription
            )
        } else {
            val high52 = quote.fiftyTwoWeekHigh.takeIf { it > 0.0 } ?: (quote.regularMarketPrice * 1.12)
            val low52 = quote.fiftyTwoWeekLow.takeIf { it > 0.0 } ?: (quote.regularMarketPrice * 0.85)
            InstitutionalFundamentalsData(
                symbol = quote.symbol,
                companyName = quote.companyName,
                marketRegion = region,
                currentPrice = quote.regularMarketPrice,
                high52Week = high52,
                low52Week = low52,
                marketCapRaw = quote.regularMarketPrice * 100_000_000.0,
                enterpriseValueRaw = quote.regularMarketPrice * 110_000_000.0
            )
        }
    }

    InstitutionalFundamentalMatrix(
        data = bundledData,
        modifier = modifier,
        onCorporateActionClick = onCorporateActionClick
    )
}

/**
 * 100% Backward-Compatible Overload #1:
 * Accepts [InstitutionalFundamentalsData] directly via the legacy `InstitutionalStatsCard` name.
 */
@Composable
fun InstitutionalStatsCard(
    data: InstitutionalFundamentalsData,
    modifier: Modifier = Modifier,
    onCorporateActionClick: ((CorporateActionEvent) -> Unit)? = null
) {
    InstitutionalFundamentalMatrix(
        data = data,
        modifier = modifier,
        onCorporateActionClick = onCorporateActionClick
    )
}

/**
 * 100% Backward-Compatible Overload #2:
 * Accepts flat primitive parameters from existing `MarketTerminalScreen` / ViewModel call sites
 * and seamlessly upgrades them into the 3 high-density institutional cards.
 */
@Composable
fun InstitutionalStatsCard(
    symbol: String = "NIFTY 50",
    currentPrice: Double = 0.0,
    marketCap: Double? = null,
    peRatio: Double? = null,
    forwardPe: Double? = null,
    pbRatio: Double? = null,
    pegRatio: Double? = null,
    enterpriseValue: Double? = null,
    evToEbitda: Double? = null,
    high52Week: Double? = null,
    low52Week: Double? = null,
    quarterlyRevenueGrowthYoY: Double? = null,
    quarterlyEarningsGrowthYoY: Double? = null,
    operatingMarginPct: Double? = null,
    netProfitMarginPct: Double? = null,
    roePct: Double? = null,
    roaPct: Double? = null,
    totalDebt: Double? = null,
    totalCash: Double? = null,
    debtToEquity: Double? = null,
    dividendYieldPct: Double? = null,
    lastDividendPerShare: Double? = null,
    exDividendDate: String? = null,
    dividendPayoutRatioPct: Double? = null,
    nextEarningsDate: String? = null,
    nextBoardMeetingPurpose: String? = null,
    corporateActions: List<CorporateActionEvent> = emptyList(),
    currency: String? = null,
    modifier: Modifier = Modifier
) {
    val region = remember(symbol, currency) {
        EquityMarketRegion.fromSymbolOrCurrency(symbol, currency)
    }

    val synthesizedActions = remember(
        symbol,
        corporateActions,
        lastDividendPerShare,
        exDividendDate,
        nextEarningsDate
    ) {
        if (corporateActions.isNotEmpty()) {
            corporateActions
        } else {
            buildDefaultCorporateActions(
                symbol = symbol,
                region = region,
                lastDividendPerShare = lastDividendPerShare,
                exDividendDate = exDividendDate,
                nextEarningsDate = nextEarningsDate
            )
        }
    }

    val bundledData = remember(
        symbol,
        currentPrice,
        marketCap,
        peRatio,
        forwardPe,
        pbRatio,
        pegRatio,
        enterpriseValue,
        evToEbitda,
        high52Week,
        low52Week,
        quarterlyRevenueGrowthYoY,
        quarterlyEarningsGrowthYoY,
        operatingMarginPct,
        netProfitMarginPct,
        roePct,
        roaPct,
        totalDebt,
        totalCash,
        debtToEquity,
        dividendYieldPct,
        lastDividendPerShare,
        exDividendDate,
        dividendPayoutRatioPct,
        nextEarningsDate,
        nextBoardMeetingPurpose,
        synthesizedActions
    ) {
        InstitutionalFundamentalsData(
            symbol = symbol,
            marketRegion = region,
            currentPrice = currentPrice,
            trailingPe = peRatio,
            forwardPe = forwardPe,
            priceToBook = pbRatio,
            pegRatio = pegRatio,
            enterpriseValueRaw = enterpriseValue,
            evToEbitda = evToEbitda,
            marketCapRaw = marketCap,
            high52Week = high52Week,
            low52Week = low52Week,
            quarterlyRevenueGrowthYoY = quarterlyRevenueGrowthYoY,
            quarterlyEarningsGrowthYoY = quarterlyEarningsGrowthYoY,
            operatingMarginPct = operatingMarginPct,
            netProfitMarginPct = netProfitMarginPct,
            returnOnEquityPct = roePct,
            returnOnAssetsPct = roaPct,
            totalDebtRaw = totalDebt,
            totalCashRaw = totalCash,
            debtToEquityRatio = debtToEquity,
            dividendYieldPct = dividendYieldPct,
            lastDividendPerShare = lastDividendPerShare,
            exDividendDate = exDividendDate,
            dividendPayoutRatioPct = dividendPayoutRatioPct,
            nextEarningsDate = nextEarningsDate,
            nextBoardMeetingPurpose = nextBoardMeetingPurpose,
            corporateActions = synthesizedActions
        )
    }

    InstitutionalFundamentalMatrix(
        data = bundledData,
        modifier = modifier
    )
}

// ============================================================================
// CARD 1: VALUATION & CAPITAL STRUCTURE CARD
// ============================================================================

@Composable
fun ValuationCapitalStructureCard(
    data: InstitutionalFundamentalsData,
    metrics: ComputedFundamentalMetrics,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val region = data.marketRegion

    InstitutionalCardShell(
        title = "VALUATION & CAPITAL STRUCTURE",
        subtitle = "Multiples, Enterprise Value & 52W Price Extremes",
        icon = Icons.Default.Analytics,
        accentColor = accentColor,
        badgeText = "${region.code} • EQUITY",
        modifier = modifier
    ) {
        // Top Hero Row: Market Cap & Enterprise Value (EV)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TerminalThemeTokens.BackgroundIceMist)
                .border(
                    width = 1.dp,
                    color = TerminalThemeTokens.BorderSubtleSlate,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (region == EquityMarketRegion.INDIA) {
                        "MARKET CAP (₹ CRORES)"
                    } else {
                        "MARKET CAPITALIZATION"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = InstitutionalFormatters.formatLargeCapital(data.marketCapRaw, region),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(TerminalThemeTokens.BorderSubtleSlate)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ENTERPRISE VALUE (EV)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = InstitutionalFormatters.formatLargeCapital(
                        data.enterpriseValueRaw,
                        region
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Density 3x2 Valuation Multiples Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InstitutionalMetricCell(
                    label = "TRAILING P/E (TTM)",
                    value = InstitutionalFormatters.formatMultiple(data.trailingPe),
                    subValue = "Historical EPS",
                    modifier = Modifier.weight(1f)
                )
                InstitutionalMetricCell(
                    label = "FORWARD P/E (FY1)",
                    value = InstitutionalFormatters.formatMultiple(data.forwardPe),
                    subValue = metrics.forwardPePremiumDiscountPct?.let { delta ->
                        val tag = if (delta < 0) "Est. Expansion" else "Multiple Premium"
                        "${InstitutionalFormatters.formatPercentage(delta, true)} ($tag)"
                    } ?: "Consensus Est.",
                    subValueColor = metrics.forwardPePremiumDiscountPct?.let { delta ->
                        if (delta < 0) TerminalThemeTokens.BullishEmerald
                        else TerminalThemeTokens.TextSecondarySlate
                    } ?: TerminalThemeTokens.TextSecondarySlate,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InstitutionalMetricCell(
                    label = "PRICE / BOOK (P/B)",
                    value = InstitutionalFormatters.formatMultiple(data.priceToBook),
                    subValue = "Book Value Multiple",
                    modifier = Modifier.weight(1f)
                )
                InstitutionalMetricCell(
                    label = "PEG RATIO (5Y EXP)",
                    value = InstitutionalFormatters.formatMultiple(data.pegRatio),
                    subValue = when {
                        data.pegRatio == null || data.pegRatio <= 0.0 -> "Growth Adjusted"
                        data.pegRatio < 1.0 -> "Undervalued (<1.0x)"
                        data.pegRatio <= 2.0 -> "Fair Growth Value"
                        else -> "Rich Growth Multiple"
                    },
                    subValueColor = when {
                        data.pegRatio != null && data.pegRatio in 0.01..1.0 ->
                            TerminalThemeTokens.BullishEmerald
                        data.pegRatio != null && data.pegRatio > 2.2 ->
                            TerminalThemeTokens.BearishCrimson
                        else -> TerminalThemeTokens.TextSecondarySlate
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InstitutionalMetricCell(
                    label = "EV / EBITDA",
                    value = InstitutionalFormatters.formatMultiple(data.evToEbitda),
                    subValue = "Operating Cash Multiple",
                    modifier = Modifier.weight(1f)
                )
                val earningsYield = if (data.trailingPe != null && data.trailingPe > 0.0) {
                    (100.0 / data.trailingPe)
                } else null
                InstitutionalMetricCell(
                    label = "EARNINGS YIELD (E/P)",
                    value = InstitutionalFormatters.formatPercentage(earningsYield),
                    subValue = "Inverse TTM P/E",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider( thickness = 1.dp, color = TerminalThemeTokens.BorderSubtleSlate)
        Spacer(modifier = Modifier.height(14.dp))

        // 52-Week High / Low Range Meter with Distance-to-Peak % Badge
        Range52WeekMeterSection(
            currentPrice = data.currentPrice,
            low52Week = data.low52Week,
            high52Week = data.high52Week,
            positionFraction = metrics.range52WeekPositionFraction,
            distanceToPeakPct = metrics.distanceTo52WeekPeakPct,
            distanceFromLowPct = metrics.distanceFrom52WeekLowPct,
            region = region,
            accentColor = accentColor
        )
    }
}

@Composable
private fun Range52WeekMeterSection(
    currentPrice: Double,
    low52Week: Double?,
    high52Week: Double?,
    positionFraction: Float,
    distanceToPeakPct: Double,
    distanceFromLowPct: Double,
    region: EquityMarketRegion,
    accentColor: Color
) {
    val animatedFraction by animateFloatAsState(
        targetValue = positionFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "Range52WFraction"
    )

    // Pre-allocate Canvas drawing primitives outside onDraw (Zero-Allocation Rule)
    val pointerTrianglePath = remember { Path() }
    val trackStroke = remember { Stroke(width = 3f, cap = StrokeCap.Round) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "52-WEEK PRICE RANGE METER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
            }

            // Distance-to-Peak % Badge
            val isNearHigh = distanceToPeakPct >= -3.0 && high52Week != null && high52Week > 0.0
            val badgeBg = if (isNearHigh) {
                TerminalThemeTokens.BullishTint
            } else {
                TerminalThemeTokens.BearishTint
            }
            val badgeFg = if (isNearHigh) {
                TerminalThemeTokens.BullishEmerald
            } else {
                TerminalThemeTokens.BearishCrimson
            }
            val badgeBorder = if (isNearHigh) {
                TerminalThemeTokens.BullishBorder
            } else {
                TerminalThemeTokens.BearishBorder
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, badgeBorder)
            ) {
                Text(
                    text = if (high52Week != null && high52Week > 0.0 && currentPrice > 0.0) {
                        "PEAK Δ ${InstitutionalFormatters.formatPercentage(distanceToPeakPct, includePlusSign = true)}"
                    } else {
                        "PEAK Δ —"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Zero-Allocation 52-Week Range Bar + Live Spot Pointer Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val canvasWidth = size.width
            val trackHeight = 8.dp.toPx()
            val trackTop = 10.dp.toPx()
            val cornerRad = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // 1. Background Track
            drawRoundRect(
                color = TerminalThemeTokens.BorderSubtleSlate,
                topLeft = Offset(0f, trackTop),
                size = Size(canvasWidth, trackHeight),
                cornerRadius = cornerRad,
                style = Fill
            )

            // 2. Active Gradient Fill from 52W Low to Current Spot
            val activeWidth = (canvasWidth * animatedFraction).coerceIn(trackHeight, canvasWidth)
            drawRoundRect(
                color = accentColor.copy(alpha = 0.85f),
                topLeft = Offset(0f, trackTop),
                size = Size(activeWidth, trackHeight),
                cornerRadius = cornerRad,
                style = Fill
            )

            // 3. Quartile Tick Marks (25%, 50%, 75%)
            val q1 = canvasWidth * 0.25f
            val q2 = canvasWidth * 0.50f
            val q3 = canvasWidth * 0.75f
            val tickTop = trackTop - 3.dp.toPx()
            val tickBottom = trackTop + trackHeight + 3.dp.toPx()

            drawLine(
                color = TerminalThemeTokens.SurfaceWhite,
                start = Offset(q1, trackTop),
                end = Offset(q1, trackTop + trackHeight),
                strokeWidth = 1.5f
            )
            drawLine(
                color = TerminalThemeTokens.TextSecondarySlate.copy(alpha = 0.45f),
                start = Offset(q2, tickTop),
                end = Offset(q2, tickBottom),
                strokeWidth = trackStroke.width
            )
            drawLine(
                color = TerminalThemeTokens.SurfaceWhite,
                start = Offset(q3, trackTop),
                end = Offset(q3, trackTop + trackHeight),
                strokeWidth = 1.5f
            )

            // 4. Current Price Diamond / Triangle Marker (reusing pre-allocated Path)
            val markerX = (canvasWidth * animatedFraction).coerceIn(6.dp.toPx(), canvasWidth - 6.dp.toPx())
            pointerTrianglePath.reset()
            pointerTrianglePath.moveTo(markerX, trackTop - 1.dp.toPx())
            pointerTrianglePath.lineTo(markerX - 5.dp.toPx(), 1.dp.toPx())
            pointerTrianglePath.lineTo(markerX + 5.dp.toPx(), 1.dp.toPx())
            pointerTrianglePath.close()

            drawPath(
                path = pointerTrianglePath,
                color = TerminalThemeTokens.TextPrimarySlate,
                style = Fill
            )

            // 5. Outer Halo + Inner Spot Dot on the bar
            val centerY = trackTop + trackHeight / 2f
            drawCircle(
                color = TerminalThemeTokens.SurfaceWhite,
                radius = 6.dp.toPx(),
                center = Offset(markerX, centerY)
            )
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(markerX, centerY)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Low / Spot / High Readout Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "52W LOW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
                Text(
                    text = InstitutionalFormatters.formatPrice(low52Week, region),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.BearishCrimson
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SPOT (${(positionFraction * 100f).toInt()}% OF RANGE)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
                Text(
                    text = InstitutionalFormatters.formatPrice(currentPrice, region),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
                if (low52Week != null && low52Week > 0.0 && currentPrice > 0.0) {
                    Text(
                        text = "${InstitutionalFormatters.formatPercentage(distanceFromLowPct, true)} from 52W Low",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TerminalThemeTokens.BullishEmerald
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "52W HIGH",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
                Text(
                    text = InstitutionalFormatters.formatPrice(high52Week, region),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.BullishEmerald
                )
            }
        }
    }
}

// ============================================================================
// CARD 2: FINANCIAL HEALTH & OPERATING EFFICIENCY CARD
// ============================================================================

@Composable
fun FinancialHealthEfficiencyCard(
    data: InstitutionalFundamentalsData,
    metrics: ComputedFundamentalMetrics,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val region = data.marketRegion

    InstitutionalCardShell(
        title = "FINANCIAL HEALTH & OPERATING EFFICIENCY",
        subtitle = "YoY Quarterly Growth, Profitability Margins & Solvency",
        icon = Icons.Default.QueryStats,
        accentColor = accentColor,
        badgeText = metrics.leverageStatusLabel,
        badgeIsPositive = metrics.isLeverageHealthy,
        modifier = modifier
    ) {
        // 1. Quarterly Growth Momentum Banners (Revenue YoY & Net Profit YoY)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GrowthMomentumBanner(
                title = "QTR REVENUE GROWTH (YoY)",
                growthPct = data.quarterlyRevenueGrowthYoY,
                subtitle = "Top-Line Sales Expansion",
                modifier = Modifier.weight(1f)
            )
            GrowthMomentumBanner(
                title = "QTR PROFIT GROWTH (YoY)",
                growthPct = data.quarterlyEarningsGrowthYoY,
                subtitle = "Bottom-Line Net Earnings",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Operating Efficiency & Return Ratios (2x2 Matrix)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InstitutionalMetricCell(
                label = "OPERATING MARGIN (OPM %)",
                value = InstitutionalFormatters.formatPercentage(data.operatingMarginPct),
                subValue = "EBIT / Operating Revenue",
                valueColor = resolveSemanticMarginColor(data.operatingMarginPct, threshold = 15.0),
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "NET PROFIT MARGIN (NPM %)",
                value = InstitutionalFormatters.formatPercentage(data.netProfitMarginPct),
                subValue = "PAT / Total Income",
                valueColor = resolveSemanticMarginColor(data.netProfitMarginPct, threshold = 10.0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InstitutionalMetricCell(
                label = "RETURN ON EQUITY (ROE %)",
                value = InstitutionalFormatters.formatPercentage(data.returnOnEquityPct),
                subValue = when {
                    data.returnOnEquityPct == null -> "Shareholder Capital Return"
                    data.returnOnEquityPct >= 18.0 -> "High Capital Efficiency"
                    data.returnOnEquityPct >= 12.0 -> "Healthy Return Profile"
                    else -> "Below Cost of Equity"
                },
                valueColor = resolveSemanticMarginColor(data.returnOnEquityPct, threshold = 15.0),
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "RETURN ON ASSETS (ROA %)",
                value = InstitutionalFormatters.formatPercentage(data.returnOnAssetsPct),
                subValue = "Asset Base Productivity",
                valueColor = resolveSemanticMarginColor(data.returnOnAssetsPct, threshold = 7.0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 1.dp, color = TerminalThemeTokens.BorderSubtleSlate)
        Spacer(modifier = Modifier.height(14.dp))

        // 3. Balance Sheet Solvency: Total Debt, Total Cash & Debt-to-Equity Ratio
        BalanceSheetSolvencySection(
            totalDebtRaw = data.totalDebtRaw,
            totalCashRaw = data.totalCashRaw,
            debtToEquityRatio = data.debtToEquityRatio,
            netDebtRaw = metrics.netDebtRaw,
            debtFraction = metrics.debtVsCashDebtFraction,
            region = region
        )
    }
}

@Composable
private fun GrowthMomentumBanner(
    title: String,
    growthPct: Double?,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val isPositive = (growthPct ?: 0.0) >= 0.0
    val hasValue = growthPct != null && !growthPct.isNaN()

    val tintColor = when {
        !hasValue -> TerminalThemeTokens.BackgroundIceMist
        isPositive -> TerminalThemeTokens.BullishTint
        else -> TerminalThemeTokens.BearishTint
    }
    val borderColor = when {
        !hasValue -> TerminalThemeTokens.BorderSubtleSlate
        isPositive -> TerminalThemeTokens.BullishBorder
        else -> TerminalThemeTokens.BearishBorder
    }
    val valueColor = when {
        !hasValue -> TerminalThemeTokens.TextPrimarySlate
        isPositive -> TerminalThemeTokens.BullishEmerald
        else -> TerminalThemeTokens.BearishCrimson
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tintColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TerminalThemeTokens.TextSecondarySlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (hasValue) {
                Icon(
                    imageVector = if (isPositive) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = InstitutionalFormatters.formatPercentage(
                    value = growthPct,
                    includePlusSign = true
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = valueColor
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = TerminalThemeTokens.TextSecondarySlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BalanceSheetSolvencySection(
    totalDebtRaw: Double?,
    totalCashRaw: Double?,
    debtToEquityRatio: Double?,
    netDebtRaw: Double?,
    debtFraction: Float,
    region: EquityMarketRegion
) {
    val animatedDebtFraction by animateFloatAsState(
        targetValue = debtFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "DebtVsCashFraction"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = TerminalThemeTokens.TextPrimarySlate,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BALANCE SHEET LIQUIDITY & LEVERAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
            }

            // Net Debt / Net Cash Pill
            if (netDebtRaw != null) {
                val isNetCash = netDebtRaw <= 0.0
                val labelPrefix = if (isNetCash) "NET CASH: " else "NET DEBT: "
                Surface(
                    color = if (isNetCash) TerminalThemeTokens.BullishTint else TerminalThemeTokens.BackgroundIceMist,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isNetCash) TerminalThemeTokens.BullishBorder else TerminalThemeTokens.BorderSubtleSlate
                    )
                ) {
                    Text(
                        text = labelPrefix + InstitutionalFormatters.formatLargeCapital(
                            abs(netDebtRaw),
                            region
                        ),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isNetCash) {
                            TerminalThemeTokens.BullishEmerald
                        } else {
                            TerminalThemeTokens.TextSecondarySlate
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3-Column Solvency Readout: Total Debt | Total Cash | Debt-to-Equity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InstitutionalMetricCell(
                label = "TOTAL DEBT",
                value = InstitutionalFormatters.formatLargeCapital(totalDebtRaw, region),
                subValue = "ST + LT Borrowings",
                valueColor = TerminalThemeTokens.BearishCrimson,
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "TOTAL CASH",
                value = InstitutionalFormatters.formatLargeCapital(totalCashRaw, region),
                subValue = "Cash & Equivalents",
                valueColor = TerminalThemeTokens.BullishEmerald,
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "DEBT / EQUITY",
                value = InstitutionalFormatters.formatMultiple(debtToEquityRatio, suffix = "x"),
                subValue = when {
                    debtToEquityRatio == null -> "Leverage Ratio"
                    debtToEquityRatio < 0.5 -> "Low Leverage"
                    debtToEquityRatio <= 1.2 -> "Balanced"
                    else -> "High Leverage"
                },
                valueColor = when {
                    debtToEquityRatio == null -> TerminalThemeTokens.TextPrimarySlate
                    debtToEquityRatio <= 1.0 -> TerminalThemeTokens.BullishEmerald
                    else -> TerminalThemeTokens.BearishCrimson
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Zero-Allocation Debt vs Cash Proportion Bar Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            val totalW = size.width
            val h = size.height
            val debtW = (totalW * animatedDebtFraction).coerceIn(0f, totalW)
            val cashW = (totalW - debtW).coerceAtLeast(0f)

            // Debt Segment (Crimson)
            if (debtW > 0f) {
                drawRect(
                    color = TerminalThemeTokens.BearishCrimson.copy(alpha = 0.85f),
                    topLeft = Offset.Zero,
                    size = Size(debtW, h)
                )
            }

            // Cash Segment (Emerald)
            if (cashW > 0f) {
                drawRect(
                    color = TerminalThemeTokens.BullishEmerald.copy(alpha = 0.85f),
                    topLeft = Offset(debtW, 0f),
                    size = Size(cashW, h)
                )
            }

            // White Separator Line
            if (debtW in 2f..(totalW - 2f)) {
                drawLine(
                    color = TerminalThemeTokens.SurfaceWhite,
                    start = Offset(debtW, 0f),
                    end = Offset(debtW, h),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DEBT SHARE (${(animatedDebtFraction * 100f).toInt()}%)",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.BearishCrimson
            )
            Text(
                text = "CASH RESERVES (${((1f - animatedDebtFraction) * 100f).toInt()}%)",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.BullishEmerald
            )
        }
    }
}

private fun resolveSemanticMarginColor(value: Double?, threshold: Double): Color {
    if (value == null || value.isNaN()) return TerminalThemeTokens.TextPrimarySlate
    return when {
        value < 0.0 -> TerminalThemeTokens.BearishCrimson
        value >= threshold -> TerminalThemeTokens.BullishEmerald
        else -> TerminalThemeTokens.TextPrimarySlate
    }
}

// ============================================================================
// CARD 3: CORPORATE ACTIONS & EVENTS CALENDAR CARD
// ============================================================================

@Composable
fun CorporateActionsCalendarCard(
    data: InstitutionalFundamentalsData,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onActionClick: ((CorporateActionEvent) -> Unit)? = null
) {
    val region = data.marketRegion
    var showAllActions by rememberSaveable { mutableStateOf(false) }

    val actionsList = remember(data) {
        if (data.corporateActions.isNotEmpty()) {
            data.corporateActions
        } else {
            buildDefaultCorporateActions(
                symbol = data.symbol,
                region = region,
                lastDividendPerShare = data.lastDividendPerShare,
                exDividendDate = data.exDividendDate,
                nextEarningsDate = data.nextEarningsDate
            )
        }
    }

    InstitutionalCardShell(
        title = "CORPORATE ACTIONS & EVENTS CALENDAR",
        subtitle = "Dividend Profile, Earnings Schedule & Split / Bonus Tracker",
        icon = Icons.Default.CalendarMonth,
        accentColor = accentColor,
        badgeText = "${actionsList.size} EVENTS",
        modifier = modifier
    ) {
        // 1. Dividend Matrix (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InstitutionalMetricCell(
                label = "DIVIDEND YIELD (%)",
                value = InstitutionalFormatters.formatPercentage(data.dividendYieldPct),
                subValue = "Annualized Cash Yield",
                valueColor = if ((data.dividendYieldPct ?: 0.0) >= 1.5) {
                    TerminalThemeTokens.BullishEmerald
                } else {
                    TerminalThemeTokens.TextPrimarySlate
                },
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "LAST DIVIDEND PAYOUT",
                value = InstitutionalFormatters.formatPerShareAmount(
                    data.lastDividendPerShare,
                    region
                ),
                subValue = "Per Equity Share (${region.currencySymbol})",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InstitutionalMetricCell(
                label = "EX-DIVIDEND DATE",
                value = data.exDividendDate?.takeIf { it.isNotBlank() } ?: "TBA",
                subValue = "T+1 Settlement Record",
                modifier = Modifier.weight(1f)
            )
            InstitutionalMetricCell(
                label = "DIVIDEND PAYOUT RATIO",
                value = InstitutionalFormatters.formatPercentage(data.dividendPayoutRatioPct),
                subValue = "% of Net Profit Distributed",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Earnings Calendar & Next Board Meeting Banner
        EarningsCalendarBanner(
            nextEarningsDate = data.nextEarningsDate,
            boardMeetingPurpose = data.nextBoardMeetingPurpose,
            fiscalQuarterLabel = data.fiscalQuarterLabel,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 1.dp, color = TerminalThemeTokens.BorderSubtleSlate)
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Corporate Actions Tracker Header (Splits, Bonus, Dividends, Record Dates)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CORPORATE ACTIONS TRACKER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
            }

            Text(
                text = "EX-DATE / RECORD DATE",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = TerminalThemeTokens.TextSecondarySlate
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visible items (first 3 by default, expandable if > 3)
        val primaryItems = remember(actionsList) { actionsList.take(3) }
        val extraItems = remember(actionsList) {
            if (actionsList.size > 3) actionsList.drop(3) else emptyList()
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            primaryItems.forEach { action ->
                CorporateActionTrackerRow(
                    event = action,
                    accentColor = accentColor,
                    onClick = onActionClick
                )
            }

            AnimatedVisibility(
                visible = showAllActions && extraItems.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    extraItems.forEach { action ->
                        CorporateActionTrackerRow(
                            event = action,
                            accentColor = accentColor,
                            onClick = onActionClick
                        )
                    }
                }
            }
        }

        if (extraItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalThemeTokens.BackgroundIceMist)
                    .clickable { showAllActions = !showAllActions }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAllActions) {
                        "COLLAPSE ARCHIVED ACTIONS"
                    } else {
                        "VIEW ${extraItems.size} MORE CORPORATE ACTIONS"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (showAllActions) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EarningsCalendarBanner(
    nextEarningsDate: String?,
    boardMeetingPurpose: String?,
    fiscalQuarterLabel: String?,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalThemeTokens.BenchmarkAmberTint)
            .border(
                width = 1.dp,
                color = TerminalThemeTokens.BenchmarkAmberBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalThemeTokens.BenchmarkAmberGold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = TerminalThemeTokens.BenchmarkAmberDeep,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "EARNINGS & BOARD CALENDAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = TerminalThemeTokens.BenchmarkAmberDeep
                    )
                    if (!fiscalQuarterLabel.isNullOrBlank()) {
                        Surface(
                            color = TerminalThemeTokens.SurfaceWhite,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, TerminalThemeTokens.BenchmarkAmberBorder)
                        ) {
                            Text(
                                text = fiscalQuarterLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalThemeTokens.TextPrimarySlate,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = boardMeetingPurpose?.takeIf { it.isNotBlank() }
                        ?: "Quarterly Financial Results & Board Review",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalThemeTokens.TextPrimarySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "ANNOUNCEMENT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalThemeTokens.TextSecondarySlate
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nextEarningsDate?.takeIf { it.isNotBlank() } ?: "TBA",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.TextPrimarySlate
            )
        }
    }
}

@Composable
private fun CorporateActionTrackerRow(
    event: CorporateActionEvent,
    accentColor: Color,
    onClick: ((CorporateActionEvent) -> Unit)? = null
) {
    val (badgeBg, badgeFg, badgeBorder) = when (event.actionType) {
        CorporateActionType.DIVIDEND -> Triple(
            TerminalThemeTokens.BullishTint,
            TerminalThemeTokens.BullishEmerald,
            TerminalThemeTokens.BullishBorder
        )
        CorporateActionType.STOCK_SPLIT, CorporateActionType.BONUS -> Triple(
            TerminalThemeTokens.MeasureRulerTint,
            TerminalThemeTokens.MeasureRulerPurple,
            TerminalThemeTokens.MeasureRulerBorder
        )
        CorporateActionType.EARNINGS -> Triple(
            TerminalThemeTokens.BenchmarkAmberTint,
            TerminalThemeTokens.BenchmarkAmberDeep,
            TerminalThemeTokens.BenchmarkAmberBorder
        )
        CorporateActionType.RIGHTS, CorporateActionType.AGM_EGM -> Triple(
            TerminalThemeTokens.BackgroundIceMist,
            accentColor,
            TerminalThemeTokens.BorderSubtleSlate
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(TerminalThemeTokens.BackgroundIceMist)
            .border(1.dp, TerminalThemeTokens.BorderSubtleSlate, RoundedCornerShape(9.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(event) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Action Type Pill + Headline + Ratio/Amount
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, badgeBorder)
            ) {
                Text(
                    text = event.actionType.label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = event.headline,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalThemeTokens.TextPrimarySlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.ratioOrAmount.isNotBlank()) {
                        Text(
                            text = "• ${event.ratioOrAmount}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = badgeFg
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Record Date: ${event.recordDate} • ${event.statusLabel}",
                    fontSize = 10.sp,
                    color = TerminalThemeTokens.TextSecondarySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Ex-Date / Effective Date
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = event.exOrEffectiveDate,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.TextPrimarySlate
            )
            Text(
                text = if (event.isConfirmed) "CONFIRMED" else "ESTIMATED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (event.isConfirmed) {
                    TerminalThemeTokens.BullishEmerald
                } else {
                    TerminalThemeTokens.BenchmarkAmberDeep
                }
            )
        }
    }
}

// ============================================================================
// REUSABLE INSTITUTIONAL DESIGN SYSTEM BUILDING BLOCKS
// ============================================================================

@Composable
private fun InstitutionalMatrixSectionHeader(
    symbol: String,
    region: EquityMarketRegion,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "INSTITUTIONAL DEEP-DIVE MATRIX",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = TerminalThemeTokens.TextPrimarySlate
                )
                Text(
                    text = if (symbol.isNotBlank()) {
                        "$symbol • Fundamental, Solvency & Corporate Event Ledger"
                    } else {
                        "Fundamental, Solvency & Corporate Event Ledger"
                    },
                    fontSize = 10.5.sp,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
            }
        }

        Surface(
            color = TerminalThemeTokens.SurfaceWhite,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate)
        ) {
            Text(
                text = if (region == EquityMarketRegion.INDIA) "NSE / BSE (₹ Cr)" else "US MARKET (\$B)",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.TextSecondarySlate,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun InstitutionalCardShell(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    modifier: Modifier = Modifier,
    badgeIsPositive: Boolean? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalThemeTokens.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, TerminalThemeTokens.BorderSubtleSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            color = TerminalThemeTokens.TextPrimarySlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.5.sp,
                            color = TerminalThemeTokens.TextSecondarySlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val badgeBg = when (badgeIsPositive) {
                    true -> TerminalThemeTokens.BullishTint
                    false -> TerminalThemeTokens.BearishTint
                    null -> TerminalThemeTokens.BackgroundIceMist
                }
                val badgeFg = when (badgeIsPositive) {
                    true -> TerminalThemeTokens.BullishEmerald
                    false -> TerminalThemeTokens.BearishCrimson
                    null -> TerminalThemeTokens.TextSecondarySlate
                }
                val badgeBorder = when (badgeIsPositive) {
                    true -> TerminalThemeTokens.BullishBorder
                    false -> TerminalThemeTokens.BearishBorder
                    null -> TerminalThemeTokens.BorderSubtleSlate
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = TerminalThemeTokens.BorderSubtleSlate)
            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
fun InstitutionalMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    valueColor: Color = TerminalThemeTokens.TextPrimarySlate,
    subValueColor: Color = TerminalThemeTokens.TextSecondarySlate
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(TerminalThemeTokens.BackgroundIceMist)
            .border(
                width = 1.dp,
                color = TerminalThemeTokens.BorderSubtleSlate,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TerminalThemeTokens.TextSecondarySlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subValue.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = subValueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// AUXILIARY TERMINAL HUD & TOOLBAR COMPONENTS (BACKWARD COMPATIBILITY)
// ============================================================================

/**
 * Measure / Ruler HUD pill displaying 2-finger price & percentage delta in Argon 4 Purple (#7C3AED).
 */
@Composable
fun TerminalMeasureRulerBanner(
    startPrice: Double,
    endPrice: Double,
    startTimestampLabel: String,
    endTimestampLabel: String,
    barsCount: Int,
    region: EquityMarketRegion = EquityMarketRegion.INDIA,
    modifier: Modifier = Modifier
) {
    val priceDelta = endPrice - startPrice
    val pctDelta = if (startPrice > 0.0) (priceDelta / startPrice) * 100.0 else 0.0
    val isPositive = priceDelta >= 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TerminalThemeTokens.MeasureRulerTint,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, TerminalThemeTokens.MeasureRulerBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TerminalThemeTokens.MeasureRulerPurple)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RULER MEASURE ($barsCount BARS)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TerminalThemeTokens.MeasureRulerPurple
                    )
                    Text(
                        text = "$startTimestampLabel → $endTimestampLabel",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalThemeTokens.TextSecondarySlate
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${InstitutionalFormatters.formatPercentage(pctDelta, includePlusSign = true)} (${ if (isPositive) "+" else "-" }${InstitutionalFormatters.formatPrice(abs(priceDelta), region)})",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPositive) {
                        TerminalThemeTokens.BullishEmerald
                    } else {
                        TerminalThemeTokens.BearishCrimson
                    }
                )
                Text(
                    text = "${InstitutionalFormatters.formatPrice(startPrice, region)} →${
                        InstitutionalFormatters.formatPrice(endPrice, region)
                    }",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalThemeTokens.TextSecondarySlate
                )
            }
        }
    }
}

/**
 * Benchmark comparison pill (NIFTY 50 Frozen Amber Gold #F59E0B / #D97706).
 */
@Composable
fun BenchmarkAlphaComparisonBadge(
    instrumentReturnPct: Double,
    benchmarkName: String = "NIFTY 50",
    benchmarkReturnPct: Double,
    modifier: Modifier = Modifier
) {
    val alphaPct = instrumentReturnPct - benchmarkReturnPct
    val isOutperforming = alphaPct >= 0.0

    Surface(
        modifier = modifier,
        color = TerminalThemeTokens.BenchmarkAmberTint,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TerminalThemeTokens.BenchmarkAmberBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TerminalThemeTokens.BenchmarkAmberGold)
            )
            Text(
                text = "$benchmarkName:${
                    InstitutionalFormatters.formatPercentage(benchmarkReturnPct, includePlusSign = true)
                }",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TerminalThemeTokens.BenchmarkAmberDeep
            )
            Text(
                text = "|",
                fontSize = 10.sp,
                color = TerminalThemeTokens.BorderSubtleSlate
            )
            Text(
                text = "ALPHA: ${InstitutionalFormatters.formatPercentage(alphaPct, includePlusSign = true)}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isOutperforming) {
                    TerminalThemeTokens.BullishEmerald
                } else {
                    TerminalThemeTokens.BearishCrimson
                }
            )
        }
    }
}

// ============================================================================
// DEFAULT CORPORATE ACTION SYNTHESIZER (FALLBACK WHEN UPSTREAM LIST IS EMPTY)
// ============================================================================

private fun buildDefaultCorporateActions(
    symbol: String,
    region: EquityMarketRegion,
    lastDividendPerShare: Double?,
    exDividendDate: String?,
    nextEarningsDate: String?
): List<CorporateActionEvent> {
    val cleanTicker = symbol.ifBlank { "EQUITY" }
    val divAmountLabel = if (lastDividendPerShare != null && lastDividendPerShare > 0.0) {
        String.format(Locale.US, "%s%.2f / Share", region.currencySymbol, lastDividendPerShare)
    } else {
        "${region.currencySymbol}— / Share"
    }

    return listOf(
        CorporateActionEvent(
            id = "${cleanTicker}_div",
            actionType = CorporateActionType.DIVIDEND,
            headline = "Final / Interim Cash Dividend",
            ratioOrAmount = divAmountLabel,
            exOrEffectiveDate = exDividendDate?.takeIf { it.isNotBlank() } ?: "TBA",
            recordDate = exDividendDate?.takeIf { it.isNotBlank() } ?: "T+1 Settlement",
            statusLabel = if (!exDividendDate.isNullOrBlank()) "DECLARED" else "SCHEDULED",
            isConfirmed = !exDividendDate.isNullOrBlank()
        ),
        CorporateActionEvent(
            id = "${cleanTicker}_earnings",
            actionType = CorporateActionType.EARNINGS,
            headline = "Board Meeting — Quarterly Financial Results",
            ratioOrAmount = "Qtr Audit",
            exOrEffectiveDate = nextEarningsDate?.takeIf { it.isNotBlank() } ?: "TBA",
            recordDate = "Statutory Filing",
            statusLabel = "UPCOMING",
            isConfirmed = !nextEarningsDate.isNullOrBlank()
        ),
        CorporateActionEvent(
            id = "${cleanTicker}_split_bonus",
            actionType = CorporateActionType.BONUS,
            headline = "Split / Bonus Issue & Capital Restructuring",
            ratioOrAmount = "No Active Split",
            exOrEffectiveDate = "—",
            recordDate = "Exchange Registry",
            statusLabel = "MONITORING",
            isConfirmed = true
        )
    )
}