package com.example.meshsosrelay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.*

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                SonarPulse(
                    modifier = Modifier.fillMaxSize(),
                    color = SignalSosEmber,
                    ringCount = 3,
                    durationMillis = 2000
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SignalSosEmber, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "BEACON",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = OnSurfaceOffWhite
            )
            Text(
                text = "Offline P2P Emergency Link",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
                letterSpacing = 1.sp
            )
        }
    }
}
