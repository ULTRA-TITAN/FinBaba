package com.fin.mf.core.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetSlatePrimary = Color(0xFF0F172A)
private val SheetSlateSecondary = Color(0xFF475569)
private val SheetSlateBorder = Color(0xFFE2E8F0)
private val FrozenEmeraldProfit = Color(0xFF059669)
private val FrozenEmeraldSoft = Color(0xFFECFDF5)

/**
 * Dedicated Appearance Studio ModalBottomSheet:
 * Allows user to live-select high-contrast brand gradients with instant reactive
 * updates and persistent SharedPreferences storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsSheet(
    themeManager: VaultThemeManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeGradient = LocalVaultGradient.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Sheet Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = activeGradient.startColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = activeGradient.startColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Studio Theme & Visual Identity",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SheetSlatePrimary
                        )
                        Text(
                            text = "Customizes cards, buttons, and system accents. Financial charts and data semantics remain standardized.",
                            fontSize = 11.sp,
                            color = SheetSlateSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SheetSlateSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Interactive Live Preview Card
            Text(
                text = "LIVE STUDIO PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SheetSlateSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LiveThemePreviewCard(gradient = activeGradient)

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = SheetSlateBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Palette Presets Grid / List
            Text(
                text = "CURATED GRADIENT PALETTES",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SheetSlateSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VaultThemePresets.all, key = { it.id }) { preset ->
                    val isSelected = preset.id == activeGradient.id
                    ThemePresetPill(
                        preset = preset,
                        isSelected = isSelected,
                        onSelect = { themeManager.setGradientPreset(preset) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SheetSlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Frozen Semantics Guarantee: Profit emeralds, loss crimsons, SEBI asset-cap proportions, and NIFTY 50 benchmark gold curves never shift across any theme.",
                            fontSize = 11.sp,
                            color = SheetSlateSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Live Preview Card demonstrating real-time gradient application.
 */
@Composable
private fun LiveThemePreviewCard(gradient: VaultGradient) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), spotColor = gradient.startColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, gradient.startColor.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .background(gradient.brush)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INSTITUTIONAL CORE • ${gradient.displayName.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Profit badge strictly frozen
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = FrozenEmeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = FrozenEmeraldProfit,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+14.25%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FrozenEmeraldProfit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Total Portfolio Valuation",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = "₹ 14,85,250",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Gain: +₹ 1,85,250",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399) // High contrast emerald
                    )

                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Launch Engine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-contrast preset selection pill displaying swatch, name, and checkmark.
 */
@Composable
private fun ThemePresetPill(
    preset: VaultGradient,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) preset.startColor else SheetSlateBorder,
        animationSpec = tween(220),
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = preset.startColor.copy(alpha = 0.25f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dual-color horizontal pill showing exact start and end colors side-by-side
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(preset.startColor, preset.endColor)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = preset.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = SheetSlatePrimary
                    )
                    Text(
                        text = if (preset.id == "cobalt_sky") "Default Institutional Palette" else "Dynamic Studio Brand Preset",
                        fontSize = 11.sp,
                        color = SheetSlateSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = preset.startColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SheetSlateBorder, CircleShape)
                )
            }
        }
    }
}
