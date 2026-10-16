package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.UnderlyingHolding
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.WsSurfaceWhite
import com.fin.mf.ui.workspace.components.formatWsInr
import java.util.Locale

private val WsBgWhite = Color(0xFFF8FAFC)

/**
 * Full Portfolio Look-Through Constituents Modal Dialog with Reactive Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHoldingsModal(
    schemeName: String,
    holdings: List<UnderlyingHolding>,
    schemeValue: Double,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    var filterText by remember { mutableStateOf("") }

    val filtered = remember(holdings, filterText) {
        if (filterText.isBlank()) holdings else {
            val q = filterText.trim()
            holdings.filter {
                it.companyName.contains(q, ignoreCase = true) ||
                        it.ticker.contains(q, ignoreCase = true) ||
                        it.sector.contains(q, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WsBgWhite
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Full Portfolio Constituents",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = schemeName,
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = gradient.startColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WsSurfaceWhite)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search stock, ticker, or sector...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gradient.startColor) },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { filterText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total ${filtered.size} constituent holdings (${holdings.size} total in fund)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WsSlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { holding ->
                            val rupeeContribution = schemeValue * (holding.weightPercent / 100.0)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${holding.companyName} (${holding.ticker})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WsSlateTextPrimary
                                        )
                                        Text(
                                            text = "${holding.sector} • ${holding.marketCapBucket}",
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f%%", holding.weightPercent),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = gradient.startColor
                                        )
                                        Text(
                                            text = formatWsInr(rupeeContribution, isMasked),
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
