package com.fin.mf.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolId {
    PORTFOLIO_LOOKTHROUGH,
    GLOBAL_MARKET_TERMINAL,
    SIP_CAGR_SIMULATOR
}

data class ToolMetadata(
    val id: ToolId,
    val toolNumber: String,
    val statusLabel: String,
    val isAvailable: Boolean,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val metricsHighlight: String,
    val primaryButtonText: String,
    val keyFeatures: List<String>
)

object ToolRegistry {
    val tools: List<ToolMetadata> = listOf(
        ToolMetadata(
            id = ToolId.PORTFOLIO_LOOKTHROUGH,
            toolNumber = "TOOL #1",
            statusLabel = "ACTIVE",
            isAvailable = true,
            icon = Icons.Default.PieChart,
            title = "Mutual Fund Portfolio & Look-Through Tracker",
            description = "Institutional workspace with live parallel AMFI NAV syncing, position CRUD, CSV import/export, and look-through constituent inspection.",
            metricsHighlight = "20 Schemes • Real-Time AMFI Engine • Overlap X-Ray",
            primaryButtonText = "Open Workspace",
            keyFeatures = listOf(
                "Live AMFI NAV Sync across 20+ schemes",
                "Look-Through Equity & Debt Overlap Analyzer",
                "Custom CSV Import / Export",
                "MF-Hunter with Smart Alias Search"
            )
        ),
        ToolMetadata(
            id = ToolId.GLOBAL_MARKET_TERMINAL,
            toolNumber = "TOOL #2",
            statusLabel = "TOOL #2 ACTIVE",
            isAvailable = true,
            icon = Icons.AutoMirrored.Filled.ShowChart,
            title = "Global Market Terminal & Broker Studio",
            description = "Real-time quotes, multi-timeframe charts, dual-pointer scrubbing, and Nifty 50 benchmark comparison for any Indian or global asset.",
            metricsHighlight = "NSE/BSE Equities & Global Assets • Benchmark Alpha",
            primaryButtonText = "Launch Terminal",
            keyFeatures = listOf(
                "NSE / BSE Indian Stocks (.NS, .BO) & Global Assets",
                "Interactive Dual-Pointer Crosshair & Scrubbing",
                "Normalized Benchmark Comparison vs NIFTY 50 (^NSEI)",
                "Multi-Timeframe Charts (1D, 5D, 1M, 6M, 1Y, 5Y)"
            )
        ),
        ToolMetadata(
            id = ToolId.SIP_CAGR_SIMULATOR,
            toolNumber = "TOOL #3",
            statusLabel = "UPCOMING • TOOL #3",
            isAvailable = false,
            icon = Icons.Default.Analytics,
            title = "CAGR & Step-Up SIP Simulator",
            description = "Model multi-decade wealth compounding combining your active portfolio equity with annual step-up SIP contributions and custom CAGR projections.",
            metricsHighlight = "Interactive 3–30 Year Wealth Compounding Architect",
            primaryButtonText = "Preview Coming Soon",
            keyFeatures = listOf(
                "Dynamic Step-Up SIP Calculator",
                "Multi-Decade Compounding Projections",
                "Portfolio Integration Assumption Engine"
            )
        )
    )
}
