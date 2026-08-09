package com.example.meshsosrelay.mesh

import android.content.Context
import android.content.SharedPreferences
import com.example.meshsosrelay.contract.SosPacket
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists SosPackets to disk instantly to survive app kills.
 */
class PacketStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mesh_sos_packets", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun savePacket(packet: SosPacket) {
        val serialized = json.encodeToString(packet)
        prefs.edit().putString(packet.msg_id, serialized).apply()
    }

    fun getPendingPackets(): List<SosPacket> {
        val allEntries = prefs.all
        val pending = mutableListOf<SosPacket>()
        for ((key, value) in allEntries) {
            if (value is String) {
                try {
                    val packet = json.decodeFromString<SosPacket>(value)
                    pending.add(packet)
                } catch (e: Exception) {
                    // Ignore corrupted or invalid JSON packets
                }
            }
        }
        return pending
    }

    fun markAsRelayed(msgId: String) {
        prefs.edit().remove(msgId).apply()
    }
}
