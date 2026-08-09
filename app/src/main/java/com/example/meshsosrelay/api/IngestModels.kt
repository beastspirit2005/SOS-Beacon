package com.example.meshsosrelay.api

import com.example.meshsosrelay.contract.SosPacket
import kotlinx.serialization.Serializable

@Serializable
data class IngestRequest(
    val packet: SosPacket,
    val received_at: Long
)

@Serializable
data class IngestResult(
    val sos_id: String,
    val msg_id: String,
    val status: String,       // "accepted" | "duplicate"
    val priority: String,     // "low" | "medium" | "high"
    val escalation: String,   // "contacts" | "responders"
    val request_id: String
)
