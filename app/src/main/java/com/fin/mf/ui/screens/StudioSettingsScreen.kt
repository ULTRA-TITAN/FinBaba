package com.fin.mf.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import com.fin.mf.data.PortfolioData
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.ui.workspace.dialogs.UniversalDataManagementDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.CardHardwareVariant
import com.fin.mf.core.theme.LocalCardHardware
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.VaultGradient
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.core.theme.VaultThemePresets
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.components.bounceClick
import com.fin.mf.ui.workspace.components.WsCrimsonNegative
import com.fin.mf.ui.workspace.components.WsPrimaryIndigo
import com.fin.mf.ui.workspace.components.WsSlateBorder
import com.fin.mf.ui.workspace.components.WsSlateTextPrimary
import com.fin.mf.ui.workspace.components.WsSlateTextSecondary

private val SettingsBg = Color(0xFFF8FAFC)
private val SettingsSurface = Color(0xFFFFFFFF)

/**
 * Dedicated Full-Screen Destination for FinBaba Studio Settings & Configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsScreen(
    portfolioViewModel: PortfolioViewModel,
    themeManager: VaultThemeManager,
    onNavigateBack: () -> Unit
) {
    val gradient = LocalVaultGradient.current
    val currentHardware = LocalCardHardware.current
    val uiState by portfolioViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showClearConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showUniversalDataDialog by rememberSaveable { mutableStateOf(false) }
    var universalInitialTabIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = SettingsBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsSurface,
                    titleContentColor = WsSlateTextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = gradient.startColor
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Studio Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WsSlateTextPrimary
                        )
                        Text(
                            text = "Hardware finish, appearance, interface & vault data",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =========================================================================
            // SECTION A: CARD HARDWARE VARIANT
            // =========================================================================
            item {
                SectionHeader(
                    icon = Icons.Default.CreditCard,
                    title = "SECTION A // CARD HARDWARE FINISH",
                    subtitle = "Selectable ID-1 authentic banking card physical material & finish"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardHardwareVariant.all.forEach { variant ->
                        val isSelected = variant.id == currentHardware.id
                        CardHardwarePill(
                            variant = variant,
                            isSelected = isSelected,
                            onSelect = {
                                themeManager.setCardHardwareVariant(variant)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION B: APPEARANCE STUDIO & BRAND GRADIENTS
            // =========================================================================
            item {
                SectionHeader(
                    icon = Icons.Default.Palette,
                    title = "SECTION B // APPEARANCE STUDIO",
                    subtitle = "High-contrast dynamic brand gradients (Financial semantics remain frozen)"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultThemePresets.all.forEach { preset ->
                        val isSelected = preset.id == gradient.id
                        SettingsThemePill(
                            preset = preset,
                            isSelected = isSelected,
                            onSelect = {
                                themeManager.setGradientPreset(preset)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Text(
                            text = "LAUNCH ENGINE (APPLY THEME)",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION C: INTERFACE & PERFORMANCE TOGGLES
            // =========================================================================
            item {
                SectionHeader(
                    icon = Icons.Default.Tune,
                    title = "SECTION C // INTERFACE & PERFORMANCE TOGGLES",
                    subtitle = "Live overlays, accessibility, and high-frequency UI components"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SettingsSurface),
                    border = BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Toggle 1: Privacy Mask
                        ToggleRow(
                            title = "Privacy Mask Mode",
                            subtitle = "Obscures sensitive rupee numbers on all cards (₹ ••••••)",
                            isChecked = uiState.isPrivacyMasked,
                            onCheckedChange = { portfolioViewModel.togglePrivacyMask() },
                            tint = gradient.startColor
                        )

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // Toggle 2: Floating Action Button
                        ToggleRow(
                            title = "Floating Action Button (FAB)",
                            subtitle = "Quick-access '+ Add Scheme' button on workspace screens",
                            isChecked = uiState.isFloatingFabEnabled,
                            onCheckedChange = { portfolioViewModel.setFloatingFabEnabled(it) },
                            tint = gradient.startColor
                        )

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // Toggle 3: Market Status & Holiday Ticker
                        ToggleRow(
                            title = "Market Status & Holiday Ticker",
                            subtitle = "Live NSE/BSE trading status pill & scheduled holiday alert",
                            isChecked = uiState.isMarketStatusTickerEnabled,
                            onCheckedChange = { portfolioViewModel.setMarketStatusTickerEnabled(it) },
                            tint = gradient.startColor
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION D: DATA & STORAGE MANAGEMENT
            // =========================================================================
            item {
                SectionHeader(
                    icon = Icons.Default.Settings,
                    title = "SECTION D // DATA & STORAGE MANAGEMENT",
                    subtitle = "Broker spreadsheet import, institutional audit reports, and vault cache"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SettingsSurface),
                    border = BorderStroke(1.dp, WsSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. Launch Universal Data Suite (Primary Action)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showUniversalDataDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .bounceClick()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LAUNCH IMPORT & EXPORT SUITE",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // 2. Import Broker Spreadsheet (.xlsx / .csv)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import Broker Spreadsheet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = "Ingest CAS / broker Excel (.xlsx) or CSV holding files",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    universalInitialTabIndex = 0
                                    showUniversalDataDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // 3. Generate Executive Audit Reports (PDF & Multi-Tab XLSX)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Generate Audit Reports",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = "Executive multi-page A4 PDF audit or multi-tab Excel",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    universalInitialTabIndex = 1
                                    showUniversalDataDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // 4. Draft Reset Action (if present)
                        if (uiState.cachedReconciliationDraft != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reconciliation Draft Active",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WsSlateTextPrimary
                                    )
                                    Text(
                                        text = "${uiState.cachedReconciliationDraft?.size ?: 0} pending schemes in draft memory",
                                        fontSize = 11.sp,
                                        color = WsSlateTextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { portfolioViewModel.discardReconciliationDraft() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WsCrimsonNegative)
                                ) {
                                    Text("Discard Draft", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))
                        }

                        // 5. Export Portfolio CSV
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Portfolio CSV",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsSlateTextPrimary
                                )
                                Text(
                                    text = "Copies full portfolio CSV data directly to clipboard",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val csv = portfolioViewModel.exportPortfolioCsv()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Portfolio CSV", csv))
                                    Toast.makeText(context, "Copied portfolio CSV to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = WsSlateBorder.copy(alpha = 0.6f))

                        // 6. Clear Active Vault
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Active Vault",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WsCrimsonNegative
                                )
                                Text(
                                    text = "Clears all positions from active vault JSON storage",
                                    fontSize = 11.sp,
                                    color = WsSlateTextSecondary
                                )
                            }
                            Button(
                                onClick = { showClearConfirmDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WsCrimsonNegative)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FINBABA FINANCIAL TERMINAL // V1.0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = WsSlateTextSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "High-Reasoning Pure Android Fintech Architecture",
                        fontSize = 10.sp,
                        color = WsSlateTextSecondary.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Reset Active Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all schemes from the active portfolio vault? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        portfolioViewModel.clearActivePortfolio()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = WsCrimsonNegative, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Universal Data & Audit Management Hub (Import & Export Engine)
    if (showUniversalDataDialog) {
        val currentPortfolio = uiState.portfolioData ?: PortfolioData(
            portfolioName = "Primary Vault",
            ownerName = "Investor",
            lastSyncedEpochMs = System.currentTimeMillis(),
            schemes = emptyList()
        )
        val currentAnalytics = uiState.analytics ?: PortfolioAnalyticsSummary(
            totalInvested = 0.0,
            totalCurrentValue = 0.0,
            totalGain = 0.0,
            totalReturnPercent = 0.0,
            weightedExpenseRatio = 0.0,
            annualFeeDragRupees = 0.0,
            assetClassBreakdown = emptyList(),
            marketCapBreakdown = emptyList(),
            topHoldings = emptyList(),
            sectorBreakdown = emptyList(),
            highOverlapStocksCount = 0
        )
        UniversalDataManagementDialog(
            portfolioData = currentPortfolio,
            analytics = currentAnalytics,
            healthScore = 85,
            initialTabIndex = universalInitialTabIndex,
            onDismissRequest = { showUniversalDataDialog = false },
            onImportDocumentUri = { uri ->
                val filename = uri.lastPathSegment ?: "statement.xlsx"
                portfolioViewModel.processSpreadsheetImport(uri, context.contentResolver, filename)
                showUniversalDataDialog = false
            },
            onImportRawCsvText = { rawText ->
                portfolioViewModel.importPortfolioFromCsv(rawText)
                showUniversalDataDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalVaultGradient.current.startColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                color = WsSlateTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = WsSlateTextSecondary
            )
        }
    }
}

@Composable
private fun CardHardwarePill(
    variant: CardHardwareVariant,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) variant.accentColor else WsSlateBorder,
        animationSpec = tween(200),
        label = "hardwareBorder"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SettingsSurface else Color(0xFFF1F5F9).copy(alpha = 0.6f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable(onClick = onSelect)
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = variant.startColor.copy(alpha = 0.3f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(variant.brush)
                        .border(1.dp, variant.accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = variant.displayName,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = variant.subtitle,
                        fontSize = 10.5.sp,
                        color = WsSlateTextSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = variant.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, WsSlateBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SettingsThemePill(
    preset: VaultGradient,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preset.startColor else WsSlateBorder,
        animationSpec = tween(200),
        label = "themeBorder"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SettingsSurface else Color(0xFFF1F5F9).copy(alpha = 0.6f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable(onClick = onSelect)
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = preset.startColor.copy(alpha = 0.25f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(preset.startColor, preset.endColor))
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = preset.displayName,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = WsSlateTextPrimary
                    )
                    Text(
                        text = if (preset.id == "cobalt_sky") "Default Studio Palette" else "Dynamic Brand Palette",
                        fontSize = 10.5.sp,
                        color = WsSlateTextSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = preset.startColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, WsSlateBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = WsSlateTextSecondary
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = tint)
        )
    }
}
