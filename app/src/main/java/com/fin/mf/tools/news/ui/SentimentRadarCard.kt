package com.fin.mf.tools.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient
import com.fin.mf.tools.news.data.SentimentScore

/**
 * Institutional Sentiment Radar Card rendering Bullish vs Bearish split bars and look-through tickers.
 */
@Composable
fun SentimentRadarCard(
    portfolioTickers: List<String>,
    sentimentRadar: Map<String, SentimentScore>,
    activeLookthroughTickersWithNews: Set<String>,
    modifier: Modifier = Modifier
) {
    if (portfolioTickers.isEmpty()) return

    var selectedTicker by remember(portfolioTickers) {
        mutableStateOf(portfolioTickers.firstOrNull() ?: "NVDA")
    }

    val gradient = LocalVaultGradient.current
    val activeScore = sentimentRadar[selectedTicker] ?: SentimentScore(bearishPercent = 0.40, bullishPercent = 0.60)
    val bullRatio = activeScore.bullishPercent.toFloat().coerceIn(0.1f, 0.9f)
    val bearRatio = 1f - bullRatio

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NewsSlateBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewsSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title and Live Pulse Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NewsBlueTagSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = NewsBlueTag,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LOOK-THROUGH SENTIMENT RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = NewsSlatePrimary,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Finnhub Institutional Market Pulse",
                            fontSize = 11.sp,
                            color = NewsSlateSecondary
                        )
                    }
                }

                Surface(
                    color = NewsEmeraldSoft,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NewsEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE PULSE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NewsEmerald,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Ticker Strip
            Text(
                text = "PORTFOLIO CONSTITUENTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NewsSlateMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                portfolioTickers.forEach { ticker ->
                    val isSelected = ticker == selectedTicker
                    val hasActiveNews = activeLookthroughTickersWithNews.contains(ticker.uppercase())

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTicker = ticker },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NewsSlatePrimary else NewsBgIce,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) NewsSlatePrimary else NewsSlateBorder
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (hasActiveNews) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF34D399) else NewsEmerald)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = ticker,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else NewsSlatePrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge Details for Selected Ticker
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NewsBgIce,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, NewsSlateBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedTicker Sentiment Distribution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NewsSlatePrimary
                        )

                        Text(
                            text = if (activeScore.isBullish) "BULLISH BIAS" else "BEARISH BIAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeScore.isBullish) NewsEmerald else NewsCrimson
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Split Progress Bar: Bullish vs Bearish
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(NewsSlateBorder)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Bullish segment
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(bullRatio)
                                    .background(NewsEmerald)
                            )
                            // Spacer/Divider
                            Spacer(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Color.White)
                            )
                            // Bearish segment
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(bearRatio)
                                    .background(NewsCrimson)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Percentages Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "▲ Bullish: ${activeScore.formattedBullish}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NewsEmerald
                        )
                        Text(
                            text = "Institutional Radar",
                            fontSize = 11.sp,
                            color = NewsSlateMuted
                        )
                        Text(
                            text = "▼ Bearish: ${activeScore.formattedBearish}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NewsCrimson
                        )
                    }
                }
            }
        }
    }
}
