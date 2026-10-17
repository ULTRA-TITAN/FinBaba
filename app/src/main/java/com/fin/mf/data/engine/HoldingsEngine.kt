package com.fin.mf.data.engine

import com.fin.mf.data.UnderlyingHolding
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Deterministic institutional constituent generator and AMFI scheme metadata classifier.
 *
 * When a user searches or adds any of the 15,000+ AMFI schemes via `api.mfapi.in` (which provides
 * scheme name, AMC, category, and historical NAVs, but not constituent stock weights),
 * [HoldingsEngine] deterministically synthesizes category-accurate Indian equity, debt, or hybrid
 * look-through holdings seeded by `schemeCode` so overlap and X-Ray analytics work seamlessly.
 */
object HoldingsEngine {

    private data class SecurityTemplate(
        val ticker: String,
        val companyName: String,
        val sector: String,
        val marketCapBucket: String
    )

    private val largeCapUniverse = listOf(
        SecurityTemplate("HDFCBANK", "HDFC Bank Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("ICICIBANK", "ICICI Bank Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("RELIANCE", "Reliance Industries Ltd.", "Energy & Conglomerate", "Large Cap"),
        SecurityTemplate("INFY", "Infosys Ltd.", "Information Technology", "Large Cap"),
        SecurityTemplate("TCS", "Tata Consultancy Services Ltd.", "Information Technology", "Large Cap"),
        SecurityTemplate("BHARTIARTL", "Bharti Airtel Ltd.", "Telecommunications", "Large Cap"),
        SecurityTemplate("LT", "Larsen & Toubro Ltd.", "Capital Goods & Infra", "Large Cap"),
        SecurityTemplate("ITC", "ITC Ltd.", "FMCG", "Large Cap"),
        SecurityTemplate("SBIN", "State Bank of India", "Financial Services", "Large Cap"),
        SecurityTemplate("AXISBANK", "Axis Bank Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("KOTAKBANK", "Kotak Mahindra Bank Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("SUNPHARMA", "Sun Pharmaceutical Industries Ltd.", "Healthcare & Pharma", "Large Cap"),
        SecurityTemplate("M&M", "Mahindra & Mahindra Ltd.", "Automobile", "Large Cap"),
        SecurityTemplate("MARUTI", "Maruti Suzuki India Ltd.", "Automobile", "Large Cap"),
        SecurityTemplate("NTPC", "NTPC Ltd.", "Power & Utilities", "Large Cap"),
        SecurityTemplate("POWERGRID", "Power Grid Corporation of India Ltd.", "Power & Utilities", "Large Cap"),
        SecurityTemplate("TATAMOTORS", "Tata Motors Ltd.", "Automobile", "Large Cap"),
        SecurityTemplate("ULTRACEMCO", "UltraTech Cement Ltd.", "Materials & Cement", "Large Cap"),
        SecurityTemplate("BAJFINANCE", "Bajaj Finance Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("HCLTECH", "HCL Technologies Ltd.", "Information Technology", "Large Cap"),
        SecurityTemplate("HINDUNILVR", "Hindustan Unilever Ltd.", "FMCG", "Large Cap"),
        SecurityTemplate("TITAN", "Titan Company Ltd.", "Consumer Discretionary", "Large Cap"),
        SecurityTemplate("ADANIPORTS", "Adani Ports & SEZ Ltd.", "Capital Goods & Infra", "Large Cap"),
        SecurityTemplate("COALINDIA", "Coal India Ltd.", "Metals & Mining", "Large Cap"),
        SecurityTemplate("HAL", "Hindustan Aeronautics Ltd.", "Aerospace & Defence", "Large Cap"),
        SecurityTemplate("ASIANPAINT", "Asian Paints Ltd.", "Consumer Discretionary", "Large Cap"),
        SecurityTemplate("BAJAJFINSV", "Bajaj Finserv Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("TRENT", "Trent Ltd.", "Consumer Discretionary", "Large Cap"),
        SecurityTemplate("ZOMATO", "Zomato Ltd.", "Consumer Services", "Large Cap"),
        SecurityTemplate("GRASIM", "Grasim Industries Ltd.", "Materials & Cement", "Large Cap"),
        SecurityTemplate("WIPRO", "Wipro Ltd.", "Information Technology", "Large Cap"),
        SecurityTemplate("TATASTEEL", "Tata Steel Ltd.", "Metals & Mining", "Large Cap"),
        SecurityTemplate("JSWSTEEL", "JSW Steel Ltd.", "Metals & Mining", "Large Cap"),
        SecurityTemplate("SIEMENS", "Siemens India Ltd.", "Capital Goods & Infra", "Large Cap"),
        SecurityTemplate("ABB", "ABB India Ltd.", "Capital Goods & Infra", "Large Cap"),
        SecurityTemplate("VBL", "Varun Beverages Ltd.", "FMCG", "Large Cap"),
        SecurityTemplate("BEL", "Bharat Electronics Ltd.", "Aerospace & Defence", "Large Cap"),
        SecurityTemplate("CHOLAFIN", "Cholamandalam Investment & Finance", "Financial Services", "Large Cap"),
        SecurityTemplate("INDUSINDBK", "IndusInd Bank Ltd.", "Financial Services", "Large Cap"),
        SecurityTemplate("DLF", "DLF Ltd.", "Realty & Infrastructure", "Large Cap")
    )

    private val midCapUniverse = listOf(
        SecurityTemplate("PERSISTENT", "Persistent Systems Ltd.", "Information Technology", "Mid Cap"),
        SecurityTemplate("COFORGE", "Coforge Ltd.", "Information Technology", "Mid Cap"),
        SecurityTemplate("MAXHEALTH", "Max Healthcare Institute Ltd.", "Healthcare & Pharma", "Mid Cap"),
        SecurityTemplate("POLYCAB", "Polycab India Ltd.", "Capital Goods & Infra", "Mid Cap"),
        SecurityTemplate("CUMMINSIND", "Cummins India Ltd.", "Capital Goods & Infra", "Mid Cap"),
        SecurityTemplate("DIXON", "Dixon Technologies (India) Ltd.", "Consumer Electronics", "Mid Cap"),
        SecurityTemplate("FEDERALBNK", "The Federal Bank Ltd.", "Financial Services", "Mid Cap"),
        SecurityTemplate("INDIANB", "Indian Bank", "Financial Services", "Mid Cap"),
        SecurityTemplate("LUPIN", "Lupin Ltd.", "Healthcare & Pharma", "Mid Cap"),
        SecurityTemplate("AUROPHARMA", "Aurobindo Pharma Ltd.", "Healthcare & Pharma", "Mid Cap"),
        SecurityTemplate("PIIND", "PI Industries Ltd.", "Chemicals & Agri", "Mid Cap"),
        SecurityTemplate("SRF", "SRF Ltd.", "Chemicals & Agri", "Mid Cap"),
        SecurityTemplate("VOLTAS", "Voltas Ltd.", "Consumer Discretionary", "Mid Cap"),
        SecurityTemplate("PAGEIND", "Page Industries Ltd.", "Consumer Discretionary", "Mid Cap"),
        SecurityTemplate("ASHOKLEY", "Ashok Leyland Ltd.", "Automobile", "Mid Cap"),
        SecurityTemplate("BHARATFORG", "Bharat Forge Ltd.", "Capital Goods & Infra", "Mid Cap"),
        SecurityTemplate("ASTRAL", "Astral Ltd.", "Materials & Cement", "Mid Cap"),
        SecurityTemplate("SUZLON", "Suzlon Energy Ltd.", "Power & Utilities", "Mid Cap"),
        SecurityTemplate("BSE", "BSE Ltd.", "Financial Services", "Mid Cap"),
        SecurityTemplate("MCX", "Multi Commodity Exchange of India Ltd.", "Financial Services", "Mid Cap"),
        SecurityTemplate("TVSMOTOR", "TVS Motor Company Ltd.", "Automobile", "Mid Cap"),
        SecurityTemplate("TATACOMM", "Tata Communications Ltd.", "Telecommunications", "Mid Cap"),
        SecurityTemplate("LTIM", "LTIMindtree Ltd.", "Information Technology", "Mid Cap"),
        SecurityTemplate("TORNTPHARM", "Torrent Pharmaceuticals Ltd.", "Healthcare & Pharma", "Mid Cap"),
        SecurityTemplate("ALKEM", "Alkem Laboratories Ltd.", "Healthcare & Pharma", "Mid Cap"),
        SecurityTemplate("GODREJPROP", "Godrej Properties Ltd.", "Realty & Infrastructure", "Mid Cap"),
        SecurityTemplate("BDL", "Bharat Dynamics Ltd.", "Aerospace & Defence", "Mid Cap"),
        SecurityTemplate("TATAELXSI", "Tata Elxsi Ltd.", "Information Technology", "Mid Cap"),
        SecurityTemplate("MUTHOOTFIN", "Muthoot Finance Ltd.", "Financial Services", "Mid Cap"),
        SecurityTemplate("BALKRISIND", "Balkrishna Industries Ltd.", "Automobile", "Mid Cap"),
        SecurityTemplate("ESCORTS", "Escorts Kubota Ltd.", "Capital Goods & Infra", "Mid Cap"),
        SecurityTemplate("DEEPAKNTR", "Deepak Nitrite Ltd.", "Chemicals & Agri", "Mid Cap"),
        SecurityTemplate("DALBHARAT", "Dalmia Bharat Ltd.", "Materials & Cement", "Mid Cap")
    )

    private val smallCapUniverse = listOf(
        SecurityTemplate("CDSL", "Central Depository Services (India) Ltd.", "Financial Services", "Small Cap"),
        SecurityTemplate("CAMS", "Computer Age Management Services Ltd.", "Financial Services", "Small Cap"),
        SecurityTemplate("KAYNES", "Kaynes Technology India Ltd.", "Consumer Electronics", "Small Cap"),
        SecurityTemplate("KPITTECH", "KPIT Technologies Ltd.", "Information Technology", "Small Cap"),
        SecurityTemplate("CYIENT", "Cyient Ltd.", "Information Technology", "Small Cap"),
        SecurityTemplate("SONACOMS", "Sona BLW Precision Forgings Ltd.", "Automobile", "Small Cap"),
        SecurityTemplate("JBCHEPHARM", "J.B. Chemicals & Pharmaceuticals Ltd.", "Healthcare & Pharma", "Small Cap"),
        SecurityTemplate("BLUEJET", "Blue Jet Healthcare Ltd.", "Healthcare & Pharma", "Small Cap"),
        SecurityTemplate("KEI", "KEI Industries Ltd.", "Capital Goods & Infra", "Small Cap"),
        SecurityTemplate("RATNAMANI", "Ratnamani Metals & Tubes Ltd.", "Metals & Mining", "Small Cap"),
        SecurityTemplate("APARINDS", "Apar Industries Ltd.", "Capital Goods & Infra", "Small Cap"),
        SecurityTemplate("KARURVYSYA", "Karur Vysya Bank Ltd.", "Financial Services", "Small Cap"),
        SecurityTemplate("REDINGTON", "Redington Ltd.", "Information Technology", "Small Cap"),
        SecurityTemplate("NAVINFLUOR", "Navin Fluorine International Ltd.", "Chemicals & Agri", "Small Cap"),
        SecurityTemplate("RADICO", "Radico Khaitan Ltd.", "FMCG", "Small Cap"),
        SecurityTemplate("ROUTE", "Route Mobile Ltd.", "Telecommunications", "Small Cap"),
        SecurityTemplate("AFFLE", "Affle (India) Ltd.", "Information Technology", "Small Cap"),
        SecurityTemplate("CEATLTD", "CEAT Ltd.", "Automobile", "Small Cap"),
        SecurityTemplate("HFCL", "HFCL Ltd.", "Telecommunications", "Small Cap"),
        SecurityTemplate("CASTROLIND", "Castrol India Ltd.", "Chemicals & Agri", "Small Cap"),
        SecurityTemplate("CENTURYPLY", "Century Plyboards (India) Ltd.", "Materials & Cement", "Small Cap"),
        SecurityTemplate("TEJASNET", "Tejas Networks Ltd.", "Telecommunications", "Small Cap"),
        SecurityTemplate("PPLPHARMA", "Piramal Pharma Ltd.", "Healthcare & Pharma", "Small Cap"),
        SecurityTemplate("NCC", "NCC Ltd.", "Capital Goods & Infra", "Small Cap"),
        SecurityTemplate("FINCABLES", "Finolex Cables Ltd.", "Capital Goods & Infra", "Small Cap"),
        SecurityTemplate("ELECON", "Elecon Engineering Company Ltd.", "Capital Goods & Infra", "Small Cap"),
        SecurityTemplate("BLS", "BLS International Services Ltd.", "Consumer Services", "Small Cap"),
        SecurityTemplate("GRAVITA", "Gravita India Ltd.", "Metals & Mining", "Small Cap"),
        SecurityTemplate("ANANTRAJ", "Anant Raj Ltd.", "Realty & Infrastructure", "Small Cap"),
        SecurityTemplate("JUBLINGREA", "Jubilant Ingrevia Ltd.", "Chemicals & Agri", "Small Cap")
    )

    private val microCapUniverse = listOf(
        SecurityTemplate("TRITURBINE", "Triveni Turbine Ltd.", "Capital Goods & Infra", "Micro Cap"),
        SecurityTemplate("PRAJIND", "Praj Industries Ltd.", "Capital Goods & Infra", "Micro Cap"),
        SecurityTemplate("EQUITASBNK", "Equitas Small Finance Bank Ltd.", "Financial Services", "Micro Cap"),
        SecurityTemplate("KNRCON", "KNR Constructions Ltd.", "Capital Goods & Infra", "Micro Cap"),
        SecurityTemplate("GATEWAY", "Gateway Distriparks Ltd.", "Logistics & Transport", "Micro Cap"),
        SecurityTemplate("DODLA", "Dodla Dairy Ltd.", "FMCG", "Micro Cap"),
        SecurityTemplate("PARAS", "Paras Defence & Space Technologies", "Aerospace & Defence", "Micro Cap"),
        SecurityTemplate("DCBBANK", "DCB Bank Ltd.", "Financial Services", "Micro Cap"),
        SecurityTemplate("SANGHVIMOV", "Sanghvi Movers Ltd.", "Capital Goods & Infra", "Micro Cap"),
        SecurityTemplate("SHILPAMED", "Shilpa Medicare Ltd.", "Healthcare & Pharma", "Micro Cap"),
        SecurityTemplate("TIPSINDLTD", "Tips Music Ltd.", "Consumer Services", "Micro Cap"),
        SecurityTemplate("STERTOOLS", "Sterling Tools Ltd.", "Automobile", "Micro Cap"),
        SecurityTemplate("APCOTEXIND", "Apcotex Industries Ltd.", "Chemicals & Agri", "Micro Cap"),
        SecurityTemplate("VIJAYA", "Vijaya Diagnostic Centre Ltd.", "Healthcare & Pharma", "Micro Cap"),
        SecurityTemplate("LANDMARK", "Landmark Cars Ltd.", "Automobile", "Micro Cap")
    )

    private val debtAndMoneyMarketUniverse = listOf(
        SecurityTemplate("GOI-7.18-2033", "7.18% Govt of India Sovereign G-Sec 2033", "Sovereign Debt", "Debt & Cash"),
        SecurityTemplate("GOI-7.26-2032", "7.26% Govt of India Sovereign G-Sec 2032", "Sovereign Debt", "Debt & Cash"),
        SecurityTemplate("GOI-7.30-2053", "7.30% Govt of India Long G-Sec 2053", "Sovereign Debt", "Debt & Cash"),
        SecurityTemplate("NABARD-AAA", "NABARD 7.65% AAA Corporate Bond", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("REC-AAA", "REC Ltd. 7.72% AAA Bond Series", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("PFC-AAA", "Power Finance Corp 7.68% AAA NCD", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("HDFC-NCD", "HDFC Bank 7.85% Senior Tier-II Bond", "Corporate Debt", "Debt & Cash"),
        SecurityTemplate("LICHF-AAA", "LIC Housing Finance 7.78% AAA NCD", "Corporate Debt", "Debt & Cash"),
        SecurityTemplate("IRFC-AAA", "Indian Railway Finance Corp 7.55% AAA NCD", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("NHAI-AAA", "National Highways Authority 7.60% AAA Bond", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("EXIM-AAA", "Export-Import Bank of India 7.70% AAA Bond", "PSU & Quasi-Sovereign", "Debt & Cash"),
        SecurityTemplate("RBI-TBILL-91D", "91-Day Government of India Treasury Bill", "Money Market", "Debt & Cash"),
        SecurityTemplate("RBI-TBILL-182D", "182-Day Government of India Treasury Bill", "Money Market", "Debt & Cash"),
        SecurityTemplate("RBI-TBILL-364D", "364-Day Government of India Treasury Bill", "Money Market", "Debt & Cash"),
        SecurityTemplate("SIDBI-CP", "SIDBI Commercial Paper A1+", "Money Market", "Debt & Cash"),
        SecurityTemplate("AXIS-CP", "Axis Bank Commercial Paper A1+", "Money Market", "Debt & Cash"),
        SecurityTemplate("CANARA-CD", "Canara Bank Certificate of Deposit A1+", "Money Market", "Debt & Cash"),
        SecurityTemplate("TREPS-CCIL", "CCIL Tri-Party Repo (TREPS) Overnight", "Cash & Equivalents", "Debt & Cash")
    )

    /**
     * Infers the standardized mutual fund category and asset class from an AMFI scheme name.
     */
    fun inferCategoryAndAssetClass(schemeName: String): Pair<String, String> {
        val lower = schemeName.lowercase(Locale.US)
        return when {
            lower.contains("liquid") -> Pair("Liquid", "Debt")
            lower.contains("overnight") -> Pair("Overnight", "Debt")
            lower.contains("gilt") || lower.contains("government") -> Pair("Gilt", "Debt")
            lower.contains("corporate bond") -> Pair("Corporate Bond", "Debt")
            lower.contains("short duration") || lower.contains("short term") -> Pair("Short Duration", "Debt")
            lower.contains("banking and psu") || lower.contains("banking & psu") -> Pair("Banking & PSU Debt", "Debt")
            lower.contains("debt") || lower.contains("income") || lower.contains("bond") || lower.contains("money market") ->
                Pair("Fixed Income", "Debt")

            lower.contains("balanced advantage") || lower.contains("dynamic asset") ->
                Pair("Balanced Advantage", "Hybrid")
            lower.contains("aggressive hybrid") || lower.contains("equity & debt") ->
                Pair("Aggressive Hybrid", "Hybrid")
            lower.contains("arbitrage") -> Pair("Arbitrage", "Hybrid")
            lower.contains("multi asset") -> Pair("Multi Asset Allocation", "Hybrid")
            lower.contains("hybrid") || lower.contains("balanced") -> Pair("Hybrid", "Hybrid")

            lower.contains("small cap") || lower.contains("smallcap") -> Pair("Small Cap", "Equity")
            lower.contains("mid cap") || lower.contains("midcap") -> Pair("Mid Cap", "Equity")
            lower.contains("large & mid") || lower.contains("large and mid") -> Pair("Large & Mid Cap", "Equity")
            lower.contains("flexi cap") || lower.contains("flexicap") -> Pair("Flexi Cap", "Equity")
            lower.contains("multi cap") || lower.contains("multicap") -> Pair("Multi Cap", "Equity")
            lower.contains("elss") || lower.contains("tax saver") -> Pair("ELSS Tax Saver", "Equity")
            lower.contains("index") || lower.contains("nifty") || lower.contains("sensex") -> Pair("Index / Passive", "Equity")
            lower.contains("focused") -> Pair("Focused Equity", "Equity")
            lower.contains("value") || lower.contains("contra") -> Pair("Value / Contra", "Equity")
            lower.contains("technology") || lower.contains("digital") || lower.contains("it ") -> Pair("Sectoral - IT", "Equity")
            lower.contains("pharma") || lower.contains("healthcare") -> Pair("Sectoral - Healthcare", "Equity")
            lower.contains("banking") || lower.contains("financial") -> Pair("Sectoral - Banking", "Equity")
            lower.contains("infra") -> Pair("Sectoral - Infra", "Equity")
            else -> Pair("Large Cap", "Equity")
        }
    }

    /**
     * Infers the Asset Management Company (AMC) display name from the scheme title.
     */
    fun inferAmcName(schemeName: String): String {
        val prefixes = listOf(
            "Parag Parikh" to "PPFAS Mutual Fund",
            "PPFAS" to "PPFAS Mutual Fund",
            "HDFC" to "HDFC Mutual Fund",
            "ICICI Prudential" to "ICICI Prudential Mutual Fund",
            "SBI" to "SBI Mutual Fund",
            "Nippon India" to "Nippon India Mutual Fund",
            "Kotak" to "Kotak Mahindra Mutual Fund",
            "Axis" to "Axis Mutual Fund",
            "Quant" to "Quant Mutual Fund",
            "Mirae Asset" to "Mirae Asset Mutual Fund",
            "Motilal Oswal" to "Motilal Oswal Mutual Fund",
            "UTI" to "UTI Mutual Fund",
            "Tata" to "Tata Mutual Fund",
            "DSP" to "DSP Mutual Fund",
            "Aditya Birla" to "Aditya Birla Sun Life Mutual Fund",
            "Franklin" to "Franklin Templeton Mutual Fund",
            "Edelweiss" to "Edelweiss Mutual Fund",
            "Canara Robeco" to "Canara Robeco Mutual Fund",
            "Invesco" to "Invesco Mutual Fund",
            "Bandhan" to "Bandhan Mutual Fund",
            "HSBC" to "HSBC Mutual Fund",
            "Sundaram" to "Sundaram Mutual Fund",
            "Zerodha" to "Zerodha Fund House",
            "Navi" to "Navi Mutual Fund"
        )
        for ((key, amcName) in prefixes) {
            if (schemeName.contains(key, ignoreCase = true)) return amcName
        }
        val words = schemeName.trim().split(" ")
        return if (words.size >= 2) "${words[0]}${words[1]} Mutual Fund" else "AMFI Registered AMC"
    }

    /**
     * Infers the appropriate SEBI benchmark index for a given category.
     */
    fun inferBenchmark(category: String): String = when {
        category.contains("Small Cap", ignoreCase = true) -> "NIFTY Smallcap 250 TRI"
        category.contains("Mid Cap", ignoreCase = true) -> "NIFTY Midcap 150 TRI"
        category.contains("Flexi Cap", ignoreCase = true) || category.contains("Multi Cap", ignoreCase = true) ->
            "NIFTY 500 TRI"
        category.contains("Hybrid", ignoreCase = true) || category.contains("Balanced", ignoreCase = true) ->
            "CRISIL Hybrid 35+65 Aggressive Index"
        category.contains("Liquid", ignoreCase = true) || category.contains("Overnight", ignoreCase = true) ->
            "CRISIL Liquid Debt A-I Index"
        category.contains("Debt", ignoreCase = true) || category.contains("Gilt", ignoreCase = true) ||
            category.contains("Bond", ignoreCase = true) || category.contains("Income", ignoreCase = true) ->
            "CRISIL Medium to Long Duration Debt Index"
        else -> "NIFTY 50 TRI"
    }

    /**
     * Infers a realistic Direct/Regular Total Expense Ratio (TER %) for a scheme.
     */
    fun inferExpenseRatio(category: String, isDirect: Boolean): Double {
        val baseDirectTer = when {
            category.contains("Index", ignoreCase = true) -> 0.18
            category.contains("Liquid", ignoreCase = true) || category.contains("Overnight", ignoreCase = true) -> 0.15
            category.contains("Debt", ignoreCase = true) || category.contains("Bond", ignoreCase = true) ||
                category.contains("Gilt", ignoreCase = true) -> 0.38
            category.contains("Small Cap", ignoreCase = true) || category.contains("Mid Cap", ignoreCase = true) -> 0.68
            category.contains("Hybrid", ignoreCase = true) || category.contains("Balanced", ignoreCase = true) -> 0.58
            else -> 0.54
        }
        return if (isDirect) baseDirectTer else ((baseDirectTer + 0.85) * 100.0).roundToInt() / 100.0
    }

    /**
     * Infers SEBI Riskometer classification from category and asset class.
     */
    fun inferRiskLevel(category: String, assetClass: String): String = when {
        category.contains("Liquid", ignoreCase = true) || category.contains("Overnight", ignoreCase = true) ->
            "Low to Moderate"
        assetClass.equals("Debt", ignoreCase = true) -> "Moderate"
        assetClass.equals("Hybrid", ignoreCase = true) -> "High"
        else -> "Very High"
    }

    /**
     * Backward-compatible alias for [generateHoldingsForScheme].
     */
    fun generateHoldings(
        schemeCode: Int,
        schemeName: String,
        category: String,
        assetClass: String
    ): List<UnderlyingHolding> = generateHoldingsForScheme(schemeCode, schemeName, category, assetClass)

    /**
     * Deterministically generates 45 to 70+ realistic constituent holdings for a given scheme without artificial caps.
     */
    fun generateHoldingsForScheme(
        schemeCode: Int,
        schemeName: String,
        category: String,
        assetClass: String
    ): List<UnderlyingHolding> {
        val rng = Random(schemeCode.toLong() * 31_415_926L)
        val candidatePool = mutableListOf<SecurityTemplate>()

        when {
            assetClass.equals("Debt", ignoreCase = true) -> {
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng))
            }
            assetClass.equals("Hybrid", ignoreCase = true) -> {
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(28))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(15))
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng).take(12))
            }
            category.contains("Small Cap", ignoreCase = true) -> {
                candidatePool.addAll(smallCapUniverse.shuffled(rng).take(26))
                candidatePool.addAll(microCapUniverse.shuffled(rng).take(14))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(15))
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(6))
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng).take(2))
            }
            category.contains("Mid Cap", ignoreCase = true) -> {
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(30))
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(15))
                candidatePool.addAll(smallCapUniverse.shuffled(rng).take(15))
                candidatePool.addAll(microCapUniverse.shuffled(rng).take(4))
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng).take(2))
            }
            category.contains("Sectoral - IT", ignoreCase = true) -> {
                val itStocks = (largeCapUniverse + midCapUniverse + smallCapUniverse + microCapUniverse)
                    .filter { it.sector == "Information Technology" }
                candidatePool.addAll(itStocks.shuffled(rng))
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(18))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(10))
            }
            category.contains("Sectoral - Healthcare", ignoreCase = true) -> {
                val pharmaStocks = (largeCapUniverse + midCapUniverse + smallCapUniverse + microCapUniverse)
                    .filter { it.sector == "Healthcare & Pharma" }
                candidatePool.addAll(pharmaStocks.shuffled(rng))
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(18))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(10))
            }
            category.contains("Flexi Cap", ignoreCase = true) || category.contains("Multi Cap", ignoreCase = true) -> {
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(30))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(20))
                candidatePool.addAll(smallCapUniverse.shuffled(rng).take(14))
                candidatePool.addAll(microCapUniverse.shuffled(rng).take(6))
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng).take(3))
            }
            else -> {
                // Default Large Cap / Index / ELSS mix
                candidatePool.addAll(largeCapUniverse.shuffled(rng).take(35))
                candidatePool.addAll(midCapUniverse.shuffled(rng).take(18))
                candidatePool.addAll(smallCapUniverse.shuffled(rng).take(6))
                candidatePool.addAll(debtAndMoneyMarketUniverse.shuffled(rng).take(2))
            }
        }

        val uniquePool = candidatePool.distinctBy { it.ticker }
        if (uniquePool.isEmpty()) return emptyList()

        // Generate descending institutional weights summing to ~98.2%
        val rawScores = uniquePool.mapIndexed { idx, _ ->
            val rankFactor = (uniquePool.size - idx).toDouble().pow(1.32)
            val jitter = 0.85 + (rng.nextDouble() * 0.30)
            rankFactor * jitter
        }
        val totalScore = rawScores.sum().coerceAtLeast(1.0)
        val targetAllocatedPercent = 98.2

        return uniquePool.mapIndexed { idx, security ->
            val rawWeight = (rawScores[idx] / totalScore) * targetAllocatedPercent
            val roundedWeight = ((rawWeight * 100.0).roundToInt() / 100.0).coerceAtLeast(0.15)
            UnderlyingHolding(
                ticker = security.ticker,
                companyName = security.companyName,
                sector = security.sector,
                marketCapBucket = security.marketCapBucket,
                weightPercent = roundedWeight
            )
        }.sortedByDescending { it.weightPercent }
    }

    fun isCashHolding(h: UnderlyingHolding): Boolean =
        h.sector.contains("Cash", ignoreCase = true) ||
                h.marketCapBucket.contains("Cash & Net", ignoreCase = true) ||
                h.ticker.contains("TREPS", ignoreCase = true) ||
                h.ticker.contains("CASH", ignoreCase = true) ||
                h.ticker.contains("NET_RECEIVABLES", ignoreCase = true)

    fun isDebtHolding(h: UnderlyingHolding): Boolean =
        !isCashHolding(h) && (
                h.sector.contains("Debt", ignoreCase = true) ||
                h.sector.contains("Sovereign", ignoreCase = true) ||
                h.sector.contains("Money Market", ignoreCase = true) ||
                h.sector.contains("PSU & Quasi", ignoreCase = true) ||
                h.sector.contains("Corporate Debt", ignoreCase = true) ||
                h.marketCapBucket.contains("Debt", ignoreCase = true) ||
                h.companyName.contains("Bond", ignoreCase = true) ||
                h.companyName.contains("G-Sec", ignoreCase = true) ||
                h.companyName.contains("Treasury Bill", ignoreCase = true) ||
                h.companyName.contains("Commercial Paper", ignoreCase = true) ||
                h.companyName.contains("Certificate of Deposit", ignoreCase = true)
        )

    fun isEquityHolding(h: UnderlyingHolding): Boolean =
        !isCashHolding(h) && !isDebtHolding(h)
}