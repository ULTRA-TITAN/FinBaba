package com.fin.mf.core.theme

import androidx.compose.ui.graphics.Color

/**
 * App-wide frozen financial semantic colors for FinBaba.
 *
 * CRITICAL INVARIANT:
 * These colors represent institutional financial metrics and regulatory categories.
 * They MUST NEVER be overwritten or replaced with dynamic brand gradient colors.
 */
object SemanticFinanceColors {
    // Financial Returns (Profit / Gain)
    val ProfitGreen = Color(0xFF059669)
    val ProfitGreenLight = Color(0xFF34D399)
    val ProfitGreenBackground = Color(0xFFECFDF5)

    // Financial Returns (Loss / Drawdown)
    val LossRed = Color(0xFFDC2626)
    val LossRedLight = Color(0xFFF87171)
    val LossRedBackground = Color(0xFFFEF2F2)

    // Index & Benchmark Comparisons (NIFTY 50 / NIFTY 500)
    val BenchmarkAmber = Color(0xFFD97706)
    val BenchmarkAmberLight = Color(0xFFF59E0B)

    // Portfolio Composite Overlays
    val PortfolioPurple = Color(0xFF7C3AED)

    // SEBI Market Capitalization Tiers
    val SebiLargeCap = Color(0xFF1E3A8A) // Deep Navy
    val SebiMidCap = Color(0xFF2563EB)   // Royal Blue
    val SebiSmallCap = Color(0xFF0891B2) // Ocean Cyan
    val SebiMicroCap = Color(0xFFD97706) // Amber
    val SebiDebtCash = Color(0xFF64748B) // Slate Gray

    // Neutral Surfaces & Chart Typography
    val SlatePrimary = Color(0xFF0F172A)
    val SlateSecondary = Color(0xFF475569)
    val SlateMuted = Color(0xFF94A3B8)
    val SlateBorder = Color(0xFFE2E8F0)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val BackgroundIce = Color(0xFFF8FAFC)
}
