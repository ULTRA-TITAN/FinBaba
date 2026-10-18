package com.fin.mf.ui.workspace.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fin.mf.ui.components.bounceClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.workspace.components.OnboardingTutorialCard
import com.fin.mf.ui.workspace.components.WsSchemeSummaryCard
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite

internal data class CategoryFilterItem(
    val name: String,
    val icon: ImageVector
)

/**
 * Tab #2: Portfolio Schemes List with Asset Class Filters and Search (Renamed from Schemes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacePortfolioTab(
    uiState: PortfolioUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSchemeSelected: (Int) -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit,
    onAddSchemeClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onResumeDraft: () -> Unit = {},
    onDiscardDraft: () -> Unit = {}
) {
    val gradient = LocalVaultGradient.current
    val allSchemes = uiState.portfolioData?.schemes.orEmpty()
    val hasDraft = !uiState.cachedReconciliationDraft.isNullOrEmpty()

    if (allSchemes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasDraft) {
                DraftImportBanner(
                    count = uiState.cachedReconciliationDraft!!.size,
                    onResume = onResumeDraft,
                    onDiscard = onDiscardDraft
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                OnboardingTutorialCard(
                    onAddSchemeClick = onAddSchemeClick
                )
            }
        }
        return
    }

    val categoryCounts = remember(allSchemes) {
        mapOf(
            "All" to allSchemes.size,
            "Large Cap" to allSchemes.count { it.assetClass.equals("Large Cap", true) || it.category.contains("Large Cap", true) },
            "Mid Cap" to allSchemes.count { it.assetClass.equals("Mid Cap", true) || it.category.contains("Mid Cap", true) },
            "Small Cap" to allSchemes.count { it.assetClass.equals("Small Cap", true) || it.category.contains("Small Cap", true) },
            "Hybrid" to allSchemes.count { it.assetClass.equals("Hybrid", true) || it.category.contains("Hybrid", true) },
            "Debt" to allSchemes.count { it.assetClass.equals("Debt", true) || it.category.contains("Debt", true) }
        )
    }

    val categoryFilters = remember {
        listOf(
            CategoryFilterItem("All", Icons.Default.Layers),
            CategoryFilterItem("Large Cap", Icons.Default.AccountBalance),
            CategoryFilterItem("Mid Cap", Icons.AutoMirrored.Filled.TrendingUp),
            CategoryFilterItem("Small Cap", Icons.Default.RocketLaunch),
            CategoryFilterItem("Hybrid", Icons.Default.Balance),
            CategoryFilterItem("Debt", Icons.Default.Shield)
        )
    }
    val schemes = uiState.filteredSchemes

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = WsSurfaceWhite,
            border = BorderStroke(1.dp, WsSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // =========================================================================
                // DISPLAY TEXT CONFIGURATION: Search placeholder
                // =========================================================================
                val searchPlaceholder = "Search fund name, AMC, code, or stock..."
                // =========================================================================

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = WsSlateTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = WsSlateTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryFilters.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val selected = uiState.selectedAssetClassFilter.equals(item.name, ignoreCase = true)
                                val count = categoryCounts[item.name] ?: 0
                                val shape = RoundedCornerShape(10.dp)

                                Surface(
                                    onClick = { onFilterChange(item.name) },
                                    shape = shape,
                                    color = if (selected) gradient.startColor else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) gradient.startColor else WsSlateBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = if (selected) Color.White else WsSlateTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else WsSlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (selected) Color.White.copy(alpha = 0.25f)
                                                    else Color(0xFFE2E8F0)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) Color.White else WsSlateTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = uiState.isSyncingNavs,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isSyncingNavs,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = gradient.startColor
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasDraft) {
                    item(key = "reconciliation_draft_banner") {
                        DraftImportBanner(
                            count = uiState.cachedReconciliationDraft!!.size,
                            onResume = onResumeDraft,
                            onDiscard = onDiscardDraft
                        )
                    }
                }

                item {
                    // =========================================================================
                    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
                    // =========================================================================
                    val totalCount = uiState.portfolioData?.schemes?.size ?: 0
                    val headerText = "Showing ${schemes.size} of $totalCount Active Schemes"
                    val addSchemeText = "+ Add Scheme"
                    // =========================================================================

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WsSlateTextSecondary
                        )
                        OutlinedButton(
                            onClick = onAddSchemeClick,
                            modifier = Modifier.bounceClick(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = gradient.startColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = addSchemeText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = gradient.startColor
                            )
                        }
                    }
                }

                items(items = schemes, key = { it.schemeCode }) { scheme ->
                    WsSchemeSummaryCard(
                        scheme = scheme,
                        isMasked = uiState.isPrivacyMasked,
                        onClick = { onSchemeSelected(scheme.schemeCode) },
                        onDelete = { onDeleteScheme(scheme) }
                    )
                }
            }
        }
    }
}

/**
 * Slim, high-visibility banner rendered when an imported spreadsheet reconciliation draft is pending.
 */
@Composable
private fun DraftImportBanner(
    count: Int,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Draft Import Available",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "$count schemes ready for review",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDiscard,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Discard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.bounceClick()
                ) {
                    Text(
                        text = "Review",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
