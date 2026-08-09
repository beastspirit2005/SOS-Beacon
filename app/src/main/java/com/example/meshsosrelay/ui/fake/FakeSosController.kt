package com.example.meshsosrelay.ui.fake

import com.example.meshsosrelay.contract.*
import com.example.meshsosrelay.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FakeSosController : SosController {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _meshState = MutableStateFlow<MeshState>(MeshState.Idle)
    override val meshState: StateFlow<MeshState> = _meshState

    // =========================================================================
    // SEAM FOR SYSTEMS TEAM (PENDING IMPLEMENTATION):
    // The deviceRole represents the local device's active role in the mesh network.
    // In the real implementation, this must be exposed dynamically by the mesh core
    // (e.g., via MeshState or a separate StateFlow on SosController) rather than
    // being a manually cycled value.
    // =========================================================================
    val deviceRole = MutableStateFlow("observer")

    val soundEnabled = MutableStateFlow(false)
    val volunteerMode = MutableStateFlow(false)
    private val _deliveryState = MutableStateFlow<DeliveryState>(DeliveryState.Idle)
    override val deliveryState: StateFlow<DeliveryState> = _deliveryState

    // Fake Topology Flow
    private val _meshTopology = MutableStateFlow(
        MeshTopology(
            nodes = listOf(
                TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                TopoNode("peer_b", "Peer B", isRelay = true, nodeRole = "relay", priority = 4)
            ),
            edges = emptyList(),
            activeHopPath = emptyList()
        )
    )
    val meshTopology: StateFlow<MeshTopology> = _meshTopology

    // Fake Incoming RELAY alerts
    private val _receivedAlerts = MutableStateFlow(
        listOf(
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_alpha",
                created_at = System.currentTimeMillis() - 600000,
                lat = 12.9716,
                lon = 77.5946,
                acc = 10.0f,
                severity = "critical",
                confidence = 0.9f,
                trigger_type = "fall",
                ttl = 4,
                hops = 2,
                payload = "Severe impact detected. Stillness timeout. Request assistance.",
                signature = "fake_sig_1",
                priority = 5
            ),
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_beta",
                created_at = System.currentTimeMillis() - 300000,
                lat = 12.9722,
                lon = 77.5950,
                acc = 15.0f,
                severity = "warn",
                confidence = 0.7f,
                trigger_type = "manual",
                payload = "Sprained ankle on trail. Slowly moving towards base camp.",
                ttl = 5,
                hops = 1,
                signature = "fake_sig_2",
                priority = 4
            ),
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_gamma",
                created_at = System.currentTimeMillis() - 50000,
                lat = 12.9705,
                lon = 77.5930,
                acc = 8.0f,
                severity = "info",
                confidence = 0.5f,
                trigger_type = "manual",
                payload = "All clear. Arrived at base camp. Relaying status.",
                ttl = 6,
                hops = 0,
                signature = "fake_sig_3",
                priority = 3
            )
        )
    )
    val receivedAlerts: StateFlow<List<SosPacket>> = _receivedAlerts

    override fun trigger(draft: SosDraft) {
        scope.launch {
            println("FakeSosController: Triggered with: $draft")

            // Phase 1: Triggered -> Searching
            _deliveryState.value = DeliveryState.Pending
            _meshState.value = MeshState.Searching(peers = 2)
            _meshTopology.value = MeshTopology(
                nodes = listOf(
                    TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                    TopoNode("peer_b", "Peer B (Relay)", isRelay = true, nodeRole = "relay", priority = 4)
                ),
                edges = listOf(TopoEdge("victim", "peer_b")),
                activeHopPath = listOf("victim")
            )
            println("FakeSosController: State: Pending | Mesh: Searching(2)")

            delay(5000)

            // Phase 2: InFlight (1 hop)
            _meshState.value = MeshState.InFlight(peers = 2, hops = 1)
            _meshTopology.value = MeshTopology(
                nodes = listOf(
                    TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                    TopoNode("peer_b", "Peer B (Relay)", isRelay = true, nodeRole = "relay", priority = 4),
                    TopoNode("peer_c", "Peer C (Relay)", isRelay = true, nodeRole = "relay", priority = 5)
                ),
                edges = listOf(
                    TopoEdge("victim", "peer_b"),
                    TopoEdge("peer_b", "peer_c")
                ),
                activeHopPath = listOf("victim", "peer_b")
            )
            println("FakeSosController: State: Pending | Mesh: InFlight(2 peers, 1 hop)")

            delay(1500)

            // Phase 3: InFlight (2 hops)
            _meshState.value = MeshState.InFlight(peers = 3, hops = 2)
            _meshTopology.value = MeshTopology(
                nodes = listOf(
                    TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                    TopoNode("peer_b", "Peer B (Relay)", isRelay = true, nodeRole = "relay", priority = 4),
                    TopoNode("peer_c", "Peer C (Relay)", isRelay = true, nodeRole = "relay", priority = 5),
                    TopoNode("gateway", "Gateway Phone", isGateway = true, nodeRole = "gateway", priority = 3)
                ),
                edges = listOf(
                    TopoEdge("victim", "peer_b"),
                    TopoEdge("peer_b", "peer_c"),
                    TopoEdge("peer_c", "gateway")
                ),
                activeHopPath = listOf("victim", "peer_b", "peer_c")
            )
            println("FakeSosController: State: Pending | Mesh: InFlight(3 peers, 2 hops)")

            delay(1500)

            // Phase 4: Delivered / Notified
            _deliveryState.value = DeliveryState.Notified
            _meshState.value = MeshState.Delivered
            _meshTopology.value = _meshTopology.value.copy(
                activeHopPath = listOf("victim", "peer_b", "peer_c", "gateway")
            )
            println("FakeSosController: State: Notified | Mesh: Delivered")
        }
    }

    fun reset() {
        _deliveryState.value = DeliveryState.Idle
        _meshState.value = MeshState.Idle
        _meshTopology.value = MeshTopology(
            nodes = listOf(
                TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                TopoNode("peer_b", "Peer B", isRelay = true, nodeRole = "relay", priority = 4)
            ),
            edges = emptyList(),
            activeHopPath = emptyList()
        )
        println("FakeSosController: Reset to Idle")
    }

    fun clearReceivedAlerts() {
        _receivedAlerts.value = emptyList()
        println("FakeSosController: Received alerts cleared")
    }

    fun populateReceivedAlerts() {
        _receivedAlerts.value = listOf(
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_alpha",
                created_at = System.currentTimeMillis() - 600000,
                lat = 12.9716,
                lon = 77.5946,
                acc = 10.0f,
                severity = "critical",
                confidence = 0.9f,
                trigger_type = "fall",
                ttl = 4,
                hops = 2,
                payload = "Severe impact detected. Stillness timeout. Request assistance.",
                signature = "fake_sig_1",
                priority = 5
            ),
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_beta",
                created_at = System.currentTimeMillis() - 300000,
                lat = 12.9722,
                lon = 77.5950,
                acc = 15.0f,
                severity = "warn",
                confidence = 0.7f,
                trigger_type = "manual",
                payload = "Sprained ankle on trail. Slowly moving towards base camp.",
                ttl = 5,
                hops = 1,
                signature = "fake_sig_2",
                priority = 4
            ),
            SosPacket(
                msg_id = UUID.randomUUID().toString(),
                origin_id = "device_gamma",
                created_at = System.currentTimeMillis() - 50000,
                lat = 12.9705,
                lon = 77.5930,
                acc = 8.0f,
                severity = "info",
                confidence = 0.5f,
                trigger_type = "manual",
                payload = "All clear. Arrived at base camp. Relaying status.",
                ttl = 6,
                hops = 0,
                signature = "fake_sig_3",
                priority = 3
            )
        )
        println("FakeSosController: Received alerts populated")
    }

    fun cycleDeviceRole() {
        deviceRole.value = when (deviceRole.value) {
            "observer" -> "relay"
            "relay" -> "gateway"
            "gateway" -> "victim"
            else -> "observer"
        }
        println("FakeSosController: Cycled device role to: ${deviceRole.value}")
    }
}
