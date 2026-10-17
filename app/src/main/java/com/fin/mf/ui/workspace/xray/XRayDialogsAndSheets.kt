package com.fin.mf.ui.workspace.xray

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale

/**
 * Bottom Sheet displaying the full factor-by-factor breakdown of the Health Scorecard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScoreBreakdownSheet(
    diagnostic: PortfolioHealthDiagnostic,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = diagnostic.ratingColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Health Score Breakdown",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = "Institutional Multi-Factor Scoring Formula",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Hero Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = diagnostic.ratingColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, diagnostic.ratingColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OVERALL RATING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = diagnostic.ratingColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = diagnostic.rating,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = WsSlateTextPrimary
                        )
                    }
                    Text(
                        text = "${diagnostic.score}/100",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = diagnostic.ratingColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Factor Deductions & Criteria:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Factor 1: Base Score
            FactorRow(
                title = "Base Institutional Quality",
                detail = "Initial reference benchmark",
                ptsText = "+100 pts",
                isDeduction = false,
                iconColor = SemanticFinanceColors.ProfitGreen
            )

            // Factor 2: Duplicate Overlap
            val overlapDeduct = diagnostic.duplicateOverlapDeduction
            FactorRow(
                title = "Duplicate Holdings Commonality",
                detail = "${diagnostic.duplicateOverlapPct}% overlap across funds (subtract 1 pt per 2% above 30%)",
                ptsText = if (overlapDeduct > 0) "-$overlapDeduct pts" else "0 pts (Compliant)",
                isDeduction = overlapDeduct > 0,
                iconColor = if (overlapDeduct > 0) SemanticFinanceColors.BenchmarkAmber else SemanticFinanceColors.ProfitGreen
            )

            // Factor 3: Single Stock Concentration
            val stockDeduct = diagnostic.singleStockConcentrationDeduction
            FactorRow(
                title = "Single Counterparty Concentration",
                detail = "Max holding ${diagnostic.maxStockTicker} is ${diagnostic.maxStockConcentrationPct}% (threshold: 10%)",
                ptsText = if (stockDeduct > 0) "-$stockDeduct pts" else "0 pts (Compliant)",
                isDeduction = stockDeduct > 0,
                iconColor = if (stockDeduct > 0) SemanticFinanceColors.LossRed else SemanticFinanceColors.ProfitGreen
            )

            // Factor 4: Market Cap Drift
            val driftDeduct = diagnostic.marketCapDriftDeduction
            FactorRow(
                title = "SEBI Market Cap Mandate Drift",
                detail = "Max drift in ${diagnostic.maxDriftTier}: ${diagnostic.maxDriftPct}% vs target (threshold: ±8%)",
                ptsText = if (driftDeduct > 0) "-$driftDeduct pts" else "0 pts (Compliant)",
                isDeduction = driftDeduct > 0,
                iconColor = if (driftDeduct > 0) SemanticFinanceColors.BenchmarkAmber else SemanticFinanceColors.ProfitGreen
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WsPrimaryIndigo)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FactorRow(
    title: String,
    detail: String,
    ptsText: String,
    isDeduction: Boolean,
    iconColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = detail,
                        fontSize = 10.sp,
                        color = WsSlateTextSecondary
                    )
                }
            }
            Text(
                text = ptsText,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDeduction) SemanticFinanceColors.LossRed else SemanticFinanceColors.ProfitGreen
            )
        }
    }
}

/**
 * Universal Drill-Down Bottom Sheet for constituents (Market Cap tiers, Index Matrix, Sectors, or Overlap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstituentDrillDownSheet(
    title: String,
    subtitle: String,
    constituents: List<AggregatedStockExposure>,
    isMasked: Boolean,
    onSchemeClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (constituents.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "No constituent instruments found in this category.",
                        fontSize = 13.sp,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = constituents, key = { it.ticker }) { stock ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFFEFF6FF),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stock.ticker,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = WsPrimaryIndigo,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${stock.heldInSchemesCount} Funds",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (stock.heldInSchemesCount >= 3) SemanticFinanceColors.BenchmarkAmber else WsSlateTextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stock.companyName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsSlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stock.sector} • ${stock.marketCapBucket}",
                                            fontSize = 10.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatWsInr(stock.totalRupeeExposure, isMasked),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsSlateTextPrimary
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f%%", stock.portfolioWeightPercent),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WsPrimaryIndigo
                                        )
                                    }
                                }

                                if (isExpanded && stock.schemeContributions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = WsSlateBorder)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Holding Fund Contributions:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WsSlateTextSecondary
                                    )
                                    stock.schemeContributions.forEach { contribution ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSchemeClick(contribution.schemeCode) }
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${contribution.schemeName.take(30)}...",
                                                fontSize = 11.sp,
                                                color = WsPrimaryIndigo,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f%%", contribution.weightInSchemePercent)} (${formatWsInr(contribution.rupeeContribution, isMasked)})",
                                                fontSize = 10.sp,
                                                color = WsSlateTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * What-If Inflow Rebalance Simulator Modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatIfInflowRebalanceModal(
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val gradient = LocalVaultGradient.current

    var inflowText by remember { mutableStateOf("50000") }
    val inflowVal = inflowText.toDoubleOrNull() ?: 50000.0

    val plan = remember(inflowVal, analytics) {
        XRayDiagnosticsEngine.simulateInflowRebalance(analytics, inflowVal)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = gradient.startColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "What-If Inflow Rebalancer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = "Simulate capital injection to eliminate SEBI mandate drift",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Simulated Inflow Amount (INR):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WsSlateTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = inflowText,
                onValueChange = { inflowText = it.filter { char -> char.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = WsSlateTextPrimary) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25000, 50000, 100000, 250000).forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { inflowText = preset.toString() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (inflowText == preset.toString()) gradient.startColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (inflowText == preset.toString()) gradient.startColor else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            text = "₹${preset / 1000}k",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inflowText == preset.toString()) gradient.startColor else WsSlateTextSecondary,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recommended Capital Allocation:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 3 Tier recommendation rows
            RebalanceTierRow(
                label = "Large Cap (50% Target)",
                color = SemanticFinanceColors.SebiLargeCap,
                allocRupees = plan.allocatedLargeRupees,
                currentPct = plan.currentLargePct,
                projectedPct = plan.projectedLargePct,
                isMasked = isMasked
            )
            RebalanceTierRow(
                label = "Mid Cap (25% Target)",
                color = SemanticFinanceColors.SebiMidCap,
                allocRupees = plan.allocatedMidRupees,
                currentPct = plan.currentMidPct,
                projectedPct = plan.projectedMidPct,
                isMasked = isMasked
            )
            RebalanceTierRow(
                label = "Small Cap (25% Target)",
                color = SemanticFinanceColors.SebiSmallCap,
                allocRupees = plan.allocatedSmallRupees,
                currentPct = plan.currentSmallPct,
                projectedPct = plan.projectedSmallPct,
                isMasked = isMasked
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Inflow rebalance plan saved! Deploy funds accordingly in your active schemes.",
                        Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Commit Inflow Plan", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RebalanceTierRow(
    label: String,
    color: Color,
    allocRupees: Double,
    currentPct: Double,
    projectedPct: Double,
    isMasked: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "Current: ${String.format(Locale.US, "%.1f%%", currentPct)} ➔ Projected: ${String.format(Locale.US, "%.1f%%", projectedPct)}",
                        fontSize = 10.sp,
                        color = WsSlateTextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${formatWsInr(allocRupees, isMasked)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = "Recommended",
                    fontSize = 9.sp,
                    color = WsSlateTextSecondary
                )
            }
        }
    }
}
