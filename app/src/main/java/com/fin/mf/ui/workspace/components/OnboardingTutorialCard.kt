package com.fin.mf.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mf.core.theme.LocalVaultGradient

/**
 * Aesthetic First-Time Onboarding & Tutorial Guide Card displayed when no schemes are loaded.
 */
@Composable
fun OnboardingTutorialCard(
    modifier: Modifier = Modifier,
    onAddSchemeClick: () -> Unit
) {
    val gradient = LocalVaultGradient.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WsSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, WsSlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Institutional Shield / Mascot Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(gradient.startColor, gradient.endColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header
            Text(
                text = "Welcome to FinBaba Vault",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = WsSlateTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "A private, local-first mutual fund look-through and portfolio diagnostic studio.",
                fontSize = 13.sp,
                color = WsSlateTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3-Step Tutorial Checklist
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TutorialStepRow(
                    stepNumber = "1",
                    icon = Icons.Default.Search,
                    title = "Search & Add Funds",
                    description = "Find any Indian mutual fund scheme via live AMFI search and enter your allotted units.",
                    accentColor = gradient.startColor
                )

                TutorialStepRow(
                    stepNumber = "2",
                    icon = Icons.Default.QueryStats,
                    title = "Deep Look-Through",
                    description = "Automatically unpack 700+ underlying stocks, detect multi-fund overlap, and track true sector exposures.",
                    accentColor = gradient.endColor
                )

                TutorialStepRow(
                    stepNumber = "3",
                    icon = Icons.Default.PieChart,
                    title = "Target Allocation & Drift",
                    description = "Configure custom market cap mandates and simulate smart cash inflows.",
                    accentColor = gradient.startColor
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Primary CTA: Add Your First Scheme
            Button(
                onClick = onAddSchemeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gradient.startColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Your First Scheme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TutorialStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$stepNumber. $title",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WsSlateTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = WsSlateTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
