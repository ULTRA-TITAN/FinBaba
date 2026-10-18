package com.fin.mf.core.export

import com.fin.mf.data.AggregatedStockExposure
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.MarketCapMandate
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.SectorExposure
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

// ============================================================================
// ZERO-DEPENDENCY OPENXML (.XLSX) WORKBOOK GENERATOR
// ============================================================================

/**
 * Generates an institutional 4-tab OpenXML (.xlsx) workbook using pure Kotlin
 * and [ZipOutputStream] without relying on Apache POI or external libraries.
 */
class OpenXmlXlsxWorkbookGenerator {

    companion object {
        // Style indices mapped to xl/styles.xml <cellXfs>
        private const val STYLE_DEFAULT = 0
        private const val STYLE_TITLE_BANNER = 1
        private const val STYLE_SECTION_SUBHEADER = 2
        private const val STYLE_TABLE_HEADER_LEFT = 3
        private const val STYLE_TABLE_HEADER_RIGHT = 4
        private const val STYLE_META_LABEL = 5
        private const val STYLE_META_VALUE_TEXT = 6
        private const val STYLE_META_VALUE_CURRENCY = 7
        private const val STYLE_META_VALUE_PERCENT = 8
        private const val STYLE_TEXT_EVEN = 9
        private const val STYLE_TEXT_ODD = 10
        private const val STYLE_CURRENCY_EVEN = 11
        private const val STYLE_CURRENCY_ODD = 12
        private const val STYLE_PERCENT_EVEN = 13
        private const val STYLE_PERCENT_ODD = 14
        private const val STYLE_DECIMAL_EVEN = 15
        private const val STYLE_DECIMAL_ODD = 16
        private const val STYLE_INT_EVEN = 17
        private const val STYLE_INT_ODD = 18
        private const val STYLE_TOTAL_LABEL = 19
        private const val STYLE_TOTAL_CURRENCY = 20
        private const val STYLE_TOTAL_PERCENT = 21
        private const val STYLE_STATUS_POSITIVE = 22
        private const val STYLE_STATUS_WARNING = 23

        private const val SHEET_1_NAME = "Executive Overview"
        private const val SHEET_2_NAME = "Look-Through Stock Matrix"
        private const val SHEET_3_NAME = "Sector & Asset Split"
        private const val SHEET_4_NAME = "SEBI Mandate & Rebalancing"

        private const val DRIFT_TOLERANCE_PERCENT = 2.0
    }

    /**
     * Builds the complete `.xlsx` archive and writes it to [outputStream].
     * Does not close the caller's underlying stream unless managed externally,
     * though the internal [ZipOutputStream] finishes and flushes all entries.
     */
    fun generateWorkbook(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary,
        outputStream: OutputStream
    ) {
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))
        try {
            writeZipEntry(zipOut, "[Content_Types].xml", buildContentTypesXml())
            writeZipEntry(zipOut, "_rels/.rels", buildRootRelsXml())
            writeZipEntry(zipOut, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
            writeZipEntry(zipOut, "xl/workbook.xml", buildWorkbookXml())
            writeZipEntry(zipOut, "xl/styles.xml", buildStylesXml())

            writeZipEntry(
                zipOut,
                "xl/worksheets/sheet1.xml",
                buildExecutiveOverviewSheetXml(portfolio, analytics)
            )
            writeZipEntry(
                zipOut,
                "xl/worksheets/sheet2.xml",
                buildLookThroughStockMatrixSheetXml(analytics)
            )
            writeZipEntry(
                zipOut,
                "xl/worksheets/sheet3.xml",
                buildSectorAndAssetSplitSheetXml(portfolio, analytics)
            )
            writeZipEntry(
                zipOut,
                "xl/worksheets/sheet4.xml",
                buildSebiMandateRebalancingSheetXml(portfolio, analytics)
            )

            zipOut.finish()
            zipOut.flush()
        } finally {
            zipOut.close()
        }
    }

    // ========================================================================
    // OPENXML PACKAGE PARTS
    // ========================================================================

    private fun buildContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    private fun buildRootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildWorkbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
            <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
            <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
            <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildWorkbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="${escapeXml(SHEET_1_NAME)}" sheetId="1" r:id="rId1"/>
                <sheet name="${escapeXml(SHEET_2_NAME)}" sheetId="2" r:id="rId2"/>
                <sheet name="${escapeXml(SHEET_3_NAME)}" sheetId="3" r:id="rId3"/>
                <sheet name="${escapeXml(SHEET_4_NAME)}" sheetId="4" r:id="rId4"/>
            </sheets>
        </workbook>
    """.trimIndent()

    /**
     * Defines institutional stylesheet:
     * - Custom NumFmts:
     *   164 -> Indian Rupee Format: `₹ #,##,##0.00;[Red]-₹ #,##,##0.00;₹ 0.00`
     *   165 -> Percentage Format: `0.00%`
     *   166 -> NAV / Units 4-Decimal Format: `#,##,##0.0000`
     *   167 -> Integer Rank / Count Format: `#,##0`
     */
    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <numFmts count="4">
                <numFmt numFmtId="164" formatCode="&#8377;\ #,##,##0.00;[Red]\-&#8377;\ #,##,##0.00;&#8377;\ 0.00"/>
                <numFmt numFmtId="165" formatCode="0.00%"/>
                <numFmt numFmtId="166" formatCode="#,##,##0.0000"/>
                <numFmt numFmtId="167" formatCode="#,##0"/>
            </numFmts>
            <fonts count="6">
                <font>
                    <sz val="10"/>
                    <color rgb="FF0F172A"/>
                    <name val="Calibri"/>
                </font>
                <font>
                    <b/>
                    <sz val="10"/>
                    <color rgb="FFFFFFFF"/>
                    <name val="Calibri"/>
                </font>
                <font>
                    <b/>
                    <sz val="14"/>
                    <color rgb="FF1E3A8A"/>
                    <name val="Calibri"/>
                </font>
                <font>
                    <b/>
                    <sz val="11"/>
                    <color rgb="FF1E3A8A"/>
                    <name val="Calibri"/>
                </font>
                <font>
                    <b/>
                    <sz val="10"/>
                    <color rgb="FF0F172A"/>
                    <name val="Calibri"/>
                </font>
                <font>
                    <b/>
                    <sz val="10"/>
                    <color rgb="FF15803D"/>
                    <name val="Calibri"/>
                </font>
            </fonts>
            <fills count="6">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill>
                    <patternFill patternType="solid">
                        <fgColor rgb="FF1E3A8A"/>
                        <bgColor indexed="64"/>
                    </patternFill>
                </fill>
                <fill>
                    <patternFill patternType="solid">
                        <fgColor rgb="FFF1F5F9"/>
                        <bgColor indexed="64"/>
                    </patternFill>
                </fill>
                <fill>
                    <patternFill patternType="solid">
                        <fgColor rgb="FFEFF6FF"/>
                        <bgColor indexed="64"/>
                    </patternFill>
                </fill>
                <fill>
                    <patternFill patternType="solid">
                        <fgColor rgb="FFE2E8F0"/>
                        <bgColor indexed="64"/>
                    </patternFill>
                </fill>
            </fills>
            <borders count="3">
                <border>
                    <left/><right/><top/><bottom/><diagonal/>
                </border>
                <border>
                    <left style="thin"><color rgb="FFCBD5E1"/></left>
                    <right style="thin"><color rgb="FFCBD5E1"/></right>
                    <top style="thin"><color rgb="FFCBD5E1"/></top>
                    <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
                    <diagonal/>
                </border>
                <border>
                    <left style="thin"><color rgb="FF94A3B8"/></left>
                    <right style="thin"><color rgb="FF94A3B8"/></right>
                    <top style="medium"><color rgb="FF1E3A8A"/></top>
                    <bottom style="double"><color rgb="FF1E3A8A"/></bottom>
                    <diagonal/>
                </border>
            </borders>
            <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
            </cellStyleXfs>
            <cellXfs count="24">
                <!-- 0: STYLE_DEFAULT -->
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <!-- 1: STYLE_TITLE_BANNER -->
                <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <!-- 2: STYLE_SECTION_SUBHEADER -->
                <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <!-- 3: STYLE_TABLE_HEADER_LEFT -->
                <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center" wrapText="1"/>
                </xf>
                <!-- 4: STYLE_TABLE_HEADER_RIGHT -->
                <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center" wrapText="1"/>
                </xf>
                <!-- 5: STYLE_META_LABEL -->
                <xf numFmtId="0" fontId="4" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center"/>
                </xf>
                <!-- 6: STYLE_META_VALUE_TEXT -->
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center"/>
                </xf>
                <!-- 7: STYLE_META_VALUE_CURRENCY -->
                <xf numFmtId="164" fontId="4" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 8: STYLE_META_VALUE_PERCENT -->
                <xf numFmtId="165" fontId="4" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 9: STYLE_TEXT_EVEN -->
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center"/>
                </xf>
                <!-- 10: STYLE_TEXT_ODD -->
                <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center"/>
                </xf>
                <!-- 11: STYLE_CURRENCY_EVEN -->
                <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 12: STYLE_CURRENCY_ODD -->
                <xf numFmtId="164" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 13: STYLE_PERCENT_EVEN -->
                <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 14: STYLE_PERCENT_ODD -->
                <xf numFmtId="165" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 15: STYLE_DECIMAL_EVEN -->
                <xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 16: STYLE_DECIMAL_ODD -->
                <xf numFmtId="166" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 17: STYLE_INT_EVEN -->
                <xf numFmtId="167" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 18: STYLE_INT_ODD -->
                <xf numFmtId="167" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 19: STYLE_TOTAL_LABEL -->
                <xf numFmtId="0" fontId="4" fillId="5" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="left" vertical="center"/>
                </xf>
                <!-- 20: STYLE_TOTAL_CURRENCY -->
                <xf numFmtId="164" fontId="4" fillId="5" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 21: STYLE_TOTAL_PERCENT -->
                <xf numFmtId="165" fontId="4" fillId="5" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="right" vertical="center"/>
                </xf>
                <!-- 22: STYLE_STATUS_POSITIVE -->
                <xf numFmtId="0" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="center" vertical="center"/>
                </xf>
                <!-- 23: STYLE_STATUS_WARNING -->
                <xf numFmtId="0" fontId="4" fillId="4" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
                    <alignment horizontal="center" vertical="center"/>
                </xf>
            </cellXfs>
        </styleSheet>
    """.trimIndent()

    // ========================================================================
    // TAB 1: EXECUTIVE OVERVIEW
    // ========================================================================

    private fun buildExecutiveOverviewSheetXml(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): String {
        val sb = StringBuilder(16_384)
        val columnWidths = listOf(14, 38, 24, 22, 16, 18, 18, 20, 20, 14, 14)
        startWorksheet(sb, columnWidths)

        var rowIdx = 1

        // Banner Title
        appendRow(sb, rowIdx++, height = 26) { r ->
            appendStringCell(r, 1, "FINBABA INSTITUTIONAL PORTFOLIO X-RAY — EXECUTIVE OVERVIEW", STYLE_TITLE_BANNER)
        }

        // Blank Spacer
        rowIdx++

        // Vault Metadata Section Header
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "VAULT SUMMARY & FEE TELEMETRY", STYLE_SECTION_SUBHEADER)
        }

        val syncDateFormatted = formatEpochTimestamp(portfolio.lastSyncedEpochMs)

        // Vault Metadata Rows (2-column pairs across A:B and D:E)
        appendRow(sb, rowIdx++) { r ->
            appendStringCell(r, 1, "Vault Name", STYLE_META_LABEL)
            appendStringCell(r, 2, portfolio.portfolioName, STYLE_META_VALUE_TEXT)
            appendStringCell(r, 4, "Total Valuation (₹)", STYLE_META_LABEL)
            appendNumberCell(r, 5, analytics.totalCurrentValue, STYLE_META_VALUE_CURRENCY)
        }
        appendRow(sb, rowIdx++) { r ->
            appendStringCell(r, 1, "Primary Owner", STYLE_META_LABEL)
            appendStringCell(r, 2, portfolio.ownerName, STYLE_META_VALUE_TEXT)
            appendStringCell(r, 4, "Net Invested (₹)", STYLE_META_LABEL)
            appendNumberCell(r, 5, analytics.totalInvested, STYLE_META_VALUE_CURRENCY)
        }
        appendRow(sb, rowIdx++) { r ->
            appendStringCell(r, 1, "Audit Valuation Date", STYLE_META_LABEL)
            appendStringCell(r, 2, syncDateFormatted, STYLE_META_VALUE_TEXT)
            appendStringCell(r, 4, "Total Absolute Gain (₹)", STYLE_META_LABEL)
            appendNumberCell(r, 5, analytics.totalGain, STYLE_META_VALUE_CURRENCY)
        }
        appendRow(sb, rowIdx++) { r ->
            appendStringCell(r, 1, "Weighted Portfolio TER (%)", STYLE_META_LABEL)
            appendPercentCell(r, 2, analytics.weightedExpenseRatio, STYLE_META_VALUE_PERCENT)
            appendStringCell(r, 4, "Total Return (%)", STYLE_META_LABEL)
            appendPercentCell(r, 5, analytics.totalReturnPercent, STYLE_META_VALUE_PERCENT)
        }
        appendRow(sb, rowIdx++) { r ->
            appendStringCell(r, 1, "Annual TER Fee Drag (₹)", STYLE_META_LABEL)
            appendNumberCell(r, 2, analytics.annualFeeDragRupees, STYLE_META_VALUE_CURRENCY)
            appendStringCell(r, 4, "Active Mutual Fund Schemes", STYLE_META_LABEL)
            appendNumberCell(r, 5, portfolio.schemes.size.toDouble(), STYLE_INT_EVEN)
        }

        // Blank Spacer
        rowIdx++

        // Active Funds Section Header
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "ACTIVE MUTUAL FUND HOLDINGS", STYLE_SECTION_SUBHEADER)
        }

        // Table Header Row
        appendRow(sb, rowIdx++, height = 24) { r ->
            appendStringCell(r, 1, "Code", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 2, "Scheme Name", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 3, "AMC", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 4, "Category", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 5, "Units", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 6, "Avg Buy NAV (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 7, "Current NAV (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 8, "Invested (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 9, "Value (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 10, "Gain (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 11, "Weight (%)", STYLE_TABLE_HEADER_RIGHT)
        }

        val sortedSchemes = portfolio.schemes.sortedByDescending { it.currentValue }
        sortedSchemes.forEachIndexed { index, scheme ->
            val isOdd = index % 2 == 1
            val textStyle = if (isOdd) STYLE_TEXT_ODD else STYLE_TEXT_EVEN
            val currencyStyle = if (isOdd) STYLE_CURRENCY_ODD else STYLE_CURRENCY_EVEN
            val percentStyle = if (isOdd) STYLE_PERCENT_ODD else STYLE_PERCENT_EVEN
            val decimalStyle = if (isOdd) STYLE_DECIMAL_ODD else STYLE_DECIMAL_EVEN

            val computedWeight = if (analytics.totalCurrentValue > 0.0) {
                (scheme.currentValue / analytics.totalCurrentValue) * 100.0
            } else {
                0.0
            }

            appendRow(sb, rowIdx++) { r ->
                appendStringCell(r, 1, scheme.schemeCode.toString(), textStyle)
                appendStringCell(r, 2, scheme.schemeName, textStyle)
                appendStringCell(r, 3, scheme.amc, textStyle)
                appendStringCell(r, 4, scheme.category, textStyle)
                appendNumberCell(r, 5, scheme.units, decimalStyle)
                appendNumberCell(r, 6, scheme.averageBuyNav, decimalStyle)
                appendNumberCell(r, 7, scheme.fallbackNav, decimalStyle)
                appendNumberCell(r, 8, scheme.investedAmount, currencyStyle)
                appendNumberCell(r, 9, scheme.currentValue, currencyStyle)
                appendPercentCell(r, 10, scheme.absoluteReturnPercent, percentStyle)
                appendPercentCell(r, 11, computedWeight, percentStyle)
            }
        }

        // Totals Row
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "TOTAL", STYLE_TOTAL_LABEL)
            appendStringCell(r, 2, "${sortedSchemes.size} Active Schemes", STYLE_TOTAL_LABEL)
            appendStringCell(r, 3, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 4, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 5, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 6, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 7, "", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 8, analytics.totalInvested, STYLE_TOTAL_CURRENCY)
            appendNumberCell(r, 9, analytics.totalCurrentValue, STYLE_TOTAL_CURRENCY)
            appendPercentCell(r, 10, analytics.totalReturnPercent, STYLE_TOTAL_PERCENT)
            appendPercentCell(r, 11, 100.0, STYLE_TOTAL_PERCENT)
        }

        endWorksheet(sb)
        return sb.toString()
    }

    // ========================================================================
    // TAB 2: LOOK-THROUGH STOCK MATRIX (700+ Constituents)
    // ========================================================================

    private fun buildLookThroughStockMatrixSheetXml(
        analytics: PortfolioAnalyticsSummary
    ): String {
        val sb = StringBuilder(32_768)
        val columnWidths = listOf(8, 14, 34, 22, 18, 22, 18, 14, 50)
        startWorksheet(sb, columnWidths)

        var rowIdx = 1

        // Title Banner
        appendRow(sb, rowIdx++, height = 26) { r ->
            appendStringCell(
                r,
                1,
                "LOOK-THROUGH UNDERLYING EQUITY MATRIX (FULL UNCAPPED EXPOSURE)",
                STYLE_TITLE_BANNER
            )
        }

        rowIdx++

        // Table Header Row
        appendRow(sb, rowIdx++, height = 24) { r ->
            appendStringCell(r, 1, "Rank", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 2, "Ticker", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 3, "Company Name", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 4, "Sector", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 5, "Market Cap Tier", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 6, "Total Rupee Exposure (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 7, "Effective Portfolio Weight (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 8, "Fund Count", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 9, "Contributing Schemes Breakdown", STYLE_TABLE_HEADER_LEFT)
        }

        val sortedHoldings = analytics.topHoldings
            .sortedByDescending { it.totalRupeeExposure }

        var totalEquityRupees = 0.0
        var totalEquityWeight = 0.0

        sortedHoldings.forEachIndexed { index, holding ->
            val isOdd = index % 2 == 1
            val textStyle = if (isOdd) STYLE_TEXT_ODD else STYLE_TEXT_EVEN
            val currencyStyle = if (isOdd) STYLE_CURRENCY_ODD else STYLE_CURRENCY_EVEN
            val percentStyle = if (isOdd) STYLE_PERCENT_ODD else STYLE_PERCENT_EVEN
            val intStyle = if (isOdd) STYLE_INT_ODD else STYLE_INT_EVEN

            totalEquityRupees += holding.totalRupeeExposure
            totalEquityWeight += holding.portfolioWeightPercent

            val schemeBreakdownText = holding.schemeContributions
                .sortedByDescending { it.rupeeContribution }
                .joinToString(separator = " | ") { contrib ->
                    val formattedVal = String.format(Locale.US, "₹%,.0f", contrib.rupeeContribution)
                    val formattedPct = String.format(Locale.US, "%.2f%%", contrib.weightInSchemePercent)
                    "${contrib.schemeName} ($formattedPct, $formattedVal)"
                }

            appendRow(sb, rowIdx++) { r ->
                appendNumberCell(r, 1, (index + 1).toDouble(), intStyle)
                appendStringCell(r, 2, holding.ticker, textStyle)
                appendStringCell(r, 3, holding.companyName, textStyle)
                appendStringCell(r, 4, holding.sector, textStyle)
                appendStringCell(r, 5, holding.marketCapBucket, textStyle)
                appendNumberCell(r, 6, holding.totalRupeeExposure, currencyStyle)
                appendPercentCell(r, 7, holding.portfolioWeightPercent, percentStyle)
                appendNumberCell(r, 8, holding.schemeContributions.size.toDouble(), intStyle)
                appendStringCell(r, 9, schemeBreakdownText, textStyle)
            }
        }

        // Totals Row
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 2, "TOTAL", STYLE_TOTAL_LABEL)
            appendStringCell(r, 3, "${sortedHoldings.size} Distinct Underlying Stocks", STYLE_TOTAL_LABEL)
            appendStringCell(r, 4, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 5, "", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 6, totalEquityRupees, STYLE_TOTAL_CURRENCY)
            appendPercentCell(r, 7, totalEquityWeight, STYLE_TOTAL_PERCENT)
            appendStringCell(r, 8, "", STYLE_TOTAL_LABEL)
            appendStringCell(r, 9, "", STYLE_TOTAL_LABEL)
        }

        endWorksheet(sb)
        return sb.toString()
    }

    // ========================================================================
    // TAB 3: SECTOR & ASSET SPLIT
    // ========================================================================

    private fun buildSectorAndAssetSplitSheetXml(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): String {
        val sb = StringBuilder(16_384)
        val columnWidths = listOf(34, 24, 18, 16)
        startWorksheet(sb, columnWidths)

        var rowIdx = 1

        // Title Banner
        appendRow(sb, rowIdx++, height = 26) { r ->
            appendStringCell(r, 1, "SECTOR EXPOSURE & ASSET CLASS ALLOCATION", STYLE_TITLE_BANNER)
        }

        rowIdx++

        // Section 1: Sector Table
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "GICS / AMFI SECTOR BREAKDOWN", STYLE_SECTION_SUBHEADER)
        }

        appendRow(sb, rowIdx++, height = 24) { r ->
            appendStringCell(r, 1, "Sector Name", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 2, "Total Rupee Exposure (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 3, "Weight (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 4, "Stock Count", STYLE_TABLE_HEADER_RIGHT)
        }

        val sortedSectors = analytics.sectorBreakdown.sortedByDescending { it.totalRupeeValue }
        var totalSectorRupees = 0.0
        var totalSectorWeight = 0.0
        var totalSectorStocks = 0

        sortedSectors.forEachIndexed { index, sector ->
            val isOdd = index % 2 == 1
            val textStyle = if (isOdd) STYLE_TEXT_ODD else STYLE_TEXT_EVEN
            val currencyStyle = if (isOdd) STYLE_CURRENCY_ODD else STYLE_CURRENCY_EVEN
            val percentStyle = if (isOdd) STYLE_PERCENT_ODD else STYLE_PERCENT_EVEN
            val intStyle = if (isOdd) STYLE_INT_ODD else STYLE_INT_EVEN

            totalSectorRupees += sector.totalRupeeValue
            totalSectorWeight += sector.portfolioWeightPercent
            totalSectorStocks += sector.stockCount

            appendRow(sb, rowIdx++) { r ->
                appendStringCell(r, 1, sector.sectorName, textStyle)
                appendNumberCell(r, 2, sector.totalRupeeValue, currencyStyle)
                appendPercentCell(r, 3, sector.portfolioWeightPercent, percentStyle)
                appendNumberCell(r, 4, sector.stockCount.toDouble(), intStyle)
            }
        }

        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "TOTAL SECTOR ALLOCATION", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 2, totalSectorRupees, STYLE_TOTAL_CURRENCY)
            appendPercentCell(r, 3, totalSectorWeight, STYLE_TOTAL_PERCENT)
            appendNumberCell(r, 4, totalSectorStocks.toDouble(), STYLE_INT_EVEN)
        }

        // Blank Spacer Rows
        rowIdx += 2

        // Section 2: Asset Split Table (Equity, Debt, Cash & TREPS)
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "MACRO ASSET CLASS SPLIT", STYLE_SECTION_SUBHEADER)
        }

        appendRow(sb, rowIdx++, height = 24) { r ->
            appendStringCell(r, 1, "Asset Class", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 2, "Valuation (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 3, "Portfolio Share (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 4, "Classification", STYLE_TABLE_HEADER_LEFT)
        }

        val assetSplitRows = computeMacroAssetSplit(portfolio, analytics)
        assetSplitRows.forEachIndexed { index, slice ->
            val isOdd = index % 2 == 1
            val textStyle = if (isOdd) STYLE_TEXT_ODD else STYLE_TEXT_EVEN
            val currencyStyle = if (isOdd) STYLE_CURRENCY_ODD else STYLE_CURRENCY_EVEN
            val percentStyle = if (isOdd) STYLE_PERCENT_ODD else STYLE_PERCENT_EVEN

            appendRow(sb, rowIdx++) { r ->
                appendStringCell(r, 1, slice.first, textStyle)
                appendNumberCell(r, 2, slice.second, currencyStyle)
                appendPercentCell(r, 3, slice.third, percentStyle)
                appendStringCell(r, 4, slice.fourth, textStyle)
            }
        }

        val totalAssetRupees = assetSplitRows.sumOf { it.second }
        val totalAssetWeight = assetSplitRows.sumOf { it.third }

        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "TOTAL ASSET VALUATION", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 2, totalAssetRupees, STYLE_TOTAL_CURRENCY)
            appendPercentCell(r, 3, totalAssetWeight, STYLE_TOTAL_PERCENT)
            appendStringCell(r, 4, "100% Portfolio Coverage", STYLE_TOTAL_LABEL)
        }

        endWorksheet(sb)
        return sb.toString()
    }

    // ========================================================================
    // TAB 4: SEBI MANDATE & REBALANCING
    // ========================================================================

    private fun buildSebiMandateRebalancingSheetXml(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): String {
        val sb = StringBuilder(16_384)
        val columnWidths = listOf(22, 22, 18, 18, 16, 18, 30)
        startWorksheet(sb, columnWidths)

        var rowIdx = 1

        // Title Banner
        appendRow(sb, rowIdx++, height = 26) { r ->
            appendStringCell(
                r,
                1,
                "SEBI MARKET-CAP MANDATE & INSTITUTIONAL REBALANCING MATRIX",
                STYLE_TITLE_BANNER
            )
        }

        rowIdx++

        // Subheader
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(
                r,
                1,
                "MARKET-CAP DRIFT AUDIT (TOLERANCE BAND: ±2.00%)",
                STYLE_SECTION_SUBHEADER
            )
        }

        appendRow(sb, rowIdx++, height = 24) { r ->
            appendStringCell(r, 1, "Tier", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 2, "Actual Valuation (₹)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 3, "Actual Weight (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 4, "Target Mandate (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 5, "Drift (%)", STYLE_TABLE_HEADER_RIGHT)
            appendStringCell(r, 6, "Status", STYLE_TABLE_HEADER_LEFT)
            appendStringCell(r, 7, "Suggested Inflow/Outflow Adjustment (₹)", STYLE_TABLE_HEADER_RIGHT)
        }

        val mandate = portfolio.marketCapMandate
        val tiers = listOf(
            Triple("Large Cap", findTierSlice(analytics.marketCapBreakdown, "Large"), mandate.largeCapTarget),
            Triple("Mid Cap", findTierSlice(analytics.marketCapBreakdown, "Mid"), mandate.midCapTarget),
            Triple("Small Cap", findTierSlice(analytics.marketCapBreakdown, "Small"), mandate.smallCapTarget),
            Triple(
                "Micro Cap & Cash",
                findMicroAndCashSlice(analytics.marketCapBreakdown, analytics.totalCurrentValue),
                mandate.microCapTarget
            )
        )

        var sumActualRupees = 0.0
        var sumActualWeight = 0.0
        var sumTargetWeight = 0.0

        tiers.forEachIndexed { index, (tierName, slice, targetWeightPct) ->
            val isOdd = index % 2 == 1
            val textStyle = if (isOdd) STYLE_TEXT_ODD else STYLE_TEXT_EVEN
            val currencyStyle = if (isOdd) STYLE_CURRENCY_ODD else STYLE_CURRENCY_EVEN
            val percentStyle = if (isOdd) STYLE_PERCENT_ODD else STYLE_PERCENT_EVEN

            val actualRupees = slice.rupeeValue
            val actualWeightPct = if (slice.weightPercent > 0.0 || analytics.totalCurrentValue <= 0.0) {
                slice.weightPercent
            } else {
                (actualRupees / analytics.totalCurrentValue) * 100.0
            }

            val driftPct = actualWeightPct - targetWeightPct
            val status = when {
                driftPct > DRIFT_TOLERANCE_PERCENT -> "Overweight"
                driftPct < -DRIFT_TOLERANCE_PERCENT -> "Underweight"
                else -> "In-Line"
            }
            val statusStyle = if (status == "In-Line") STYLE_STATUS_POSITIVE else STYLE_STATUS_WARNING

            // Positive adjustment means buy/inflow needed; negative means trim/outflow needed
            val targetRupees = (targetWeightPct / 100.0) * analytics.totalCurrentValue
            val suggestedAdjustmentRupees = targetRupees - actualRupees

            sumActualRupees += actualRupees
            sumActualWeight += actualWeightPct
            sumTargetWeight += targetWeightPct

            appendRow(sb, rowIdx++) { r ->
                appendStringCell(r, 1, tierName, textStyle)
                appendNumberCell(r, 2, actualRupees, currencyStyle)
                appendPercentCell(r, 3, actualWeightPct, percentStyle)
                appendPercentCell(r, 4, targetWeightPct, percentStyle)
                appendPercentCell(r, 5, driftPct, percentStyle)
                appendStringCell(r, 6, status, statusStyle)
                appendNumberCell(r, 7, suggestedAdjustmentRupees, currencyStyle)
            }
        }

        // Totals Row
        appendRow(sb, rowIdx++, height = 22) { r ->
            appendStringCell(r, 1, "TOTAL PORTFOLIO MANDATE", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 2, sumActualRupees, STYLE_TOTAL_CURRENCY)
            appendPercentCell(r, 3, sumActualWeight, STYLE_TOTAL_PERCENT)
            appendPercentCell(r, 4, sumTargetWeight, STYLE_TOTAL_PERCENT)
            appendPercentCell(r, 5, sumActualWeight - sumTargetWeight, STYLE_TOTAL_PERCENT)
            appendStringCell(r, 6, "Rebalanced Net Zero", STYLE_TOTAL_LABEL)
            appendNumberCell(r, 7, 0.0, STYLE_TOTAL_CURRENCY)
        }

        endWorksheet(sb)
        return sb.toString()
    }

    // ========================================================================
    // ANALYTICS & CLASSIFICATION HELPERS
    // ========================================================================

    private fun computeMacroAssetSplit(
        portfolio: PortfolioData,
        analytics: PortfolioAnalyticsSummary
    ): List<Quadruple<String, Double, Double, String>> {
        val totalVal = analytics.totalCurrentValue.coerceAtLeast(0.0)

        var equityRupees = 0.0
        var debtRupees = 0.0
        var cashRupees = 0.0

        if (analytics.assetClassBreakdown.isNotEmpty()) {
            for (slice in analytics.assetClassBreakdown) {
                when {
                    slice.label.contains("Debt", ignoreCase = true) -> debtRupees += slice.rupeeValue
                    slice.label.contains("Hybrid", ignoreCase = true) -> {
                        equityRupees += slice.rupeeValue * 0.65
                        debtRupees += slice.rupeeValue * 0.30
                        cashRupees += slice.rupeeValue * 0.05
                    }
                    slice.label.contains("Cash", ignoreCase = true) || slice.label.contains("Commodit", ignoreCase = true) -> {
                        cashRupees += slice.rupeeValue
                    }
                    else -> equityRupees += slice.rupeeValue
                }
            }
        } else if (portfolio.schemes.isNotEmpty()) {
            for (scheme in portfolio.schemes) {
                when {
                    scheme.assetClass.contains("Debt", ignoreCase = true) -> debtRupees += scheme.currentValue
                    scheme.assetClass.contains("Hybrid", ignoreCase = true) -> {
                        equityRupees += scheme.currentValue * 0.65
                        debtRupees += scheme.currentValue * 0.30
                        cashRupees += scheme.currentValue * 0.05
                    }
                    else -> equityRupees += scheme.currentValue
                }
            }
        } else {
            equityRupees = analytics.topHoldings.sumOf { it.totalRupeeExposure }.coerceAtMost(totalVal)
            cashRupees = (totalVal - equityRupees).coerceAtLeast(0.0)
        }

        val equityPct = if (totalVal > 0.0) (equityRupees / totalVal) * 100.0 else 0.0
        val debtPct = if (totalVal > 0.0) (debtRupees / totalVal) * 100.0 else 0.0
        val cashPct = if (totalVal > 0.0) (cashRupees / totalVal) * 100.0 else 0.0

        return listOf(
            Quadruple("Equity", equityRupees, equityPct, "Listed Domestic & International Equities"),
            Quadruple("Debt", debtRupees, debtPct, "Sovereign G-Secs, Corporate Bonds & SDLs"),
            Quadruple("Cash & TREPS", cashRupees, cashPct, "Tri-Party Repo, Net Receivables & Liquid Cash")
        )
    }

    private fun findTierSlice(
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

    private fun findMicroAndCashSlice(
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

        val largeMidSmallRupees = slices
            .filter {
                it.label.contains("Large", ignoreCase = true) ||
                    it.label.contains("Mid", ignoreCase = true) ||
                    it.label.contains("Small", ignoreCase = true)
            }
            .sumOf { it.rupeeValue }

        val residualRupees = (totalPortfolioValue - largeMidSmallRupees).coerceAtLeast(0.0)
        val residualWeight = if (totalPortfolioValue > 0.0) {
            (residualRupees / totalPortfolioValue) * 100.0
        } else {
            0.0
        }
        return AllocationSlice("Micro Cap & Cash", residualRupees, residualWeight)
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    // ========================================================================
    // LOW-LEVEL OPENXML WORKSHEET STREAMING HELPERS
    // ========================================================================

    private fun startWorksheet(sb: StringBuilder, columnWidths: List<Int>) {
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""").append('\n')
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""").append('\n')
        sb.append("""  <sheetViews><sheetView workbookViewId="0" showGridLines="1"/></sheetViews>""").append('\n')
        sb.append("""  <sheetFormatPr defaultRowHeight="19"/>""").append('\n')
        sb.append("""  <cols>""").append('\n')
        columnWidths.forEachIndexed { index, width ->
            val colNum = index + 1
            sb.append("""    <col min="$colNum" max="$colNum" width="$width" customWidth="1"/>""").append('\n')
        }
        sb.append("""  </cols>""").append('\n')
        sb.append("""  <sheetData>""").append('\n')
    }

    private fun endWorksheet(sb: StringBuilder) {
        sb.append("""  </sheetData>""").append('\n')
        sb.append("""</worksheet>""")
    }

    private inline fun appendRow(
        sb: StringBuilder,
        rowIndex: Int,
        height: Int? = null,
        block: (RowBuilder) -> Unit
    ) {
        if (height != null) {
            sb.append("""    <row r="$rowIndex" ht="$height" customHeight="1">""").append('\n')
        } else {
            sb.append("""    <row r="$rowIndex">""").append('\n')
        }
        val rowBuilder = RowBuilder(sb, rowIndex)
        block(rowBuilder)
        sb.append("""    </row>""").append('\n')
    }

    private class RowBuilder(
        private val sb: StringBuilder,
        val rowIndex: Int
    ) {
        fun appendRawCell(cellXml: String) {
            sb.append("      ").append(cellXml).append('\n')
        }
    }

    private fun appendStringCell(
        row: RowBuilder,
        colIndex: Int,
        value: String,
        styleIndex: Int
    ) {
        val cellRef = "${columnName(colIndex)}${row.rowIndex}"
        val safeText = escapeXml(value)
        row.appendRawCell(
            """<c r="$cellRef" s="$styleIndex" t="inlineStr"><is><t xml:space="preserve">$safeText</t></is></c>"""
        )
    }

    private fun appendNumberCell(
        row: RowBuilder,
        colIndex: Int,
        value: Double,
        styleIndex: Int
    ) {
        val cellRef = "${columnName(colIndex)}${row.rowIndex}"
        val cleanNumber = if (value.isFinite()) value else 0.0
        row.appendRawCell(
            """<c r="$cellRef" s="$styleIndex"><v>$cleanNumber</v></c>"""
        )
    }

    /**
     * Writes a percentage value to an OpenXML cell styled with `0.00%` (`numFmtId="165"`).
     * Excel's percentage number format multiplies the stored value by 100 on display,
     * so `12.5` (representing 12.50%) is stored as `0.125`.
     */
    private fun appendPercentCell(
        row: RowBuilder,
        colIndex: Int,
        percentValue: Double,
        styleIndex: Int
    ) {
        val fractional = if (percentValue.isFinite()) percentValue / 100.0 else 0.0
        appendNumberCell(row, colIndex, fractional, styleIndex)
    }

    /**
     * Converts a 1-based column number (1 = A, 26 = Z, 27 = AA) into an Excel column reference.
     */
    private fun columnName(colIndex: Int): String {
        var dividend = colIndex
        val colName = StringBuilder()
        while (dividend > 0) {
            val modulo = (dividend - 1) % 26
            colName.insert(0, ('A'.code + modulo).toChar())
            dividend = (dividend - modulo) / 26
        }
        return colName.toString()
    }

    /**
     * Escapes XML special characters and strips invalid control characters disallowed by XML 1.0.
     */
    private fun escapeXml(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                '\t', '\n', '\r' -> sb.append(ch)
                else -> if (ch.code >= 0x20) sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun formatEpochTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val targetEpoch = if (epochMs > 0L) epochMs else System.currentTimeMillis()
        return sdf.format(Date(targetEpoch))
    }

    private fun writeZipEntry(
        zipOut: ZipOutputStream,
        entryPath: String,
        xmlContent: String
    ) {
        val entry = ZipEntry(entryPath)
        zipOut.putNextEntry(entry)
        val bytes = xmlContent.toByteArray(StandardCharsets.UTF_8)
        zipOut.write(bytes)
        zipOut.closeEntry()
    }
}
