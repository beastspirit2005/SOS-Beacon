package com.example.meshsosrelay

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Home : NavKey
@Serializable data object Sending : NavKey
@Serializable data object Status : NavKey
@Serializable data object Delivered : NavKey
@Serializable data object ReceivedAlerts : NavKey
@Serializable data object MeshView : NavKey
