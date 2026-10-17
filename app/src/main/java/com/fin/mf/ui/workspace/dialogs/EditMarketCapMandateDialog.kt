package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.MarketCapMandate
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import kotlin.math.roundToInt

/**
 * Interactive Dialog for configuring SEBI Market Cap Target Mandate (Large, Mid, Small, Micro).
 * Includes interactive steppers, sliders, live 100% validator, and institutional presets.
 */
@Composable
fun EditMarketCapMandateDialog(
    currentMandate: MarketCapMandate,
    onSave: (MarketCapMandate) -> Unit,
    onDismiss: () -> Unit
) {
    var largeCap by remember { mutableDoubleStateOf(currentMandate.largeCapTarget) }
    var midCap by remember { mutableDoubleStateOf(currentMandate.midCapTarget) }
    var smallCap by remember { mutableDoubleStateOf(currentMandate.smallCapTarget) }
    var microCap by remember { mutableDoubleStateOf(currentMandate.microCapTarget) }

    val total = (largeCap + midCap + smallCap + microCap).roundToInt()
    val isValid = total == 100
    val gradient = LocalVaultGradient.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Configure Target Mandate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Text(
                    text = "Set your ideal SEBI market cap allocation strategy.",
                    fontSize = 12.sp,
                    color = WsSlateTextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Preset Quick-Chips
                Text(
                    text = "STRATEGY PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(
                        modifier = Modifier.weight(1f),
                        label = "Balanced",
                        onClick = {
                            largeCap = 50.0
                            midCap = 25.0
                            smallCap = 20.0
                            microCap = 5.0
                        }
                    )
                    PresetChip(
                        modifier = Modifier.weight(1f),
                        label = "Conservative",
                        onClick = {
                            largeCap = 65.0
                            midCap = 20.0
                            smallCap = 10.0
                            microCap = 5.0
                        }
                    )
                    PresetChip(
                        modifier = Modifier.weight(1f),
                        label = "Aggressive",
                        onClick = {
                            largeCap = 35.0
                            midCap = 35.0
                            smallCap = 20.0
                            microCap = 10.0
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tier 1: Large Cap
                MandateTierSlider(
                    tierName = "Large Cap",
                    subtext = "Top 100 Indian Companies",
                    dotColor = SemanticFinanceColors.SebiLargeCap,
                    value = largeCap,
                    onValueChange = { largeCap = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tier 2: Mid Cap
                MandateTierSlider(
                    tierName = "Mid Cap",
                    subtext = "Rank 101 - 250",
                    dotColor = SemanticFinanceColors.SebiMidCap,
                    value = midCap,
                    onValueChange = { midCap = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tier 3: Small Cap
                MandateTierSlider(
                    tierName = "Small Cap",
                    subtext = "Rank 251 - 500",
                    dotColor = SemanticFinanceColors.SebiSmallCap,
                    value = smallCap,
                    onValueChange = { smallCap = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tier 4: Micro Cap
                MandateTierSlider(
                    tierName = "Micro Cap",
                    subtext = "Rank 501+ & High Alpha",
                    dotColor = SemanticFinanceColors.SebiMicroCap,
                    value = microCap,
                    onValueChange = { microCap = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live 100% Validator Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isValid) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isValid) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isValid) Color(0xFF059669) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isValid) "Total Mandate: 100% (Valid)" else "Total Mandate: $total% (Must sum to 100%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isValid) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                            if (!isValid) {
                                Text(
                                    text = if (total < 100) "Need +${100 - total}% to reach 100%" else "Reduce by ${total - 100}%",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            MarketCapMandate(
                                largeCapTarget = largeCap,
                                midCapTarget = midCap,
                                smallCapTarget = smallCap,
                                microCapTarget = microCap
                            )
                        )
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gradient.startColor,
                    disabledContainerColor = Color(0xFFCBD5E1)
                )
            ) {
                Text("Save Mandate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WsSlateTextSecondary)
            }
        }
    )
}

@Composable
private fun PresetChip(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = WsSlateTextPrimary,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MandateTierSlider(
    tierName: String,
    subtext: String,
    dotColor: Color,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = tierName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = subtext,
                            fontSize = 10.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onValueChange((value - 1.0).coerceIn(0.0, 100.0)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(16.dp),
                            tint = WsSlateTextSecondary
                        )
                    }

                    Text(
                        text = "${value.roundToInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = WsSlateTextPrimary,
                        modifier = Modifier.width(42.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    IconButton(
                        onClick = { onValueChange((value + 1.0).coerceIn(0.0, 100.0)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(16.dp),
                            tint = WsSlateTextSecondary
                        )
                    }
                }
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toDouble().roundToInt().toDouble()) },
                valueRange = 0f..100f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = dotColor,
                    activeTrackColor = dotColor,
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}
