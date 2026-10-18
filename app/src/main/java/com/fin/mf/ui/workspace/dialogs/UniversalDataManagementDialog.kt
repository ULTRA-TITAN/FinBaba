package com.fin.mf.ui.workspace.dialogs

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.core.export.MediaStoreExportManager
import com.fin.mf.data.PortfolioAnalyticsSummary
import com.fin.mf.data.PortfolioData
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.pdf.PortfolioAuditPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================================
// THEME PALETTE & SPRING BOUNCE MODIFIER
// ============================================================================

private val DeepSlate = Color(0xFF0F172A)
private val SlateSurface = Color(0xFF1E293B)
private val PrimaryNavy = Color(0xFF1E3A8A)
private val AccentBlue = Color(0xFF2563EB)
private val ProfitEmerald = Color(0xFF059669)
private val LossCrimson = Color(0xFFDC2626)
private val MutedSlate = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)
private val SoftCardBg = Color(0xFFF8FAFC)

/**
 * Interactive tactile spring-scaling modifier (`.bounceClick()`) that smoothly
 * compresses to [scaleDown] on press and rebounds with a low-damping spring.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BounceClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

// ============================================================================
// UNIVERSAL DATA MANAGEMENT DIALOG (IMPORT & EXPORT ENGINE UI)
// ============================================================================

/**
 * Modern Jetpack Compose Material 3 dialog replacing the legacy `CsvManagerDialog`.
 *
 * Provides a unified 2-tab hub:
 * - **Tab 1 (Import Data)**: Broker Spreadsheet picker (`.xlsx` / `.csv` via `OpenDocument`)
 *   and a "Paste Raw CSV" fallback editor.
 * - **Tab 2 (Export Reports)**:
 *   1. Multi-Tab Institutional Excel (`.xlsx`)
 *   2. Executive A4 Audit PDF Report (`.pdf`)
 *   3. Standard Portfolio CSV (`.csv`)
 */
@Composable
fun UniversalDataManagementDialog(
    portfolioData: PortfolioData,
    analytics: PortfolioAnalyticsSummary,
    healthScore: Int = 82,
    initialTabIndex: Int = 0,
    onDismissRequest: () -> Unit,
    onImportDocumentUri: (Uri) -> Unit = {},
    onImportRawCsvText: (String) -> Unit = {},
    exportManager: MediaStoreExportManager = remember { MediaStoreExportManager() },
    pdfGenerator: PortfolioAuditPdfGenerator = remember { PortfolioAuditPdfGenerator(exportManager) }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex.coerceIn(0, 1)) }
    var rawCsvInput by rememberSaveable { mutableStateOf("") }
    var showRawCsvSection by rememberSaveable { mutableStateOf(false) }
    var selectedFileBadge by rememberSaveable { mutableStateOf<String?>(null) }
    var activeExportTask by remember { mutableStateOf<ExportTaskType?>(null) }

    // Android Storage Access Framework Document Picker for .xlsx and .csv
    val spreadsheetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Non-persistable URI provider; transient read permission is already granted
            }
            selectedFileBadge = uri.lastPathSegment ?: "Broker_Statement"
            onImportDocumentUri(uri)
            Toast.makeText(
                context,
                "Importing spreadsheet: ${selectedFileBadge.orEmpty()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(
        onDismissRequest = {
            if (activeExportTask == null) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = activeExportTask == null,
            dismissOnClickOutside = activeExportTask == null
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Institutional Gradient Dialog Banner
                DialogHeaderBanner(
                    vaultName = portfolioData.portfolioName,
                    schemeCount = portfolioData.schemes.size,
                    stockCount = analytics.topHoldings.size,
                    onClose = {
                        if (activeExportTask == null) onDismissRequest()
                    }
                )

                // Top TabRow (Import Data | Export Reports)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SoftCardBg,
                    contentColor = PrimaryNavy,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 3.dp,
                                color = PrimaryNavy
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Import Data",
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Export Reports",
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    )
                }

                // Animated Tab Body
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "UniversalDataDialogTabBody"
                ) { tabIdx ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (tabIdx == 0) {
                            ImportDataTabContent(
                                context = context,
                                selectedFileBadge = selectedFileBadge,
                                showRawCsvSection = showRawCsvSection,
                                rawCsvInput = rawCsvInput,
                                onToggleRawCsvSection = { showRawCsvSection = !showRawCsvSection },
                                onRawCsvChange = { rawCsvInput = it },
                                onPickBrokerSpreadsheet = {
                                    spreadsheetPickerLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/vnd.ms-excel",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "application/csv",
                                            "text/plain"
                                        )
                                    )
                                },
                                onSubmitRawCsv = {
                                    val trimmed = rawCsvInput.trim()
                                    if (trimmed.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Please paste valid CSV lines before importing.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onImportRawCsvText(trimmed)
                                        Toast.makeText(
                                            context,
                                            "Raw CSV queued for portfolio ingestion.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismissRequest()
                                    }
                                }
                            )
                        } else {
                            ExportReportsTabContent(
                                portfolioData = portfolioData,
                                analytics = analytics,
                                activeExportTask = activeExportTask,
                                onExportExcel = {
                                    if (activeExportTask != null) return@ExportReportsTabContent
                                    activeExportTask = ExportTaskType.EXCEL
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            exportManager.exportExcelFile(
                                                context = context,
                                                portfolio = portfolioData,
                                                analytics = analytics
                                            )
                                        }
                                        activeExportTask = null
                                        result.onSuccess { uri ->
                                            Toast.makeText(
                                                context,
                                                "Saved 4-Tab Institutional Excel (.xlsx) to Downloads/FinBaba",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            runCatching {
                                                exportManager.dispatchShareOrOpenChooser(
                                                    context = context,
                                                    exportedUri = uri,
                                                    mimeType = MediaStoreExportManager.MIME_TYPE_EXCEL,
                                                    chooserTitle = "Open or Share Institutional Excel (.xlsx)"
                                                )
                                            }
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                "Excel Export Failed: ${err.localizedMessage ?: "Storage error"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onExportPdf = {
                                    if (activeExportTask != null) return@ExportReportsTabContent
                                    activeExportTask = ExportTaskType.PDF
                                    pdfGenerator.generateAndSharePdf(
                                        context = context,
                                        portfolioData = portfolioData,
                                        analytics = analytics,
                                        healthScore = healthScore,
                                        onSuccess = { _, _ ->
                                            activeExportTask = null
                                            Toast.makeText(
                                                context,
                                                "Executive 2-Page A4 Audit PDF saved to Downloads & ready to share!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        onError = { err ->
                                            activeExportTask = null
                                            Toast.makeText(
                                                context,
                                                "PDF Audit Failed: ${err.localizedMessage ?: "Render error"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                },
                                onExportCsv = {
                                    if (activeExportTask != null) return@ExportReportsTabContent
                                    activeExportTask = ExportTaskType.CSV
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            exportManager.exportCsvFile(
                                                context = context,
                                                portfolio = portfolioData
                                            )
                                        }
                                        activeExportTask = null
                                        result.onSuccess { uri ->
                                            Toast.makeText(
                                                context,
                                                "Portfolio CSV saved to Downloads/FinBaba",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            runCatching {
                                                exportManager.dispatchShareOrOpenChooser(
                                                    context = context,
                                                    exportedUri = uri,
                                                    mimeType = MediaStoreExportManager.MIME_TYPE_CSV,
                                                    chooserTitle = "Open or Share Portfolio CSV"
                                                )
                                            }
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                "CSV Export Failed: ${err.localizedMessage ?: "Storage error"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderGray, thickness = 1.dp)

                // Footer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftCardBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scoped Storage • Zero External Dependencies",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = {
                            if (activeExportTask == null) onDismissRequest()
                        },
                        enabled = activeExportTask == null
                    ) {
                        Text(
                            text = "Close",
                            color = DeepSlate,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private enum class ExportTaskType {
    EXCEL,
    PDF,
    CSV
}

// ============================================================================
// DIALOG HEADER BANNER
// ============================================================================

@Composable
private fun DialogHeaderBanner(
    vaultName: String,
    schemeCount: Int,
    stockCount: Int,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = LocalVaultGradient.current.horizontalBrush
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UNIVERSAL DATA & AUDIT ENGINE",
                    color = Color(0xFF93C5FD),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vaultName.ifBlank { "Primary Institutional Vault" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$schemeCount Active Funds  •  $stockCount Look-Through Equities",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Dialog",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================================================
// TAB 1: IMPORT DATA CONTENT
// ============================================================================

@Composable
private fun ImportDataTabContent(
    context: Context,
    selectedFileBadge: String?,
    showRawCsvSection: Boolean,
    rawCsvInput: String,
    onToggleRawCsvSection: () -> Unit,
    onRawCsvChange: (String) -> Unit,
    onPickBrokerSpreadsheet: () -> Unit,
    onSubmitRawCsv: () -> Unit
) {
    // Primary Broker Spreadsheet Import Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onPickBrokerSpreadsheet)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF),
                        Color(0xFFDBEAFE)
                    )
                )
            )
            .border(1.5.dp, AccentBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Broker Spreadsheet (.xlsx / .csv)",
                        color = DeepSlate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supports Zerodha Coin, Groww, Kuvera, CAMS/KFintech & FinBaba backups",
                        color = MutedSlate,
                        fontSize = 12.sp
                    )
                }
            }

            if (selectedFileBadge != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Selected: $selectedFileBadge",
                        color = ProfitEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onPickBrokerSpreadsheet,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNavy,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select .XLSX or .CSV File from Device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Fallback Toggle Card: Paste Raw CSV
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRawCsvSection() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = PrimaryNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Paste Raw CSV Text (Fallback)",
                            color = DeepSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Directly paste comma-separated scheme rows",
                            color = MutedSlate,
                            fontSize = 11.5.sp
                        )
                    }
                }

                TextButton(onClick = onToggleRawCsvSection) {
                    Text(
                        text = if (showRawCsvSection) "Hide" else "Expand",
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (showRawCsvSection) {
                OutlinedTextField(
                    value = rawCsvInput,
                    onValueChange = onRawCsvChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    placeholder = {
                        Text(
                            text = "Scheme Code,Scheme Name,Units,Avg Buy NAV\n120503,Axis ELSS Tax Saver Direct Growth,1450.25,64.12",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MutedSlate
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = DeepSlate
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryNavy,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (!clipText.isNullOrBlank()) {
                                onRawCsvChange(clipText)
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = DeepSlate
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Clipboard", color = DeepSlate, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSubmitRawCsv,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfitEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Parse & Import CSV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: EXPORT REPORTS CONTENT (3 INSTITUTIONAL CARDS)
// ============================================================================

@Composable
private fun ExportReportsTabContent(
    portfolioData: PortfolioData,
    analytics: PortfolioAnalyticsSummary,
    activeExportTask: ExportTaskType?,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    // Card 1: Multi-Tab Institutional Excel (.xlsx)
    ExportReportCard(
        title = "Multi-Tab Institutional Excel (.xlsx)",
        badgeText = "4 WORKSHEETS • UNCAPPED",
        description = "Full ${analytics.topHoldings.size}+ underlying stock matrix, Executive Overview, GICS Sector Split, and SEBI Mandate Rebalancing tabs with ₹ formatting.",
        icon = Icons.Default.TableChart,
        gradientColors = listOf(Color(0xFF065F46), Color(0xFF059669)),
        tintBorderColor = Color(0xFF10B981).copy(alpha = 0.35f),
        actionLabel = "Download 4-Tab Excel (.xlsx)",
        isLoading = activeExportTask == ExportTaskType.EXCEL,
        enabled = activeExportTask == null,
        onClick = onExportExcel
    )

    // Card 2: Executive A4 Audit PDF Report
    ExportReportCard(
        title = "Executive A4 Audit PDF Report",
        badgeText = "2-PAGE PRINTABLE A4",
        description = "Institutional health scorecard, core valuation telemetry, SEBI drift badges, Top 8 sector bar gauges, duplicate stock overlap matrix & instant share sheet.",
        icon = Icons.Default.PictureAsPdf,
        gradientColors = listOf(DeepSlate, PrimaryNavy),
        tintBorderColor = AccentBlue.copy(alpha = 0.35f),
        actionLabel = "Generate & Share Audit PDF",
        isLoading = activeExportTask == ExportTaskType.PDF,
        enabled = activeExportTask == null,
        onClick = onExportPdf
    )

    // Card 3: Standard Portfolio CSV
    ExportReportCard(
        title = "Standard Portfolio CSV",
        badgeText = "UTF-8 BOM • UNIVERSAL",
        description = "Clean RFC-4180 flat snapshot of all ${portfolioData.schemes.size} active mutual fund schemes, units, buy/current NAVs, TER, and asset splits saved to Downloads.",
        icon = Icons.Default.Description,
        gradientColors = listOf(Color(0xFF334155), Color(0xFF475569)),
        tintBorderColor = BorderGray,
        actionLabel = "Download Standard CSV",
        isLoading = activeExportTask == ExportTaskType.CSV,
        enabled = activeExportTask == null,
        onClick = onExportCsv
    )
}

@Composable
private fun ExportReportCard(
    title: String,
    badgeText: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    tintBorderColor: Color,
    actionLabel: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCardBg)
            .border(1.2.dp, tintBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = badgeText,
                            color = gradientColors.last(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        color = DeepSlate,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MutedSlate,
                        fontSize = 11.8.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gradientColors.first(),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating Artifact…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}