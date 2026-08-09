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
    val status: String,
    val sos_id: String,
    val duplicate: Boolean,
    val message: String
)
