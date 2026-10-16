package com.fin.mf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fin.mf.data.ChartTimeframe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullscreenNavChartDialog(
    schemeName: String,
    schemeCode: Int,
    visiblePoints: List<NavChartPoint>,
    selectedTimeframe: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    customSliceRange: ClosedFloatingPointRange<Float>,
    onCustomSliceRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    isTwoPointAnalyzerEnabled: Boolean,
    onTwoPointAnalyzerToggled: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ChartBgLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ChartSurfaceWhite,
                        titleContentColor = ChartSlateTextPrimary
                    ),
                    title = {
                        Column {
                            Text(
                                text = schemeName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Full Screen Chart Studio • AMFI #$schemeCode",
                                fontSize = 11.sp,
                                color = ChartSlateTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Full Screen",
                                tint = ChartPrimaryIndigo
                            )
                        }
                    }
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    TimeframeChipsRow(
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeSelected = onTimeframeSelected
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Range Slider in Full Screen
                    AnimatedVisibility(visible = selectedTimeframe == ChartTimeframe.CUSTOM) {
                        Column {
                            RangeSlider(
                                value = customSliceRange,
                                onValueChange = onCustomSliceRangeChanged,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ChartPrimaryIndigo,
                                    activeTrackColor = ChartAccentRoyal
                                )
                            )
                        }
                    }

                    // 2-Point Analyzer Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChartSurfaceWhite, RoundedCornerShape(10.dp))
                            .border(1.dp, ChartSlateBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Two-Point Return & Annualized CAGR Analyzer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChartPrimaryIndigo
                        )
                        Switch(
                            checked = isTwoPointAnalyzerEnabled,
                            onCheckedChange = onTwoPointAnalyzerToggled,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = ChartPrimaryIndigo
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChartSurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChartSlateBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ChartCanvasWithInteractivity(
                                visiblePoints = visiblePoints,
                                timeframe = selectedTimeframe,
                                isTwoPointAnalyzer = isTwoPointAnalyzerEnabled,
                                chartHeightDp = 380,
                                isFullScreen = true
                            )
                        }
                    }
                }
            }
        }
    }
}
