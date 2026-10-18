package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.clickable
import com.fin.mf.ui.components.bounceClick
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.workspace.components.WsAccentRoyal
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary

/**
 * Universal AMFI Scheme Addition Dialog with Filter Chips and Spacious UI.
 */
@Composable
fun AmfiAddSchemeModal(
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var selectedSchemeCode by remember { mutableStateOf<Int?>(null) }
    var selectedSchemeName by remember { mutableStateOf("") }
    var unitsInput by remember { mutableStateOf("100.0") }
    var investedInput by remember { mutableStateOf("10000.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Mutual Fund Scheme",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                if (selectedSchemeCode == null) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            portfolioViewModel.searchAmfiSchemes(it)
                        },
                        label = { Text("Search AMFI Scheme Name / Code...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    portfolioViewModel.clearAmfiSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            color = WsSlateTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gradient.startColor,
                            unfocusedBorderColor = WsSlateBorder,
                            cursorColor = gradient.startColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Smart Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Direct - Growth", "Large Cap", "Mid Cap", "Small Cap", "Debt").forEach { cat ->
                            val isSel = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gradient.startColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredResults = remember(uiState.amfiSearchResults, selectedCategoryFilter) {
                        val raw = uiState.amfiSearchResults
                        if (selectedCategoryFilter == "All") {
                            raw
                        } else if (selectedCategoryFilter == "Direct - Growth") {
                            raw.filter { it.schemeName.contains("Direct", ignoreCase = true) && it.schemeName.contains("Growth", ignoreCase = true) }
                        } else {
                            raw.filter { it.schemeName.contains(selectedCategoryFilter, ignoreCase = true) }
                        }
                    }

                    when {
                        uiState.isAmfiSearching -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = WsAccentRoyal)
                            }
                        }
                        filteredResults.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items = filteredResults, key = { it.schemeCode }) { res ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceClick()
                                            .clickable {
                                                selectedSchemeCode = res.schemeCode
                                                selectedSchemeName = res.schemeName
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = res.schemeName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = WsSlateTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "AMFI Code: #${res.schemeCode}",
                                                fontSize = 11.sp,
                                                color = gradient.startColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Type any mutual fund query (e.g. 'Parag', 'Quant', 'SBI').",
                                    fontSize = 12.sp,
                                    color = WsSlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Enter units and invested amount
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedSchemeName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradient.startColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Code: #$selectedSchemeCode", fontSize = 11.sp, color = WsSlateTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Holding Units") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = investedInput,
                        onValueChange = { investedInput = it },
                        label = { Text("Invested Capital (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedSchemeCode = null }) {
                        Text("← Choose different scheme")
                    }
                }
            }
        },
        confirmButton = {
            if (selectedSchemeCode != null) {
                Button(
                    onClick = {
                        val units = unitsInput.toDoubleOrNull() ?: 100.0
                        val invested = investedInput.toDoubleOrNull() ?: 10000.0
                        selectedSchemeCode?.let { code ->
                            portfolioViewModel.addSearchedScheme(code, units, invested)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
                ) {
                    Text("Add to Vault")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
