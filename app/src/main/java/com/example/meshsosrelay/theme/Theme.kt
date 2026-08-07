package com.example.meshsosrelay.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SignalSosEmber,
    secondary = SignalSafeTeal,
    background = CanvasNearBlack,
    surface = SurfaceNearBlack,
    onBackground = OnSurfaceOffWhite,
    onSurface = OnSurfaceOffWhite,
    onSurfaceVariant = MutedGray,
    onPrimary = CanvasNearBlack,
    onSecondary = CanvasNearBlack
)

@Composable
fun MeshSosRelayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
