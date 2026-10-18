package com.fin.mf.data.engine

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Confidence bucket for AMFI scheme reconciliation.
 */
enum class MatchConfidence {
    AUTO_MATCHED,
    AMBIGUOUS,
    UNMATCHED
}

/**
 * Output model representing a reconciled mutual fund scheme ready for user preview.
 */
data class ReconciledSchemeItem(
    val originalRawName: String,
    val matchedSchemeCode: Int?,
    val matchedSchemeName: String?,
    val category: String,
    val units: Double,
    val investedAmount: Double,
    val matchConfidence: MatchConfidence,
    val similarityScore: Float,
    val candidateSuggestions: List<Pair<Int, String>> = emptyList()
)

/**
 * Intelligent fuzzy scheme reconciliation engine combining noise-word stripping,
 * Token Sort Ratio (Levenshtein on sorted tokens), and Jaccard token overlap.
 */
object AmfiFuzzyReconciler {

    private val NOISE_WORDS = setOf(
        "DIRECT",
        "REGULAR",
        "GROWTH",
        "IDCW",
        "DIVIDEND",
        "PLAN",
        "OPTION",
        "PAYOUT",
        "REINVESTMENT",
        "MUTUAL",
        "FUND",
        "LTD",
        "LIMITED",
        "INSTITUTIONAL"
    )

    private const val AUTO_MATCH_THRESHOLD = 0.85f
    private const val AMBIGUOUS_THRESHOLD = 0.65f
    private const val CLOSE_CANDIDATE_MARGIN = 0.05f
    private const val MAX_SUGGESTIONS = 5

    private data class PreparedDictionaryEntry(
        val code: Int,
        val originalName: String,
        val tokens: Set<String>,
        val sortedTokenString: String,
        val isDirect: Boolean,
        val isIdcw: Boolean
    )

    /**
     * Reconciles a batch of [RawBrokerRecord] items against an AMFI scheme dictionary.
     *
     * @param records Normalized records from [BrokerFormatNormalizer].
     * @param amfiDictionary List of `(schemeCode, schemeName)` pairs.
     */
    fun reconcileAll(
        records: List<RawBrokerRecord>,
        amfiDictionary: List<Pair<Int, String>>
    ): List<ReconciledSchemeItem> {
        if (records.isEmpty()) return emptyList()

        val preparedDict = amfiDictionary.map { (code, name) ->
            val tokens = extractCleanTokens(name)
            val upper = name.uppercase(Locale.ROOT)
            PreparedDictionaryEntry(
                code = code,
                originalName = name,
                tokens = tokens,
                sortedTokenString = tokens.sorted().joinToString(" "),
                isDirect = upper.contains("DIRECT"),
                isIdcw = upper.contains("IDCW") || upper.contains("DIVIDEND")
            )
        }

        val dictByCode = preparedDict.associateBy { it.code }

        return records.map { record ->
            reconcileSingle(record, preparedDict, dictByCode)
        }
    }

    /**
     * Reconciles a single raw scheme name against the AMFI dictionary.
     */
    fun reconcileScheme(
        rawName: String,
        units: Double,
        investedAmount: Double,
        amfiDictionary: List<Pair<Int, String>>,
        rawCode: Int? = null
    ): ReconciledSchemeItem {
        val record = RawBrokerRecord(
            rawSchemeName = rawName,
            units = units,
            investedCost = investedAmount,
            rawCode = rawCode,
            rowNumber = 1
        )
        return reconcileAll(listOf(record), amfiDictionary).first()
    }

    private fun reconcileSingle(
        record: RawBrokerRecord,
        preparedDict: List<PreparedDictionaryEntry>,
        dictByCode: Map<Int, PreparedDictionaryEntry>
    ): ReconciledSchemeItem {
        val rawName = record.rawSchemeName.trim()
        if (rawName.isBlank()) {
            return ReconciledSchemeItem(
                originalRawName = rawName,
                matchedSchemeCode = null,
                matchedSchemeName = null,
                category = "Uncategorized",
                units = record.units,
                investedAmount = record.investedCost,
                matchConfidence = MatchConfidence.UNMATCHED,
                similarityScore = 0f,
                candidateSuggestions = emptyList()
            )
        }

        // 1. Direct AMFI scheme code lookup if present in the spreadsheet
        if (record.rawCode != null) {
            val exactEntry = dictByCode[record.rawCode]
            if (exactEntry != null) {
                return ReconciledSchemeItem(
                    originalRawName = rawName,
                    matchedSchemeCode = exactEntry.code,
                    matchedSchemeName = exactEntry.originalName,
                    category = inferCategory(exactEntry.originalName),
                    units = record.units,
                    investedAmount = record.investedCost,
                    matchConfidence = MatchConfidence.AUTO_MATCHED,
                    similarityScore = 1.0f,
                    candidateSuggestions = listOf(exactEntry.code to exactEntry.originalName)
                )
            }
        }

        val queryTokens = extractCleanTokens(rawName)
        if (queryTokens.isEmpty() || preparedDict.isEmpty()) {
            return ReconciledSchemeItem(
                originalRawName = rawName,
                matchedSchemeCode = null,
                matchedSchemeName = null,
                category = inferCategory(rawName),
                units = record.units,
                investedAmount = record.investedCost,
                matchConfidence = MatchConfidence.UNMATCHED,
                similarityScore = 0f,
                candidateSuggestions = emptyList()
            )
        }

        val querySortedStr = queryTokens.sorted().joinToString(" ")
        val rawUpper = rawName.uppercase(Locale.ROOT)
        val queryWantsDirect = rawUpper.contains("DIRECT") || rawUpper.contains(" DIR ")
        val queryWantsRegular = rawUpper.contains("REGULAR") || rawUpper.contains(" REG ")
        val queryWantsIdcw = rawUpper.contains("IDCW") || rawUpper.contains("DIVIDEND")

        data class ScoredCandidate(
            val entry: PreparedDictionaryEntry,
            val combinedScore: Float
        )

        val scoredCandidates = ArrayList<ScoredCandidate>(preparedDict.size)

        for (candidate in preparedDict) {
            if (candidate.tokens.isEmpty()) continue

            val jaccard = computeJaccardSimilarity(queryTokens, candidate.tokens)
            // Fast pre-filter: if there is zero token overlap and strings are long, skip heavy Levenshtein
            if (jaccard == 0f && querySortedStr.length > 8 && candidate.sortedTokenString.length > 8) {
                val firstTokenMatch = queryTokens.any { qt ->
                    candidate.tokens.any { ct -> ct.startsWith(qt.take(3)) || qt.startsWith(ct.take(3)) }
                }
                if (!firstTokenMatch) continue
            }

            val tokenSortRatio = computeLevenshteinSimilarity(querySortedStr, candidate.sortedTokenString)

            // Blend Token Sort Ratio (60%) + Jaccard Token Overlap (40%)
            var score = (0.60f * tokenSortRatio) + (0.40f * jaccard)

            // Subtle tie-breaker alignment when user raw text specifies Direct/Regular or Growth/IDCW
            if (queryWantsDirect && !candidate.isDirect) score -= 0.015f
            if (queryWantsRegular && candidate.isDirect) score -= 0.015f
            if (queryWantsIdcw != candidate.isIdcw) score -= 0.010f

            scoredCandidates.add(
                ScoredCandidate(
                    entry = candidate,
                    combinedScore = score.coerceIn(0f, 1f)
                )
            )
        }

        scoredCandidates.sortByDescending { it.combinedScore }

        val topMatch = scoredCandidates.firstOrNull()
        val secondMatch = scoredCandidates.getOrNull(1)
        val topSuggestions = scoredCandidates
            .take(MAX_SUGGESTIONS)
            .map { it.entry.code to it.entry.originalName }

        if (topMatch == null || topMatch.combinedScore < AMBIGUOUS_THRESHOLD) {
            return ReconciledSchemeItem(
                originalRawName = rawName,
                matchedSchemeCode = null,
                matchedSchemeName = null,
                category = inferCategory(rawName),
                units = record.units,
                investedAmount = record.investedCost,
                matchConfidence = MatchConfidence.UNMATCHED,
                similarityScore = topMatch?.combinedScore ?: 0f,
                candidateSuggestions = topSuggestions
            )
        }

        val hasCloseRunnerUp = secondMatch != null &&
            (topMatch.combinedScore - secondMatch.combinedScore) < CLOSE_CANDIDATE_MARGIN &&
            extractCleanTokens(topMatch.entry.originalName) != extractCleanTokens(secondMatch.entry.originalName)

        val confidence = when {
            topMatch.combinedScore >= AUTO_MATCH_THRESHOLD && !hasCloseRunnerUp ->
                MatchConfidence.AUTO_MATCHED
            else ->
                MatchConfidence.AMBIGUOUS
        }

        return ReconciledSchemeItem(
            originalRawName = rawName,
            matchedSchemeCode = topMatch.entry.code,
            matchedSchemeName = topMatch.entry.originalName,
            category = inferCategory(topMatch.entry.originalName),
            units = record.units,
            investedAmount = record.investedCost,
            matchConfidence = confidence,
            similarityScore = topMatch.combinedScore,
            candidateSuggestions = topSuggestions
        )
    }

    /**
     * Cleanses a scheme name by uppercasing, replacing punctuation with spaces,
     * and stripping noise words defined in [NOISE_WORDS].
     */
    fun extractCleanTokens(rawName: String): Set<String> {
        val normalized = rawName
            .uppercase(Locale.ROOT)
            .replace("&", " AND ")
            .replace(Regex("[^A-Z0-9\\s]"), " ")

        val rawTokens = normalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in NOISE_WORDS }

        return rawTokens.toSet()
    }

    /**
     * Computes Jaccard overlap: `|A ∩ B| / |A ∪ B|`.
     */
    private fun computeJaccardSimilarity(tokensA: Set<String>, tokensB: Set<String>): Float {
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0f
        var intersection = 0
        for (token in tokensA) {
            if (token in tokensB) intersection++
        }
        val union = tokensA.size + tokensB.size - intersection
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Computes normalized Levenshtein similarity in `[0.0f, 1.0f]` using O(min(N, M)) space.
     */
    private fun computeLevenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f

        val distance = levenshteinDistance(s1, s2)
        return (1.0f - (distance.toFloat() / maxLen.toFloat())).coerceIn(0f, 1f)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length

        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1)

        for (i in 1..rhsLen) {
            newCost[0] = i
            val rhsChar = rhs[i - 1]
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhsChar) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }

    /**
     * Infers a SEBI-aligned mutual fund category from the scheme title.
     */
    fun inferCategory(schemeName: String): String {
        val upper = schemeName.uppercase(Locale.ROOT)
        return when {
            upper.contains("ELSS") || upper.contains("TAX SAVER") -> "Equity • ELSS"
            upper.contains("LIQUID") || upper.contains("OVERNIGHT") || upper.contains("MONEY MARKET") -> "Debt • Liquid"
            upper.contains("GILT") || upper.contains("CORPORATE BOND") || upper.contains("SHORT DURATION") || upper.contains("DYNAMIC BOND") -> "Debt • Fixed Income"
            upper.contains("ARBITRAGE") || upper.contains("BALANCED ADVANTAGE") || upper.contains("AGGRESSIVE HYBRID") || upper.contains("HYBRID") || upper.contains("MULTI ASSET") -> "Hybrid"
            upper.contains("INDEX") || upper.contains("NIFTY") || upper.contains("SENSEX") || upper.contains("ETF") -> "Passive • Index / ETF"
            upper.contains("SMALL CAP") || upper.contains("SMALLCAP") -> "Equity • Small Cap"
            upper.contains("MID CAP") || upper.contains("MIDCAP") -> "Equity • Mid Cap"
            upper.contains("FLEXI CAP") || upper.contains("FLEXICAP") || upper.contains("MULTI CAP") || upper.contains("MULTICAP") -> "Equity • Flexi / Multi Cap"
            upper.contains("LARGE CAP") || upper.contains("LARGECAP") || upper.contains("BLUECHIP") -> "Equity • Large Cap"
            upper.contains("GOLD") || upper.contains("SILVER") || upper.contains("COMMODIT") -> "Commodities"
            else -> "Equity • Diversified"
        }
    }
}