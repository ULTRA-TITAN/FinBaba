package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import java.util.Locale

@Composable
fun WsEditHoldingDialog(
    scheme: MutualFundScheme,
    isNewScheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    val gradient = LocalVaultGradient.current
    var unitsInput by remember { mutableStateOf(scheme.units.toString()) }
    var investedInput by remember { mutableStateOf(scheme.investedAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNewScheme) "Add Position: ${scheme.schemeName.take(20)}..." else "Edit Position: ${scheme.schemeName.take(20)}...",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AMFI #${scheme.schemeCode} • Latest NAV: ₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                    fontSize = 12.sp,
                    color = WsSlateTextSecondary
                )
                OutlinedTextField(
                    value = unitsInput,
                    onValueChange = { unitsInput = it },
                    label = { Text("Holding Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = investedInput,
                    onValueChange = { investedInput = it },
                    label = { Text("Invested Capital (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = unitsInput.toDoubleOrNull() ?: scheme.units
                    val inv = investedInput.toDoubleOrNull() ?: scheme.investedAmount
                    onConfirm(u, inv)
                },
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
            ) {
                Text(if (isNewScheme) "Add Scheme" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
