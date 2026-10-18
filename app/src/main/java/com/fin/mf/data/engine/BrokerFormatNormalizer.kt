package com.fin.mf.data.engine

import java.util.Locale

/**
 * Normalized record extracted from a broker or CAS portfolio spreadsheet row.
 */
data class RawBrokerRecord(
    val rawSchemeName: String,
    val units: Double,
    val investedCost: Double,
    val currentValue: Double? = null,
    val rawCode: Int? = null,
    val rowNumber: Int
)

/**
 * Intelligent table header detector and column mapper supporting CAMS CAS, KFintech,
 * Zerodha Coin, Groww, INDmoney, Kuvera, ETMONEY, and custom portfolio sheets.
 */
object BrokerFormatNormalizer {

    private val SCHEME_NAME_REGEX =
        Regex("(?i).*(scheme|fund|security|scrip|instrument|folio.*desc|particulars|description).*")

    private val UNITS_REGEX =
        Regex("(?i).*(units|unit|qty|quantity|balance.*unit|total.*unit|balance|closing).*")

    private val INVESTED_COST_REGEX =
        Regex("(?i).*(invested|cost.*value|purchase.*cost|purchase.*val|amount.*invested|total.*cost|cost|buy.*value|purchase.*amt|invested.*amount).*")

    private val CURRENT_VALUE_REGEX =
        Regex("(?i).*(current.*val|market.*val|nav.*val|latest.*val|present.*val|valuation|value|avg.*nav|purchase.*nav|buy.*price|cost.*nav).*")

    private val AMFI_OR_ISIN_REGEX =
        Regex("(?i).*(amfi|scheme.*code|isin|fund.*code).*")

    private val SUMMARY_ROW_REGEX =
        Regex("(?i)^\\s*(total|grand\\s*total|sub\\s*total|portfolio\\s*total|summary|disclaimer|note\\b).*")

    private data class ColumnMapping(
        val headerRowIndex: Int,
        val schemeNameCol: Int,
        val unitsCol: Int,
        val investedCostCol: Int,
        val currentValueCol: Int = -1,
        val rawCodeCol: Int = -1
    )

    /**
     * Scans raw spreadsheet rows, locates the header row, maps columns, and extracts
     * valid mutual fund holding records.
     */
    fun normalizeRows(rows: List<List<String>>): List<RawBrokerRecord> {
        if (rows.isEmpty()) return emptyList()

        val mapping = detectHeaderAndColumns(rows) ?: return emptyList()
        val records = ArrayList<RawBrokerRecord>()

        for (rowIdx in (mapping.headerRowIndex + 1) until rows.size) {
            val row = rows[rowIdx]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            val rawName = row.getOrNull(mapping.schemeNameCol)?.trim().orEmpty()
            if (rawName.isBlank() || rawName.length < 3) continue
            if (SUMMARY_ROW_REGEX.matches(rawName)) continue
            // Skip repeated header rows in multi-page exports
            if (SCHEME_NAME_REGEX.matches(rawName) && rawName.length < 25) continue

            val rawUnitsStr = row.getOrNull(mapping.unitsCol).orEmpty()
            val rawInvestedStr = row.getOrNull(mapping.investedCostCol).orEmpty()
            val rawCurrentStr = if (mapping.currentValueCol >= 0) {
                row.getOrNull(mapping.currentValueCol).orEmpty()
            } else {
                ""
            }
            val rawCodeStr = if (mapping.rawCodeCol >= 0) {
                row.getOrNull(mapping.rawCodeCol).orEmpty()
            } else {
                ""
            }

            var units = parseNumericValue(rawUnitsStr) ?: 0.0
            var investedCost = parseNumericValue(rawInvestedStr) ?: 0.0
            var currentValue = parseNumericValue(rawCurrentStr)
            val rawCode = parseAmfiCode(rawCodeStr)

            // If invested cost is missing but current value is present, use current value as fallback
            if (investedCost <= 0.0 && currentValue != null && currentValue > 0.0) {
                investedCost = currentValue
            }
            // If current value is missing but invested cost is present, use invested cost as fallback
            if ((currentValue == null || currentValue <= 0.0) && investedCost > 0.0) {
                currentValue = investedCost
            }

            // Require positive units or positive invested cost to filter out zeroed/closed folios
            if (units <= 0.0 && investedCost <= 0.0) continue

            records.add(
                RawBrokerRecord(
                    rawSchemeName = cleanBrokerSchemeCell(rawName),
                    units = units.coerceAtLeast(0.0),
                    investedCost = investedCost.coerceAtLeast(0.0),
                    currentValue = currentValue?.coerceAtLeast(0.0),
                    rawCode = rawCode,
                    rowNumber = rowIdx + 1 // 1-based human-readable row index
                )
            )
        }

        return records
    }

    /**
     * Evaluates the top 60 rows to find the genuine table header row, skipping broker logos,
     * client info blocks, and account summaries.
     */
    private fun detectHeaderAndColumns(rows: List<List<String>>): ColumnMapping? {
        val searchLimit = rows.size.coerceAtMost(60)
        var bestMapping: ColumnMapping? = null
        var bestScore = -1

        for (rowIndex in 0 until searchLimit) {
            val row = rows[rowIndex]
            if (row.size < 2) continue

            var schemeCol = -1
            var unitsCol = -1
            var investedCol = -1
            var currentCol = -1
            var codeCol = -1

            for (colIndex in row.indices) {
                val cellText = row[colIndex].trim().replace('\n', ' ')
                if (cellText.isBlank()) continue

                // Prioritize AMFI/Code before Scheme Name since "Scheme Code" matches both regexes
                if (codeCol == -1 && AMFI_OR_ISIN_REGEX.matches(cellText)) {
                    codeCol = colIndex
                    continue
                }
                if (schemeCol == -1 && SCHEME_NAME_REGEX.matches(cellText)) {
                    schemeCol = colIndex
                    continue
                }
                if (unitsCol == -1 && UNITS_REGEX.matches(cellText)) {
                    unitsCol = colIndex
                    continue
                }
                if (investedCol == -1 && INVESTED_COST_REGEX.matches(cellText)) {
                    investedCol = colIndex
                    continue
                }
                if (currentCol == -1 && CURRENT_VALUE_REGEX.matches(cellText)) {
                    currentCol = colIndex
                    continue
                }
            }

            // A valid table header must have at least a Scheme Name column and either Units or Invested Cost
            if (schemeCol != -1 && (unitsCol != -1 || investedCol != -1)) {
                var score = 10
                if (unitsCol != -1) score += 5
                if (investedCol != -1) score += 5
                if (currentCol != -1) score += 3
                if (codeCol != -1) score += 3

                if (score > bestScore) {
                    bestScore = score
                    bestMapping = ColumnMapping(
                        headerRowIndex = rowIndex,
                        schemeNameCol = schemeCol,
                        unitsCol = unitsCol,
                        investedCostCol = investedCol,
                        currentValueCol = currentCol,
                        rawCodeCol = codeCol
                    )
                }
            }
        }

        return bestMapping
    }

    /**
     * Parses Indian formatted numbers (e.g. "₹ 1,24,500.50", "1,234.567", "(450.00)", "Rs. 50,000").
     */
    private fun parseNumericValue(raw: String): Double? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        if (trimmed == "-" || trimmed == "--" || trimmed.equals("N/A", ignoreCase = true) || trimmed.equals("NA", ignoreCase = true)) {
            return null
        }

        val isNegative = (trimmed.startsWith("(") && trimmed.endsWith(")")) || trimmed.startsWith("-")

        val cleaned = trimmed
            .replace("₹", "")
            .replace(Regex("(?i)rs\\.?"), "")
            .replace(Regex("(?i)inr"), "")
            .replace(Regex("(?i)\\bcr\\b|\\bdr\\b"), "")
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
            .replace(" ", "")
            .trim()

        val value = cleaned.toDoubleOrNull() ?: return null
        return if (isNegative && value > 0) -value else value
    }

    /**
     * Extracts a 6-digit integer AMFI scheme code if present in the code cell.
     */
    private fun parseAmfiCode(raw: String): Int? {
        if (raw.isBlank()) return null
        val digitsOnly = raw.trim().removeSuffix(".0").trim()
        val exactInt = digitsOnly.toIntOrNull()
        if (exactInt != null && exactInt in 100_000..999_999) {
            return exactInt
        }
        val match = Regex("\\b(\\d{6})\\b").find(raw)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Strips trailing ISIN tags or folio prefixes sometimes concatenated into CAMS/KFintech scheme cells.
     */
    private fun cleanBrokerSchemeCell(rawName: String): String {
        return rawName
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?i)\\bISIN\\s*[:\\-]?\\s*INF[A-Z0-9]{9}\\b"), "")
            .trim()
    }
}