package com.fin.mf.ui.workspace.components

import com.fin.mf.ui.components.bounceClick
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.CardHardwareVariant
import com.fin.mf.core.theme.LocalCardHardware
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.core.theme.SemanticFinanceColors
import com.fin.mf.data.AllocationSlice
import com.fin.mf.data.MutualFundScheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// Base neutral and semantic colors for workspace components
val WsSurfaceWhite = Color(0xFFFFFFFF)
val WsSlateBorder = Color(0xFFE2E8F0)
val WsSlateTextPrimary = Color(0xFF0F172A)
val WsSlateTextSecondary = Color(0xFF475569)
val WsPrimaryIndigo = Color(0xFF1E3A8A)
val WsAccentRoyal = Color(0xFF2563EB)
val WsEmeraldPositive = SemanticFinanceColors.ProfitGreen
val WsCrimsonNegative = SemanticFinanceColors.LossRed

// Monospace Financial Figures Styling (tnum)
const val WsFinancialFontFeature = "tnum"
val WsFinancialFigureStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum"
)
val WsMonospaceFigureStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum"
)

val TopLevelInrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

fun formatWsInr(amount: Double, isMasked: Boolean, showPlus: Boolean = false): String {
    if (isMasked) return "₹ ••••••"
    val formatted = TopLevelInrFormatter.format(amount)
    return if (showPlus && amount > 0) "+$formatted" else formatted
}

fun formatWsPercent(pct: Double): String {
    val rounded = (pct * 100.0).roundToInt() / 100.0
    val sign = if (rounded >= 0.0) "+" else ""
    return "$sign$rounded%"
}

fun wsSliceColor(index: Int): Color {
    val palette = listOf(
        Color(0xFF1E3A8A), // Indigo
        Color(0xFF2563EB), // Royal
        Color(0xFF059669), // Emerald
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED), // Purple
        Color(0xFFDC2626), // Crimson
        Color(0xFF0D9488), // Teal
        Color(0xFF64748B)  // Slate
    )
    return palette[index % palette.size]
}

/**
 * Compact Monospace Tag Pill for institutional fintech labeling.
 */
@Composable
fun WsTerminalTag(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = WsSlateTextSecondary,
    backgroundColor: Color = Color(0xFFF1F5F9),
    borderColor: Color = WsSlateBorder
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        )
    }
}

/**
 * Hardware Component 1: Tactile EMV Smart Chip (40.dp x 30.dp) with brass/gold sheen and micro-contact traces.
 */
@Composable
fun EmvSmartChip(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 30.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE5C07B),
                        Color(0xFFF3E5AB),
                        Color(0xFFD4AF37),
                        Color(0xFFC5A059)
                    )
                )
            )
            .border(1.dp, Color(0xFF996515).copy(alpha = 0.7f), RoundedCornerShape(5.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val traceColor = Color(0xFF6B4E1B).copy(alpha = 0.55f)
            val strokeW = 1.dp.toPx()

            // Horizontal center line
            drawLine(
                color = traceColor,
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = strokeW
            )
            // Left vertical branch
            drawLine(
                color = traceColor,
                start = Offset(w * 0.35f, 0f),
                end = Offset(w * 0.35f, h),
                strokeWidth = strokeW
            )
            // Right vertical branch
            drawLine(
                color = traceColor,
                start = Offset(w * 0.65f, 0f),
                end = Offset(w * 0.65f, h),
                strokeWidth = strokeW
            )
            // Central micro-pad ring
            drawRoundRect(
                color = traceColor,
                topLeft = Offset(w * 0.35f, h * 0.28f),
                size = Size(w * 0.3f, h * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = strokeW)
            )
        }
    }
}

/**
 * Hardware Component 2: 4 radiating contactless curved signal waves in semi-transparent silver.
 */
@Composable
fun ContactlessWaves(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.65f)
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeW = 1.6.dp.toPx()
        val centerX = 0f
        val centerY = size.height / 2f
        val radii = listOf(size.width * 0.35f, size.width * 0.55f, size.width * 0.75f, size.width * 0.95f)

        radii.forEach { r ->
            drawArc(
                color = tint,
                startAngle = -42f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(centerX - r, centerY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Bespoke Luxury Financial Hero Card:
 * Institutional ID-1 credit card proportions, allocation-free guilloche vector mesh,
 * specular highlight, EMV smart chip, contactless signal arcs, tabular monospaced numbers,
 * holographic return pill, and bottom cardholder metadata bar.
 */
/**
 * Hardware Component 3: Holographic Card Network Badge (Interlocking metallic rings).
 */
@Composable
fun HolographicCardNetworkBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 30.dp, height = 18.dp)) {
        val r = size.height * 0.45f
        val centerY = size.height / 2f
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.85f),
            radius = r,
            center = Offset(r + 1.dp.toPx(), centerY)
        )
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.80f),
            radius = r,
            center = Offset(size.width - r - 1.dp.toPx(), centerY)
        )
    }
}

/**
 * Formats total portfolio valuation as a 16-digit embossed number grouping:
 * e.g., ₹ 0450   8921   4000   .50 or ₹ ••••   ••••   ••••   •••• when masked.
 */
fun formatCardNumberClusters(value: Double, isMasked: Boolean): String {
    if (isMasked) return "₹ ••••   ••••   ••••   ••••"
    val integerPart = value.toLong().coerceAtLeast(0L)
    val fraction = ((value - integerPart) * 100).roundToInt().coerceIn(0, 99)
    val padded = String.format(Locale.US, "%012d", integerPart)
    return "₹ ${padded.substring(0, 4)}   ${padded.substring(4, 8)}   ${padded.substring(8, 12)}   .${String.format(Locale.US, "%02d", fraction)}"
}

/**
 * High-precision 1.5dp reading/scroll progress indicator pinned directly beneath the TopAppBar.
 * Smoothly interpolates the scroll fraction from [listState] with zero frame drops.
 */
@Composable
fun HairlineScrollProgressIndicator(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val gradient = LocalVaultGradient.current
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 1) 0f
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                val itemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                ((firstVisible + (offset.toFloat() / itemHeight.toFloat())) / (totalItems - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Color(0xFFE2E8F0))
    ) {
        if (scrollProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(scrollProgress)
                    .height(1.5.dp)
                    .background(gradient.brush)
            )
        }
    }
}

/**
 * Bespoke Luxury Financial Hero Card:
 * Strict ISO/IEC 7810 ID-1 card proportions (~1.586:1 aspect ratio), procedural Canvas mesh,
 * brushed EMV smart chip, contactless signal arcs, 16-digit embossed valuation,
 * and authenticated bottom cardholder metadata bar. 100% free of extraneous logos or clutter.
 */
@Composable
fun LuxuryCreditCardHero(
    title: String = "PRIMARY VAULT",
    vaultName: String = "PRIMARY WEALTH VAULT",
    currentValue: Double,
    investedValue: Double,
    gainValue: Double,
    returnPercent: Double,
    todayGain: Double? = null,
    schemeCount: Int? = null,
    isMasked: Boolean,
    modifier: Modifier = Modifier,
    hardwareVariant: CardHardwareVariant = LocalCardHardware.current,
    trailingBadge: @Composable (() -> Unit)? = null
) {
    val isPositive = gainValue >= 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, hardwareVariant.accentColor.copy(alpha = 0.38f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hardwareVariant.brush)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val specularBrush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.15f),
                        radius = w * 0.65f
                    )
                    onDrawBehind {
                        when (hardwareVariant) {
                            CardHardwareVariant.RUBY_OBSIDIAN -> {
                                // Fine diagonal carbon-fiber weave / topographic contour curves with alpha 0.08f
                                val step = 14.dp.toPx()
                                val totalSpan = w + h
                                var off = 0f
                                while (off < totalSpan) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(off, 0f),
                                        end = Offset(0f, off),
                                        strokeWidth = 1.2f
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.04f),
                                        start = Offset(off - h, 0f),
                                        end = Offset(off, h),
                                        strokeWidth = 0.8f
                                    )
                                    off += step
                                }
                                // Diagonal specular sheen stripe across left-center
                                val sheenPath = Path().apply {
                                    moveTo(w * 0.25f, 0f)
                                    lineTo(w * 0.45f, 0f)
                                    lineTo(w * 0.15f, h)
                                    lineTo(-w * 0.05f, h)
                                    close()
                                }
                                drawPath(
                                    path = sheenPath,
                                    color = Color.White.copy(alpha = 0.06f)
                                )
                            }
                            CardHardwareVariant.TITANIUM_BLACK -> {
                                // Fine horizontal hairline brushed metal streaks and micro-grid dots
                                val lineStep = 5.dp.toPx()
                                var y = 0f
                                var i = 0
                                while (y < h) {
                                    val startX = if (i % 2 == 0) 0f else w * 0.2f
                                    val endX = if (i % 3 == 0) w * 0.85f else w
                                    drawLine(
                                        color = Color.White.copy(alpha = if (i % 2 == 0) 0.05f else 0.025f),
                                        start = Offset(startX, y),
                                        end = Offset(endX, y),
                                        strokeWidth = 0.75f
                                    )
                                    y += lineStep
                                    i++
                                }
                                // Micro-grid dots
                                val dotStep = 18.dp.toPx()
                                var dx = 0f
                                while (dx < w) {
                                    var dy = 0f
                                    while (dy < h) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.05f),
                                            radius = 0.9f,
                                            center = Offset(dx, dy)
                                        )
                                        dy += dotStep
                                    }
                                    dx += dotStep
                                }
                            }
                            CardHardwareVariant.NEO_GOLD -> {
                                // Bauhaus geometric mosaic art on right 40% (inspired by reference image 4)
                                val rightStart = w * 0.58f
                                val mosaicCell = 24.dp.toPx()
                                var col = 0
                                var mx = rightStart
                                while (mx < w) {
                                    var my = 0f
                                    var row = 0
                                    while (my < h) {
                                        when ((row + col) % 4) {
                                            0 -> {
                                                drawArc(
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    startAngle = 0f,
                                                    sweepAngle = 90f,
                                                    useCenter = true,
                                                    topLeft = Offset(mx, my),
                                                    size = Size(mosaicCell, mosaicCell)
                                                )
                                            }
                                            1 -> {
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.10f),
                                                    radius = 2.5f,
                                                    center = Offset(mx + mosaicCell / 2f, my + mosaicCell / 2f)
                                                )
                                            }
                                            2 -> {
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.07f),
                                                    radius = mosaicCell * 0.35f,
                                                    center = Offset(mx + mosaicCell / 2f, my + mosaicCell / 2f),
                                                    style = Stroke(width = 1f)
                                                )
                                            }
                                            3 -> {
                                                drawRect(
                                                    color = Color.White.copy(alpha = 0.05f),
                                                    topLeft = Offset(mx + 2.dp.toPx(), my + 2.dp.toPx()),
                                                    size = Size(mosaicCell - 4.dp.toPx(), mosaicCell - 4.dp.toPx())
                                                )
                                            }
                                        }
                                        my += mosaicCell
                                        row++
                                    }
                                    mx += mosaicCell
                                    col++
                                }
                            }
                            CardHardwareVariant.CYBER_EMERALD -> {
                                // Isometric micro-dot matrix and angular circuit lines
                                val cell = 20.dp.toPx()
                                var x = 0f
                                while (x < w) {
                                    var y = 0f
                                    while (y < h) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.08f),
                                            radius = 1.2f,
                                            center = Offset(x, y)
                                        )
                                        if (((x + y) / cell).toInt() % 3 == 0) {
                                            drawLine(
                                                color = Color(0xFF2DD4BF).copy(alpha = 0.08f),
                                                start = Offset(x, y),
                                                end = Offset(x + cell * 0.5f, y + cell * 0.5f),
                                                strokeWidth = 1f
                                            )
                                        }
                                        y += cell
                                    }
                                    x += cell
                                }
                            }
                            CardHardwareVariant.ROYAL_SAPPHIRE -> {
                                // Concentric geometric diamond & circular line art (inspired by RuPay / SBI and purple cards)
                                val center = Offset(w * 0.80f, h * 0.50f)
                                val maxR = w * 0.75f
                                val ringStep = 18.dp.toPx()
                                var r = ringStep
                                while (r < maxR) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.06f),
                                        radius = r,
                                        center = center,
                                        style = Stroke(width = 1f)
                                    )
                                    val dPath = Path().apply {
                                        moveTo(center.x, center.y - r * 0.7f)
                                        lineTo(center.x + r * 0.7f, center.y)
                                        lineTo(center.x, center.y + r * 0.7f)
                                        lineTo(center.x - r * 0.7f, center.y)
                                        close()
                                    }
                                    drawPath(
                                        path = dPath,
                                        color = Color.White.copy(alpha = 0.035f),
                                        style = Stroke(width = 0.8f)
                                    )
                                    r += ringStep
                                }
                            }
                        }
                        // Specular light sheen across top quadrant
                        drawRect(brush = specularBrush)
                    }
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: EMV Chip + Contactless Waves on left; Holographic Network Badge + Tier Tag on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EmvSmartChip(modifier = Modifier.size(width = 38.dp, height = 28.dp))
                        ContactlessWaves(tint = Color.White.copy(alpha = 0.70f))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HolographicCardNetworkBadge()
                        Surface(
                            color = Color.Black.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = hardwareVariant.displayName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Middle: 16-Digit Embossed Valuation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "VALUATION // CARD IDENTIFIER",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                    Text(
                        text = formatCardNumberClusters(currentValue, isMasked),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            fontFeatureSettings = "tnum",
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom Row: Cardholder / Vault on left, Return Pill & Expiry on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "CARDHOLDER // VAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                        Text(
                            text = vaultName.uppercase(),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.6.sp,
                                fontFeatureSettings = "tnum",
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tactical Return Pill
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (isPositive) Color(0xFF34D399).copy(alpha = 0.70f)
                                else Color(0xFFF87171).copy(alpha = 0.70f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
                                    else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) Color(0xFF34D399) else Color(0xFFF87171),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatWsPercent(returnPercent),
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontFeatureSettings = "tnum",
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EXPIRES // SYNC",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.60f)
                            )
                            Text(
                                text = "LIVE AMFI",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.8.sp,
                                    color = hardwareVariant.accentColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun WsElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
        border = BorderStroke(1.dp, WsSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
fun WsMetricPillar(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color = WsSlateTextSecondary
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun WsAllocationBar(slices: List<AllocationSlice>) {
    if (slices.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        slices.forEachIndexed { index, slice ->
            val weightFloat = (slice.weightPercent.toFloat() / 100f).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(weightFloat)
                    .fillMaxSize()
                    .background(wsSliceColor(index))
            )
        }
    }
}

/**
 * Modern institutional mutual fund summary card:
 * Row 1: Current Value | Net Gain %
 * Row 2: Invested | Latest NAV
 */
@Composable
fun WsSchemeSummaryCard(
    scheme: MutualFundScheme,
    isMasked: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isGain = scheme.absoluteGain >= 0

    WsElevatedCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category, Asset Class, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WsTerminalTag(
                        text = scheme.assetClass,
                        textColor = WsPrimaryIndigo,
                        backgroundColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE)
                    )
                    WsTerminalTag(
                        text = scheme.category,
                        textColor = WsSlateTextSecondary,
                        backgroundColor = Color(0xFFF8FAFC),
                        borderColor = WsSlateBorder
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Scheme",
                        tint = WsSlateTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scheme.schemeName,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = WsSlateBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Clean 2x2 Grid Layout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Current Value | Net Gain / Return
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Current Value",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.currentValue, isMasked),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum",
                                color = WsSlateTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Net Gain",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary,
                            textAlign = TextAlign.End
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isGain) WsEmeraldPositive else WsCrimsonNegative,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formatWsPercent(scheme.absoluteReturnPercent),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    fontFeatureSettings = "tnum",
                                    color = if (isGain) WsEmeraldPositive else WsCrimsonNegative
                                )
                            )
                        }
                    }
                }

                // Row 2: Invested | Latest NAV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Invested Capital",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary
                        )
                        Text(
                            text = formatWsInr(scheme.investedAmount, isMasked),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                                color = WsSlateTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Latest NAV (${scheme.lastNavDate})",
                            fontSize = 11.sp,
                            color = WsSlateTextSecondary,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.4f", scheme.fallbackNav)}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                                color = WsAccentRoyal
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
