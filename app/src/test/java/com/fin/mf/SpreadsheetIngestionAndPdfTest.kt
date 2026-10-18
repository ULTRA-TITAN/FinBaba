package com.fin.mf

import com.fin.mf.data.engine.BrokerFormatNormalizer
import com.fin.mf.data.engine.UniversalSpreadsheetReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpreadsheetIngestionAndPdfTest {

    private val sampleExcelPath = "C:/Users/Tanmay/.gemini/antigravity/brain/f832f6f3-3a82-46a9-bd40-f491350ac226/.user_uploaded/media_1792330825991_894d6559.xlsx"

    @Test
    fun testSampleExcelIngestion_selectsMfInvestmentSheetAndExtractsAllFunds() {
        val sampleFile = File(sampleExcelPath)
        assertTrue("Sample excel file must exist on disk", sampleFile.exists())

        val bytes = sampleFile.readBytes()
        val rawRows = UniversalSpreadsheetReader.readSpreadsheetRowsFromBytes(bytes, sampleFile.name)

        assertFalse("Parsed rows must not be empty", rawRows.isEmpty())

        // Header must be Scheme Name, not Name of the Instrument
        val headerRow = rawRows.first()
        val headerJoined = headerRow.joinToString(" ").lowercase()
        assertTrue("Header must contain 'scheme name'", headerJoined.contains("scheme name"))
        assertFalse("Header must not contain constituent stock columns", headerJoined.contains("name of the instrument"))

        // Run through BrokerFormatNormalizer
        val records = BrokerFormatNormalizer.normalizeRows(rawRows)

        // Must extract consolidated mutual fund positions (26 raw entries deduplicated into 21 unique schemes)
        assertEquals(21, records.size)

        // Total invested must match exact sum of positions: ₹3,32,200.0
        val totalInvested = records.sumOf { it.investedCost }
        assertEquals(332200.0, totalInvested, 0.01)

        // Check that none of the 127 individual stocks (Torrent, Krishna, City Union Bank, etc.) leaked in
        for (rec in records) {
            val name = rec.rawSchemeName.lowercase()
            assertFalse("Must not contain stock 'torrent'", name.contains("torrent"))
            assertFalse("Must not contain stock 'krishna'", name.contains("krishna"))
            assertFalse("Must not contain stock 'city union bank'", name.contains("city union bank"))
            assertTrue("Units must be strictly positive", rec.units > 0.001)
            assertTrue("Invested cost must be strictly positive", rec.investedCost > 0.0)
        }

        // Check key schemes are present
        val ppfas = records.find { it.rawSchemeName.contains("Parag Parikh", ignoreCase = true) }
        assertNotNull("Parag Parikh Flexi Cap must be present", ppfas)
        assertEquals(60000.0, ppfas!!.investedCost, 0.01)

        val hdfcFlexi = records.find { it.rawSchemeName.contains("HDFC Flexi", ignoreCase = true) }
        assertNotNull("HDFC Flexi Cap must be present", hdfcFlexi)
        // 30000 + 60700 = 90700
        assertEquals(90700.0, hdfcFlexi!!.investedCost, 0.01)
    }

    @Test
    fun testMultiTableDisqualification_ignoresStockConstituentTable() {
        // Construct a single sheet with a stock constituent table preceding the genuine mutual fund table
        val syntheticRows = listOf(
            listOf("Portfolio Disclosure for Equity Schemes"),
            listOf("Axis Small Cap Fund - Underlying Holdings"),
            listOf("Name of the Instrument", "ISIN", "Industry", "Quantity", "Market Value", "% to Net Assets"),
            listOf("Torrent Pharmaceuticals Ltd.", "INE685A01028", "Healthcare", "45000", "1250.0", "3.2"),
            listOf("City Union Bank Ltd.", "INE491A01021", "Banking", "150000", "890.0", "2.1"),
            listOf("Krishna Institute of Medical Sciences", "INE967H01016", "Healthcare", "30000", "640.0", "1.8"),
            listOf("Total", "", "", "", "2780.0", "7.1"),
            listOf(""),
            listOf(""),
            listOf("Investor Mutual Fund Portfolio Statement"),
            listOf("Scheme Name", "AMC Name", "Category", "Investment"),
            listOf("DSP Business Cycle Fund - Direct - Growth", "DSP Mutual Fund", "Thematic", "15000"),
            listOf("Parag Parikh Flexi Cap Fund - Direct Plan Growth", "PPFAS Mutual Fund", "Flexi Cap", "50000"),
            listOf("HDFC Mid Cap Fund - Direct Plan - Growth", "HDFC Mutual Fund", "Mid Cap", "25000")
        )

        val records = BrokerFormatNormalizer.normalizeRows(syntheticRows)

        assertEquals("Must extract exactly 3 mutual fund schemes", 3, records.size)
        assertEquals(90000.0, records.sumOf { it.investedCost }, 0.01)
        assertTrue(records.any { it.rawSchemeName.contains("DSP Business") })
        assertTrue(records.any { it.rawSchemeName.contains("Parag Parikh") })
        assertTrue(records.any { it.rawSchemeName.contains("HDFC Mid Cap") })
    }
}
