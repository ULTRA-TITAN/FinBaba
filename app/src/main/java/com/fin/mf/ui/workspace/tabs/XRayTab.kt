package com.fin.mf.ui.workspace.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.pdf.PortfolioAuditPdfGenerator
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.SectorExposure
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.formatWsInr
import com.fin.mf.ui.workspace.components.formatWsPercent
import com.fin.mf.ui.workspace.xray.BenchmarkIndexRow
import com.fin.mf.ui.workspace.xray.ConstituentDrillDownSheet
import com.fin.mf.ui.workspace.xray.HealthScoreBreakdownSheet
import com.fin.mf.ui.workspace.xray.PortfolioHealthDiagnostic
import com.fin.mf.ui.workspace.xray.WhatIfInflowRebalanceModal
import com.fin.mf.ui.workspace.xray.XRayDiagnosticsEngine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val WsAmberWarning = SemanticFinanceColors.BenchmarkAmber
private val WsAmberSoft = Color(0xFFFEF3C7)
private val WsRedSoft = Color(0xFFFEF2F2)
private val WsEmeraldSoft = Color(0xFFECFDF5)

/**
 * Tab #3: Comprehensive Institutional Portfolio X-Ray & Health Diagnostic Workspace.
 */
@Composable
fun WorkspaceXRayTab(
    analytics: PortfolioAnalyticsSummary?,
    isMasked: Boolean,
    onSchemeClick: (Int) -> Unit,
    portfolioData: PortfolioData? = null,
    onNavigateToRebalance: (() -> Unit)? = null
) {
    if (analytics == null) return

    val context = LocalContext.current
    val schemes = portfolioData?.schemes.orEmpty()

    // 1. Multi-Factor Health Diagnostic
    val diagnostic = remember(analytics, schemes) {
        XRayDiagnosticsEngine.computeHealthDiagnostic(analytics, schemes)
    }

    // 2. Benchmark Index Matrix
    val indexMatrix = remember(analytics) {
        XRayDiagnosticsEngine.computeIndexMatrix(analytics)
    }

    // Interactive State Modals
    var showHealthSheet by rememberSaveable { mutableStateOf(false) }
    var showInflowRebalanceModal by rememberSaveable { mutableStateOf(false) }
    var drillDownData by remember {
        mutableStateOf<Triple<String, String, List<AggregatedStockExposure>>?>(null)
    }
    var isSectorsExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedEquitiesFilter by rememberSaveable { mutableStateOf("All Top 10") }

    // Redundant Overlap Stocks & Equities Filters (computed in Composable scope)
    val redundantStocks = remember(analytics) {
        analytics.topHoldings.filter { it.heldInSchemesCount >= 3 }.sortedByDescending { it.totalRupeeExposure }
    }

    val filteredEquities = remember(analytics, selectedEquitiesFilter) {
        when (selectedEquitiesFilter) {
            "Gainers" -> analytics.topHoldings.sortedByDescending { it.portfolioWeightPercent }.take(10)
            "High P/E" -> analytics.topHoldings.filter {
                it.sector.contains("Tech", ignoreCase = true) ||
                        it.sector.contains("Consumer", ignoreCase = true) ||
                        it.sector.contains("Pharma", ignoreCase = true)
            }.take(10).ifEmpty { analytics.topHoldings.take(10) }
            else -> analytics.topHoldings.take(10)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Health Scorecard Card
        item {
            HealthScorecardCard(
                diagnostic = diagnostic,
                analytics = analytics,
                isMasked = isMasked,
                onCardClick = { showHealthSheet = true }
            )
        }

        // 2. SEBI Market Cap Alignment Card
        item {
            SebiMarketCapAlignmentCard(
                analytics = analytics,
                isMasked = isMasked,
                onTierClick = { tierName ->
                    val matchingStocks = analytics.topHoldings.filter {
                        if (tierName.contains("Large", ignoreCase = true)) {
                            it.marketCapBucket.contains("Large", ignoreCase = true)
                        } else if (tierName.contains("Mid", ignoreCase = true)) {
                            it.marketCapBucket.contains("Mid", ignoreCase = true)
                        } else if (tierName.contains("Small", ignoreCase = true)) {
                            it.marketCapBucket.contains("Small", ignoreCase = true)
                        } else {
                            !it.marketCapBucket.contains("Large", ignoreCase = true) &&
                                    !it.marketCapBucket.contains("Mid", ignoreCase = true) &&
                                    !it.marketCapBucket.contains("Small", ignoreCase = true)
                        }
                    }
                    drillDownData = Triple("$tierName Constituents", "${matchingStocks.size} stocks held", matchingStocks)
                }
            )
        }

        // 3. Index Matrix Card
        item {
            IndexMatrixCard(
                indexMatrix = indexMatrix,
                isMasked = isMasked,
                onIndexClick = { row ->
                    drillDownData = Triple("${row.title} Holdings", "${row.constituents.size} securities mapping to benchmark", row.constituents)
                }
            )
        }

        // 4. Sector Exposures Card
        item {
            SectorExposuresCard(
                sectorBreakdown = analytics.sectorBreakdown,
                isExpanded = isSectorsExpanded,
                onToggleExpand = { isSectorsExpanded = !isSectorsExpanded },
                onSectorClick = { sector ->
                    val matching = analytics.topHoldings.filter { it.sector.equals(sector.sectorName, ignoreCase = true) }
                    drillDownData = Triple("${sector.sectorName} Companies", "${matching.size} companies held across portfolio", matching)
                }
            )
        }

        // 5. High Duplicate Overlap (Red Alert) Card
        item {
            HighDuplicateOverlapCard(
                redundantStocks = redundantStocks,
                overlapPct = diagnostic.duplicateOverlapPct,
                isMasked = isMasked,
                onStockClick = { stock ->
                    drillDownData = Triple("${stock.ticker} Fund Allocations", stock.companyName, listOf(stock))
                }
            )
        }

        // 6. Consolidated Equities List Card
        item {
            ConsolidatedEquitiesCard(
                equities = filteredEquities,
                totalStocks = analytics.topHoldings.size,
                selectedFilter = selectedEquitiesFilter,
                isMasked = isMasked,
                onFilterSelected = { selectedEquitiesFilter = it },
                onStockClick = { stock ->
                    drillDownData = Triple("${stock.ticker} Inspection", stock.companyName, listOf(stock))
                }
            )
        }

        // 7. Bottom Persistent CTAs
        item {
            BottomPersistentCtas(
                onDownloadPdf = {
                    val activeData = portfolioData ?: PortfolioData(
                        portfolioName = "Primary Vault",
                        ownerName = "Active Investor",
                        lastSyncedEpochMs = System.currentTimeMillis(),
                        schemes = schemes
                    )
                    PortfolioAuditPdfGenerator.generateAndSharePdf(
                        context = context,
                        portfolioData = activeData,
                        analytics = analytics,
                        healthScore = diagnostic.score
                    )
                },
                onSimulateInflow = {
                    if (onNavigateToRebalance != null) {
                        onNavigateToRebalance()
                    } else {
                        showInflowRebalanceModal = true
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Sheets
    if (showHealthSheet) {
        HealthScoreBreakdownSheet(
            diagnostic = diagnostic,
            onDismiss = { showHealthSheet = false }
        )
    }

    if (showInflowRebalanceModal) {
        WhatIfInflowRebalanceModal(
            analytics = analytics,
            isMasked = isMasked,
            onDismiss = { showInflowRebalanceModal = false }
        )
    }

    drillDownData?.let { (title, subtitle, constituents) ->
        ConstituentDrillDownSheet(
            title = title,
            subtitle = subtitle,
            constituents = constituents,
            isMasked = isMasked,
            onSchemeClick = onSchemeClick,
            onDismiss = { drillDownData = null }
        )
    }
}

/**
 * Component 1: Health Scorecard Card.
 */
@Composable
private fun HealthScorecardCard(
    diagnostic: PortfolioHealthDiagnostic,
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean,
    onCardClick: () -> Unit
) {
    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Health Scorecard"
    val cardSubtitle = "Multi-factor portfolio quality & balance metric"
    // =========================================================================

    WsElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = cardTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = cardSubtitle,
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }

                Surface(
                    color = diagnostic.ratingColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, diagnostic.ratingColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = diagnostic.rating,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = diagnostic.ratingColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularHealthGauge(
                    score = diagnostic.score,
                    ratingColor = diagnostic.ratingColor
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticMatrixBox(
                            modifier = Modifier.weight(1f),
                            label = "TOTAL VALUE",
                            primaryText = formatWsInr(analytics.totalCurrentValue, isMasked),
                            chipText = formatWsPercent(analytics.totalReturnPercent),
                            chipColor = if (analytics.totalGain >= 0) SemanticFinanceColors.ProfitGreen else SemanticFinanceColors.LossRed
                        )
                        DiagnosticMatrixBox(
                            modifier = Modifier.weight(1f),
                            label = "STOCK COMMONALITY",
                            primaryText = "${diagnostic.uniqueStocksCount} / ${diagnostic.grossHoldingsCount} Gross",
                            chipText = "${diagnostic.duplicateOverlapPct}% Overlap",
                            chipColor = if (diagnostic.duplicateOverlapPct > 30.0) SemanticFinanceColors.BenchmarkAmber else SemanticFinanceColors.ProfitGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticMatrixBox(
                            modifier = Modifier.weight(1f),
                            label = "ACTIVE SHARE",
                            primaryText = "${String.format(Locale.US, "%.1f%%", diagnostic.activeSharePercent)} vs Nifty 50",
                            chipText = "High Active",
                            chipColor = WsPrimaryIndigo
                        )
                        DiagnosticMatrixBox(
                            modifier = Modifier.weight(1f),
                            label = "EXPENSE (TER)",
                            primaryText = String.format(Locale.US, "%.2f%%", analytics.weightedExpenseRatio),
                            chipText = "~${formatWsInr(analytics.annualFeeDragRupees, isMasked)}/yr",
                            chipColor = WsSlateTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to inspect factor breakdown ➔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = diagnostic.ratingColor
                )
            }
        }
    }
}

@Composable
private fun CircularHealthGauge(
    score: Int,
    ratingColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 7.dp.toPx()
            val diameter = size.minDimension - strokeW
            val topLeft = Offset(strokeW / 2f, strokeW / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            val sweep = (score.toFloat() / 100f) * 360f
            drawArc(
                color = ratingColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = WsSlateTextPrimary
            )
            Text(
                text = "/ 100",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextSecondary
            )
        }
    }
}

@Composable
private fun DiagnosticMatrixBox(
    modifier: Modifier = Modifier,
    label: String,
    primaryText: String,
    chipText: String,
    chipColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = WsSlateTextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = chipColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = chipText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Component 2: SEBI Market Cap Alignment Card.
 */
@Composable
private fun SebiMarketCapAlignmentCard(
    analytics: PortfolioAnalyticsSummary,
    isMasked: Boolean,
    onTierClick: (String) -> Unit
) {
    val largeSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Large", ignoreCase = true) }
    val midSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Mid", ignoreCase = true) }
    val smallSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Small", ignoreCase = true) }
    val microSlice = analytics.marketCapBreakdown.firstOrNull { it.label.contains("Micro", ignoreCase = true) || it.label.contains("Tail", ignoreCase = true) }

    val largeWeight = largeSlice?.weightPercent ?: 0.0
    val midWeight = midSlice?.weightPercent ?: 0.0
    val smallWeight = smallSlice?.weightPercent ?: 0.0
    val microWeight = microSlice?.weightPercent ?: 0.0

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Market Cap Alignment"
    val badgeLabel = "SEBI Breakdown"
    // =========================================================================

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (largeWeight > 0) Box(modifier = Modifier.weight(largeWeight.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(SemanticFinanceColors.SebiLargeCap))
                if (midWeight > 0) Box(modifier = Modifier.weight(midWeight.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(SemanticFinanceColors.SebiMidCap))
                if (smallWeight > 0) Box(modifier = Modifier.weight(smallWeight.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(SemanticFinanceColors.SebiSmallCap))
                if (microWeight > 0) Box(modifier = Modifier.weight(microWeight.toFloat().coerceAtLeast(0.01f)).fillMaxSize().background(SemanticFinanceColors.SebiMicroCap))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tier Rows
            CapTierDetailRow(
                title = "Large Cap • Top 100 Listed Enterprises",
                dotColor = SemanticFinanceColors.SebiLargeCap,
                weightPct = largeWeight,
                targetPct = 50.0,
                rupeeVal = largeSlice?.rupeeValue ?: 0.0,
                isMasked = isMasked,
                onClick = { onTierClick("Large Cap") }
            )
            CapTierDetailRow(
                title = "Mid Cap • 101 - 250 Listed Enterprises",
                dotColor = SemanticFinanceColors.SebiMidCap,
                weightPct = midWeight,
                targetPct = 25.0,
                rupeeVal = midSlice?.rupeeValue ?: 0.0,
                isMasked = isMasked,
                onClick = { onTierClick("Mid Cap") }
            )
            CapTierDetailRow(
                title = "Small Cap • 251 - 500 Listed Enterprises",
                dotColor = SemanticFinanceColors.SebiSmallCap,
                weightPct = smallWeight,
                targetPct = 25.0,
                rupeeVal = smallSlice?.rupeeValue ?: 0.0,
                isMasked = isMasked,
                onClick = { onTierClick("Small Cap") }
            )
            CapTierDetailRow(
                title = "Micro & Arbitrage • Beyond 500 & Cash Equivalents",
                dotColor = SemanticFinanceColors.SebiMicroCap,
                weightPct = microWeight,
                targetPct = 0.0,
                rupeeVal = microSlice?.rupeeValue ?: 0.0,
                isMasked = isMasked,
                onClick = { onTierClick("Micro & Cash") }
            )
        }
    }
}

@Composable
private fun CapTierDetailRow(
    title: String,
    dotColor: Color,
    weightPct: Double,
    targetPct: Double,
    rupeeVal: Double,
    isMasked: Boolean,
    onClick: () -> Unit
) {
    val delta = weightPct - targetPct
    val driftText = if (targetPct == 0.0) {
        if (weightPct <= 1.0) "Compliant" else "+${roundOne(weightPct)}% Tail"
    } else {
        when {
            abs(delta) <= 2.0 -> "Balanced"
            delta > 0 -> "+${roundOne(delta)}% Over"
            else -> "-${roundOne(abs(delta))}% Under"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WsSlateTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Target: ${String.format(Locale.US, "%.0f%%", targetPct)} • ${formatWsInr(rupeeVal, isMasked)}", fontSize = 10.sp, color = WsSlateTextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format(Locale.US, "%.1f%%", weightPct), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WsSlateTextPrimary)
                Surface(
                    color = if (driftText.contains("Under")) WsRedSoft else if (driftText.contains("Over")) WsAmberSoft else WsEmeraldSoft,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = driftText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (driftText.contains("Under")) SemanticFinanceColors.LossRed else if (driftText.contains("Over")) SemanticFinanceColors.BenchmarkAmber else SemanticFinanceColors.ProfitGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Component 3: Index Matrix Card.
 */
@Composable
private fun IndexMatrixCard(
    indexMatrix: List<BenchmarkIndexRow>,
    isMasked: Boolean,
    onIndexClick: (BenchmarkIndexRow) -> Unit
) {
    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Index Matrix"
    val cardSubtitle = "Constituent exposure benchmarked against major indices"
    // =========================================================================

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = cardSubtitle,
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "5 Benchmarks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                indexMatrix.forEach { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIndexClick(row) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(row.color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${row.constituents.size} stocks)",
                                    fontSize = 10.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f%%", row.weightPercent),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = row.color
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { (row.weightPercent.toFloat() / 50f).coerceIn(0.04f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = row.color,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component 4: Sector Exposures & Accordion Card.
 */
@Composable
private fun SectorExposuresCard(
    sectorBreakdown: List<SectorExposure>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSectorClick: (SectorExposure) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val displayedSectors = if (isExpanded) sectorBreakdown else sectorBreakdown.take(5)
    val maxWeight = sectorBreakdown.maxOfOrNull { it.portfolioWeightPercent } ?: 1.0

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Sector Exposures"
    val trackedSectorsLabel = "${sectorBreakdown.size} Tracked Sectors"
    // =========================================================================

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = trackedSectorsLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradient.startColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayedSectors.forEachIndexed { index, sector ->
                    val fraction = if (maxWeight > 0.0) {
                        (sector.portfolioWeightPercent / maxWeight).toFloat().coerceIn(0.05f, 1f)
                    } else 0.05f

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSectorClick(sector) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d", index + 1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = gradient.startColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = sector.sectorName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsSlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sector.stockCount} Companies",
                                            fontSize = 10.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "%.1f%%", sector.portfolioWeightPercent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WsSlateTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(gradient.startColor)
                                )
                            }
                        }
                    }
                }
            }

            if (sectorBreakdown.size > 5) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) "Show Top 5 Only" else "Expand All ${sectorBreakdown.size} Sectors",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Component 5: High Duplicate Overlap (Red Alert) Card.
 */
@Composable
private fun HighDuplicateOverlapCard(
    redundantStocks: List<AggregatedStockExposure>,
    overlapPct: Double,
    isMasked: Boolean,
    onStockClick: (AggregatedStockExposure) -> Unit
) {
    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "High Duplicate Overlap"
    // =========================================================================

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SemanticFinanceColors.LossRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cardTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                }

                Surface(
                    color = WsRedSoft,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SemanticFinanceColors.LossRed.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.0f%%", overlapPct)} Overlap",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SemanticFinanceColors.LossRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The following companies are held redundantly across 3 or more of your active mutual fund schemes, concentrating single-counterparty exposure.",
                fontSize = 11.sp,
                color = WsSlateTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (redundantStocks.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = WsEmeraldSoft
                ) {
                    Text(
                        text = "✓ Zero redundant stocks held in 3+ funds. Counterparty exposure is well diversified.",
                        fontSize = 12.sp,
                        color = SemanticFinanceColors.ProfitGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    redundantStocks.take(5).forEach { stock ->
                        val fundNames = stock.schemeContributions.take(3).joinToString(", ") { it.schemeName.take(15) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStockClick(stock) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = stock.ticker,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = WsPrimaryIndigo,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = stock.companyName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WsSlateTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${stock.marketCapBucket} • ${stock.sector}",
                                                fontSize = 10.sp,
                                                color = WsSlateTextSecondary
                                            )
                                        }
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
                                            color = SemanticFinanceColors.LossRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = WsAmberSoft,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Held in ${stock.heldInSchemesCount} Active Schemes: $fundNames...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WsAmberWarning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Component 6: Consolidated Equities List Card.
 */
@Composable
private fun ConsolidatedEquitiesCard(
    equities: List<AggregatedStockExposure>,
    totalStocks: Int,
    selectedFilter: String,
    isMasked: Boolean,
    onFilterSelected: (String) -> Unit,
    onStockClick: (AggregatedStockExposure) -> Unit
) {
    val gradient = LocalVaultGradient.current

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val cardTitle = "Consolidated Equities"
    val subtitleText = "Top 10 of $totalStocks"
    // =========================================================================

    WsElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                }

                // Filter Segmented Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All Top 10", "Gainers", "High P/E").forEach { filter ->
                        Surface(
                            modifier = Modifier.clickable { onFilterSelected(filter) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedFilter == filter) gradient.startColor else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedFilter == filter) gradient.startColor else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFilter == filter) Color.White else WsSlateTextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                equities.forEachIndexed { index, stock ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStockClick(stock) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = WsSlateTextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${stock.companyName} (${stock.ticker})",
                                        fontSize = 12.sp,
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
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f%%", stock.portfolioWeightPercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WsPrimaryIndigo
                                )
                                Text(
                                    text = formatWsInr(stock.totalRupeeExposure, isMasked),
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

/**
 * Component 7: Bottom Persistent CTAs.
 */
@Composable
private fun BottomPersistentCtas(
    onDownloadPdf: () -> Unit,
    onSimulateInflow: () -> Unit
) {
    val gradient = LocalVaultGradient.current

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val downloadPdfText = "Download Comprehensive Audit (PDF)"
    val simulateInflowText = "Simulate Inflow Rebalance"
    // =========================================================================

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onDownloadPdf,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = downloadPdfText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onSimulateInflow,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, gradient.startColor)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = gradient.startColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = simulateInflowText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = gradient.startColor
            )
        }
    }
}

private fun roundOne(num: Double): Double {
    return (num * 10.0).roundToInt() / 10.0
}
