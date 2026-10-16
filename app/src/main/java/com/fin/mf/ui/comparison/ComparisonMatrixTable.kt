package com.fin.mf.ui.comparison

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.data.FundComparisonItem
import com.fin.mf.data.engine.PortfolioAnalyticsEngine
import java.util.Locale
import kotlin.math.roundToInt

private val CmpSlatePrimary = Color(0xFF0F172A)
private val CmpSlateSecondary = Color(0xFF475569)
private val CmpSlateBorder = Color(0xFFE2E8F0)
private val CmpAccentRoyal = Color(0xFF2563EB)

fun formatCagr(cagr: Double?): String {
    if (cagr == null) return "N/A"
    val sign = if (cagr >= 0) "+" else ""
    val rounded = (cagr * 100.0).roundToInt() / 100.0
    return "$sign$rounded%"
}

@Composable
fun MatrixRow(label: String, totalWidthDp: Int, values: List<String>) {
    Row(
        modifier = Modifier
            .width(totalWidthDp.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CmpSlateSecondary,
            modifier = Modifier.width(120.dp)
        )
        values.forEach { v ->
            Text(
                text = v,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CmpSlatePrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

/**
 * Side-by-side factor matrix table comparing key metrics across up to 5 funds with horizontal scrolling.
 */
@Composable
fun ComparisonMatrixTable(funds: List<FundComparisonItem>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.width((120 + funds.size * 120).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metric",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CmpSlateSecondary,
                modifier = Modifier.width(120.dp)
            )
            funds.forEachIndexed { i, fund ->
                val color = ComparisonFundLineColors.getOrElse(i) { CmpAccentRoyal }
                Text(
                    text = fund.schemeName.take(15),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        HorizontalDivider(color = CmpSlateBorder)

        MatrixRow(
            label = "Latest NAV",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { "₹" + String.format(Locale.US, "%.2f", it.latestNav) }
        )

        MatrixRow(
            label = "1-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr1Y) }
        )

        MatrixRow(
            label = "3-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr3Y) }
        )

        MatrixRow(
            label = "5-Year CAGR",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { formatCagr(it.cagr5Y) }
        )

        MatrixRow(
            label = "Expense Ratio",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { "${it.expenseRatio}%" }
        )

        MatrixRow(
            label = "Risk Level",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { it.riskLevel }
        )

        MatrixRow(
            label = "Category",
            totalWidthDp = 120 + funds.size * 120,
            values = funds.map { it.category }
        )

        // Holdings overlap compared to Fund #1 using PortfolioAnalyticsEngine
        val overlapValues = remember(funds) {
            if (funds.size >= 2) {
                val fund0Holdings = funds[0].topHoldings
                funds.mapIndexed { idx, targetFund ->
                    if (idx == 0) {
                        "100%"
                    } else {
                        val overlapPct = PortfolioAnalyticsEngine.computeHoldingsOverlap(fund0Holdings, targetFund.topHoldings)
                        val rounded = (overlapPct * 10.0).roundToInt() / 10.0
                        "$rounded%"
                    }
                }
            } else funds.map { "N/A" }
        }

        MatrixRow(
            label = "Overlap vs Fund #1",
            totalWidthDp = 120 + funds.size * 120,
            values = overlapValues
        )
    }
}
