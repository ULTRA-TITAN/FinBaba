package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.ui.PortfolioUiState
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.workspace.components.WsCrimsonNegative
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary

/**
 * Settings & Studio Controls Modal Dialog (Accessible via Top Right Gear Icon).
 */
@Composable
fun SettingsModalDialog(
    uiState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
    onOpenCsv: () -> Unit,
    onPromptClearPortfolio: () -> Unit,
    onOpenAppearanceSheet: () -> Unit
) {
    val gradient = LocalVaultGradient.current

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val dialogTitle = "Studio Settings & Tools"
    val visualIdentityTitle = "Visual Identity & Palette"
    val privacyModeTitle = "Privacy Mask Mode"
    val privacyModeSubtitle = "Obscures rupee valuations (₹ ••••••)"
    val dataManagementTitle = "Data Management"
    val btnCsvText = "Import / Export Portfolio"
    val btnClearVaultText = "Clear Active Vault"
    val appEditionTitle = "github.com/ULTRA-TITAN"
    // =========================================================================

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = gradient.startColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dialogTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Visual Identity & Appearance Studio
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(gradient.brush)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = visualIdentityTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = gradient.displayName,
                                    fontSize = 11.sp,
                                    color = gradient.startColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAppearanceSheet,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Feature Toggle: Privacy Mode
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = privacyModeTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = privacyModeSubtitle,
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isPrivacyMasked,
                            onCheckedChange = { portfolioViewModel.togglePrivacyMask() },
                            colors = SwitchDefaults.colors(checkedTrackColor = gradient.startColor)
                        )
                    }
                }

                // Feature Toggle: Floating Quick-Action Button (FAB)
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Action Button",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "Quick-access '+ Add Scheme' button",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isFloatingFabEnabled,
                            onCheckedChange = { portfolioViewModel.setFloatingFabEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = gradient.startColor)
                        )
                    }
                }

                // Feature Toggle: Floating Market Status / Holiday Ticker Pill
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Market Status & Holiday Ticker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WsSlateTextPrimary
                            )
                            Text(
                                text = "Live NSE/BSE trading status & holiday pill",
                                fontSize = 11.sp,
                                color = WsSlateTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isMarketStatusTickerEnabled,
                            onCheckedChange = { portfolioViewModel.setMarketStatusTickerEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = gradient.startColor)
                        )
                    }
                }

                // Data Management Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = dataManagementTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextSecondary
                    )

                    OutlinedButton(
                        onClick = onOpenCsv,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(btnCsvText, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onPromptClearPortfolio,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WsCrimsonNegative)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(btnClearVaultText, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = WsSlateBorder)

                // App Info & Diagnostic Stats
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = appEditionTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = "API: api.mfapi.in • Package: com.fin.mf",
                        fontSize = 11.sp,
                        color = WsSlateTextSecondary
                    )
                    Text(
                        text = "${uiState.portfolioData?.schemes?.size ?: 0} Active Schemes Persisted to Internal Storage",
                        fontSize = 11.sp,
                        color = gradient.startColor
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
            ) {
                Text("Done")
            }
        }
    )
}
