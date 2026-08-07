package com.example.meshsosrelay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.*

@Composable
fun ThemePreviewScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "BEACON // SYSTEM SYSTEM PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = SignalSafeTeal,
                letterSpacing = 2.sp
            )
            Text(
                text = "Signal in the Dark",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurfaceOffWhite,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = SurfaceNearBlack, thickness = 2.dp)

        // Palette Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "PALETTE SWATCHES",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                letterSpacing = 1.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SwatchItem(color = CanvasNearBlack, name = "Canvas", hex = "#0A0B0D", onColor = OnSurfaceOffWhite, modifier = Modifier.weight(1f))
                SwatchItem(color = SurfaceNearBlack, name = "Surface", hex = "#131519", onColor = OnSurfaceOffWhite, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SwatchItem(color = SignalSafeTeal, name = "Safe", hex = "#2DE1C2", onColor = CanvasNearBlack, modifier = Modifier.weight(1f))
                SwatchItem(color = SignalSosEmber, name = "SOS", hex = "#FF5A3C", onColor = CanvasNearBlack, modifier = Modifier.weight(1f))
            }
        }

        // Typography Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TYPOGRAPHY",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Humanist Heading (Inter)",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurfaceOffWhite
                    )
                    Text(
                        text = "This is standard body text in Inter. It is highly readable, clean, and clean for instructions and descriptions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray
                    )
                    HorizontalDivider(color = CanvasNearBlack, thickness = 1.dp)
                    Text(
                        text = "NUMERALS & LABELS (JetBrains Mono)",
                        style = MaterialTheme.typography.labelLarge,
                        color = SignalSafeTeal
                    )
                    Text(
                        text = "PEERS: 04 // HOPS: 02 // LAT: 12.9716 // LON: 77.5946",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceOffWhite
                    )
                }
            }
        }

        // Sonar Pulse Animation Section
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SONAR PULSE MOTION (LIVE ANIMATION)",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(SurfaceNearBlack),
                contentAlignment = Alignment.Center
            ) {
                // Live pulsing rings
                SonarPulse(
                    modifier = Modifier.size(240.dp),
                    color = SignalSosEmber,
                    ringCount = 3,
                    durationMillis = 2400
                )

                // Perfect circle action button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SignalSosEmber),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                        color = CanvasNearBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SwatchItem(
    color: Color,
    name: String,
    hex: String,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(64.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = onColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hex,
                    style = MaterialTheme.typography.labelSmall,
                    color = onColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ThemePreviewScreenPreview() {
    MeshSosRelayTheme {
        ThemePreviewScreen()
    }
}
