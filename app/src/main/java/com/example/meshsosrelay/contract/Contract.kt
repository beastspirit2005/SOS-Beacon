package com.example.meshsosrelay.contract

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SosDraft(
    val severity: String, // "info" | "warn" | "critical"
    val payload: String   // short message (<= 240 chars)
)

@Serializable
data class SosPacket(
    val msg_id: String,
    val origin_id: String,
    val created_at: Long,
    val lat: Double,
    val lon: Double,
    val acc: Float,
    val severity: String,
    val confidence: Float,
    val trigger_type: String, // "manual" | "fall" | "scream" | "crash"
    val ttl: Int,
    val hops: Int,
    val payload: String,
    val signature: String,
    val priority: Int = 3 // 1..5, Packet Priority Engine; default 3 = manual SOS
)

sealed interface MeshState {
    data object Idle : MeshState
    data class Searching(val peers: Int) : MeshState
    data class InFlight(val peers: Int, val hops: Int) : MeshState
    data object Delivered : MeshState
}

sealed interface DeliveryState {
    data object Idle : DeliveryState
    data object Pending : DeliveryState
    data object Notified : DeliveryState
}

interface SosController {
    fun trigger(draft: SosDraft)
    val meshState: StateFlow<MeshState>
    val deliveryState: StateFlow<DeliveryState>
}
