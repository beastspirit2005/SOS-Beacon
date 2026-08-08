package com.example.meshsosrelay.theme

import androidx.compose.ui.graphics.Color

val CanvasNearBlack = Color(0xFF0A0B0D)
val SurfaceNearBlack = Color(0xFF131519)
val OnSurfaceOffWhite = Color(0xFFEDEEF0)
val MutedGray = Color(0xFF8A8F98)

val SignalSafeTeal = Color(0xFF2DE1C2)
val SignalSosEmber = Color(0xFFFF5A3C)

// Priority Scale Colors (Beacon Theme)
val PriorityL5Color = Color(0xFFFF1744) // Intense Ember (Mass casualty)
val PriorityL4Color = Color(0xFFFF5A3C) // SignalSosEmber (Critical)
val PriorityL3Color = Color(0xFFFF9F0A) // Amber (SOS)
val PriorityL2Color = Color(0xFF2DE1C2) // SignalSafeTeal (Update)
val PriorityL1Color = Color(0xFF8A8F98) // MutedGray (Maintenance)

fun getPriorityLabel(priority: Int): String {
    return when (priority) {
        5 -> "Mass casualty"
        4 -> "Critical"
        3 -> "SOS"
        2 -> "Update"
        1 -> "Maintenance"
        else -> "SOS"
    }
}

fun getPriorityColor(priority: Int): Color {
    return when (priority) {
        5 -> PriorityL5Color
        4 -> PriorityL4Color
        3 -> PriorityL3Color
        2 -> PriorityL2Color
        1 -> PriorityL1Color
        else -> PriorityL3Color
    }
}
