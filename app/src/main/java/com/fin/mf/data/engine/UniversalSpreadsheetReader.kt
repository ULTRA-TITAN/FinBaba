package com.fin.mf.data.engine

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max

/**
 * Zero-dependency streaming reader for CSV, TSV, and Microsoft Excel (.xlsx) files.
 * Designed for low heap footprint and bounded execution (capped at [MAX_ROWS] rows).
 */
object UniversalSpreadsheetReader {

    private const val MAX_ROWS = 5_000
    private const val MAX_COLS = 64
    private const val UTF8_BOM = "\uFEFF"

    /**
     * Reads spreadsheet rows from a [ByteArray] in memory, fully isolating stream
     * lifecycle from coroutine context switches and preventing stream closure exceptions.
     */
    fun readSpreadsheetRowsFromBytes(bytes: ByteArray, filename: String): List<List<String>> {
        val lowerName = filename.lowercase(Locale.ROOT).trim()
        val isXlsxByExt = lowerName.endsWith(".xlsx")
        val isXlsxByMagic = isZipArchive(ByteArrayInputStream(bytes))

        return if (isXlsxByExt || isXlsxByMagic) {
            readXlsxFromBytes(bytes)
        } else {
            readCsvOrTsvStream(ByteArrayInputStream(bytes))
        }
    }

    /**
     * Reads spreadsheet rows from an [InputStream] based on the [filename] extension
     * or ZIP magic-byte header detection.
     */
    fun readSpreadsheetRows(inputStream: InputStream, filename: String): List<List<String>> {
        val bytes = inputStream.readBytes()
        return readSpreadsheetRowsFromBytes(bytes, filename)
    }

    /**
     * Peeks at the first 4 bytes to detect PK\x03\x04 ZIP header used by OpenXML (.xlsx).
     */
    private fun isZipArchive(stream: InputStream): Boolean {
        return try {
            stream.mark(8)
            val header = ByteArray(4)
            val bytesRead = stream.read(header)
            stream.reset()
            bytesRead == 4 &&
                header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // 1. RFC-4180 CSV / TSV STREAMING PARSER
    // =========================================================================

    private fun readCsvOrTsvStream(inputStream: InputStream): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8), 16_384)
        reader.mark(8_192)

        // Peek initial lines to auto-detect delimiter (comma, tab, semicolon)
        val sampleChars = CharArray(4_096)
        val sampleRead = reader.read(sampleChars, 0, sampleChars.size)
        reader.reset()

        val delimiter = if (sampleRead > 0) {
            detectDelimiter(String(sampleChars, 0, sampleRead))
        } else {
            ','
        }

        val rows = ArrayList<List<String>>()
        var currentRow = ArrayList<String>()
        val currentField = StringBuilder()

        var inQuotes = false
        var isFirstCharOfFile = true
        var prevCharWasCr = false

        var intChar = reader.read()
        while (intChar != -1 && rows.size < MAX_ROWS) {
            val ch = intChar.toChar()

            // Strip UTF-8 BOM if present at the very start of the stream
            if (isFirstCharOfFile) {
                isFirstCharOfFile = false
                if (ch == UTF8_BOM[0]) {
                    intChar = reader.read()
                    continue
                }
            }

            if (inQuotes) {
                if (ch == '"') {
                    reader.mark(2)
                    val nextInt = reader.read()
                    if (nextInt != -1 && nextInt.toChar() == '"') {
                        // Escaped quote ("") inside quoted string
                        currentField.append('"')
                    } else {
                        // Closing quote
                        inQuotes = false
                        if (nextInt != -1) {
                            reader.reset()
                        }
                    }
                } else {
                    currentField.append(ch)
                }
                prevCharWasCr = false
            } else {
                when (ch) {
                    '"' -> {
                        if (currentField.isEmpty()) {
                            inQuotes = true
                        } else {
                            currentField.append(ch)
                        }
                        prevCharWasCr = false
                    }
                    delimiter -> {
                        if (currentRow.size < MAX_COLS) {
                            currentRow.add(currentField.toString().trim())
                        }
                        currentField.clear()
                        prevCharWasCr = false
                    }
                    '\r' -> {
                        if (currentRow.size < MAX_COLS) {
                            currentRow.add(currentField.toString().trim())
                        }
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow)
                        }
                        currentRow = ArrayList()
                        prevCharWasCr = true
                    }
                    '\n' -> {
                        if (!prevCharWasCr) {
                            if (currentRow.size < MAX_COLS) {
                                currentRow.add(currentField.toString().trim())
                            }
                            currentField.clear()
                            if (currentRow.any { it.isNotEmpty() }) {
                                rows.add(currentRow)
                            }
                            currentRow = ArrayList()
                        }
                        prevCharWasCr = false
                    }
                    else -> {
                        currentField.append(ch)
                        prevCharWasCr = false
                    }
                }
            }

            intChar = reader.read()
        }

        // Flush trailing field/row if file does not end with a newline
        if ((currentField.isNotEmpty() || currentRow.isNotEmpty()) && rows.size < MAX_ROWS) {
            if (currentRow.size < MAX_COLS) {
                currentRow.add(currentField.toString().trim())
            }
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow)
            }
        }

        return rows
    }

    /**
     * Auto-detects delimiter among comma (,), tab (\t), and semicolon (;)
     * by evaluating consistency and frequency outside quoted segments.
     */
    private fun detectDelimiter(sample: String): Char {
        val candidates = charArrayOf(',', '\t', ';')
        var bestDelimiter = ','
        var bestScore = -1

        val lines = sample.lines().filter { it.isNotBlank() }.take(10)
        if (lines.isEmpty()) return ','

        for (candidate in candidates) {
            var totalCount = 0
            var nonZeroLines = 0
            for (line in lines) {
                var inQuotes = false
                var lineCount = 0
                for (ch in line) {
                    if (ch == '"') inQuotes = !inQuotes
                    else if (!inQuotes && ch == candidate) lineCount++
                }
                if (lineCount > 0) {
                    nonZeroLines++
                    totalCount += lineCount
                }
            }
            val score = nonZeroLines * 100 + totalCount
            if (score > bestScore) {
                bestScore = score
                bestDelimiter = candidate
            }
        }
        return bestDelimiter
    }

    // =========================================================================
    // 2. OPENXML (.XLSX) STREAMING PARSER (ZipInputStream + XmlPullParser)
    // =========================================================================

    private data class RawXlsxCell(
        val colIndex: Int,
        val cellType: String?,
        val rawValue: String
    )

    private fun readXlsxFromBytes(bytes: ByteArray): List<List<String>> {
        var sharedStringsBytes: ByteArray? = null
        var sheetBytes: ByteArray? = null
        var firstSheetBytes: ByteArray? = null

        // 1. Single scan through ZIP archive to buffer XML bytes in memory
        ZipInputStream(ByteArrayInputStream(bytes)).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                val entryName = entry.name.lowercase(Locale.ROOT)
                when {
                    entryName == "xl/sharedstrings.xml" -> {
                        sharedStringsBytes = zipStream.readBytes()
                    }
                    entryName == "xl/worksheets/sheet1.xml" -> {
                        sheetBytes = zipStream.readBytes()
                    }
                    entryName.startsWith("xl/worksheets/sheet") &&
                        entryName.endsWith(".xml") &&
                        firstSheetBytes == null -> {
                        firstSheetBytes = zipStream.readBytes()
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        val targetSheetBytes = sheetBytes ?: firstSheetBytes ?: return emptyList()

        // 2. Parse shared strings if present
        val sharedStrings = ArrayList<String>()
        if (sharedStringsBytes != null) {
            parseSharedStringsXml(ByteArrayInputStream(sharedStringsBytes), sharedStrings)
        }

        // 3. Parse worksheet XML
        val rawSheetRows = parseSheetXml(ByteArrayInputStream(targetSheetBytes))
        if (rawSheetRows.isEmpty()) return emptyList()

        // 4. Resolve shared string indices into final row vectors
        val resolvedRows = ArrayList<List<String>>(rawSheetRows.size)
        for (rawRow in rawSheetRows) {
            if (rawRow.isEmpty()) continue
            val maxCol = rawRow.maxOf { it.colIndex }.coerceAtMost(MAX_COLS - 1)
            val rowValues = MutableList(maxCol + 1) { "" }

            for (cell in rawRow) {
                if (cell.colIndex !in 0..maxCol) continue
                val resolvedText = when (cell.cellType) {
                    "s" -> {
                        val idx = cell.rawValue.trim().toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) {
                            sharedStrings[idx].trim()
                        } else {
                            cell.rawValue.trim()
                        }
                    }
                    "b" -> if (cell.rawValue == "1") "TRUE" else "FALSE"
                    else -> cell.rawValue.trim()
                }
                rowValues[cell.colIndex] = resolvedText
            }

            if (rowValues.any { it.isNotEmpty() }) {
                resolvedRows.add(rowValues)
            }
        }

        return resolvedRows
    }

    /**
     * Streams `xl/sharedStrings.xml` and extracts each `<si>` string item,
     * concatenating rich-text `<r><t>` runs within the same `<si>` node.
     */
    private fun parseSharedStringsXml(
        stream: InputStream,
        outStrings: MutableList<String>
    ) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, StandardCharsets.UTF_8.name())

        var eventType = parser.eventType
        var insideSi = false
        var insideT = false
        val currentSiBuilder = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            currentSiBuilder.clear()
                        }
                        "t" -> {
                            if (insideSi) insideT = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi && insideT) {
                        currentSiBuilder.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> insideT = false
                        "si" -> {
                            if (insideSi) {
                                outStrings.add(currentSiBuilder.toString())
                                insideSi = false
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    /**
     * Streams `xl/worksheets/sheet1.xml`, reading `<row>`, `<c r="A1" t="s">`,
     * `<v>` values, and `<is><t>` inline strings up to [MAX_ROWS].
     */
    private fun parseSheetXml(stream: InputStream): List<List<RawXlsxCell>> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, StandardCharsets.UTF_8.name())

        val rows = ArrayList<List<RawXlsxCell>>()
        var currentRowCells = ArrayList<RawXlsxCell>()

        var insideRow = false
        var insideCell = false
        var insideValue = false
        var insideInlineText = false

        var currentCellType: String? = null
        var currentColIndex = 0
        var nextSequentialCol = 0
        val valueBuilder = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && rows.size < MAX_ROWS) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            insideRow = true
                            currentRowCells = ArrayList()
                            nextSequentialCol = 0
                        }
                        "c" -> {
                            if (insideRow) {
                                insideCell = true
                                valueBuilder.clear()
                                currentCellType = parser.getAttributeValue(null, "t")
                                val cellRef = parser.getAttributeValue(null, "r")
                                currentColIndex = if (!cellRef.isNullOrBlank()) {
                                    parseColumnIndex(cellRef)
                                } else {
                                    nextSequentialCol
                                }
                                nextSequentialCol = max(nextSequentialCol, currentColIndex + 1)
                            }
                        }
                        "v" -> {
                            if (insideCell) insideValue = true
                        }
                        "t" -> {
                            if (insideCell) insideInlineText = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideCell && (insideValue || insideInlineText)) {
                        valueBuilder.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> insideValue = false
                        "t" -> insideInlineText = false
                        "c" -> {
                            if (insideCell) {
                                if (currentColIndex < MAX_COLS) {
                                    currentRowCells.add(
                                        RawXlsxCell(
                                            colIndex = currentColIndex,
                                            cellType = currentCellType,
                                            rawValue = valueBuilder.toString()
                                        )
                                    )
                                }
                                insideCell = false
                            }
                        }
                        "row" -> {
                            if (insideRow) {
                                if (currentRowCells.isNotEmpty()) {
                                    rows.add(currentRowCells)
                                }
                                insideRow = false
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    /**
     * Converts Excel A1-style cell coordinate (e.g. "A1" -> 0, "C12" -> 2, "AA5" -> 26)
     * into a zero-based column index.
     */
    private fun parseColumnIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            val upper = ch.uppercaseChar()
            if (upper in 'A'..'Z') {
                col = col * 26 + (upper - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    /**
     * Prevents [XmlPullParser] from closing the parent [ZipInputStream] when finishing an entry.
     */
    private class NonClosingInputStream(private val delegate: InputStream) : InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray): Int = delegate.read(b)
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun close() {
            // Intentionally no-op so ZipInputStream can advance to nextEntry
        }
    }
}