package com.fin.mf.ui.workspace.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.MutualFundScheme
import com.fin.mf.data.engine.AmfiFuzzyReconciler
import com.fin.mf.data.engine.HoldingsEngine
import com.fin.mf.data.engine.MatchConfidence
import com.fin.mf.data.engine.ReconciledSchemeItem
import java.util.Locale
import kotlin.math.roundToInt

// =============================================================================
// DESIGN TOKENS & INTERACTION MODIFIERS
// =============================================================================

val WsSlateTextPrimary = Color(0xFF0F172A)
val WsSlateTextSecondary = Color(0xFF64748B)
val WsSlateBorder = Color(0xFFE2E8F0)
private val WsSurfaceLight = Color(0xFFF8FAFC)

fun Modifier.bounceClick(scaleDown: Float = 0.97f, onClick: (() -> Unit)? = null): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "bounceClickScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
        .pointerInput(Unit) {
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

// =============================================================================
// DOMAIN MODELS FOR VAULT COMMIT
// =============================================================================

enum class VaultImportMode {
    MERGE_WITH_ACTIVE_VAULT,
    REPLACE_ENTIRE_VAULT
}

// =============================================================================
// RECONCILIATION PREVIEW MODAL BOTTOM SHEET
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationPreviewSheet(
    initialItems: List<ReconciledSchemeItem>,
    amfiDictionary: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onCommitToVault: (schemes: List<MutualFundScheme>, mode: VaultImportMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableItems = remember(initialItems) {
        mutableStateListOf<ReconciledSchemeItem>().apply { addAll(initialItems) }
    }

    var importMode by remember { mutableStateOf(VaultImportMode.MERGE_WITH_ACTIVE_VAULT) }
    var activePickerIndex by remember { mutableStateOf<Int?>(null) }

    val matchedCount = editableItems.count { it.matchConfidence == MatchConfidence.AUTO_MATCHED }
    val reviewCount = editableItems.size - matchedCount
    val validCommitItems = editableItems.filter {
        it.matchedSchemeCode != null &&
            !it.matchedSchemeName.isNullOrBlank() &&
            it.units > 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Header Banner with LocalVaultGradient
            ReconciliationHeader(
                matchedCount = matchedCount,
                reviewCount = reviewCount,
                totalCount = editableItems.size,
                onClose = onDismiss
            )

            // 2. Import Mode Selector ("Merge with Active Vault" vs "Replace Entire Vault")
            ImportModeSelector(
                selectedMode = importMode,
                onModeSelected = { importMode = it }
            )

            HorizontalDivider(color = WsSlateBorder, thickness = 1.dp)

            // 3. Scrollable Reconciled Records List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(WsSurfaceLight),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = editableItems,
                    key = { idx, item -> "${idx}_${item.originalRawName}" }
                ) { index, item ->
                    ReconciledSchemeCard(
                        item = item,
                        onOpenSchemePicker = { activePickerIndex = index },
                        onSelectSuggestion = { code, name ->
                            editableItems[index] = item.copy(
                                matchedSchemeCode = code,
                                matchedSchemeName = name,
                                category = AmfiFuzzyReconciler.inferCategory(name),
                                matchConfidence = MatchConfidence.AUTO_MATCHED,
                                similarityScore = 1.0f
                            )
                        },
                        onUpdateUnitsAndCost = { newUnits, newInvested ->
                            editableItems[index] = item.copy(
                                units = newUnits,
                                investedAmount = newInvested
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = WsSlateBorder, thickness = 1.dp)

            // 4. Bottom Action Bar
            BottomCommitBar(
                validCount = validCommitItems.size,
                totalInvested = validCommitItems.sumOf { it.investedAmount },
                onCancel = onDismiss,
                onCommit = {
                    val convertedSchemes = validCommitItems.map { reconciled ->
                        val schemeName = reconciled.matchedSchemeName.orEmpty()
                        val safeUnits = reconciled.units.coerceAtLeast(0.0001)
                        val avgNav = if (reconciled.investedAmount > 0.0) {
                            reconciled.investedAmount / safeUnits
                        } else {
                            10.0
                        }
                        val code = reconciled.matchedSchemeCode ?: 0
                        val (inferredCat, inferredAssetClass) = HoldingsEngine.inferCategoryAndAssetClass(schemeName)
                        val category = if (reconciled.category.isNotBlank() && reconciled.category != "Uncategorized") {
                            reconciled.category
                        } else {
                            inferredCat
                        }
                        MutualFundScheme(
                            schemeCode = code,
                            schemeName = schemeName,
                            amc = HoldingsEngine.inferAmcName(schemeName),
                            category = category,
                            assetClass = inferredAssetClass,
                            riskLevel = HoldingsEngine.inferRiskLevel(category, inferredAssetClass),
                            expenseRatio = HoldingsEngine.inferExpenseRatio(category, isDirect = schemeName.contains("direct", ignoreCase = true)),
                            exitLoad = "1.0% within 365 days",
                            benchmark = HoldingsEngine.inferBenchmark(category),
                            units = reconciled.units,
                            investedAmount = reconciled.investedAmount,
                            fallbackNav = avgNav,
                            lastNavDate = "Live",
                            holdings = HoldingsEngine.generateHoldings(code, schemeName, category, inferredAssetClass),
                            dayChange = 0.0
                        )
                    }
                    onCommitToVault(convertedSchemes, importMode)
                }
            )
        }
    }

    // 5. Manual AMFI Scheme Search Dialog
    val targetIndex = activePickerIndex
    if (targetIndex != null && targetIndex in editableItems.indices) {
        val currentItem = editableItems[targetIndex]
        AmfiSchemeSearchDialog(
            initialItem = currentItem,
            amfiDictionary = amfiDictionary,
            onDismiss = { activePickerIndex = null },
            onSchemeSelected = { selectedCode, selectedName ->
                editableItems[targetIndex] = currentItem.copy(
                    matchedSchemeCode = selectedCode,
                    matchedSchemeName = selectedName,
                    category = AmfiFuzzyReconciler.inferCategory(selectedName),
                    matchConfidence = MatchConfidence.AUTO_MATCHED,
                    similarityScore = 1.0f
                )
                activePickerIndex = null
            }
        )
    }
}

@Composable
private fun ReconciliationHeader(
    matchedCount: Int,
    reviewCount: Int,
    totalCount: Int,
    onClose: () -> Unit
) {
    val vaultGradient = LocalVaultGradient.current.brush

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(vaultGradient)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portfolio Import Reconciliation",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Review and verify matched AMFI schemes ($totalCount total)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$matchedCount Auto-Matched",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                if (reviewCount > 0) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD600))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$reviewCount Needs Review",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportModeSelector(
    selectedMode: VaultImportMode,
    onModeSelected: (VaultImportMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "VAULT MERGE STRATEGY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = WsSlateTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WsSurfaceLight)
                .border(1.dp, WsSlateBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ImportModePill(
                title = "Merge with Active Vault",
                selected = selectedMode == VaultImportMode.MERGE_WITH_ACTIVE_VAULT,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(VaultImportMode.MERGE_WITH_ACTIVE_VAULT) }
            )
            ImportModePill(
                title = "Replace Entire Vault",
                selected = selectedMode == VaultImportMode.REPLACE_ENTIRE_VAULT,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(VaultImportMode.REPLACE_ENTIRE_VAULT) }
            )
        }
    }
}

@Composable
private fun ImportModePill(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) WsSlateTextPrimary else Color.Transparent,
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else WsSlateTextSecondary,
        label = "pillText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .bounceClick(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReconciledSchemeCard(
    item: ReconciledSchemeItem,
    onOpenSchemePicker: () -> Unit,
    onSelectSuggestion: (Int, String) -> Unit,
    onUpdateUnitsAndCost: (Double, Double) -> Unit
) {
    var unitsText by remember(item.units) {
        mutableStateOf(formatDecimalInput(item.units))
    }
    var investedText by remember(item.investedAmount) {
        mutableStateOf(formatDecimalInput(item.investedAmount))
    }

    val (badgeLabel, badgeColor, badgeIcon) = when (item.matchConfidence) {
        MatchConfidence.AUTO_MATCHED -> Triple(
            "MATCHED (${(item.similarityScore * 100).roundToInt()}%)",
            SemanticFinanceColors.ProfitGreen,
            Icons.Default.CheckCircle
        )
        MatchConfidence.AMBIGUOUS -> Triple(
            "REVIEW REQUIRED (${(item.similarityScore * 100).roundToInt()}%)",
            SemanticFinanceColors.BenchmarkAmber,
            Icons.Default.WarningAmber
        )
        MatchConfidence.UNMATCHED -> Triple(
            "MANUAL SEARCH REQUIRED",
            SemanticFinanceColors.LossRed,
            Icons.Default.ErrorOutline
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.matchConfidence == MatchConfidence.UNMATCHED) {
                SemanticFinanceColors.LossRed.copy(alpha = 0.45f)
            } else if (item.matchConfidence == MatchConfidence.AMBIGUOUS) {
                SemanticFinanceColors.BenchmarkAmber.copy(alpha = 0.35f)
            } else {
                WsSlateBorder
            }
        ),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Confidence Tag & AMFI Code Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeColor
                        )
                    }
                }

                Surface(
                    color = WsSurfaceLight,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, WsSlateBorder)
                ) {
                    Text(
                        text = if (item.matchedSchemeCode != null) {
                            "AMFI #${item.matchedSchemeCode}"
                        } else {
                            "Unlinked Code"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = WsSlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original Broker Text
            Text(
                text = "STATEMENT TEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WsSlateTextSecondary
            )
            Text(
                text = item.originalRawName.ifBlank { "Unnamed Scheme Row" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = WsSlateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tappable Matched AMFI Scheme Selector Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (item.matchedSchemeName != null) WsSurfaceLight else SemanticFinanceColors.LossRed.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, if (item.matchedSchemeName != null) WsSlateBorder else SemanticFinanceColors.LossRed.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = onOpenSchemePicker)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MATCHED AMFI SCHEME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WsSlateTextSecondary
                            )
                            if (item.category.isNotBlank() && item.category != "Uncategorized") {
                                Surface(
                                    color = SemanticFinanceColors.ProfitGreen.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = SemanticFinanceColors.ProfitGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.matchedSchemeName ?: "Tap to search & link AMFI scheme...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (item.matchedSchemeName != null) {
                                WsSlateTextPrimary
                            } else {
                                SemanticFinanceColors.LossRed
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Change AMFI Scheme",
                        tint = WsSlateTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Suggestions Chips (if ambiguous and has top candidates)
            if (item.matchConfidence != MatchConfidence.AUTO_MATCHED && item.candidateSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "QUICK SUGGESTIONS:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = WsSlateTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.candidateSuggestions.take(3).forEach { (code, name) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, WsSlateBorder),
                            modifier = Modifier.bounceClick { onSelectSuggestion(code, name) }
                        ) {
                            Text(
                                text = name.take(34) + if (name.length > 34) "…" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = WsSlateTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editable Units & Invested Cost Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { updated ->
                        unitsText = updated
                        val parsedUnits = updated.toDoubleOrNull() ?: 0.0
                        val parsedCost = investedText.toDoubleOrNull() ?: item.investedAmount
                        onUpdateUnitsAndCost(parsedUnits, parsedCost)
                    },
                    label = { Text("Units") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = WsSlateTextSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WsSlateTextPrimary,
                        unfocusedBorderColor = WsSlateBorder,
                        focusedTextColor = WsSlateTextPrimary,
                        unfocusedTextColor = WsSlateTextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = investedText,
                    onValueChange = { updated ->
                        investedText = updated
                        val parsedCost = updated.toDoubleOrNull() ?: 0.0
                        val parsedUnits = unitsText.toDoubleOrNull() ?: item.units
                        onUpdateUnitsAndCost(parsedUnits, parsedCost)
                    },
                    label = { Text("Invested Cost (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = WsSlateTextSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WsSlateTextPrimary,
                        unfocusedBorderColor = WsSlateBorder,
                        focusedTextColor = WsSlateTextPrimary,
                        unfocusedTextColor = WsSlateTextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomCommitBar(
    validCount: Int,
    totalInvested: Double,
    onCancel: () -> Unit,
    onCommit: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WsSlateBorder),
                modifier = Modifier
                    .weight(0.32f)
                    .height(50.dp)
                    .bounceClick()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = WsSlateTextPrimary
                )
            }

            Button(
                onClick = onCommit,
                enabled = validCount > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SemanticFinanceColors.ProfitGreen,
                    disabledContainerColor = WsSlateBorder
                ),
                modifier = Modifier
                    .weight(0.68f)
                    .height(50.dp)
                    .bounceClick()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Commit to Vault ($validCount Schemes)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (totalInvested > 0.0) {
                        Text(
                            text = "Total: ₹${String.format(Locale.US, "%,.0f", totalInvested)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// QUICK SEARCH DIALOG FOR MANUAL AMFI SCHEME SELECTION
// =============================================================================

@Composable
private fun AmfiSchemeSearchDialog(
    initialItem: ReconciledSchemeItem,
    amfiDictionary: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSchemeSelected: (Int, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSchemes = remember(searchQuery, initialItem, amfiDictionary) {
        if (searchQuery.isBlank()) {
            val suggestedCodes = initialItem.candidateSuggestions.map { it.first }.toSet()
            val remainder = amfiDictionary
                .asSequence()
                .filter { it.first !in suggestedCodes }
                .take(40)
                .toList()
            initialItem.candidateSuggestions + remainder
        } else {
            val queryTokens = searchQuery
                .trim()
                .lowercase(Locale.ROOT)
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }

            amfiDictionary.asSequence()
                .filter { (code, name) ->
                    val lowerName = name.lowercase(Locale.ROOT)
                    val codeStr = code.toString()
                    queryTokens.all { token ->
                        lowerName.contains(token) || codeStr.contains(token)
                    }
                }
                .take(60)
                .toList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Authentic AMFI Scheme",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WsSlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Raw: ${initialItem.originalRawName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WsSlateTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WsSlateTextSecondary
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search scheme name or 6-digit AMFI code...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = WsSlateTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = WsSlateTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WsSlateTextPrimary,
                        unfocusedBorderColor = WsSlateBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = WsSlateBorder)

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchQuery.isBlank() && initialItem.candidateSuggestions.isNotEmpty()) {
                        item {
                            Text(
                                text = "TOP FUZZY MATCHES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SemanticFinanceColors.ProfitGreen,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    itemsIndexed(
                        items = filteredSchemes,
                        key = { idx, pair -> "${pair.first}_$idx" }
                    ) { _, (code, name) ->
                        val isSuggested = initialItem.candidateSuggestions.any { it.first == code }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSuggested && searchQuery.isBlank()) {
                                SemanticFinanceColors.ProfitGreen.copy(alpha = 0.06f)
                            } else {
                                WsSurfaceLight
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSuggested && searchQuery.isBlank()) {
                                    SemanticFinanceColors.ProfitGreen.copy(alpha = 0.35f)
                                } else {
                                    WsSlateBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { onSchemeSelected(code, name) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = WsSlateTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "AMFI Code: $code • ${AmfiFuzzyReconciler.inferCategory(name)}",
                                        style = MaterialTheme.typography.labelSmall,
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

// =============================================================================
// FORMATTING & DOMAIN EXTRACTION HELPERS
// =============================================================================

private fun formatDecimalInput(value: Double): String {
    if (value == 0.0) return "0"
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) {
        asLong.toString()
    } else {
        "%.4f".format(value).trimEnd('0').trimEnd('.')
    }
}
