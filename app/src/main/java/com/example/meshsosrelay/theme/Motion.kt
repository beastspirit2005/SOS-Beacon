package com.example.meshsosrelay.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

object MotionTokens {
    // A soft spring for screen/item entrances (slightly bouncy, smooth)
    val SoftSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // A stiff spring for high-impact actions like the SOS press
    val StiffSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

@Composable
fun SonarPulse(
    modifier: Modifier = Modifier,
    color: Color = SignalSosEmber,
    ringCount: Int = 3,
    durationMillis: Int = 2400
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonarPulse")
    
    Box(modifier = modifier) {
        for (i in 0 until ringCount) {
            val delay = (durationMillis / ringCount) * i
            
            val progress = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "ring-$i"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2
                val currentRadius = maxRadius * progress.value
                val alpha = (1f - progress.value) * 0.6f
                
                drawCircle(
                    color = color,
                    radius = currentRadius,
                    alpha = alpha,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
