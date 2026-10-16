package com.fin.mf.ui.components

import androidx.compose.ui.graphics.Color
import com.fin.mf.data.ChartTimeframe
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Top-level cached colors and formatters for 60/120 FPS performance
internal val ChartPrimaryIndigo = Color(0xFF1E3A8A)
internal val ChartAccentRoyal = Color(0xFF2563EB)
internal val ChartEmeraldPositive = Color(0xFF059669)
internal val ChartEmeraldSoft = Color(0xFFECFDF5)
internal val ChartCrimsonNegative = Color(0xFFDC2626)
internal val ChartCrimsonSoft = Color(0xFFFEF2F2)
internal val ChartAmberAccent = Color(0xFFD97706)
internal val ChartSlateTextPrimary = Color(0xFF0F172A)
internal val ChartSlateTextSecondary = Color(0xFF475569)
internal val ChartSlateBorder = Color(0xFFE2E8F0)
internal val ChartSurfaceWhite = Color(0xFFFFFFFF)
internal val ChartBgLight = Color(0xFFF8FAFC)

internal val TopLevelCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}

data class NavChartPoint(
    val dateString: String,
    val epochMs: Long,
    val nav: Double
)

internal fun filterPointsByTimeframe(
    allPoints: List<NavChartPoint>,
    timeframe: ChartTimeframe,
    customRange: ClosedFloatingPointRange<Float>
): List<NavChartPoint> {
    if (allPoints.size < 2) return allPoints

    return when (timeframe) {
        ChartTimeframe.CUSTOM -> {
            val startIdx = (customRange.start * allPoints.lastIndex).roundToInt().coerceIn(0, allPoints.lastIndex)
            val endIdx = (customRange.endInclusive * allPoints.lastIndex).roundToInt().coerceIn(startIdx + 1, allPoints.lastIndex)
            allPoints.subList(startIdx, (endIdx + 1).coerceAtMost(allPoints.size))
        }
        ChartTimeframe.YTD -> {
            val calendar = Calendar.getInstance()
            val latestEpoch = allPoints.last().epochMs
            calendar.timeInMillis = latestEpoch
            val currentYear = calendar.get(Calendar.YEAR)

            val ytdPoints = allPoints.filter { pt ->
                val c = Calendar.getInstance()
                c.timeInMillis = pt.epochMs
                c.get(Calendar.YEAR) == currentYear
            }
            if (ytdPoints.size >= 2) ytdPoints else allPoints.takeLast(30)
        }
        else -> {
            val days = timeframe.calendarDays
            val msThreshold = days.toLong() * 86_400_000L
            val latestEpoch = allPoints.last().epochMs
            val cutoff = latestEpoch - msThreshold
            val filtered = allPoints.filter { it.epochMs >= cutoff }
            if (filtered.size >= 2) {
                filtered
            } else {
                allPoints.takeLast(days.coerceAtLeast(2))
            }
        }
    }
}
