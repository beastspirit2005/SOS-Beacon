package com.example.meshsosrelay.ui

/**
 * Minimal data shape for representing the mesh network topology.
 * Documented: to be matched to the mesh team's real shape when integrated.
 */
data class TopoNode(
    val id: String,
    val label: String,
    val isVictim: Boolean = false,
    val isRelay: Boolean = false,
    val isGateway: Boolean = false
)

data class TopoEdge(
    val fromNodeId: String,
    val toNodeId: String
)

data class MeshTopology(
    val nodes: List<TopoNode>,
    val edges: List<TopoEdge>,
    val activeHopPath: List<String>
)
