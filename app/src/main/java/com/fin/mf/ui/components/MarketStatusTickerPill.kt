package com.fin.mf.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.data.engine.MarketHolidayEngine
import com.fin.mf.data.engine.MarketStatus
import com.fin.mf.data.engine.TradingHoliday

/**
 * Institutional Floating Market Status Pill for NSE & BSE India.
 * Displays live trading status, pulsing indicators, time-to-close countdown,
 * and upcoming trading holidays. Tapping launches the Holiday Calendar modal.
 */
@Composable
fun MarketStatusTickerPill(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status: MarketStatus = remember { MarketHolidayEngine.computeMarketStatus(context) }
    var showCalendarModal by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val (dotColor, dotBg) = when {
        status.isOpen -> Pair(Color(0xFF059669), Color(0xFFD1FAE5)) // Live Green
        status.isPreMarket -> Pair(Color(0xFF2563EB), Color(0xFFDBEAFE)) // Pre-Market Blue
        status.isHolidayToday -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2)) // Red Holiday
        else -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7)) // Amber Closed
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick { showCalendarModal = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing Status Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(dotBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .then(if (status.isOpen) Modifier.scale(pulseScale) else Modifier)
                            .background(dotColor, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = status.statusTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp,
                            color = dotColor
                        )
                    }
                    Text(
                        text = status.statusSubtitle,
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Next Holiday Pill
            if (status.nextHoliday != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Holiday",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = status.nextHoliday.holidayName,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showCalendarModal) {
        TradingHolidaysCalendarModal(
            onDismiss = { showCalendarModal = false }
        )
    }
}

/**
 * Interactive Dialog detailing the official NSE/BSE Trading Holidays calendar.
 */
@Composable
fun TradingHolidaysCalendarModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val holidays = remember { MarketHolidayEngine.getTradingHolidays(context) }
    var selectedYear by remember { mutableStateOf("All") }
    val gradient = LocalVaultGradient.current

    val filteredList = remember(selectedYear, holidays) {
        when (selectedYear) {
            "2025" -> holidays.filter { it.date.startsWith("2025") }
            "2026" -> holidays.filter { it.date.startsWith("2026") }
            else -> holidays
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NSE / BSE Trading Holidays",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Indian Capital Market Settlement Calendar",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Year Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "2025", "2026").forEach { year ->
                        val isSelected = selectedYear == year
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = year },
                            label = { Text(year, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gradient.startColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.date + it.holidayName }) { holiday ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = holiday.holidayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${holiday.dayOfWeek} • ${holiday.closedExchange}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = holiday.date,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = gradient.startColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
