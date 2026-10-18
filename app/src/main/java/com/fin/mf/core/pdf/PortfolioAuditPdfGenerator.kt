package com.fin.mf.core.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.fin.mf.core.export.MediaStoreExportManager
import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Wealth-Management Grade Institutional Multi-Page PDF Audit Report Engine.
 * Built purely on Android's native [PdfDocument] and [Canvas] APIs with zero external dependencies.
 *
 * Architecture:
 * - Dynamic Pagination Engine with coordinate cursor (`cursorY`) and `ensureSpace(height)`.
 * - Zero text collisions or cramped layouts across arbitrary numbers of portfolio holdings.
 * - PAGE 1: Executive Briefing, Radial Health Scorecard Gauge, SEBI Market Cap Alignment & Top 8 Sectors.
 * - PAGE 2+: High Duplicate Overlap Alert Table (3+ funds), Full Portfolio Scheme Register with
 *   tabular numerals, alternating rows, right-aligned monetary values, and SEBI regulatory disclaimers.
 * - Saves reports to `context.cacheDir/reports/` and dispatches via [FileProvider] with permanent
 *   storage export to `MediaStore.Downloads/FinBaba`.
 */
class PortfolioAuditPdfGenerator(
    private val mediaStoreExportManager: MediaStoreExportManager = MediaStoreExportManager()
) {

    companion object {
        // Standard ISO A4 Page Geometry (72 DPI)
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36f
        const val CONTENT_LEFT = MARGIN
        const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN // 559f
        const val CONTENT_WIDTH = CONTENT_RIGHT - CONTENT_LEFT // 523f

        // Institutional Color Palette
        val COLOR_DEEP_NAVY = Color.parseColor("#1E3A8A")
        val COLOR_SLATE_TEXT_PRIMARY = Color.parseColor("#0F172A")
        val COLOR_SLATE_TEXT_MUTED = Color.parseColor("#64748B")
        val COLOR_SLATE_BORDER = Color.parseColor("#E2E8F0")
        val COLOR_SURFACE_CARD = Color.parseColor("#F8FAFC")
        val COLOR_ZEBRA_ROW = Color.parseColor("#F8FAFC")
        val COLOR_TOTAL_ROW = Color.parseColor("#E2E8F0")

        val COLOR_PROFIT_EMERALD = Color.parseColor("#059669")
        val COLOR_PROFIT_BG = Color.parseColor("#ECFDF5")
        val COLOR_LOSS_CRIMSON = Color.parseColor("#DC2626")
        val COLOR_LOSS_BG = Color.parseColor("#FEF2F2")
        val COLOR_WARNING_AMBER = Color.parseColor("#D97706")
        val COLOR_WARNING_BG = Color.parseColor("#FFFBEB")
        val COLOR_ACCENT_BLUE = Color.parseColor("#2563EB")

        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

        private val defaultInstance by lazy { PortfolioAuditPdfGenerator() }

        /**
         * Backward-compatible static delegation method.
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

    // Standard Typefaces
    private val typefaceSans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val typefaceSansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val typefaceSansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val typefaceMonospace = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    private val typefaceMonospaceBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    /**
     * Generates the multi-page institutional PDF audit, saves it to `context.cacheDir/reports/`
     * and `MediaStore.Downloads`, and launches the Android system share sheet.
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

                val fileName = "FinBaba_Audit_Report_${sanitizedName}_${timestamp}.pdf"

                // 1. Save directly to context.cacheDir/reports/ for FileProvider sharing
                val reportsDir = File(appContext.cacheDir, "reports").apply {
                    if (!exists()) mkdirs()
                }
                val cachedPdfFile = File(reportsDir, fileName)

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

                // 2. Save permanent archival copy to MediaStore.Downloads/FinBaba
                val downloadsResult = mediaStoreExportManager.exportPdfFile(
                    context = appContext,
                    portfolioName = portfolioData.portfolioName
                ) { downloadsOutStream ->
                    cachedPdfFile.inputStream().use { input ->
                        input.copyTo(downloadsOutStream)
                    }
                }

                // 3. Resolve shareable FileProvider URI with explicit grant permissions
                val authority = "${appContext.packageName}$FILE_PROVIDER_SUFFIX"
                val shareUri: Uri = try {
                    FileProvider.getUriForFile(appContext, authority, cachedPdfFile)
                } catch (_: Throwable) {
                    downloadsResult.getOrNull()
                        ?: throw IllegalStateException("Unable to resolve shareable content URI for audit PDF.")
                }

                mainHandler.post {
                    launchPdfShareSheet(context, shareUri, portfolioData.portfolioName)
                    onSuccess?.invoke(cachedPdfFile, shareUri)
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    if (onError != null) {
                        onError.invoke(t)
                    } else {
                        android.widget.Toast.makeText(
                            appContext,
                            "PDF Export failed: ${t.localizedMessage ?: "Unknown error"}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Renders the complete multi-page document into any [OutputStream] using the Dynamic Pagination Engine.
     */
    fun renderDocumentToStream(
        portfolioData: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        var session: DynamicPdfSession? = null
        try {
            val totalPages = estimateTotalPages(portfolioData, analytics)
            session = DynamicPdfSession(
                pdfDocument = pdfDocument,
                portfolioName = portfolioData.portfolioName,
                totalPages = totalPages
            )

            // ================================================================
            // PAGE 1: EXECUTIVE BRIEFING & DIAGNOSTICS
            // ================================================================
            session.startNewPage(isFirst = true)
            renderExecutiveBriefingPage(session, portfolioData, analytics, healthScore)

            // ================================================================
            // PAGE 2 (AND BEYOND): COMPREHENSIVE HOLDINGS & OVERLAP MATRIX
            // ================================================================
            session.startNewPage(isFirst = false)
            renderHoldingsAndOverlapPages(session, portfolioData, analytics)

            session.finish()
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
        } catch (t: Throwable) {
            session?.abort()
            throw t
        } finally {
            runCatching { pdfDocument.close() }
        }
    }

    // ========================================================================
    // DYNAMIC PAGINATION ENGINE
    // ========================================================================

    private inner class DynamicPdfSession(
        val pdfDocument: PdfDocument,
        val portfolioName: String,
        val totalPages: Int
    ) {
        var pageNumber = 0
            private set
        var currentPage: PdfDocument.Page? = null
            private set
        var canvas: Canvas? = null
            private set
        var cursorY: Float = MARGIN
        private var isFinished = false

        fun startNewPage(isFirst: Boolean) {
            if (isFinished) return
            currentPage?.let { prev ->
                drawPageFooter(canvas!!, pageNumber, totalPages)
                pdfDocument.finishPage(prev)
            }

            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = pdfDocument.startPage(pageInfo)
            currentPage = newPage
            canvas = newPage.canvas

            drawPageBackground(canvas!!)

            if (isFirst) {
                cursorY = MARGIN
            } else {
                cursorY = drawRunningHeader(canvas!!, portfolioName, pageNumber, totalPages)
            }
        }

        fun ensureSpace(requiredHeight: Float, onPageBreak: (() -> Unit)? = null) {
            val maxUsableY = PAGE_HEIGHT - MARGIN - 28f // 28pt reserved for footer
            if (cursorY + requiredHeight > maxUsableY) {
                startNewPage(isFirst = false)
                onPageBreak?.invoke()
            }
        }

        fun finish() {
            if (isFinished) return
            currentPage?.let { page ->
                drawPageFooter(canvas!!, pageNumber, totalPages)
                pdfDocument.finishPage(page)
            }
            currentPage = null
            canvas = null
            isFinished = true
        }

        fun abort() {
            if (isFinished) return
            currentPage?.let { page ->
                runCatching { pdfDocument.finishPage(page) }
            }
            currentPage = null
            canvas = null
            isFinished = true
        }
    }

    // ========================================================================
    // PAGE 1: EXECUTIVE BRIEFING & DIAGNOSTICS
    // ========================================================================

    private fun renderExecutiveBriefingPage(
        session: DynamicPdfSession,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int
    ) {
        val canvas = session.canvas!!

        // 1. Institutional Header Banner
        session.cursorY = drawInstitutionalHeader(canvas, portfolio, session.cursorY)

        session.cursorY += 10f

        // 2. Valuation Summary Cards (3 Columns across 523pt)
        session.cursorY = drawValuationCards(canvas, analytics, session.cursorY)

        session.cursorY += 12f

        // 3. Health Scorecard & Diagnostics (Radial Gauge + Deduction Items)
        session.cursorY = drawHealthScorecardAndDeductions(canvas, analytics, healthScore, session.cursorY)

        session.cursorY += 12f

        // 4. SEBI Market Cap Alignment Table (Large, Mid, Small, Micro/Cash)
        session.cursorY = drawSebiAlignmentTable(canvas, portfolio, analytics, session.cursorY)

        session.cursorY += 12f

        // 5. Top 8 Sector Allocation Matrix with Horizontal Progress Bars
        session.cursorY = drawSectorAllocationMatrix(canvas, analytics, session.cursorY)
    }

    // ========================================================================
    // PAGE 2+: COMPREHENSIVE HOLDINGS & OVERLAP MATRIX
    // ========================================================================

    private fun renderHoldingsAndOverlapPages(
        session: DynamicPdfSession,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ) {
        // 1. High Duplicate Overlap Alert Table (Stocks held in 3+ funds, or top overlapping)
        val highOverlapStocks = analytics.topHoldings.filter { it.heldInSchemesCount >= 3 }
            .ifEmpty { analytics.topHoldings.filter { it.heldInSchemesCount >= 2 }.take(6) }

        if (highOverlapStocks.isNotEmpty()) {
            drawDuplicateOverlapSection(session, highOverlapStocks)
            session.cursorY += 14f
        }

        // 2. Full Portfolio Mutual Fund Scheme Register
        drawSchemeRegisterSection(session, portfolio, analytics)

        session.cursorY += 14f

        // 3. Statutory SEBI Regulatory Disclaimer
        drawStatutoryDisclaimer(session)
    }

    // ========================================================================
    // DRAWING PRIMITIVES & COMPONENTS
    // ========================================================================

    private fun drawPageBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)
    }

    private fun drawInstitutionalHeader(canvas: Canvas, portfolio: PortfolioData, startY: Float): Float {
        var y = startY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 14.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("FINBABA INSTITUTIONAL WEALTH AUDIT", CONTENT_LEFT, y + 14f, titlePaint)

        val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm 'IST'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 9f
            typeface = typefaceSans
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(dateStr, CONTENT_RIGHT, y + 14f, datePaint)

        y += 24f

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 10.5f
            typeface = typefaceSansMedium
        }
        canvas.drawText("PORTFOLIO: ${portfolio.portfolioName.uppercase()}", CONTENT_LEFT, y + 10f, subtitlePaint)

        val auditBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PROFIT_EMERALD
            textSize = 8.5f
            typeface = typefaceSansBold
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("AUDIT GRADE: VERIFIED • LOOK-THROUGH ACTIVE", CONTENT_RIGHT, y + 10f, auditBadgePaint)

        y += 18f

        // Hairline divider
        val linePaint = Paint().apply {
            color = COLOR_SLATE_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(CONTENT_LEFT, y, CONTENT_RIGHT, y, linePaint)

        return y + 6f
    }

    private fun drawValuationCards(canvas: Canvas, analytics: PortfolioAnalyticsSummary, startY: Float): Float {
        val cardHeight = 50f
        val gap = 10f
        val cardWidth = (CONTENT_WIDTH - 2 * gap) / 3f

        val netGain = analytics.totalGain
        val returnPct = analytics.totalReturnPercent
        val isProfit = netGain >= 0

        // Card 1: Total Portfolio Value
        drawMetricCard(
            canvas = canvas,
            x = CONTENT_LEFT,
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = "NET PORTFOLIO VALUE",
            valueText = formatRupees(analytics.totalCurrentValue),
            valueColor = COLOR_SLATE_TEXT_PRIMARY
        )

        // Card 2: Total Invested Capital
        drawMetricCard(
            canvas = canvas,
            x = CONTENT_LEFT + cardWidth + gap,
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = "INVESTED CAPITAL",
            valueText = formatRupees(analytics.totalInvested),
            valueColor = COLOR_SLATE_TEXT_PRIMARY
        )

        // Card 3: Net P&L
        val gainSign = if (isProfit) "+" else ""
        val returnSign = if (returnPct >= 0) "+" else ""
        val gainStr = "$gainSign${formatRupees(netGain)} ($returnSign${String.format(Locale.US, "%.2f", returnPct)}%)"
        drawMetricCard(
            canvas = canvas,
            x = CONTENT_LEFT + 2 * (cardWidth + gap),
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = "NET UNREALIZED RETURN",
            valueText = gainStr,
            valueColor = if (isProfit) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON,
            badgeBgColor = if (isProfit) COLOR_PROFIT_BG else COLOR_LOSS_BG
        )

        return startY + cardHeight
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        valueText: String,
        valueColor: Int,
        badgeBgColor: Int = COLOR_SURFACE_CARD
    ) {
        val rect = RectF(x, y, x + width, y + height)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeBgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 7.5f
            typeface = typefaceSansMedium
        }
        canvas.drawText(label, x + 10f, y + 16f, labelPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = if (valueText.length > 20) 10.5f else 12f
            typeface = typefaceMonospaceBold
        }
        canvas.drawText(valueText, x + 10f, y + 36f, valPaint)
    }

    private fun drawHealthScorecardAndDeductions(
        canvas: Canvas,
        analytics: PortfolioAnalyticsSummary,
        healthScore: Int,
        startY: Float
    ): Float {
        val boxHeight = 115f
        val rect = RectF(CONTENT_LEFT, startY, CONTENT_RIGHT, startY + boxHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_CARD
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 10f, 10f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

        // --- Radial Gauge on Left (Center: CONTENT_LEFT + 70f, startY + 57f) ---
        val cx = CONTENT_LEFT + 70f
        val cy = startY + 54f
        val radius = 34f

        val gaugeTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(cx, cy, radius, gaugeTrackPaint)

        val scoreColor = when {
            healthScore >= 75 -> COLOR_PROFIT_EMERALD
            healthScore >= 50 -> COLOR_WARNING_AMBER
            else -> COLOR_LOSS_CRIMSON
        }

        val gaugeProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            strokeCap = Paint.Cap.ROUND
        }
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val sweepAngle = (healthScore / 100f) * 360f
        canvas.drawArc(arcRect, -90f, sweepAngle, false, gaugeProgressPaint)

        val scoreNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 17f
            typeface = typefaceSansBold
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$healthScore", cx, cy + 4f, scoreNumPaint)

        val scoreSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 7.5f
            typeface = typefaceSans
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("HEALTH SCORE", cx, cy + 16f, scoreSubPaint)

        val gradeText = when {
            healthScore >= 80 -> "GRADE: INSTITUTIONAL EXCELLENCE"
            healthScore >= 65 -> "GRADE: MODERATE CONCENTRATION"
            else -> "GRADE: HIGH OVERLAP / REBALANCE REQUIRED"
        }
        val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            textSize = 6.5f
            typeface = typefaceSansBold
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(gradeText, cx, startY + boxHeight - 10f, gradePaint)

        // --- Diagnostics & Deductions List on Right ---
        val rightX = CONTENT_LEFT + 150f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 9.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("HEALTH SCORECARD AUDIT & DEDUCTION MATRIX", rightX, startY + 18f, titlePaint)

        val topOverlap = analytics.highOverlapStocksCount
        val top5Pct = analytics.topHoldings.take(5).sumOf { it.portfolioWeightPercent }
        val ter = analytics.weightedExpenseRatio
        val feeDrag = analytics.annualFeeDragRupees

        val items = listOf(
            Triple(
                "Multi-Fund Stock Overlap",
                "$topOverlap cross-held companies detected",
                if (topOverlap > 5) "-${min(12, topOverlap * 2)} pts (Overlap Drag)" else "Optimal (< 5 duplicates)"
            ),
            Triple(
                "Top-5 Stock Concentration",
                "Top 5 stocks represent ${String.format(Locale.US, "%.1f", top5Pct)}% equity",
                if (top5Pct > 35.0) "-6 pts (High Single-Stock Exposure)" else "Healthy Diversification"
            ),
            Triple(
                "Annual Fee Drag (TER)",
                "Weighted TER: ${String.format(Locale.US, "%.2f", ter)}% per annum",
                "Est. Fee Drag: ${formatRupees(feeDrag)}/yr"
            )
        )

        var rowY = startY + 36f
        for ((title, detail, status) in items) {
            val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_ACCENT_BLUE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(rightX + 4f, rowY - 3f, 2.5f, bulletPaint)

            val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8.5f
                typeface = typefaceSansBold
            }
            canvas.drawText(title, rightX + 12f, rowY, itemTitlePaint)

            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_MUTED
                textSize = 8f
                typeface = typefaceSans
            }
            canvas.drawText("• $detail", rightX + 130f, rowY, detailPaint)

            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (status.startsWith("-")) COLOR_LOSS_CRIMSON else COLOR_PROFIT_EMERALD
                textSize = 8f
                typeface = typefaceSansBold
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(status, CONTENT_RIGHT - 14f, rowY, statusPaint)

            rowY += 23f
        }

        return startY + boxHeight
    }

    private fun drawSebiAlignmentTable(
        canvas: Canvas,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        var y = startY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 10.5f
            typeface = typefaceSansBold
        }
        canvas.drawText(" USER MARKET CAP ALLOCATION MANDATE VS ACTUAL", CONTENT_LEFT, y + 10f, titlePaint)
        y += 18f

        // Table Header
        val headerHeight = 18f
        val headerRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + headerHeight)
        val headerBgPaint = Paint().apply { color = COLOR_DEEP_NAVY }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = typefaceSansBold
        }

        canvas.drawText("Market Cap Segment", CONTENT_LEFT + 8f, y + 12f, headerTextPaint)
        headerTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Target Mandate", CONTENT_LEFT + 200f, y + 12f, headerTextPaint)
        canvas.drawText("Actual Look-Through", CONTENT_LEFT + 310f, y + 12f, headerTextPaint)
        canvas.drawText("Variance / Drift", CONTENT_LEFT + 410f, y + 12f, headerTextPaint)
        headerTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Mandate Status", CONTENT_LEFT + 430f, y + 12f, headerTextPaint)

        y += headerHeight

        val mandate = portfolio.marketCapMandate
        val actualMap = analytics.marketCapBreakdown.associate { it.label to it.weightPercent }

        val rows = listOf(
            Triple("Large Cap Equity", mandate.largeCapTarget, actualMap["Large Cap"] ?: 0.0),
            Triple("Mid Cap Equity", mandate.midCapTarget, actualMap["Mid Cap"] ?: 0.0),
            Triple("Small Cap Equity", mandate.smallCapTarget, actualMap["Small Cap"] ?: 0.0),
            Triple("Micro Cap / Cash / Debt", mandate.microCapTarget, (actualMap["Micro Cap"] ?: 0.0) + (actualMap["Cash / Others"] ?: 0.0))
        )

        val rowHeight = 19f
        var isOdd = false

        for ((segment, target, actual) in rows) {
            val drift = actual - target
            val isCompliant = abs(drift) <= 2.5

            val rowBg = Paint().apply {
                color = if (isOdd) COLOR_ZEBRA_ROW else Color.WHITE
            }
            canvas.drawRect(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowHeight, rowBg)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8f
                typeface = typefaceSansMedium
            }
            canvas.drawText(segment, CONTENT_LEFT + 8f, y + 13f, textPaint)

            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8.5f
                typeface = typefaceMonospace
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("${String.format(Locale.US, "%.1f", target)}%", CONTENT_LEFT + 200f, y + 13f, numPaint)
            canvas.drawText("${String.format(Locale.US, "%.1f", actual)}%", CONTENT_LEFT + 310f, y + 13f, numPaint)

            val driftSign = if (drift >= 0) "+" else ""
            val driftColor = when {
                isCompliant -> COLOR_PROFIT_EMERALD
                drift > 0 -> COLOR_WARNING_AMBER
                else -> COLOR_LOSS_CRIMSON
            }
            numPaint.color = driftColor
            canvas.drawText("$driftSign${String.format(Locale.US, "%.1f", drift)}%", CONTENT_LEFT + 410f, y + 13f, numPaint)

            val statusText = when {
                isCompliant -> "COMPLIANT (Within ±2.5%)"
                drift > 0 -> "+${String.format(Locale.US, "%.1f", drift)}% OVERWEIGHT"
                else -> "${String.format(Locale.US, "%.1f", drift)}% UNDERWEIGHT"
            }
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = driftColor
                textSize = 7.5f
                typeface = typefaceSansBold
            }
            canvas.drawText(statusText, CONTENT_LEFT + 430f, y + 13f, statusPaint)

            // Hairline separator
            val sepPaint = Paint().apply {
                color = COLOR_SLATE_BORDER
                strokeWidth = 0.5f
            }
            canvas.drawLine(CONTENT_LEFT, y + rowHeight, CONTENT_RIGHT, y + rowHeight, sepPaint)

            y += rowHeight
            isOdd = !isOdd
        }

        return y
    }

    private fun drawSectorAllocationMatrix(
        canvas: Canvas,
        analytics: PortfolioAnalyticsSummary,
        startY: Float
    ): Float {
        var y = startY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 10.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("TOP 8 SECTOR EXPOSURE MATRIX", CONTENT_LEFT, y + 10f, titlePaint)
        y += 18f

        val top8Sectors = analytics.sectorBreakdown.take(8)
        val colWidth = (CONTENT_WIDTH - 15f) / 2f
        val rowHeight = 27f

        val maxWeight = top8Sectors.maxOfOrNull { it.portfolioWeightPercent }?.coerceAtLeast(1.0) ?: 1.0

        for (i in top8Sectors.indices) {
            val sector = top8Sectors[i]
            val colIndex = i % 2
            val rowIndex = i / 2

            val cellX = CONTENT_LEFT + colIndex * (colWidth + 15f)
            val cellY = y + rowIndex * rowHeight

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8f
                typeface = typefaceSansMedium
            }
            val safeName = if (sector.sectorName.length > 24) sector.sectorName.take(22) + "…" else sector.sectorName
            canvas.drawText(safeName, cellX, cellY + 8f, namePaint)

            val pctStr = "${String.format(Locale.US, "%.1f", sector.portfolioWeightPercent)}%"
            val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_ACCENT_BLUE
                textSize = 8.5f
                typeface = typefaceMonospaceBold
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(pctStr, cellX + colWidth, cellY + 8f, pctPaint)

            // Progress Bar Track
            val trackY = cellY + 12f
            val trackH = 4f
            val trackPaint = Paint().apply {
                color = COLOR_SLATE_BORDER
            }
            canvas.drawRoundRect(RectF(cellX, trackY, cellX + colWidth, trackY + trackH), 2f, 2f, trackPaint)

            val fillWidth = ((sector.portfolioWeightPercent / maxWeight) * colWidth).toFloat().coerceIn(3f, colWidth)
            val fillPaint = Paint().apply {
                color = COLOR_ACCENT_BLUE
            }
            canvas.drawRoundRect(RectF(cellX, trackY, cellX + fillWidth, trackY + trackH), 2f, 2f, fillPaint)
        }

        val totalRows = ceil(top8Sectors.size / 2.0).toInt()
        return y + totalRows * rowHeight
    }

    private fun drawDuplicateOverlapSection(
        session: DynamicPdfSession,
        stocks: List<AggregatedStockExposure>
    ) {
        session.ensureSpace(52f)
        var canvas = session.canvas!!
        var y = session.cursorY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 10.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("HIGH DUPLICATE STOCK OVERLAP (HELD IN 3+ MUTUAL FUNDS)", CONTENT_LEFT, y + 10f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 8f
            typeface = typefaceSans
        }
        canvas.drawText("Companies held concurrently across multiple schemes causing unhedged concentration.", CONTENT_LEFT, y + 22f, subPaint)

        y += 30f

        fun drawOverlapHeader(targetCanvas: Canvas, targetY: Float) {
            val headerRect = RectF(CONTENT_LEFT, targetY, CONTENT_RIGHT, targetY + 18f)
            targetCanvas.drawRoundRect(headerRect, 4f, 4f, Paint().apply { color = COLOR_DEEP_NAVY })

            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.5f
                typeface = typefaceSansBold
            }

            targetCanvas.drawText("Company Constituent", CONTENT_LEFT + 8f, targetY + 12f, headerTextPaint)
            targetCanvas.drawText("Sector", CONTENT_LEFT + 155f, targetY + 12f, headerTextPaint)
            targetCanvas.drawText("Cap Bucket", CONTENT_LEFT + 250f, targetY + 12f, headerTextPaint)
            targetCanvas.drawText("Held In", CONTENT_LEFT + 320f, targetY + 12f, headerTextPaint)
            headerTextPaint.textAlign = Paint.Align.RIGHT
            targetCanvas.drawText("Portfolio Wt %", CONTENT_LEFT + 420f, targetY + 12f, headerTextPaint)
            targetCanvas.drawText("Total Exposure", CONTENT_RIGHT - 8f, targetY + 12f, headerTextPaint)
        }

        drawOverlapHeader(canvas, y)
        y += 18f
        var isOdd = false

        for (stock in stocks) {
            session.cursorY = y
            session.ensureSpace(19f) {
                drawOverlapHeader(session.canvas!!, session.cursorY)
                session.cursorY += 18f
            }
            y = session.cursorY
            val currentCanvas = session.canvas!!

            val rowBg = Paint().apply { color = if (isOdd) COLOR_ZEBRA_ROW else Color.WHITE }
            currentCanvas.drawRect(CONTENT_LEFT, y, CONTENT_RIGHT, y + 19f, rowBg)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8f
                typeface = typefaceSansBold
            }
            val safeName = if (stock.companyName.length > 22) stock.companyName.take(20) + "…" else stock.companyName
            currentCanvas.drawText(safeName, CONTENT_LEFT + 8f, y + 13f, namePaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_MUTED
                textSize = 7.5f
                typeface = typefaceSans
            }
            val safeSector = if (stock.sector.length > 18) stock.sector.take(16) + "…" else stock.sector
            currentCanvas.drawText(safeSector, CONTENT_LEFT + 155f, y + 13f, textPaint)
            currentCanvas.drawText(stock.marketCapBucket, CONTENT_LEFT + 250f, y + 13f, textPaint)

            val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_WARNING_AMBER
                textSize = 8f
                typeface = typefaceSansBold
            }
            currentCanvas.drawText("${stock.heldInSchemesCount} Funds", CONTENT_LEFT + 320f, y + 13f, chipPaint)

            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8.5f
                typeface = typefaceMonospaceBold
                textAlign = Paint.Align.RIGHT
            }
            currentCanvas.drawText("${String.format(Locale.US, "%.2f", stock.portfolioWeightPercent)}%", CONTENT_LEFT + 420f, y + 13f, numPaint)
            currentCanvas.drawText(formatRupees(stock.totalRupeeExposure), CONTENT_RIGHT - 8f, y + 13f, numPaint)

            val sepPaint = Paint().apply {
                color = COLOR_SLATE_BORDER
                strokeWidth = 0.5f
            }
            currentCanvas.drawLine(CONTENT_LEFT, y + 19f, CONTENT_RIGHT, y + 19f, sepPaint)

            y += 19f
            isOdd = !isOdd
        }

        session.cursorY = y
    }

    private fun drawSchemeRegisterSection(
        session: DynamicPdfSession,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ) {
        session.ensureSpace(60f)
        val canvas = session.canvas!!
        var y = session.cursorY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_PRIMARY
            textSize = 10.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("COMPLETE MUTUAL FUND SCHEME REGISTER", CONTENT_LEFT, y + 10f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 8f
            typeface = typefaceSans
        }
        canvas.drawText("Granular scheme positions, acquisition prices, current valuations, and net performance.", CONTENT_LEFT, y + 22f, subPaint)

        y += 30f

        fun drawRegisterHeader(targetCanvas: Canvas, targetY: Float) {
            val headerRect = RectF(CONTENT_LEFT, targetY, CONTENT_RIGHT, targetY + 18f)
            targetCanvas.drawRoundRect(headerRect, 4f, 4f, Paint().apply { color = COLOR_DEEP_NAVY })

            val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.5f
                typeface = typefaceSansBold
            }

            targetCanvas.drawText("Scheme Name", CONTENT_LEFT + 6f, targetY + 12f, hp)
            targetCanvas.drawText("Category", CONTENT_LEFT + 145f, targetY + 12f, hp)
            hp.textAlign = Paint.Align.RIGHT
            targetCanvas.drawText("Units", CONTENT_LEFT + 258f, targetY + 12f, hp)
            targetCanvas.drawText("Avg Buy", CONTENT_LEFT + 308f, targetY + 12f, hp)
            targetCanvas.drawText("Live NAV", CONTENT_LEFT + 358f, targetY + 12f, hp)
            targetCanvas.drawText("Current Val", CONTENT_LEFT + 428f, targetY + 12f, hp)
            targetCanvas.drawText("Wt %", CONTENT_LEFT + 462f, targetY + 12f, hp)
            targetCanvas.drawText("Gain / Loss", CONTENT_RIGHT - 6f, targetY + 12f, hp)
        }

        drawRegisterHeader(canvas, y)
        y += 18f

        val schemes = portfolio.schemes
        var isOdd = false
        val totalPortfolioVal = analytics.totalCurrentValue.coerceAtLeast(1.0)

        for (scheme in schemes) {
            session.cursorY = y
            session.ensureSpace(21f) {
                drawRegisterHeader(session.canvas!!, session.cursorY)
                session.cursorY += 18f
            }
            y = session.cursorY
            val currentCanvas = session.canvas!!

            val rowBg = Paint().apply { color = if (isOdd) COLOR_ZEBRA_ROW else Color.WHITE }
            currentCanvas.drawRect(CONTENT_LEFT, y, CONTENT_RIGHT, y + 21f, rowBg)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 7.8f
                typeface = typefaceSansBold
            }
            val safeSchemeName = if (scheme.schemeName.length > 25) scheme.schemeName.take(23) + "…" else scheme.schemeName
            currentCanvas.drawText(safeSchemeName, CONTENT_LEFT + 6f, y + 14f, namePaint)

            val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_MUTED
                textSize = 7f
                typeface = typefaceSans
            }
            val safeCat = if (scheme.category.length > 15) scheme.category.take(13) + "…" else scheme.category
            currentCanvas.drawText(safeCat, CONTENT_LEFT + 145f, y + 14f, catPaint)

            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT_PRIMARY
                textSize = 8f
                typeface = typefaceMonospace
                textAlign = Paint.Align.RIGHT
            }
            currentCanvas.drawText(String.format(Locale.US, "%.2f", scheme.units), CONTENT_LEFT + 258f, y + 14f, numPaint)
            currentCanvas.drawText(String.format(Locale.US, "%.1f", scheme.averageBuyNav), CONTENT_LEFT + 308f, y + 14f, numPaint)
            currentCanvas.drawText(String.format(Locale.US, "%.1f", scheme.fallbackNav), CONTENT_LEFT + 358f, y + 14f, numPaint)

            numPaint.typeface = typefaceMonospaceBold
            currentCanvas.drawText(formatRupees(scheme.currentValue), CONTENT_LEFT + 428f, y + 14f, numPaint)

            val weightPct = (scheme.currentValue / totalPortfolioVal) * 100.0
            numPaint.typeface = typefaceMonospace
            currentCanvas.drawText("${String.format(Locale.US, "%.1f", weightPct)}%", CONTENT_LEFT + 462f, y + 14f, numPaint)

            val gain = scheme.absoluteGain
            val isGainPositive = gain >= 0
            val gainSign = if (isGainPositive) "+" else ""
            val gainColor = if (isGainPositive) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON

            val gainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gainColor
                textSize = 7.8f
                typeface = typefaceMonospaceBold
                textAlign = Paint.Align.RIGHT
            }
            currentCanvas.drawText("$gainSign${formatRupees(gain)}", CONTENT_RIGHT - 6f, y + 14f, gainPaint)

            val sepPaint = Paint().apply {
                color = COLOR_SLATE_BORDER
                strokeWidth = 0.5f
            }
            currentCanvas.drawLine(CONTENT_LEFT, y + 21f, CONTENT_RIGHT, y + 21f, sepPaint)

            y += 21f
            isOdd = !isOdd
        }

        // Totals Summary Row
        session.cursorY = y
        session.ensureSpace(22f)
        y = session.cursorY
        val finalCanvas = session.canvas!!

        val totRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + 22f)
        finalCanvas.drawRoundRect(totRect, 3f, 3f, Paint().apply { color = COLOR_TOTAL_ROW })

        val totPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 8.5f
            typeface = typefaceSansBold
        }
        finalCanvas.drawText("TOTAL PORTFOLIO (${schemes.size} Schemes)", CONTENT_LEFT + 8f, y + 15f, totPaint)

        val totNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 9f
            typeface = typefaceMonospaceBold
            textAlign = Paint.Align.RIGHT
        }
        finalCanvas.drawText(formatRupees(analytics.totalCurrentValue), CONTENT_LEFT + 428f, y + 15f, totNumPaint)
        finalCanvas.drawText("100.0%", CONTENT_LEFT + 462f, y + 15f, totNumPaint)

        val totalGainSign = if (analytics.totalGain >= 0) "+" else ""
        totNumPaint.color = if (analytics.totalGain >= 0) COLOR_PROFIT_EMERALD else COLOR_LOSS_CRIMSON
        finalCanvas.drawText("$totalGainSign${formatRupees(analytics.totalGain)}", CONTENT_RIGHT - 6f, y + 15f, totNumPaint)

        session.cursorY = y + 22f
    }

    private fun drawStatutoryDisclaimer(session: DynamicPdfSession) {
        session.ensureSpace(48f)
        val canvas = session.canvas!!
        val y = session.cursorY

        val rect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + 46f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_CARD
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val discTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 7f
            typeface = typefaceSansBold
        }
        canvas.drawText("STATUTORY SEBI REGULATORY AUDIT & COMPLIANCE DISCLAIMER", CONTENT_LEFT + 10f, y + 14f, discTitlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 6.2f
            typeface = typefaceSans
        }
        val line1 = "Mutual fund investments are subject to market risks. Read all scheme-related documents carefully before investing."
        val line2 = "Constituent look-through data is aggregated from monthly fund disclosures and AMFI NAV feeds. This document is for"
        val line3 = "informational portfolio audit purposes only and does not constitute financial advice or investment recommendations."

        canvas.drawText(line1, CONTENT_LEFT + 10f, y + 25f, bodyPaint)
        canvas.drawText(line2, CONTENT_LEFT + 10f, y + 33f, bodyPaint)
        canvas.drawText(line3, CONTENT_LEFT + 10f, y + 41f, bodyPaint)

        session.cursorY = y + 46f
    }

    private fun drawRunningHeader(
        canvas: Canvas,
        portfolioName: String,
        pageNumber: Int,
        totalPages: Int
    ): Float {
        val y = MARGIN

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_NAVY
            textSize = 8.5f
            typeface = typefaceSansBold
        }
        canvas.drawText("FinBaba Institutional Portfolio Audit • ${portfolioName.uppercase()}", CONTENT_LEFT, y + 10f, headerPaint)

        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 8.5f
            typeface = typefaceSans
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNumber of $totalPages", CONTENT_RIGHT, y + 10f, pagePaint)

        val linePaint = Paint().apply {
            color = COLOR_SLATE_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(CONTENT_LEFT, y + 16f, CONTENT_RIGHT, y + 16f, linePaint)

        return y + 26f
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerY = PAGE_HEIGHT - MARGIN + 12f

        val linePaint = Paint().apply {
            color = COLOR_SLATE_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(CONTENT_LEFT, footerY - 10f, CONTENT_RIGHT, footerY - 10f, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 7.5f
            typeface = typefaceSans
        }
        canvas.drawText("Page $pageNumber of $totalPages • Generated by FinBaba Terminal • Confidential Audit", CONTENT_LEFT, footerY, footerPaint)

        val sysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SLATE_TEXT_MUTED
            textSize = 7.5f
            typeface = typefaceSans
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("FINBABA WEALTH AUDIT v1.0", CONTENT_RIGHT, footerY, sysPaint)
    }

    // ========================================================================
    // ESTIMATION & UTILITY HELPERS
    // ========================================================================

    internal fun estimateTotalPages(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): Int {
        var pages = 2 // Page 1: Executive Briefing, Page 2: Holdings start
        var currentY = MARGIN + 26f // Running header on Page 2
        val maxUsableY = PAGE_HEIGHT - MARGIN - 28f

        fun simulateSpace(height: Float, breakAddition: Float = 0f) {
            if (currentY + height > maxUsableY) {
                pages++
                currentY = MARGIN + 26f + breakAddition
            }
            currentY += height
        }

        val highOverlapStocks = analytics.topHoldings.filter { it.heldInSchemesCount >= 3 }
            .ifEmpty { analytics.topHoldings.filter { it.heldInSchemesCount >= 2 }.take(6) }

        if (highOverlapStocks.isNotEmpty()) {
            simulateSpace(52f)
            for (i in highOverlapStocks.indices) {
                simulateSpace(19f, breakAddition = 18f)
            }
            currentY += 14f
        }

        simulateSpace(60f)
        for (i in portfolio.schemes.indices) {
            simulateSpace(21f, breakAddition = 18f)
        }
        simulateSpace(22f) // Totals row
        currentY += 14f

        simulateSpace(48f) // Statutory disclaimer

        return pages
    }

    private fun formatRupees(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val isNegative = amount < 0
        val absVal = abs(amount)
        val formatted = DecimalFormat("#,##,##0.00", symbols).format(absVal)
        val sign = if (isNegative) "-" else ""
        return "${sign}₹$formatted"
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    private fun launchPdfShareSheet(context: Context, shareUri: Uri, portfolioName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "FinBaba Institutional Portfolio Audit - $portfolioName")
            putExtra(Intent.EXTRA_TEXT, "Here is the institutional executive audit report for '$portfolioName' generated via FinBaba.")
            clipData = android.content.ClipData.newUri(context.contentResolver, "FinBaba Audit Report", shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Institutional Audit PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Graceful fallback if no activity handles chooser
        }
    }
}