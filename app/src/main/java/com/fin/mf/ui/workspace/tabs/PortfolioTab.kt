package com.fin.mf.ui.workspace.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.workspace.components.WsSchemeSummaryCard
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite

/**
 * Tab #2: Portfolio Schemes List with Asset Class Filters and Search (Renamed from Schemes).
 */
@Composable
fun WorkspacePortfolioTab(
    uiState: PortfolioUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSchemeSelected: (Int) -> Unit,
    onDeleteScheme: (MutualFundScheme) -> Unit
) {
    val gradient = LocalVaultGradient.current
    val categories = listOf("All", "Equity", "Hybrid", "Debt", "Small Cap", "Mid Cap", "Large Cap", "Flexi Cap")
    val schemes = uiState.filteredSchemes

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = WsSurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search fund name, AMC, code, or stock...",
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val selected = uiState.selectedAssetClassFilter.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gradient.startColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Showing ${schemes.size} of ${uiState.portfolioData?.schemes?.size ?: 0} Active Schemes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WsSlateTextSecondary
                )
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
