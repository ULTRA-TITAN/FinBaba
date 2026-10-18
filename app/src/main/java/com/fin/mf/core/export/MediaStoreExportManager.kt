package com.fin.mf.core.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Manages Scoped Storage exports into the device's public `Downloads` directory
 * across Android 10+ (API 29 to 35 via [MediaStore.Downloads]) and legacy Android
 * versions (API 28 and below), plus `FileProvider` URI resolution and system
 * share/open intent choosers.
 */
class MediaStoreExportManager(
    private val workbookGenerator: OpenXmlXlsxWorkbookGenerator = OpenXmlXlsxWorkbookGenerator()
) {

    companion object {
        const val MIME_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val MIME_TYPE_CSV = "text/csv"
        const val MIME_TYPE_PDF = "application/pdf"

        private const val SUBFOLDER_NAME = "FinBaba"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    }

    /**
     * Generates the 4-tab institutional OpenXML `.xlsx` audit workbook and saves it
     * directly to the user's `Downloads` directory.
     *
     * Filename pattern: `FinBaba_XRay_Audit_${portfolioName}_${timestamp}.xlsx`
     */
    fun exportExcelFile(
        context: Context,
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): Result<Uri> = runCatching {
        val sanitizedPortfolioName = sanitizeFilenameSegment(portfolio.portfolioName)
        val timestamp = buildTimestampToken()
        val displayFilename = "FinBaba_XRay_Audit_${sanitizedPortfolioName}_${timestamp}.xlsx"

        writeToDownloads(
            context = context,
            displayName = displayFilename,
            mimeType = MIME_TYPE_EXCEL
        ) { outputStream ->
            workbookGenerator.generateWorkbook(
                portfolio = portfolio,
                analytics = analytics,
                outputStream = outputStream
            )
        }
    }

    /**
     * Generates a RFC-4180 compliant UTF-8 (with BOM for Excel compatibility) CSV
     * snapshot of the portfolio schemes and saves it to the `Downloads` directory.
     *
     * Filename pattern: `FinBaba_Portfolio_${portfolioName}_${timestamp}.csv`
     */
    fun exportCsvFile(
        context: Context,
        portfolio: PortfolioData
    ): Result<Uri> = runCatching {
        val sanitizedPortfolioName = sanitizeFilenameSegment(portfolio.portfolioName)
        val timestamp = buildTimestampToken()
        val displayFilename = "FinBaba_Portfolio_${sanitizedPortfolioName}_${timestamp}.csv"

        writeToDownloads(
            context = context,
            displayName = displayFilename,
            mimeType = MIME_TYPE_CSV
        ) { outputStream ->
            writePortfolioCsvToStream(portfolio, outputStream)
        }
    }

    /**
     * Exports a pre-rendered PDF stream or byte writer to `Downloads` with `application/pdf`.
     */
    fun exportPdfFile(
        context: Context,
        portfolioName: String,
        pdfWriter: (OutputStream) -> Unit
    ): Result<Uri> = runCatching {
        val sanitizedPortfolioName = sanitizeFilenameSegment(portfolioName)
        val timestamp = buildTimestampToken()
        val displayFilename = "FinBaba_XRay_Report_${sanitizedPortfolioName}_${timestamp}.pdf"

        writeToDownloads(
            context = context,
            displayName = displayFilename,
            mimeType = MIME_TYPE_PDF,
            writer = pdfWriter
        )
    }

    /**
     * Launches a system chooser combining `Intent.ACTION_VIEW` and `Intent.ACTION_SEND`
     * with `FLAG_GRANT_READ_URI_PERMISSION` so the user can immediately view or share
     * the exported `.xlsx`, `.csv`, or `.pdf` artifact.
     */
    fun dispatchShareOrOpenChooser(
        context: Context,
        exportedUri: Uri,
        mimeType: String,
        chooserTitle: String = "Open or Share FinBaba Export"
    ) {
        val safeUri = ensureContentUri(context, exportedUri)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(safeUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, safeUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooserIntent)
    }

    /**
     * Dispatches an explicit `Intent.ACTION_VIEW` chooser for opening the exported file
     * in Microsoft Excel, Google Sheets, or a PDF viewer.
     */
    fun dispatchViewChooser(
        context: Context,
        exportedUri: Uri,
        mimeType: String,
        chooserTitle: String = "Open with"
    ) {
        val safeUri = ensureContentUri(context, exportedUri)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(safeUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ========================================================================
    // SCOPED STORAGE (API 29-35) & LEGACY DOWNLOADS WRITER
    // ========================================================================

    private fun writeToDownloads(
        context: Context,
        displayName: String,
        mimeType: String,
        writer: (OutputStream) -> Unit
    ): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + SUBFOLDER_NAME

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: throw IOException("Failed to create MediaStore.Downloads entry for $displayName")

            try {
                resolver.openOutputStream(itemUri, "w")?.use { outStream ->
                    writer(outStream)
                } ?: throw IOException("ContentResolver returned null OutputStream for $itemUri")

                val publishValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(itemUri, publishValues, null, null)
                itemUri
            } catch (t: Throwable) {
                resolver.delete(itemUri, null, null)
                throw t
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadsRoot, SUBFOLDER_NAME)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw IOException("Unable to create target directory: ${targetDir.absolutePath}")
            }

            val targetFile = File(targetDir, displayName)
            FileOutputStream(targetFile).use { fos ->
                writer(fos)
                fos.fd.sync()
            }

            ensureContentUri(context, Uri.fromFile(targetFile))
        }
    }

    /**
     * Converts a `file://` URI into a secure `content://` URI via [FileProvider]
     * using `${context.packageName}.fileprovider`. If the URI is already a `content://`
     * URI (such as from `MediaStore.Downloads`), it is returned directly.
     */
    private fun ensureContentUri(context: Context, uri: Uri): Uri {
        if (uri.scheme == "content") {
            return uri
        }
        val filePath = uri.path ?: throw IllegalArgumentException("Invalid File URI path: $uri")
        val file = File(filePath)
        val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
        return FileProvider.getUriForFile(context, authority, file)
    }

    // ========================================================================
    // RFC-4180 CSV GENERATOR
    // ========================================================================

    private fun writePortfolioCsvToStream(
        portfolio: PortfolioData,
        outputStream: OutputStream
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

        // Write UTF-8 BOM so Microsoft Excel renders ₹ symbol properly
        writer.write("\uFEFF")

        // Vault Metadata Header Comments
        val syncFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'IST'", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date(if (portfolio.lastSyncedEpochMs > 0L) portfolio.lastSyncedEpochMs else System.currentTimeMillis()))

        writer.write(
            csvRow(
                "Vault Name", portfolio.portfolioName,
                "Owner Name", portfolio.ownerName,
                "Last Synced", syncFormatted
            )
        )
        writer.newLine()

        writer.write(
            csvRow(
                "SEBI Mandate Targets (%)",
                "Large Cap: ${format2Dec(portfolio.marketCapMandate.largeCapTarget)}%",
                "Mid Cap: ${format2Dec(portfolio.marketCapMandate.midCapTarget)}%",
                "Small Cap: ${format2Dec(portfolio.marketCapMandate.smallCapTarget)}%",
                "Micro Cap: ${format2Dec(portfolio.marketCapMandate.microCapTarget)}%"
            )
        )
        writer.newLine()
        writer.newLine()

        // Column Headers
        writer.write(
            csvRow(
                "Scheme Code",
                "Scheme Name",
                "AMC",
                "Category",
                "Units",
                "Avg Buy NAV (INR)",
                "Current NAV (INR)",
                "Invested Amount (INR)",
                "Current Value (INR)",
                "Absolute Gain (INR)",
                "Gain (%)",
                "Portfolio Weight (%)",
                "TER (%)",
                "Equity Split (%)",
                "Debt Split (%)",
                "Cash Split (%)"
            )
        )
        writer.newLine()

        val totalPortfolioVal = portfolio.schemes.sumOf { it.currentValue }
        val totalInvestedVal = portfolio.schemes.sumOf { it.investedAmount }

        portfolio.schemes
            .sortedByDescending { it.currentValue }
            .forEach { scheme ->
                val absGain = scheme.absoluteGain
                val effectiveWeight = if (totalPortfolioVal > 0.0) {
                    (scheme.currentValue / totalPortfolioVal) * 100.0
                } else {
                    0.0
                }

                val eqSplit = if (scheme.assetClass.contains("Debt", ignoreCase = true)) 0.0 else if (scheme.assetClass.contains("Hybrid", ignoreCase = true)) 65.0 else 100.0
                val dtSplit = if (scheme.assetClass.contains("Debt", ignoreCase = true)) 100.0 else if (scheme.assetClass.contains("Hybrid", ignoreCase = true)) 30.0 else 0.0
                val caSplit = if (scheme.assetClass.contains("Hybrid", ignoreCase = true)) 5.0 else 0.0

                writer.write(
                    csvRow(
                        scheme.schemeCode.toString(),
                        scheme.schemeName,
                        scheme.amc,
                        scheme.category,
                        format4Dec(scheme.units),
                        format4Dec(scheme.averageBuyNav),
                        format4Dec(scheme.fallbackNav),
                        format2Dec(scheme.investedAmount),
                        format2Dec(scheme.currentValue),
                        format2Dec(absGain),
                        format2Dec(scheme.absoluteReturnPercent),
                        format2Dec(effectiveWeight),
                        format2Dec(scheme.expenseRatio),
                        format2Dec(eqSplit),
                        format2Dec(dtSplit),
                        format2Dec(caSplit)
                    )
                )
                writer.newLine()
            }

        // Summary Footer Row
        val totalAbsGain = totalPortfolioVal - totalInvestedVal
        val totalReturnPct = if (totalInvestedVal > 0.0) {
            (totalAbsGain / totalInvestedVal) * 100.0
        } else {
            0.0
        }

        writer.write(
            csvRow(
                "TOTAL",
                "${portfolio.schemes.size} Schemes",
                "",
                "",
                "",
                "",
                "",
                format2Dec(totalInvestedVal),
                format2Dec(totalPortfolioVal),
                format2Dec(totalAbsGain),
                format2Dec(totalReturnPct),
                "100.00",
                "",
                "",
                "",
                "",
                ""
            )
        )
        writer.newLine()
        writer.flush()
    }

    private fun csvRow(vararg fields: String): String {
        return fields.joinToString(separator = ",") { field ->
            escapeCsvField(field)
        }
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun sanitizeFilenameSegment(rawName: String): String {
        val cleaned = rawName
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
        return cleaned.ifEmpty { "Vault" }
    }

    private fun buildTimestampToken(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    private fun format2Dec(value: Double): String {
        val clean = if (value.isFinite()) value else 0.0
        return String.format(Locale.US, "%.2f", clean)
    }

    private fun format4Dec(value: Double): String {
        val clean = if (value.isFinite()) value else 0.0
        return String.format(Locale.US, "%.4f", clean)
    }
}
