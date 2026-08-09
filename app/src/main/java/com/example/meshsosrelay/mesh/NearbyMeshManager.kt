package com.example.meshsosrelay.mesh

import android.content.Context
import android.util.Log
import com.example.meshsosrelay.contract.SosPacket
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Handles Google Nearby Connections API.
 * Includes an LRU Cache to prevent infinite broadcast storms.
 */
class NearbyMeshManager(private val context: Context, private val packetStore: PacketStore) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val json = Json { ignoreUnknownKeys = true }

    // State flow to expose connected peers to the Controller
    private val _connectedPeers = MutableStateFlow<Set<String>>(emptySet())
    val connectedPeers: StateFlow<Set<String>> = _connectedPeers

    // LRU Cache: Stores up to 100 packet IDs. If size > 100, removes the oldest.
    private val seenPacketCache = object : java.util.LinkedHashMap<String, Long>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 100
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val payloadString = String(bytes)
                    try {
                        val packet = json.decodeFromString<SosPacket>(payloadString)
                        handleIncomingPacket(packet)
                    } catch (e: Exception) {
                        Log.e("NearbyMeshManager", "Failed to decode packet", e)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleIncomingPacket(packet: SosPacket) {
        // 1. DEDUPLICATION CHECK (Prevents Mesh Storms)
        if (seenPacketCache.containsKey(packet.msg_id)) {
            Log.d("NearbyMeshManager", "Duplicate packet dropped: ${packet.msg_id}")
            return
        }

        // 2. Mark as seen
        seenPacketCache[packet.msg_id] = System.currentTimeMillis()

        // 3. Persist to disk immediately (Survives App Kill)
        packetStore.savePacket(packet)

        // 4. Relay to other connected peers (Store-Carry-Forward)
        broadcastPacket(packet)
    }

    fun broadcastPacket(packet: SosPacket) {
        // Automatically add our own generated packets to the seen cache so we don't process them if echoed back
        seenPacketCache[packet.msg_id] = System.currentTimeMillis()

        val serialized = json.encodeToString(packet)
        val payload = Payload.fromBytes(serialized.toByteArray())

        val peers = _connectedPeers.value
        if (peers.isNotEmpty()) {
            connectionsClient.sendPayload(peers.toList(), payload)
                .addOnSuccessListener { Log.d("NearbyMeshManager", "Broadcasted to ${peers.size} peers") }
                .addOnFailureListener { e -> Log.e("NearbyMeshManager", "Broadcast failed", e) }
        } else {
            Log.d("NearbyMeshManager", "No peers available to broadcast. Saved in PacketStore for later.")
        }
    }
    
    // (In a full implementation, startAdvertising and startDiscovery would be called here
    // after checking permissions in the UI layer)
}
