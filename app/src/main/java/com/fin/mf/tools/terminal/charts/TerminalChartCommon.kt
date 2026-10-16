package com.fin.mf.tools.terminal.charts

import androidx.compose.ui.graphics.Color
import com.fin.mf.core.theme.SemanticFinanceColors
import java.util.Locale
import kotlin.math.roundToInt

val TerminalSlatePrimary = Color(0xFF0F172A)
val TerminalSlateSecondary = Color(0xFF475569)
val TerminalSlateBorder = Color(0xFFE2E8F0)
val TerminalBg = Color(0xFFF8FAFC)

// Locked Frozen Semantic Financial Colors
val TerminalEmeraldPositive = SemanticFinanceColors.ProfitGreen
val TerminalEmeraldSoft = SemanticFinanceColors.ProfitGreenBackground
val TerminalCrimsonNegative = SemanticFinanceColors.LossRed
val TerminalCrimsonSoft = SemanticFinanceColors.LossRedBackground
val TerminalAmberBenchmark = SemanticFinanceColors.BenchmarkAmber
val TerminalPurplePointer = SemanticFinanceColors.PortfolioPurple

fun roundTwo(num: Double): Double = (num * 100.0).roundToInt() / 100.0

fun formatAssetPrice(amount: Double, currency: String, showPlus: Boolean = false): String {
    val symbol = if (currency.equals("INR", ignoreCase = true)) "₹" else "$"
    val prefix = if (showPlus && amount > 0) "+" else ""
    return "$prefix$symbol${String.format(Locale.US, "%.2f", amount)}"
}
