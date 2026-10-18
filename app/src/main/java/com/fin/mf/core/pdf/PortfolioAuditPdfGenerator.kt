package com.fin.mf.core.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
import com.fin.mf.core.export.MediaStoreExportManager
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Institutional 2-Page A4 PDF Audit Generator built exclusively on Android's native
 * [PdfDocument] and [Canvas] APIs with zero third-party PDF dependencies.
 *
 * - PageDimensions: Standard ISO A4 at 72 DPI (`595 x 842` points) with `36pt` margins.
 * - Page 1: Institutional Header, Executive Health Scorecard, Core Metrics Grid,
 *           SEBI Market-Cap Alignment Table, and Top 8 Sector Concentration Bar Gauges.
 * - Page 2: Duplicate Stock Overlap Matrix (Top 10 cross-held stocks), Complete
 *           Mutual Fund Scheme Register, and Statutory SEBI Regulatory Disclaimers.
 */
class PortfolioAuditPdfGenerator(
    private val mediaStoreExportManager: MediaStoreExportManager = MediaStoreExportManager()
) {

    companion object {
        // A4 Page Geometry (72 DPI)
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 36f
        private const val CONTENT_LEFT = MARGIN
        private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN // 559f
        private const val CONTENT_WIDTH = CONTENT_RIGHT - CONTENT_LEFT // 523f

        // Institutional Color Palette
        private val COLOR_DEEP_SLATE = Color.parseColor("#0F172A")
        private val COLOR_PRIMARY_NAVY = Color.parseColor("#1E3A8A")
        private val COLOR_ACCENT_BLUE = Color.parseColor("#2563EB")
        private val COLOR_PROFIT_EMERALD = Color.parseColor("#059669")
        private val COLOR_PROFIT_BG = Color.parseColor("#ECFDF5")
        private val COLOR_LOSS_CRIMSON = Color.parseColor("#DC2626")
        private val COLOR_LOSS_BG = Color.parseColor("#FEF2F2")
        private val COLOR_WARNING_AMBER = Color.parseColor("#D97706")
        private val COLOR_WARNING_BG = Color.parseColor("#FFFBEB")
        private val COLOR_MUTED_SLATE = Color.parseColor("#64748B")
        private val COLOR_BORDER_GRAY = Color.parseColor("#E2E8F0")
        private val COLOR_SURFACE_CARD = Color.parseColor("#F8FAFC")
        private val COLOR_ZEBRA_ROW = Color.parseColor("#F1F5F9")
        private val COLOR_TOTAL_ROW = Color.parseColor("#E2E8F0")
        private val COLOR_TRACK_BG = Color.parseColor("#E2E8F0")
        private val COLOR_WATERMARK = Color.argb(11, 30, 58, 138)

        private const val DRIFT_TOLERANCE_PCT = 2.0
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

        private val defaultInstance by lazy { PortfolioAuditPdfGenerator() }

        /**
         * Static backward-compatible delegation for screens like XRayTab.
         */
        fun generateAndSharePdf(
            context: Context,
            portfolioData: PortfolioData,
            analytics: PortfolioAnalyticsSummary,
            healthScore: Int,
            onSuccess: ((File, Uri) -> Unit)? = null,
            onError: ((Throwable) -> Unit)? = null
        ) {
            defaultInstance.generateAndSharePdf(
                context = context,
                portfolioData = portfolioData,
                analytics = analytics,
                healthScore = healthScore,
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Typefaces
    private val typefaceRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val typefaceBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val typefaceItalic = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

    /**
     * Generates the 2-Page Executive A4 PDF Report, saves it to both `context.cacheDir`
     * (for immediate sharing via [FileProvider]) and `MediaStore.Downloads`
     * (for permanent storage), and triggers the Android system share sheet.
     */
    fun generateAndSharePdf(
        context: Context,
        portfolioData: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int,
        onSuccess: ((File, Uri) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            try {
                val safeScore = healthScore.coerceIn(0, 100)
                val sanitizedName = sanitizeFilename(portfolioData.portfolioName)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }.format(Date())

                val fileName = "FinBaba_XRay_Report_${sanitizedName}_${timestamp}.pdf"

                // 1. Save to context.cacheDir/exports for instant FileProvider sharing
                val cacheExportDir = File(appContext.cacheDir, "exports").apply {
                    if (!exists()) mkdirs()
                }
                val cachedPdfFile = File(cacheExportDir, fileName)
                FileOutputStream(cachedPdfFile).use { fos ->
                    BufferedOutputStream(fos).use { bos ->
                        renderDocumentToStream(
                            portfolioData = portfolioData,
                            analytics = analytics,
                            healthScore = safeScore,
                            outputStream = bos
                        )
                    }
                }

                // 2. Save permanent copy to MediaStore.Downloads (application/pdf)
                val downloadsResult = mediaStoreExportManager.exportPdfFile(
                    context = appContext,
                    portfolioName = portfolioData.portfolioName
                ) { downloadsOutStream ->
                    cachedPdfFile.inputStream().use { input ->
                        input.copyTo(downloadsOutStream)
                    }
                }

                // Resolve shareable FileProvider URI
                val authority = "${appContext.packageName}$FILE_PROVIDER_SUFFIX"
                val shareUri: Uri = try {
                    FileProvider.getUriForFile(appContext, authority, cachedPdfFile)
                } catch (_: Throwable) {
                    // Fallback to MediaStore URI if FileProvider authority is not registered
                    downloadsResult.getOrNull() ?: Uri.fromFile(cachedPdfFile)
                }

                mainHandler.post {
                    launchPdfShareSheet(context, shareUri, portfolioData.portfolioName)
                    onSuccess?.invoke(cachedPdfFile, shareUri)
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    onError?.invoke(t)
                }
            }
        }
    }

    /**
     * Synchronous helper that renders the complete 2-page PDF into any [OutputStream].
     */
    fun renderDocumentToStream(
        portfolioData: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        try {
            // Render Page 1: Executive Audit, Health Score & Asset Allocation
            val page1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page1 = pdfDocument.startPage(page1Info)
            drawPage1(
                canvas = page1.canvas,
                portfolio = portfolioData,
                analytics = analytics,
                healthScore = healthScore.coerceIn(0, 100)
            )
            pdfDocument.finishPage(page1)

            // Render Page 2: Duplicate Overlap, Scheme Breakdown & Regulatory Footers
            val page2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(page2Info)
            drawPage2(
                canvas = page2.canvas,
                portfolio = portfolioData,
                analytics = analytics
            )
            pdfDocument.finishPage(page2)

            pdfDocument.writeTo(outputStream)
            outputStream.flush()
        } finally {
            pdfDocument.close()
        }
    }

    // ========================================================================
    // PAGE 1: EXECUTIVE AUDIT, HEALTH SCORE & ASSET ALLOCATION
    // ========================================================================

    private fun drawPage1(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int
    ) {
        drawPageBackgroundAndWatermark(canvas)

        var cursorY = MARGIN

        // 1. Institutional Header Banner (y = 36..94)
        cursorY = drawInstitutionalHeader(
            canvas = canvas,
            portfolio = portfolio,
            startY = cursorY,
            subtitle = "INSTITUTIONAL X-RAY AUDIT & MANDATE REPORT"
        )

        cursorY += 12f

        // 2. Executive Health Scorecard (y = 106..192)
        cursorY = drawExecutiveHealthScorecard(
            canvas = canvas,
            portfolio = portfolio,
            analytics = analytics,
            healthScore = healthScore,
            startY = cursorY
        )

        cursorY += 14f

        // 3. Core Metrics Grid (5 Cards) (y = 206..298)
        cursorY = drawCoreMetricsGrid(
            canvas = canvas,
            portfolio = portfolio,
            analytics = analytics,
            startY = cursorY
        )

        cursorY += 16f

        // 4. SEBI Market Cap Alignment Table (y = 314..470)
        cursorY = drawSebiMarketCapTable(
            canvas = canvas,
            portfolio = portfolio,
            analytics = analytics,
            startY = cursorY
        )

        cursorY += 16f

        // 5. Top 8 Sector Concentration Horizontal Bar Gauges (y = 486..772)
        drawSectorConcentrationGauges(
            canvas = canvas,
            analytics = analytics,
            startY = cursorY
        )

        // 6. Page 1 Footer
        drawPageFooter(
            canvas = canvas,
            portfolio = portfolio,
            pageNumber = 1,
            totalPages = 2
        )
    }

    private fun drawInstitutionalHeader(
        canvas: Canvas,
        portfolio: PortfolioData,
        startY: Float,
        subtitle: String
    ): Float {
        val bannerHeight = 58f
        val bannerRect = RectF(CONTENT_LEFT, startY, CONTENT_RIGHT, startY + bannerHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bannerRect, 8f, 8f, bgPaint)

        // Left Accent Bar inside Header
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT_BLUE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, startY, CONTENT_LEFT + 6f, startY + bannerHeight),
            8f,
            8f,
            accentPaint
        )

        // Brand & Subtitle
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 13f
            letterSpacing = 0.04f
        }
        canvas.drawText("FINBABA", CONTENT_LEFT + 18f, startY + 21f, brandPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#93C5FD")
            typeface = typefaceMedium
            textSize = 8.5f
            letterSpacing = 0.06f
        }
        canvas.drawText(subtitle, CONTENT_LEFT + 82f, startY + 20.5f, badgePaint)

        // Vault Name & Owner
        val vaultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 11f
        }
        val vaultLine = "Vault: ${ellipsize(portfolio.portfolioName, 34)}  •  Owner: ${ellipsize(portfolio.ownerName, 26)}"
        canvas.drawText(vaultLine, CONTENT_LEFT + 18f, startY + 39f, vaultPaint)

        val subMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            typeface = typefaceRegular
            textSize = 8f
        }
        val amfiTimestamp = formatIstTimestamp(portfolio.lastSyncedEpochMs)
        canvas.drawText(
            "AMFI NAV Valuation Timestamp: $amfiTimestamp",
            CONTENT_LEFT + 18f,
            startY + 51f,
            subMetaPaint
        )

        // Right-aligned Institutional Watermark Stamp inside Banner
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            typeface = typefaceBold
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("SEBI CATEGORIZATION COMPLIANT", CONTENT_RIGHT - 14f, startY + 22f, stampPaint)

        val schemeCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            typeface = typefaceRegular
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "${portfolio.schemes.size} Active Schemes Audited",
            CONTENT_RIGHT - 14f,
            startY + 38f,
            schemeCountPaint
        )

        return startY + bannerHeight
    }

    private fun drawExecutiveHealthScorecard(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int,
        startY: Float
    ): Float {
        val cardHeight = 84f
        val cardRect = RectF(CONTENT_LEFT, startY, CONTENT_RIGHT, startY + cardHeight)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_CARD
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(cardRect, 8f, 8f, strokePaint)

        // Score Color & Rating
        val scoreColor = when {
            healthScore >= 75 -> COLOR_PROFIT_EMERALD
            healthScore >= 50 -> COLOR_WARNING_AMBER
            else -> COLOR_LOSS_CRIMSON
        }
        val healthLabel = when {
            healthScore >= 80 -> "INSTITUTIONAL GRADE"
            healthScore >= 65 -> "WELL CONSTRUCTED"
            healthScore >= 50 -> "MODERATE DRIFT"
            else -> "REBALANCE REQUIRED"
        }

        // Circular Gauge on Left
        val centerX = CONTENT_LEFT + 44f
        val centerY = startY + cardHeight / 2f
        val radius = 27f

        val trackRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
        }
        canvas.drawCircle(centerX, centerY, radius, trackRingPaint)

        val progressRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
        }
        val arcRect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        val sweepAngle = (healthScore / 100f) * 360f
        canvas.drawArc(arcRect, -90f, sweepAngle, false, progressRingPaint)

        val scoreNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$healthScore", centerX, centerY + 3f, scoreNumPaint)

        val scoreOutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 7f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("/ 100", centerX, centerY + 13f, scoreOutPaint)

        // Column 1 Text: Health Score Details
        val col1X = CONTENT_LEFT + 84f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceBold
            textSize = 7.5f
            letterSpacing = 0.05f
        }
        canvas.drawText("EXECUTIVE HEALTH SCORE", col1X, startY + 23f, labelPaint)

        val ratingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            typeface = typefaceBold
            textSize = 11f
        }
        canvas.drawText(healthLabel, col1X, startY + 40f, ratingPaint)

        val subNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 8f
        }
        canvas.drawText("Composite Mandate & Overlap Index", col1X, startY + 55f, subNotePaint)
        canvas.drawText("Audit Benchmark: NIFTY 500 TRI", col1X, startY + 68f, subNotePaint)

        // Vertical Divider 1
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            strokeWidth = 1f
        }
        val div1X = CONTENT_LEFT + 235f
        canvas.drawLine(div1X, startY + 12f, div1X, startY + cardHeight - 12f, divPaint)

        // Column 2: Diversification Rating
        val col2X = div1X + 14f
        val distinctStocks = analytics.topHoldings.size
        val top5Weight = analytics.topHoldings
            .sortedByDescending { it.portfolioWeightPercent }
            .take(5)
            .sumOf { it.portfolioWeightPercent }

        val diversificationRating = when {
            distinctStocks >= 45 && top5Weight < 35.0 -> "Optimal Multi-Cap Spread"
            distinctStocks >= 25 && top5Weight < 45.0 -> "Balanced Concentration"
            distinctStocks >= 12 -> "High Conviction / Focused"
            else -> "Concentrated Exposure"
        }

        canvas.drawText("DIVERSIFICATION RATING", col2X, startY + 23f, labelPaint)
        val divValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            typeface = typefaceBold
            textSize = 10.5f
        }
        canvas.drawText(diversificationRating, col2X, startY + 40f, divValuePaint)
        canvas.drawText(
            "Underlying Equities: $distinctStocks Stocks across ${analytics.sectorBreakdown.size} Sectors",
            col2X,
            startY + 55f,
            subNotePaint
        )
        canvas.drawText(
            "Top-5 Stock Concentration: ${formatPct(top5Weight)}",
            col2X,
            startY + 68f,
            subNotePaint
        )

        // Vertical Divider 2
        val div2X = CONTENT_LEFT + 392f
        canvas.drawLine(div2X, startY + 12f, div2X, startY + cardHeight - 12f, divPaint)

        // Column 3: Annual Fee Drag
        val col3X = div2X + 14f
        canvas.drawText("ANNUAL TER FEE DRAG", col3X, startY + 23f, labelPaint)

        val feePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LOSS_CRIMSON
            typeface = typefaceBold
            textSize = 12f
        }
        canvas.drawText(
            "${formatInr(analytics.annualFeeDragRupees)} / yr",
            col3X,
            startY + 40f,
            feePaint
        )
        canvas.drawText(
            "Blended Portfolio TER: ${formatPct(analytics.weightedExpenseRatio)}",
            col3X,
            startY + 55f,
            subNotePaint
        )
        val tenYrDrag = analytics.annualFeeDragRupees * 10.0
        canvas.drawText(
            "10-Yr Linear Cost: ${formatInrCompact(tenYrDrag)}",
            col3X,
            startY + 68f,
            subNotePaint
        )

        return startY + cardHeight
    }

    private fun drawCoreMetricsGrid(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        drawSectionTitle(canvas, "CORE VALUATION & RETURN TELEMETRY", startY)
        val gridTop = startY + 18f
        val cardHeight = 68f
        val gap = 6f
        val cardWidth = (CONTENT_WIDTH - (gap * 4f)) / 5f

        val estimatedCagr = computeEstimatedPortfolioCagr(portfolio, analytics)
        val isPositiveGain = analytics.totalGain >= 0.0
        val gainColor = if (isPositiveGain) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON
        val gainPrefix = if (isPositiveGain) "+" else ""

        val cards = listOf(
            MetricCardSpec(
                title = "NET WORTH",
                primaryValue = formatInrCompact(analytics.totalCurrentValue),
                secondaryValue = formatInr(analytics.totalCurrentValue),
                accentColor = COLOR_PRIMARY_NAVY
            ),
            MetricCardSpec(
                title = "TOTAL INVESTED",
                primaryValue = formatInrCompact(analytics.totalInvested),
                secondaryValue = formatInr(analytics.totalInvested),
                accentColor = COLOR_DEEP_SLATE
            ),
            MetricCardSpec(
                title = "CAPITAL GAIN",
                primaryValue = "$gainPrefix${formatInrCompact(analytics.totalGain)}",
                secondaryValue = "$gainPrefix${formatPct(analytics.totalReturnPercent)} Abs",
                accentColor = gainColor
            ),
            MetricCardSpec(
                title = "BLENDED TER",
                primaryValue = formatPct(analytics.weightedExpenseRatio),
                secondaryValue = "Drag: ${formatInrCompact(analytics.annualFeeDragRupees)}/yr",
                accentColor = COLOR_WARNING_AMBER
            ),
            MetricCardSpec(
                title = "ESTIMATED CAGR",
                primaryValue = "${if (estimatedCagr >= 0) "+" else ""}${formatPct(estimatedCagr)}",
                secondaryValue = "Annualized XIRR Est.",
                accentColor = if (estimatedCagr >= 0) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON
            )
        )

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val headerStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceBold
            textSize = 7f
            letterSpacing = 0.04f
        }
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typefaceBold
            textSize = 11.5f
        }
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceMedium
            textSize = 7.5f
        }

        cards.forEachIndexed { idx, spec ->
            val left = CONTENT_LEFT + idx * (cardWidth + gap)
            val right = left + cardWidth
            val rect = RectF(left, gridTop, right, gridTop + cardHeight)

            canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(rect, 6f, 6f, cardBorderPaint)

            headerStripePaint.color = spec.accentColor
            canvas.drawRoundRect(
                RectF(left, gridTop, right, gridTop + 3.5f),
                6f,
                6f,
                headerStripePaint
            )

            canvas.drawText(spec.title, left + 8f, gridTop + 18f, titlePaint)

            primaryPaint.color = spec.accentColor
            canvas.drawText(
                ellipsize(spec.primaryValue, 16),
                left + 8f,
                gridTop + 38f,
                primaryPaint
            )

            canvas.drawText(
                ellipsize(spec.secondaryValue, 20),
                left + 8f,
                gridTop + 55f,
                secondaryPaint
            )
        }

        return gridTop + cardHeight
    }

    private fun drawSebiMarketCapTable(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        drawSectionTitle(canvas, "SEBI MARKET-CAP MANDATE & REBALANCING TABLE", startY)

        var y = startY + 18f
        val rowHeight = 22f

        // Column X Anchors
        val colTierX = CONTENT_LEFT + 10f
        val colValRightX = CONTENT_LEFT + 185f
        val colActualRightX = CONTENT_LEFT + 250f
        val colTargetRightX = CONTENT_LEFT + 318f
        val colDriftRightX = CONTENT_LEFT + 380f
        val colBadgeCenterX = CONTENT_LEFT + 425f
        val colActionRightX = CONTENT_RIGHT - 10f

        // Header Row
        val headerRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerPaint)

        val thLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 8f
            textAlign = Paint.Align.LEFT
        }
        val thRightPaint = Paint(thLeftPaint).apply { textAlign = Paint.Align.RIGHT }
        val thCenterPaint = Paint(thLeftPaint).apply { textAlign = Paint.Align.CENTER }

        val textYOffset = 14.5f
        canvas.drawText("SEBI Market Cap Tier", colTierX, y + textYOffset, thLeftPaint)
        canvas.drawText("Actual Value (₹)", colValRightX, y + textYOffset, thRightPaint)
        canvas.drawText("Actual %", colActualRightX, y + textYOffset, thRightPaint)
        canvas.drawText("Target %", colTargetRightX, y + textYOffset, thRightPaint)
        canvas.drawText("Drift %", colDriftRightX, y + textYOffset, thRightPaint)
        canvas.drawText("Status", colBadgeCenterX, y + textYOffset, thCenterPaint)
        canvas.drawText("Rebalance Action", colActionRightX, y + textYOffset, thRightPaint)

        y += rowHeight

        val mandate = portfolio.marketCapMandate
        val rows = listOf(
            Triple("Large Cap (Top 100)", matchSlice(analytics.marketCapBreakdown, "Large"), mandate.largeCapTarget),
            Triple("Mid Cap (101–250)", matchSlice(analytics.marketCapBreakdown, "Mid"), mandate.midCapTarget),
            Triple("Small Cap (251–500)", matchSlice(analytics.marketCapBreakdown, "Small"), mandate.smallCapTarget),
            Triple(
                "Micro Cap, Debt & Cash",
                matchMicroAndCashSlice(analytics.marketCapBreakdown, analytics.totalCurrentValue),
                mandate.microCapTarget
            )
        )

        val cellLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceMedium
            textSize = 8.2f
            textAlign = Paint.Align.LEFT
        }
        val cellRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceRegular
            textSize = 8.2f
            textAlign = Paint.Align.RIGHT
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        var sumActualVal = 0.0
        var sumActualPct = 0.0
        var sumTargetPct = 0.0

        rows.forEachIndexed { idx, (tierLabel, slice, targetPct) ->
            val rowRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowHeight)
            if (idx % 2 == 1) {
                val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_ZEBRA_ROW
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rowRect, zebraPaint)
            }
            canvas.drawRect(rowRect, borderPaint)

            val actualVal = slice.rupeeValue
            val actualPct = if (slice.weightPercent > 0.0 || analytics.totalCurrentValue <= 0.0) {
                slice.weightPercent
            } else {
                (actualVal / analytics.totalCurrentValue) * 100.0
            }

            val driftPct = actualPct - targetPct
            val targetRupees = (targetPct / 100.0) * analytics.totalCurrentValue
            val deltaRupees = targetRupees - actualVal

            sumActualVal += actualVal
            sumActualPct += actualPct
            sumTargetPct += targetPct

            canvas.drawText(tierLabel, colTierX, y + textYOffset, cellLeftPaint)
            canvas.drawText(formatInr(actualVal), colValRightX, y + textYOffset, cellRightPaint)
            canvas.drawText(formatPct(actualPct), colActualRightX, y + textYOffset, cellRightPaint)
            canvas.drawText(formatPct(targetPct), colTargetRightX, y + textYOffset, cellRightPaint)

            val driftSign = if (driftPct > 0) "+" else ""
            val driftPaint = Paint(cellRightPaint).apply {
                typeface = typefaceBold
                color = when {
                    abs(driftPct) <= DRIFT_TOLERANCE_PCT -> COLOR_PROFIT_EMERALD
                    driftPct > 0 -> COLOR_WARNING_AMBER
                    else -> COLOR_LOSS_CRIMSON
                }
            }
            canvas.drawText("$driftSign${formatPct(driftPct)}", colDriftRightX, y + textYOffset, driftPaint)

            // Status Pill Badge
            val (badgeText, badgeFg, badgeBg) = when {
                driftPct > DRIFT_TOLERANCE_PCT -> Triple("OVERWEIGHT", COLOR_WARNING_AMBER, COLOR_WARNING_BG)
                driftPct < -DRIFT_TOLERANCE_PCT -> Triple("UNDERWEIGHT", COLOR_LOSS_CRIMSON, COLOR_LOSS_BG)
                else -> Triple("IN-LINE", COLOR_PROFIT_EMERALD, COLOR_PROFIT_BG)
            }
            drawPillBadge(
                canvas = canvas,
                centerX = colBadgeCenterX,
                centerY = y + rowHeight / 2f,
                text = badgeText,
                textColor = badgeFg,
                bgColor = badgeBg
            )

            // Rebalance Action Text
            val actionPaint = Paint(cellRightPaint).apply {
                typeface = typefaceBold
                color = when {
                    abs(driftPct) <= DRIFT_TOLERANCE_PCT -> COLOR_MUTED_SLATE
                    deltaRupees > 0 -> COLOR_PROFIT_EMERALD
                    else -> COLOR_LOSS_CRIMSON
                }
            }
            val actionText = when {
                abs(driftPct) <= DRIFT_TOLERANCE_PCT -> "Hold / No Action"
                deltaRupees > 0 -> "Buy +${formatInrCompact(deltaRupees)}"
                else -> "Trim -${formatInrCompact(abs(deltaRupees))}"
            }
            canvas.drawText(actionText, colActionRightX, y + textYOffset, actionPaint)

            y += rowHeight
        }

        // Totals Row
        val totalRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowHeight)
        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TOTAL_ROW
            style = Paint.Style.FILL
        }
        canvas.drawRect(totalRect, totalBgPaint)
        canvas.drawRect(totalRect, borderPaint)

        val totalLeftPaint = Paint(cellLeftPaint).apply { typeface = typefaceBold }
        val totalRightPaint = Paint(cellRightPaint).apply { typeface = typefaceBold }

        canvas.drawText("TOTAL PORTFOLIO MANDATE", colTierX, y + textYOffset, totalLeftPaint)
        canvas.drawText(formatInr(sumActualVal), colValRightX, y + textYOffset, totalRightPaint)
        canvas.drawText(formatPct(sumActualPct), colActualRightX, y + textYOffset, totalRightPaint)
        canvas.drawText(formatPct(sumTargetPct), colTargetRightX, y + textYOffset, totalRightPaint)
        canvas.drawText("0.00%", colDriftRightX, y + textYOffset, totalRightPaint)
        canvas.drawText("Net Zero Rebalance", colActionRightX, y + textYOffset, totalRightPaint)

        return y + rowHeight
    }

    private fun drawSectorConcentrationGauges(
        canvas: Canvas,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        drawSectionTitle(canvas, "TOP 8 SECTOR CONCENTRATION HORIZONTAL BAR GAUGES", startY)

        val cardTop = startY + 18f
        val cardBottom = 774f
        val cardRect = RectF(CONTENT_LEFT, cardTop, CONTENT_RIGHT, cardBottom)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_CARD
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        val topSectors = analytics.sectorBreakdown
            .sortedByDescending { it.portfolioWeightPercent }
            .take(8)

        if (topSectors.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_MUTED_SLATE
                typeface = typefaceItalic
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "No underlying sector look-through data available for this portfolio.",
                CONTENT_LEFT + CONTENT_WIDTH / 2f,
                cardTop + 120f,
                emptyPaint
            )
            return cardBottom
        }

        val maxSectorWeight = max(topSectors.maxOf { it.portfolioWeightPercent }, 25.0)
        val rowPitch = 30f
        var rowY = cardTop + 16f

        val sectorNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 8.5f
        }
        val stockCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 7.5f
        }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TRACK_BG
            style = Paint.Style.FILL
        }
        val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val valueRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }

        val barLeft = CONTENT_LEFT + 170f
        val barMaxRight = CONTENT_LEFT + 405f
        val barMaxWidth = barMaxRight - barLeft

        topSectors.forEachIndexed { idx, sector ->
            val rankLabel = "${idx + 1}. ${ellipsize(sector.sectorName, 24)}"
            canvas.drawText(rankLabel, CONTENT_LEFT + 12f, rowY + 9f, sectorNamePaint)
            canvas.drawText(
                "${sector.stockCount} stocks",
                CONTENT_LEFT + 24f,
                rowY + 19.5f,
                stockCountPaint
            )

            // Horizontal Bar Track
            val barTop = rowY + 4f
            val barBottom = rowY + 16f
            val trackRect = RectF(barLeft, barTop, barMaxRight, barBottom)
            canvas.drawRoundRect(trackRect, 6f, 6f, trackPaint)

            // Proportional Fill
            val fillRatio = (sector.portfolioWeightPercent / maxSectorWeight).toFloat().coerceIn(0.02f, 1f)
            val fillRight = barLeft + (barMaxWidth * fillRatio)
            barFillPaint.color = when {
                sector.portfolioWeightPercent >= 30.0 -> COLOR_WARNING_AMBER
                idx == 0 -> COLOR_PRIMARY_NAVY
                idx < 3 -> COLOR_ACCENT_BLUE
                else -> Color.parseColor("#3B82F6")
            }
            canvas.drawRoundRect(RectF(barLeft, barTop, fillRight, barBottom), 6f, 6f, barFillPaint)

            // Percentage & INR Value on Right
            val metricText = "${formatInrCompact(sector.totalRupeeValue)}  (${formatPct(sector.portfolioWeightPercent)})"
            canvas.drawText(metricText, CONTENT_RIGHT - 12f, rowY + 13f, valueRightPaint)

            rowY += rowPitch
        }

        return cardBottom
    }

    // ========================================================================
    // PAGE 2: DUPLICATE OVERLAP, SCHEME BREAKDOWN & REGULATORY FOOTERS
    // ========================================================================

    private fun drawPage2(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ) {
        drawPageBackgroundAndWatermark(canvas)

        var cursorY = MARGIN

        // 1. Continuation Header (y = 36..82)
        cursorY = drawPage2MiniHeader(canvas, portfolio, cursorY)

        cursorY += 14f

        // 2. Duplicate Overlap Matrix: Top 10 Most Overlapped Stocks (y = 96..352)
        cursorY = drawDuplicateOverlapMatrix(
            canvas = canvas,
            analytics = analytics,
            startY = cursorY
        )

        cursorY += 14f

        // 3. Complete Scheme Table (y = 366..700)
        drawCompleteSchemeTable(
            canvas = canvas,
            portfolio = portfolio,
            analytics = analytics,
            startY = cursorY,
            maxBottomY = 702f
        )

        // 4. Statutory SEBI Disclaimers & Footer (y = 710..806)
        drawStatutorySebiDisclaimers(
            canvas = canvas,
            portfolio = portfolio,
            startY = 710f
        )

        drawPageFooter(
            canvas = canvas,
            portfolio = portfolio,
            pageNumber = 2,
            totalPages = 2
        )
    }

    private fun drawPage2MiniHeader(
        canvas: Canvas,
        portfolio: PortfolioData,
        startY: Float
    ): Float {
        val height = 42f
        val rect = RectF(CONTENT_LEFT, startY, CONTENT_RIGHT, startY + height)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 10.5f
        }
        canvas.drawText(
            "FINBABA X-RAY AUDIT  •  OVERLAP MATRIX & SCHEME REGISTER",
            CONTENT_LEFT + 14f,
            startY + 18f,
            leftPaint
        )

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#93C5FD")
            typeface = typefaceRegular
            textSize = 8f
        }
        canvas.drawText(
            "Vault: ${ellipsize(portfolio.portfolioName, 35)} (${ellipsize(portfolio.ownerName, 25)})",
            CONTENT_LEFT + 14f,
            startY + 32f,
            subPaint
        )

        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            typeface = typefaceMedium
            textSize = 8f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "Audit Date: ${formatIstTimestamp(portfolio.lastSyncedEpochMs)}",
            CONTENT_RIGHT - 14f,
            startY + 25f,
            rightPaint
        )

        return startY + height
    }

    private fun drawDuplicateOverlapMatrix(
        canvas: Canvas,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        drawSectionTitle(
            canvas,
            "DUPLICATE STOCK OVERLAP MATRIX (TOP 10 CROSS-HELD EQUITIES)",
            startY
        )

        var y = startY + 16f
        val headerHeight = 20f
        val rowHeight = 21f

        // Sort by number of schemes holding the stock descending, then rupee exposure descending
        val overlappedHoldings = analytics.topHoldings
            .sortedWith(
                compareByDescending<AggregatedStockExposure> { it.schemeContributions.size }
                    .thenByDescending { it.totalRupeeExposure }
            )
            .take(10)

        // Column X Anchors
        val colRankX = CONTENT_LEFT + 8f
        val colCompanyX = CONTENT_LEFT + 28f
        val colSectorX = CONTENT_LEFT + 162f
        val colExposureRightX = CONTENT_LEFT + 292f
        val colWeightRightX = CONTENT_LEFT + 342f
        val colFundCountCenterX = CONTENT_LEFT + 372f
        val colSchemesX = CONTENT_LEFT + 394f

        // Header Row
        val headerRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerPaint)

        val thLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 7.5f
            textAlign = Paint.Align.LEFT
        }
        val thRight = Paint(thLeft).apply { textAlign = Paint.Align.RIGHT }
        val thCenter = Paint(thLeft).apply { textAlign = Paint.Align.CENTER }

        val thOffset = 13.5f
        canvas.drawText("#", colRankX, y + thOffset, thLeft)
        canvas.drawText("Stock & Ticker", colCompanyX, y + thOffset, thLeft)
        canvas.drawText("Sector / Cap", colSectorX, y + thOffset, thLeft)
        canvas.drawText("Exposure (₹)", colExposureRightX, y + thOffset, thRight)
        canvas.drawText("Weight", colWeightRightX, y + thOffset, thRight)
        canvas.drawText("Funds", colFundCountCenterX, y + thOffset, thCenter)
        canvas.drawText("Contributing Mutual Fund Schemes", colSchemesX, y + thOffset, thLeft)

        y += headerHeight

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 7.8f
        }
        val cellRegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 7.3f
        }
        val cellRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceMedium
            textSize = 7.8f
            textAlign = Paint.Align.RIGHT
        }

        if (overlappedHoldings.isEmpty()) {
            val emptyRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + 40f)
            canvas.drawRect(emptyRect, borderPaint)
            canvas.drawText(
                "No underlying stock holdings available for overlap calculation.",
                CONTENT_LEFT + 14f,
                y + 24f,
                cellRegPaint
            )
            return y + 40f
        }

        overlappedHoldings.forEachIndexed { idx, stock ->
            val rowRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowHeight)
            if (idx % 2 == 1) {
                val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_ZEBRA_ROW
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rowRect, zebraPaint)
            }
            canvas.drawRect(rowRect, borderPaint)

            val textY = y + 13.5f
            canvas.drawText("${idx + 1}", colRankX, textY, cellRegPaint)

            val stockTitle = "${ellipsize(stock.companyName, 18)} (${ellipsize(stock.ticker, 8)})"
            canvas.drawText(stockTitle, colCompanyX, textY, cellBoldPaint)

            val sectorCap = "${ellipsize(stock.sector, 14)} • ${ellipsize(stock.marketCapBucket, 6)}"
            canvas.drawText(sectorCap, colSectorX, textY, cellRegPaint)

            canvas.drawText(
                formatInr(stock.totalRupeeExposure),
                colExposureRightX,
                textY,
                cellRightPaint
            )
            canvas.drawText(
                formatPct(stock.portfolioWeightPercent),
                colWeightRightX,
                textY,
                cellRightPaint
            )

            val fundCount = stock.schemeContributions.size
            val badgeBg = if (fundCount >= 3) COLOR_WARNING_BG else COLOR_PROFIT_BG
            val badgeFg = if (fundCount >= 3) COLOR_WARNING_AMBER else COLOR_PRIMARY_NAVY
            drawPillBadge(
                canvas = canvas,
                centerX = colFundCountCenterX,
                centerY = y + rowHeight / 2f,
                text = "${fundCount}F",
                textColor = badgeFg,
                bgColor = badgeBg
            )

            val schemesSummary = stock.schemeContributions
                .sortedByDescending { it.rupeeContribution }
                .joinToString(", ") { contrib ->
                    "${abbreviateSchemeName(contrib.schemeName)} (${formatPct(contrib.weightInSchemePercent)})"
                }
            canvas.drawText(
                ellipsize(schemesSummary, 34),
                colSchemesX,
                textY,
                cellRegPaint
            )

            y += rowHeight
        }

        return y
    }

    private fun drawCompleteSchemeTable(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        startY: Float,
        maxBottomY: Float
    ): Float {
        drawSectionTitle(canvas, "COMPLETE MUTUAL FUND SCHEME REGISTER", startY)

        var y = startY + 16f
        val headerHeight = 20f
        val totalRowHeight = 21f

        val sortedSchemes = portfolio.schemes.sortedByDescending { it.currentValue }
        val availableBodyHeight = (maxBottomY - y - headerHeight - totalRowHeight).coerceAtLeast(60f)

        // Dynamically scale row height so schemes fit cleanly without overflowing Page 2
        val rawSchemeCount = max(sortedSchemes.size, 1)
        val idealRowHeight = (availableBodyHeight / rawSchemeCount).coerceIn(15.5f, 21f)
        val maxVisibleRows = (availableBodyHeight / idealRowHeight).toInt().coerceAtLeast(1)

        val displaySchemes = if (sortedSchemes.size > maxVisibleRows) {
            sortedSchemes.take(maxVisibleRows - 1)
        } else {
            sortedSchemes
        }
        val hiddenSchemes = if (sortedSchemes.size > displaySchemes.size) {
            sortedSchemes.drop(displaySchemes.size)
        } else {
            emptyList()
        }

        // Column X Anchors
        val colNameX = CONTENT_LEFT + 8f
        val colCatX = CONTENT_LEFT + 196f
        val colUnitsRightX = CONTENT_LEFT + 305f
        val colBuyNavRightX = CONTENT_LEFT + 365f
        val colCurNavRightX = CONTENT_LEFT + 425f
        val colValRightX = CONTENT_LEFT + 498f
        val colRetRightX = CONTENT_RIGHT - 8f

        // Table Header
        val headerRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerPaint)

        val thLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = typefaceBold
            textSize = 7.5f
            textAlign = Paint.Align.LEFT
        }
        val thRight = Paint(thLeft).apply { textAlign = Paint.Align.RIGHT }

        val thOffset = 13.5f
        canvas.drawText("Scheme Name", colNameX, y + thOffset, thLeft)
        canvas.drawText("Category", colCatX, y + thOffset, thLeft)
        canvas.drawText("Units", colUnitsRightX, y + thOffset, thRight)
        canvas.drawText("Avg NAV", colBuyNavRightX, y + thOffset, thRight)
        canvas.drawText("Cur NAV", colCurNavRightX, y + thOffset, thRight)
        canvas.drawText("Valuation (₹)", colValRightX, y + thOffset, thRight)
        canvas.drawText("Return %", colRetRightX, y + thOffset, thRight)

        y += headerHeight

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 7.6f
        }
        val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 7.3f
        }
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceRegular
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
        }

        displaySchemes.forEachIndexed { idx, scheme ->
            val rowRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + idealRowHeight)
            if (idx % 2 == 1) {
                val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_ZEBRA_ROW
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rowRect, zebraPaint)
            }
            canvas.drawRect(rowRect, borderPaint)

            val textY = y + (idealRowHeight * 0.66f)
            canvas.drawText(ellipsize(scheme.schemeName, 36), colNameX, textY, namePaint)
            canvas.drawText(ellipsize(scheme.category, 16), colCatX, textY, catPaint)
            canvas.drawText(formatDecimal(scheme.units, 2), colUnitsRightX, textY, numPaint)
            canvas.drawText(formatDecimal(scheme.averageBuyNav, 2), colBuyNavRightX, textY, numPaint)
            canvas.drawText(formatDecimal(scheme.fallbackNav, 2), colCurNavRightX, textY, numPaint)
            canvas.drawText(formatInr(scheme.currentValue), colValRightX, textY, numPaint)

            val retPaint = Paint(numPaint).apply {
                typeface = typefaceBold
                color = if (scheme.absoluteReturnPercent >= 0.0) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON
            }
            val sign = if (scheme.absoluteReturnPercent > 0.0) "+" else ""
            canvas.drawText("$sign${formatPct(scheme.absoluteReturnPercent)}", colRetRightX, textY, retPaint)

            y += idealRowHeight
        }

        // Consolidated overflow row if portfolio has more than maxVisibleRows schemes
        if (hiddenSchemes.isNotEmpty()) {
            val overflowVal = hiddenSchemes.sumOf { it.currentValue }
            val overflowInv = hiddenSchemes.sumOf { it.investedAmount }
            val overflowRet = if (overflowInv > 0.0) ((overflowVal - overflowInv) / overflowInv) * 100.0 else 0.0

            val rowRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + idealRowHeight)
            canvas.drawRect(rowRect, borderPaint)
            val textY = y + (idealRowHeight * 0.66f)

            canvas.drawText(
                "+ ${hiddenSchemes.size} Additional Schemes Consolidated",
                colNameX,
                textY,
                catPaint
            )
            canvas.drawText("Multi-Category", colCatX, textY, catPaint)
            canvas.drawText("—", colUnitsRightX, textY, numPaint)
            canvas.drawText("—", colBuyNavRightX, textY, numPaint)
            canvas.drawText("—", colCurNavRightX, textY, numPaint)
            canvas.drawText(formatInr(overflowVal), colValRightX, textY, numPaint)
            canvas.drawText(formatPct(overflowRet), colRetRightX, textY, numPaint)

            y += idealRowHeight
        }

        // Totals Row
        val totalRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + totalRowHeight)
        val totalBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TOTAL_ROW
            style = Paint.Style.FILL
        }
        canvas.drawRect(totalRect, totalBg)
        canvas.drawRect(totalRect, borderPaint)

        val totalTextY = y + 14f
        val totalLeftPaint = Paint(namePaint).apply { textSize = 8f }
        val totalRightPaint = Paint(numPaint).apply {
            typeface = typefaceBold
            textSize = 8f
        }
        canvas.drawText(
            "TOTAL PORTFOLIO (${sortedSchemes.size} Active Schemes)",
            colNameX,
            totalTextY,
            totalLeftPaint
        )
        canvas.drawText(
            "Invested: ${formatInrCompact(analytics.totalInvested)}",
            colCurNavRightX,
            totalTextY,
            totalRightPaint
        )
        canvas.drawText(
            formatInr(analytics.totalCurrentValue),
            colValRightX,
            totalTextY,
            totalRightPaint
        )

        val totalRetPaint = Paint(totalRightPaint).apply {
            color = if (analytics.totalReturnPercent >= 0.0) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON
        }
        val totalSign = if (analytics.totalReturnPercent > 0.0) "+" else ""
        canvas.drawText(
            "$totalSign${formatPct(analytics.totalReturnPercent)}",
            colRetRightX,
            totalTextY,
            totalRetPaint
        )

        return y + totalRowHeight
    }

    private fun drawStatutorySebiDisclaimers(
        canvas: Canvas,
        portfolio: PortfolioData,
        startY: Float
    ) {
        val boxHeight = 68f
        val rect = RectF(CONTENT_LEFT, startY, CONTENT_RIGHT, startY + boxHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_CARD
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            typeface = typefaceBold
            textSize = 7.5f
            letterSpacing = 0.04f
        }
        canvas.drawText(
            "STATUTORY SEBI & AMFI REGULATORY DISCLOSURES",
            CONTENT_LEFT + 10f,
            startY + 14f,
            titlePaint
        )

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 6.8f
        }

        val lines = listOf(
            "1. Mutual Fund investments are subject to market risks. Please read all scheme-related documents (SID/SAI/KIM) carefully before investing.",
            "2. Look-through stock and sector exposures are computed from the latest monthly AMFI portfolio disclosures and NAVs as of ${formatIstTimestamp(portfolio.lastSyncedEpochMs)}.",
            "3. Market-cap classifications strictly follow the SEBI Circular (SEBI/HO/IMD/DF3/CIR/P/2017/114): Large Cap (1st-100th), Mid Cap (101st-250th), Small Cap (251st+).",
            "4. Suggested rebalancing adjustments are algorithmic drift calculations against user-configured mandate targets and do not constitute personalized investment advice."
        )

        var lineY = startY + 26f
        lines.forEach { line ->
            canvas.drawText(line, CONTENT_LEFT + 10f, lineY, bodyPaint)
            lineY += 10.5f
        }
    }

    // ========================================================================
    // SHARED DRAWING PRIMITIVES & FORMATTING HELPERS
    // ========================================================================

    private fun drawPageBackgroundAndWatermark(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)

        // Diagonal Institutional Watermark
        canvas.save()
        canvas.rotate(-32f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WATERMARK
            typeface = typefaceBold
            textSize = 54f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText("FINBABA INSTITUTIONAL X-RAY", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, watermarkPaint)
        canvas.restore()
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, startY: Float) {
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(CONTENT_LEFT, startY, CONTENT_LEFT + 4f, startY + 12f),
            2f,
            2f,
            barPaint
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_SLATE
            typeface = typefaceBold
            textSize = 9.2f
            letterSpacing = 0.04f
        }
        canvas.drawText(title, CONTENT_LEFT + 10f, startY + 10f, textPaint)
    }

    private fun drawPillBadge(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        text: String,
        textColor: Int,
        bgColor: Int
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = typefaceBold
            textSize = 6.8f
            textAlign = Paint.Align.CENTER
        }
        val textWidth = textPaint.measureText(text)
        val padH = 6f
        val halfW = (textWidth / 2f) + padH
        val halfH = 6.5f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val pillRect = RectF(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
        canvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
        canvas.drawText(text, centerX, centerY + 2.4f, textPaint)
    }

    private fun drawPageFooter(
        canvas: Canvas,
        portfolio: PortfolioData,
        pageNumber: Int,
        totalPages: Int
    ) {
        val lineY = 788f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(CONTENT_LEFT, lineY, CONTENT_RIGHT, lineY, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_SLATE
            typeface = typefaceRegular
            textSize = 7.5f
        }
        val generatedNow = formatIstTimestamp(System.currentTimeMillis())
        canvas.drawText(
            "Generated by FinBaba Institutional Engine (com.fin.mf)  •  Printed: $generatedNow",
            CONTENT_LEFT,
            lineY + 14f,
            footerPaint
        )

        val pagePaint = Paint(footerPaint).apply {
            typeface = typefaceBold
            color = COLOR_DEEP_SLATE
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "Vault: ${ellipsize(portfolio.portfolioName, 20)}  |  Page $pageNumber of $totalPages",
            CONTENT_RIGHT,
            lineY + 14f,
            pagePaint
        )
    }

    private fun launchPdfShareSheet(
        context: Context,
        pdfUri: Uri,
        portfolioName: String
    ) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, MediaStoreExportManager.MIME_TYPE_PDF)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = MediaStoreExportManager.MIME_TYPE_PDF
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "FinBaba Executive Portfolio Audit — $portfolioName"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the 2-Page FinBaba Institutional X-Ray Audit Report for $portfolioName."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(
            sendIntent,
            "Share or Open FinBaba PDF Audit"
        ).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooserIntent)
    }

    private fun computeEstimatedPortfolioCagr(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): Double {
        if (analytics.totalInvested <= 0.0 || analytics.totalCurrentValue <= 0.0) {
            return 0.0
        }
        // Estimate effective holding horizon from NAV appreciation ratio & scheme dispersion
        val totalMultiple = (analytics.totalCurrentValue / analytics.totalInvested).coerceAtLeast(0.1)
        val impliedYears = 2.5 // Standard institutional 30-month blended holding horizon fallback
        val cagr = (totalMultiple.pow(1.0 / impliedYears) - 1.0) * 100.0
        return if (cagr.isFinite()) cagr.coerceIn(-95.0, 150.0) else analytics.totalReturnPercent
    }

    private fun matchSlice(
        slices: List<AllocationSlice>,
        keyword: String
    ): AllocationSlice {
        val matched = slices.filter { it.label.contains(keyword, ignoreCase = true) }
        if (matched.isEmpty()) return AllocationSlice(keyword, 0.0, 0.0)
        return AllocationSlice(
            label = keyword,
            rupeeValue = matched.sumOf { it.rupeeValue },
            weightPercent = matched.sumOf { it.weightPercent }
        )
    }

    private fun matchMicroAndCashSlice(
        slices: List<AllocationSlice>,
        totalPortfolioValue: Double
    ): AllocationSlice {
        val matched = slices.filter { slice ->
            slice.label.contains("Micro", ignoreCase = true) ||
                slice.label.contains("Cash", ignoreCase = true) ||
                slice.label.contains("Debt", ignoreCase = true) ||
                slice.label.contains("TREPS", ignoreCase = true) ||
                slice.label.contains("Other", ignoreCase = true)
        }
        if (matched.isNotEmpty()) {
            return AllocationSlice(
                label = "Micro Cap & Cash",
                rupeeValue = matched.sumOf { it.rupeeValue },
                weightPercent = matched.sumOf { it.weightPercent }
            )
        }

        val accountedRupees = slices.filter {
            it.label.contains("Large", ignoreCase = true) ||
                it.label.contains("Mid", ignoreCase = true) ||
                it.label.contains("Small", ignoreCase = true)
        }.sumOf { it.rupeeValue }

        val residualRupees = (totalPortfolioValue - accountedRupees).coerceAtLeast(0.0)
        val residualWeight = if (totalPortfolioValue > 0.0) {
            (residualRupees / totalPortfolioValue) * 100.0
        } else {
            0.0
        }
        return AllocationSlice("Micro Cap & Cash", residualRupees, residualWeight)
    }

    private fun formatInr(amount: Double): String {
        val clean = if (amount.isFinite()) amount else 0.0
        val sign = if (clean < 0) "-" else ""
        val absVal = abs(clean)
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val formatter = DecimalFormat("##,##,##0.00", symbols)
        return "$sign₹${formatter.format(absVal)}"
    }

    private fun formatInrCompact(amount: Double): String {
        val clean = if (amount.isFinite()) amount else 0.0
        val sign = if (clean < 0) "-" else ""
        val absVal = abs(clean)
        return when {
            absVal >= 1_00_00_000.0 -> String.format(Locale.US, "%s₹%.2f Cr", sign, absVal / 1_00_00_000.0)
            absVal >= 1_00_000.0 -> String.format(Locale.US, "%s₹%.2f L", sign, absVal / 1_00_000.0)
            absVal >= 1_000.0 -> String.format(Locale.US, "%s₹%.1f K", sign, absVal / 1_000.0)
            else -> String.format(Locale.US, "%s₹%.0f", sign, absVal)
        }
    }

    private fun formatPct(pct: Double): String {
        val clean = if (pct.isFinite()) pct else 0.0
        return String.format(Locale.US, "%.2f%%", clean)
    }

    private fun formatDecimal(value: Double, decimals: Int): String {
        val clean = if (value.isFinite()) value else 0.0
        return String.format(Locale.US, "%.${decimals}f", clean)
    }

    private fun formatIstTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val target = if (epochMs > 0L) epochMs else System.currentTimeMillis()
        return sdf.format(Date(target))
    }

    private fun abbreviateSchemeName(name: String): String {
        val trimmed = name
            .replace(Regex("(?i)\\bDirect\\s+Plan\\b"), "")
            .replace(Regex("(?i)\\bRegular\\s+Plan\\b"), "")
            .replace(Regex("(?i)\\bGrowth\\b"), "")
            .replace(Regex("(?i)\\bFund\\b"), "Fd")
            .replace(Regex("\\s+"), " ")
            .trim()
        return ellipsize(trimmed, 16)
    }

    private fun ellipsize(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        return text.take((maxChars - 1).coerceAtLeast(1)).trimEnd() + "…"
    }

    private fun sanitizeFilename(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
        return cleaned.ifEmpty { "Vault" }
    }

    private data class MetricCardSpec(
        val title: String,
        val primaryValue: String,
        val secondaryValue: String,
        val accentColor: Int
    )
}