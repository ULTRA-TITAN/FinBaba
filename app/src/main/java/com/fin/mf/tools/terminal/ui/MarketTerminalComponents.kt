package com.fin.mf.tools.terminal.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.tools.terminal.data.TerminalAssetQuote
import com.fin.mf.tools.terminal.data.TerminalChartPoint
import com.fin.mf.tools.terminal.data.TerminalTimeframe
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// Institutional Design System Palette
internal val TerminalBg = Color(0xFFF8FAFC)
internal val TerminalSurface = Color(0xFFFFFFFF)
internal val TerminalPrimaryIndigo = Color(0xFF1E3A8A)
internal val TerminalAccentRoyal = Color(0xFF2563EB)
internal val TerminalEmeraldPositive = Color(0xFF059669)
internal val TerminalEmeraldSoft = Color(0xFFECFDF5)
internal val TerminalCrimsonNegative = Color(0xFFDC2626)
internal val TerminalCrimsonSoft = Color(0xFFFEF2F2)
internal val TerminalAmberBenchmark = Color(0xFFD97706)
internal val TerminalAmberSoft = Color(0xFFFFFBEB)
internal val TerminalPurplePointer = Color(0xFF7C3AED)
internal val TerminalSlatePrimary = Color(0xFF0F172A)
internal val TerminalSlateSecondary = Color(0xFF475569)
internal val TerminalSlateBorder = Color(0xFFE2E8F0)

internal val TopLevelInrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}
internal val TopLevelUsdFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 2
}

internal val TerminalColorScheme = lightColorScheme(
    primary = TerminalPrimaryIndigo,
    onPrimary = Color.White,
    secondary = TerminalAccentRoyal,
    onSecondary = Color.White,
    background = TerminalBg,
    onBackground = TerminalSlatePrimary,
    surface = TerminalSurface,
    onSurface = TerminalSlatePrimary
)

@Composable
internal fun HeroQuoteCard(
    quote: TerminalAssetQuote?,
    selectedSymbol: String,
    scrubbedPoint: TerminalChartPoint?,
    selectedTimeframe: TerminalTimeframe
) {
    val gradient = LocalVaultGradient.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Symbol + Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote?.longName ?: selectedSymbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TerminalSlatePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${quote?.symbol ?: selectedSymbol} • ${quote?.currency ?: "INR"}",
                        fontSize = 12.sp,
                        color = TerminalSlateSecondary
                    )
                }

                if (quote != null) {
                    val isPositive = quote.dayChange >= 0
                    Surface(
                        color = if (isPositive) TerminalEmeraldSoft else TerminalCrimsonSoft,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${roundTwo(quote.dayChangePercent)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big Price display (or scrubbed price)
            val displayPrice = scrubbedPoint?.price
                ?: quote?.regularMarketPrice
                ?: 0.0
            val currency = quote?.currency ?: "INR"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatAssetPrice(displayPrice, currency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TerminalSlatePrimary
                    )
                    if (scrubbedPoint != null) {
                        Text(
                            text = "Scrubbed at ${scrubbedPoint.dateFormatted}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TerminalAccentRoyal
                        )
                    } else if (quote != null) {
                        val isPositive = quote.dayChange >= 0
                        Text(
                            text = "${if (isPositive) "+" else ""}${formatAssetPrice(quote.dayChange, currency)} Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPositive) TerminalEmeraldPositive else TerminalCrimsonNegative
                        )
                    }
                }

                if (quote != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${selectedTimeframe.label} Return",
                            fontSize = 11.sp,
                            color = TerminalSlateSecondary
                        )
                        val periodReturn = quote.periodReturnPercent
                        val isPeriodPos = periodReturn >= 0
                        Text(
                            text = "${if (isPeriodPos) "+" else ""}${roundTwo(periodReturn)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPeriodPos) TerminalEmeraldPositive else TerminalCrimsonNegative
                        )
                    }
                }
            }

            if (quote != null && quote.dayHigh > quote.dayLow) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = TerminalSlateBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Intraday Range Meter with marker needle
                RangeMeter(
                    label = "Today's Range",
                    current = quote.regularMarketPrice,
                    low = quote.dayLow,
                    high = quote.dayHigh,
                    currency = currency
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 52-Week Range Meter
                if (quote.fiftyTwoWeekHigh > quote.fiftyTwoWeekLow) {
                    RangeMeter(
                        label = "52-Week Range",
                        current = quote.regularMarketPrice,
                        low = quote.fiftyTwoWeekLow,
                        high = quote.fiftyTwoWeekHigh,
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
internal fun InstitutionalStatsCard(quote: TerminalAssetQuote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Trading & Valuation Metrics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalSlatePrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Prev Close",
                    value = formatAssetPrice(quote.previousClose, quote.currency)
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Open",
                    value = formatAssetPrice(quote.open, quote.currency)
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Volume",
                    value = formatCompactVolume(quote.volume)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TerminalSlateBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Day High",
                    value = formatAssetPrice(quote.dayHigh, quote.currency)
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Day Low",
                    value = formatAssetPrice(quote.dayLow, quote.currency)
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label = "Exchange",
                    value = quote.exchange
                )
            }
        }
    }
}

@Composable
internal fun RangeMeter(
    label: String,
    current: Double,
    low: Double,
    high: Double,
    currency: String
) {
    val gradient = LocalVaultGradient.current
    val fraction = if (high > low) {
        ((current - low) / (high - low)).toFloat().coerceIn(0f, 1f)
    } else 0.5f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = TerminalSlateSecondary)
            Text(
                text = "Current: ${formatAssetPrice(current, currency)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TerminalSlatePrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TerminalSlateBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(gradient.startColor)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "L: ${formatAssetPrice(low, currency)}",
                fontSize = 10.sp,
                color = TerminalSlateSecondary
            )
            Text(
                text = "H: ${formatAssetPrice(high, currency)}",
                fontSize = 10.sp,
                color = TerminalSlateSecondary
            )
        }
    }
}

@Composable
internal fun StatCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = TerminalSlateSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalSlatePrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatAssetPrice(amount: Double, currency: String, showPlus: Boolean = false): String {
    val formatted = if (currency.equals("USD", ignoreCase = true)) {
        TopLevelUsdFormatter.format(amount)
    } else {
        TopLevelInrFormatter.format(amount)
    }
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

internal fun formatCompactVolume(volume: Long): String {
    return when {
        volume >= 10_000_000 -> "${roundTwo(volume / 10_000_000.0)} Cr"
        volume >= 100_000 -> "${roundTwo(volume / 100_000.0)} L"
        volume >= 1_000 -> "${roundTwo(volume / 1_000.0)} K"
        else -> volume.toString()
    }
}

internal fun roundTwo(num: Double): Double {
    return (num * 100.0).roundToInt() / 100.0
}
