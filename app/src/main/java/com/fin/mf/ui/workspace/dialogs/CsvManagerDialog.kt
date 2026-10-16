package com.fin.mf.ui.workspace.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary

/**
 * Portfolio CSV Import / Export Dialog with Clipboard support.
 */
@Composable
fun CsvManagerModal(
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val gradient = LocalVaultGradient.current
    var csvText by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Portfolio CSV Import / Export", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isImportMode,
                        onClick = { isImportMode = false },
                        label = { Text("Export CSV") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = gradient.startColor,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = isImportMode,
                        onClick = { isImportMode = true },
                        label = { Text("Import CSV") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = gradient.startColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                if (!isImportMode) {
                    val exportCsv = remember { portfolioViewModel.exportPortfolioCsv() }
                    Text(
                        text = "Format: SchemeCode,SchemeName,Units,InvestedAmount",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    OutlinedTextField(
                        value = exportCsv,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FinBaba Portfolio CSV", exportCsv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied CSV to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy CSV to Clipboard")
                    }
                } else {
                    Text(
                        text = "Paste CSV content below (header: SchemeCode,SchemeName,Units,InvestedAmount):",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = { Text("122639,\"Parag Parikh Flexi Cap\",2450.5,145000.0\n120503,\"Axis ELSS Tax Saver\",1500.0,90000.0") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        onClick = {
                            if (csvText.isNotBlank()) {
                                portfolioViewModel.importPortfolioFromCsv(csvText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticFinanceColors.ProfitGreen)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import & Overwrite Vault")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
