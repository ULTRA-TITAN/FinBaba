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
    val avgNav: Double? = null,
    val rawCode: Int? = null,
    val rowNumber: Int
)

/**
 * Intelligent table header detector and column mapper supporting CAMS CAS, KFintech,
 * Zerodha Coin, Groww, INDmoney, Kuvera, ETMONEY, and custom portfolio sheets.
 *
 * Implements strict row fencing, summary/blacklist filtering, numeric safeguards,
 * automatic missing valuation calculation, and consolidated multi-folio deduplication.
 */
object BrokerFormatNormalizer {

    private val SCHEME_NAME_REGEX =
        Regex("(?i).*(scheme.*name|fund.*name|mutual.*fund|\\bscheme\\b|\\bfund\\b|folio.*desc).*")

    private val EXCLUDED_HEADER_REGEX =
        Regex("(?i).*(name of the instrument|instrument.*name|security.*name|stock.*name|company.*name|scrip.*name|isin.*code|ytc\\^|% to net assets).*")

    private val UNITS_REGEX =
        Regex("(?i).*(units|unit|qty|quantity|balance.*unit|total.*unit|balance|closing).*")

    private val INVESTED_COST_REGEX =
        Regex("(?i).*(invested|cost.*value|purchase.*cost|purchase.*val|amount.*invested|total.*cost|cost|buy.*value|purchase.*amt|invested.*amount|investment).*")

    private val CURRENT_VALUE_REGEX =
        Regex("(?i).*(current.*val|market.*val|latest.*val|present.*val|valuation|value|market.*value|current.*value).*")

    private val AVG_NAV_REGEX =
        Regex("(?i).*(avg.*nav|purchase.*nav|buy.*price|cost.*nav|buy.*nav|nav.*price|purchase.*price|unit.*cost|nav).*")

    private val AMFI_OR_ISIN_REGEX =
        Regex("(?i).*(amfi|scheme.*code|isin|fund.*code).*")

    // Strict blacklist for footer summary rows, disclaimers, and account metadata
    private val ROW_BLACKLIST = listOf(
        "total",
        "subtotal",
        "sub total",
        "stamp duty",
        "stt",
        "grand total",
        "portfolio value",
        "closing balance in inr",
        "disclaimer",
        "pan:",
        "folio:"
    )

    private val MUTUAL_FUND_KEYWORDS = listOf(
        "direct", "regular", "growth", "dividend", "idcw", "fund", "plan",
        "elss", "flexi", "small cap", "mid cap", "large cap", "hybrid",
        "multi asset", "index", "etf", "equity saving", "balanced advantage"
    )

    private val KNOWN_AMC_BRANDS = listOf(
        "hdfc", "sbi", "dsp", "axis", "icici", "kotak", "nippon", "mirae",
        "tata", "parag", "quant", "uti", "motilal", "bandhan", "invesco",
        "canara", "franklin", "edelweiss", "whiteoak", "ppfas", "sundaram"
    )

    private data class ColumnMapping(
        val headerRowIndex: Int,
        val schemeNameCol: Int,
        val unitsCol: Int,
        val investedCostCol: Int = -1,
        val currentValueCol: Int = -1,
        val avgNavCol: Int = -1,
        val rawCodeCol: Int = -1
    )

    /**
     * Scores a raw sheet to evaluate whether it contains genuine mutual fund portfolio records.
     * Returns -1 if no valid table header exists.
     */
    fun scoreRows(rows: List<List<String>>): Int {
        if (rows.isEmpty()) return -1
        val (mapping, score) = detectHeaderAndColumnsWithScore(rows) ?: return -1
        return score
    }

    /**
     * Scans raw spreadsheet rows, strictly fences against rows preceding the genuine header row,
     * strips metadata/summary blacklisted rows, enforces numeric/content guards,
     * computes missing values, and consolidates multi-folio holdings of the same scheme.
     */
    fun normalizeRows(rows: List<List<String>>): List<RawBrokerRecord> {
        if (rows.isEmpty()) return emptyList()

        // 1. Strict Header Anchoring: must contain Scheme Name AND (Units OR Invested Amount OR Current Value)
        val mapping = detectHeaderAndColumns(rows) ?: return emptyList()

        val parsedRecords = ArrayList<RawBrokerRecord>()

        // 2. Strict Row Fence: Discard all rows preceding the true table header
        for (rowIdx in (mapping.headerRowIndex + 1) until rows.size) {
            val row = rows[rowIdx]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            // Stop or skip if row is another header (e.g. multi-section tables)
            val joinedRow = row.joinToString(" ").lowercase(Locale.ROOT)
            if (EXCLUDED_HEADER_REGEX.containsMatchIn(joinedRow)) continue

            // Check entire row for blacklist matches
            if (isBlacklistedRow(row)) continue

            val rawName = row.getOrNull(mapping.schemeNameCol)?.trim().orEmpty()
            if (rawName.isBlank()) continue

            // Strict Content Guard: Scheme raw name length must be >= 6 and contain alphabetic text
            if (rawName.length < 6 || !rawName.any { it.isLetter() }) continue

            // Check scheme name cell specifically against blacklist
            if (isBlacklistedSchemeName(rawName)) continue

            // Skip repeated header rows in multi-page statement exports
            if (SCHEME_NAME_REGEX.matches(rawName) && rawName.length < 25) continue

            val rawUnitsStr = if (mapping.unitsCol >= 0) row.getOrNull(mapping.unitsCol).orEmpty() else ""
            val rawInvestedStr = if (mapping.investedCostCol >= 0) row.getOrNull(mapping.investedCostCol).orEmpty() else ""
            val rawCurrentStr = if (mapping.currentValueCol >= 0) row.getOrNull(mapping.currentValueCol).orEmpty() else ""
            val rawAvgNavStr = if (mapping.avgNavCol >= 0) row.getOrNull(mapping.avgNavCol).orEmpty() else ""
            val rawCodeStr = if (mapping.rawCodeCol >= 0) row.getOrNull(mapping.rawCodeCol).orEmpty() else ""

            var units = parseNumericValue(rawUnitsStr) ?: 0.0
            var investedCost = parseNumericValue(rawInvestedStr) ?: 0.0
            var currentValue = parseNumericValue(rawCurrentStr)
            var avgNav = parseNumericValue(rawAvgNavStr)
            val rawCode = parseAmfiCode(rawCodeStr)

            // Strict Guard:
            // If table has a units column, units must be positive (> 0.001)
            // If table lacks units column, must have positive invested cost or current value
            if (mapping.unitsCol >= 0) {
                if (units <= 0.001) continue
            } else {
                if (investedCost <= 0.0 && (currentValue == null || currentValue <= 0.0)) continue
                if (avgNav != null && avgNav > 0.0) {
                    units = investedCost / avgNav
                } else {
                    // Default unitization using Indian mutual fund NFO face value (₹10.00)
                    avgNav = 10.0
                    units = investedCost / 10.0
                }
            }

            // Auto-calculate missing values safely:
            // a. If Invested Value is missing or 0: Compute Units * Avg NAV (or use Current Value)
            if (investedCost <= 0.0) {
                if (avgNav != null && avgNav > 0.0) {
                    investedCost = units * avgNav
                } else if (currentValue != null && currentValue > 0.0) {
                    investedCost = currentValue
                }
            }

            // b. If Avg NAV is missing or 0: Compute Invested Value / Units
            if ((avgNav == null || avgNav <= 0.0) && investedCost > 0.0 && units > 0.001) {
                avgNav = investedCost / units
            }

            // c. If Current Value is missing or 0: fallback to Invested Cost or Units * Avg NAV
            if (currentValue == null || currentValue <= 0.0) {
                if (investedCost > 0.0) {
                    currentValue = investedCost
                } else if (avgNav != null && avgNav > 0.0) {
                    currentValue = units * avgNav
                }
            }

            parsedRecords.add(
                RawBrokerRecord(
                    rawSchemeName = cleanBrokerSchemeCell(rawName),
                    units = units,
                    investedCost = investedCost.coerceAtLeast(0.0),
                    currentValue = currentValue?.coerceAtLeast(0.0),
                    avgNav = avgNav?.coerceAtLeast(0.0),
                    rawCode = rawCode,
                    rowNumber = rowIdx + 1 // 1-based human-readable row index
                )
            )
        }

        // 3. Consolidated Multi-Folio Deduplication:
        // Merge identical schemes held across multiple folios into a single consolidated position
        return deduplicateAndConsolidateFolios(parsedRecords)
    }

    /**
     * Identifies the genuine table header row.
     */
    private fun detectHeaderAndColumns(rows: List<List<String>>): ColumnMapping? {
        return detectHeaderAndColumnsWithScore(rows)?.first
    }

    /**
     * Scans for the genuine table header and scores each candidate table.
     * Prevents stock constituent lists from masquerading as mutual fund portfolios.
     */
    private fun detectHeaderAndColumnsWithScore(rows: List<List<String>>): Pair<ColumnMapping, Int>? {
        val searchLimit = rows.size.coerceAtMost(300)
        var bestMapping: ColumnMapping? = null
        var bestScore = -1

        for (rowIndex in 0 until searchLimit) {
            val row = rows[rowIndex]
            if (row.size < 2) continue

            val rowJoined = row.joinToString(" ").lowercase(Locale.ROOT)
            if (EXCLUDED_HEADER_REGEX.containsMatchIn(rowJoined)) continue

            var schemeCol = -1
            var unitsCol = -1
            var investedCol = -1
            var currentCol = -1
            var avgNavCol = -1
            var codeCol = -1

            for (colIndex in row.indices) {
                val cellText = row[colIndex].trim().replace('\n', ' ')
                if (cellText.isBlank()) continue

                // Check AMFI/ISIN code first so "Scheme Code" is mapped to code column
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
                if (avgNavCol == -1 && AVG_NAV_REGEX.matches(cellText)) {
                    avgNavCol = colIndex
                    continue
                }
            }

            // Must have Scheme Name AND at least one financial value column (Units OR Invested Cost OR Current Value)
            if (schemeCol != -1 && (unitsCol != -1 || investedCol != -1 || currentCol != -1)) {
                var score = 20
                val schemeHeaderCell = row[schemeCol].lowercase(Locale.ROOT)
                if (schemeHeaderCell.contains("scheme") || schemeHeaderCell.contains("fund")) score += 20
                if (unitsCol != -1) score += 15
                if (investedCol != -1) score += 15
                if (currentCol != -1) score += 10
                if (avgNavCol != -1) score += 5
                if (codeCol != -1) score += 5

                // Lookahead validation across next rows
                var mfMatchCount = 0
                var stockMatchCount = 0
                val lookaheadLimit = (rowIndex + 15).coerceAtMost(rows.size)
                for (lookIdx in (rowIndex + 1) until lookaheadLimit) {
                    val nextCell = rows[lookIdx].getOrNull(schemeCol)?.lowercase(Locale.ROOT).orEmpty()
                    if (nextCell.isBlank()) continue
                    if (MUTUAL_FUND_KEYWORDS.any { nextCell.contains(it) } || KNOWN_AMC_BRANDS.any { nextCell.contains(it) }) {
                        mfMatchCount++
                    }
                    if ((nextCell.contains("ltd") || nextCell.contains("limited") || nextCell.contains("industries")) &&
                        !nextCell.contains("fund") && !nextCell.contains("mutual")
                    ) {
                        stockMatchCount++
                    }
                }
                score += mfMatchCount * 8
                score -= stockMatchCount * 15

                if (score > bestScore && score > 0) {
                    bestScore = score
                    bestMapping = ColumnMapping(
                        headerRowIndex = rowIndex,
                        schemeNameCol = schemeCol,
                        unitsCol = unitsCol,
                        investedCostCol = investedCol,
                        currentValueCol = currentCol,
                        avgNavCol = avgNavCol,
                        rawCodeCol = codeCol
                    )
                }
            }
        }

        return if (bestMapping != null) Pair(bestMapping, bestScore) else null
    }

    /**
     * Checks if the row text contains any blacklisted terms.
     */
    private fun isBlacklistedRow(row: List<String>): Boolean {
        val joined = row.joinToString(" ").lowercase(Locale.ROOT)
        for (item in ROW_BLACKLIST) {
            if (item == "total") {
                // If a cell specifically equals "total" or starts with "total "
                if (row.any { cell ->
                        val c = cell.trim().lowercase(Locale.ROOT)
                        c == "total" || c.startsWith("total:") || c.startsWith("total ") || c.startsWith("total -")
                    }) {
                    return true
                }
            } else if (joined.contains(item)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a scheme name cell specifically matches blacklisted terms.
     */
    private fun isBlacklistedSchemeName(name: String): Boolean {
        val lower = name.trim().lowercase(Locale.ROOT)
        for (item in ROW_BLACKLIST) {
            if (item == "total") {
                if (lower == "total" || lower.startsWith("total ") || lower.startsWith("total:") || lower.startsWith("total -")) {
                    return true
                }
            } else if (lower.contains(item)) {
                return true
            }
        }
        return false
    }

    /**
     * Consolidates multi-folio holdings of the same scheme into a single position:
     * - Units = Units_1 + Units_2
     * - InvestedValue = Invested_1 + Invested_2
     * - BlendedAvgNav = InvestedValue / Units
     */
    private fun deduplicateAndConsolidateFolios(records: List<RawBrokerRecord>): List<RawBrokerRecord> {
        if (records.size <= 1) return records

        val grouped = LinkedHashMap<String, MutableList<RawBrokerRecord>>()

        for (rec in records) {
            val key = if (rec.rawCode != null) {
                "CODE_${rec.rawCode}"
            } else {
                normalizeSchemeKey(rec.rawSchemeName)
            }
            grouped.getOrPut(key) { ArrayList() }.add(rec)
        }

        return grouped.values.map { list ->
            if (list.size == 1) {
                list[0]
            } else {
                val totalUnits = list.sumOf { it.units }
                val totalInvested = list.sumOf { it.investedCost }
                val totalCurrent = list.sumOf { it.currentValue ?: it.investedCost }
                val blendedAvgNav = if (totalUnits > 0.0) totalInvested / totalUnits else 0.0
                val primary = list.maxByOrNull { it.rawSchemeName.length } ?: list[0]
                val resolvedCode = list.firstOrNull { it.rawCode != null }?.rawCode

                RawBrokerRecord(
                    rawSchemeName = primary.rawSchemeName,
                    units = totalUnits,
                    investedCost = totalInvested,
                    currentValue = totalCurrent,
                    avgNav = blendedAvgNav,
                    rawCode = resolvedCode,
                    rowNumber = list[0].rowNumber
                )
            }
        }
    }

    /**
     * Normalizes raw scheme strings for folio grouping by removing folio/account numbers
     * and whitespace differences.
     */
    private fun normalizeSchemeKey(rawName: String): String {
        return rawName
            .uppercase(Locale.ROOT)
            .replace(Regex("(?i)\\bfolio.*"), "")
            .replace(Regex("(?i)\\ba/c.*"), "")
            .replace(Regex("[^A-Z0-9]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    /**
     * Parses Indian formatted numbers (e.g. "₹ 1,24,500.50", "1,234.567", "(450.00)", "Rs. 50,000").
     * Strips all currency symbols (`₹`, `Rs`, `INR`), commas, and whitespace.
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
            .replace(Regex("(?i)\\bFolio\\s*(No|Number)?\\s*[:\\-]?[A-Za-z0-9/]+\\b"), "")
            .trim()
    }
}