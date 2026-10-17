package com.fin.mf.ui.workspace.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.PortfolioManifest
import com.fin.mf.data.PortfolioMeta
import com.fin.mf.ui.workspace.components.WsElevatedCard
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary
import com.fin.mf.ui.workspace.components.formatWsInr

/**
 * Bottom sheet allowing user to inspect all local portfolios, switch active portfolio,
 * or trigger creation, rename, duplicate, and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioSwitcherBottomSheet(
    manifest: PortfolioManifest,
    isMasked: Boolean,
    onSelectPortfolio: (String) -> Unit,
    onRequestCreate: () -> Unit,
    onRequestRename: (PortfolioMeta) -> Unit,
    onRequestDuplicate: (PortfolioMeta) -> Unit,
    onRequestDelete: (PortfolioMeta) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gradient = LocalVaultGradient.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        // =========================================================================
        // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
        // =========================================================================
        val sheetTitle = "Switch Portfolio Vault"
        val sheetSubtitle = "${manifest.portfolios.size} Saved Portfolios On Device"
        val btnCreateVault = "+ Create New Vault"
        val actionRename = "Rename Vault"
        val actionDuplicate = "Duplicate"
        val actionDelete = "Delete Vault"
        // =========================================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = gradient.startColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sheetTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = sheetSubtitle,
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WsSlateTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = manifest.portfolios, key = { it.id }) { meta ->
                    val isActive = meta.id == manifest.activePortfolioId
                    val lastUpdatedStr = remember(meta.createdAtEpochMs) {
                        if (meta.createdAtEpochMs > 0L) {
                            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date(meta.createdAtEpochMs))
                        } else {
                            "Recently"
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isActive) {
                                    onSelectPortfolio(meta.id)
                                    onDismiss()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) gradient.startColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isActive) gradient.startColor else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) gradient.startColor else Color(0xFFCBD5E1))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = meta.name,
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isActive) gradient.startColor else WsSlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${meta.schemeCount} Mutual Funds • Updated $lastUpdatedStr",
                                            fontSize = 11.sp,
                                            color = WsSlateTextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatWsInr(meta.totalValue, isMasked),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WsSlateTextPrimary
                                    )
                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = gradient.startColor.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, gradient.startColor.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = gradient.startColor,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = gradient.startColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(4.dp))

                            // Item Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onRequestRename(meta) }
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(actionRename, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { onRequestDuplicate(meta) }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(actionDuplicate, fontSize = 11.sp)
                                }
                                if (manifest.portfolios.size > 1) {
                                    TextButton(
                                        onClick = { onRequestDelete(meta) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = SemanticFinanceColors.LossRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(actionDelete, fontSize = 11.sp, color = SemanticFinanceColors.LossRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Add Button
            Button(
                onClick = onRequestCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(btnCreateVault, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Dialog for creating a new portfolio (empty or seeded).
 */
@Composable
fun CreatePortfolioDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, seedWithDefault: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val gradient = LocalVaultGradient.current

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val dialogTitle = "Create New Vault"
    val dialogPrompt = "Enter a distinct name for your portfolio vault:"
    val namePlaceholder = "e.g. Retirement 2040, Tech Core"
    val helperInfo = "Initializes an authentic empty vault. Add funds anytime via live AMFI search or CSV import."
    val btnConfirmText = "Create Vault"
    // =========================================================================

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = dialogTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = dialogPrompt,
                    fontSize = 12.sp,
                    color = WsSlateTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = helperInfo,
                    fontSize = 11.5.sp,
                    color = WsSlateTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { "Custom Vault" }, false) },
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btnConfirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for renaming an existing portfolio.
 */
@Composable
fun RenamePortfolioDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    val gradient = LocalVaultGradient.current

    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val dialogTitle = "Rename Vault"
    val dialogPrompt = "Enter the new name for this vault:"
    val btnConfirmText = "Save Name"
    // =========================================================================

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(dialogTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(dialogPrompt, fontSize = 12.sp, color = WsSlateTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btnConfirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Confirmation dialog for deleting a portfolio.
 */
@Composable
fun DeletePortfolioDialog(
    portfolioName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // =========================================================================
    // DISPLAY TEXT CONFIGURATION: Safe to edit text between quotes below
    // =========================================================================
    val dialogTitle = "Delete Vault?"
    val dialogWarning = "Are you sure you want to delete '$portfolioName'? All positions and settings in this vault will be removed from your device."
    val btnConfirmText = "Delete Vault"
    // =========================================================================

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(dialogTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = dialogWarning,
                fontSize = 13.sp,
                color = WsSlateTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SemanticFinanceColors.LossRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btnConfirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
