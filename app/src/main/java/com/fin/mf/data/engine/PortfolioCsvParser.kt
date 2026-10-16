package com.fin.mf.data.engine

import com.fin.mf.data.PortfolioData
import java.io.IOException

data class ParsedCsvRow(
    val schemeCode: Int,
    val schemeName: String,
    val units: Double,
    val investedAmount: Double
)

/**
 * RFC-4180 compliant CSV import/export engine for FinBaba portfolio holdings.
 */
object PortfolioCsvParser {

    /**
     * Serializes a [PortfolioData] instance into a standardized CSV string:
     * SchemeCode,SchemeName,Units,InvestedAmount
     */
    fun exportPortfolioCsv(portfolio: PortfolioData): String {
        val sb = StringBuilder()
        sb.append("SchemeCode,SchemeName,Units,InvestedAmount\n")
        portfolio.schemes.forEach { scheme ->
            val escapedName = "\"${scheme.schemeName.replace("\"", "\"\"")}\""
            sb.append("${scheme.schemeCode},$escapedName,${scheme.units},${scheme.investedAmount}\n")
        }
        return sb.toString()
    }

    /**
     * Parses raw CSV content into a list of [ParsedCsvRow].
     * Throws [IOException] if content is empty or contains zero valid scheme rows.
     */
    fun parseCsvRows(csvContent: String): List<ParsedCsvRow> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) throw IOException("CSV content is empty")

        val startIndex = if (lines[0].contains("SchemeCode", ignoreCase = true)) 1 else 0
        val rows = lines.drop(startIndex)

        val parsedRows = mutableListOf<ParsedCsvRow>()
        for (line in rows) {
            val tokens = parseCsvLine(line)
            if (tokens.size >= 4) {
                val code = tokens[0].trim().toIntOrNull()
                val name = tokens[1].trim()
                val units = tokens[2].trim().toDoubleOrNull() ?: 0.0
                val invested = tokens[3].trim().toDoubleOrNull() ?: 0.0
                if (code != null) {
                    parsedRows.add(ParsedCsvRow(code, name, units, invested))
                }
            }
        }

        if (parsedRows.isEmpty()) {
            throw IOException("No valid scheme rows found in CSV file.")
        }

        return parsedRows
    }

    /**
     * Parses a single CSV line with quote-escaping support.
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString())
                cur.clear()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
