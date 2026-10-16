package com.fin.mf.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.MutualFundScheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// Base neutral and semantic colors for workspace components
val WsSurfaceWhite = Color(0xFFFFFFFF)
val WsSlateBorder = Color(0xFFE2E8F0)
val WsSlateTextPrimary = Color(0xFF0F172A)
val WsSlateTextSecondary = Color(0xFF475569)
val WsPrimaryIndigo = Color(0xFF1E3A8A)
val WsAccentRoyal = Color(0xFF2563EB)
val WsEmeraldPositive = SemanticFinanceColors.ProfitGreen
val WsCrimsonNegative = SemanticFinanceColors.LossRed

val TopLevelInrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

fun formatWsInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatted = TopLevelInrFormatter.format(amount)
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

fun formatWsPercent(pct: Double): String {
    val rounded = (pct * 100.0).roundToInt() / 100.0
    val sign = if (rounded >= 0.0) "+" else ""
    return "$sign$rounded%"
}

fun wsSliceColor(index: Int): Color {
    val palette = listOf(
        Color(0xFF1E3A8A), // Indigo
        Color(0xFF2563EB), // Royal
        Color(0xFF059669), // Emerald
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED), // Purple
        Color(0xFFDC2626), // Crimson
        Color(0xFF0D9488), // Teal
        Color(0xFF64748B)  // Slate
    )
    return palette[index % palette.size]
}

@Composable
fun WsElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun WsMetricPillar(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color = WsSlateTextSecondary
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun WsAllocationBar(slices: List<AllocationSlice>) {
    if (slices.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        slices.forEachIndexed { index, slice ->
            val weightFloat = (slice.weightPercent.toFloat() / 100f).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(weightFloat)
                    .fillMaxSize()
                    .background(wsSliceColor(index))
            )
        }
    }
}

/**
 * Modern institutional mutual fund summary card:
 * Row 1: Current Value | Net Gain %
 * Row 2: Invested | Latest NAV
 */
@Composable
fun WsSchemeSummaryCard(
    scheme: MutualFundScheme,
    isMasked: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isGain = scheme.absoluteGain >= 0

    WsElevatedCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category, Asset Class, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = scheme.assetClass,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsPrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = scheme.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = WsSlateTextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Scheme",
                        tint = WsSlateTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scheme.schemeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = WsSlateBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Clean 2x2 Grid Layout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Current Value | Net Gain / Return
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Value",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.currentValue, isMasked),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WsSlateTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Gain",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isGain) WsEmeraldPositive else WsCrimsonNegative,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formatWsPercent(scheme.absoluteReturnPercent),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGain) WsEmeraldPositive else WsCrimsonNegative
                            )
                        }
                    }
                }

                // Row 2: Invested | Latest NAV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Invested Capital",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.investedAmount, isMasked),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WsSlateTextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Latest NAV (${scheme.lastNavDate})",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsAccentRoyal
                        )
                    }
                }
            }
        }
    }
}
