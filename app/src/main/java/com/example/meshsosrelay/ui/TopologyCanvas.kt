package com.example.meshsosrelay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.CanvasNearBlack
import com.example.meshsosrelay.theme.SignalSafeTeal
import com.example.meshsosrelay.theme.SignalSosEmber
import com.example.meshsosrelay.theme.SurfaceNearBlack

@Composable
fun TopologyCanvas(
    topology: MeshTopology,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "topologyCanvas")

    // Node pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nodePulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nodeAlpha"
    )

    // Animated packet progress along active hop path (0.0 to 1.0)
    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "packetProgress"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE MESH TOPOLOGY (MONEY-SHOT VIEW)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalSafeTeal,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${topology.nodes.size} NODES ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(CanvasNearBlack, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Predefined smooth node coordinate layout mapping
                    val nodePositions = mapOf(
                        "node_A" to Offset(width * 0.18f, height * 0.50f), // Victim
                        "node_B" to Offset(width * 0.50f, height * 0.28f), // Relay 1
                        "node_C" to Offset(width * 0.50f, height * 0.72f), // Relay 2
                        "node_D" to Offset(width * 0.82f, height * 0.50f)  // Gateway
                    )

                    // Draw Background Grid Lines
                    val gridColor = Color.White.copy(alpha = 0.05f)
                    val step = 40.dp.toPx()
                    var x = 0f
                    while (x < width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < height) {
                        drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                        y += step
                    }

                    // 1. Draw Edges (Network Links)
                    topology.edges.forEach { edge ->
                        val startPos = nodePositions[edge.fromNodeId] ?: Offset(width * 0.3f, height * 0.5f)
                        val endPos = nodePositions[edge.toNodeId] ?: Offset(width * 0.7f, height * 0.5f)

                        // Check if this link is part of active hop path
                        val isActiveLink = topology.activeHopPath.contains(edge.fromNodeId) &&
                                topology.activeHopPath.contains(edge.toNodeId)

                        val lineColor = if (isActiveLink) SignalSafeTeal else Color.DarkGray.copy(alpha = 0.5f)
                        val strokeWidth = if (isActiveLink) 3.dp.toPx() else 1.5.dp.toPx()

                        drawLine(
                            color = lineColor,
                            start = startPos,
                            end = endPos,
                            strokeWidth = strokeWidth,
                            pathEffect = if (!isActiveLink) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    }

                    // 2. Draw Packet Particles Travelling along Active Hop Path
                    if (topology.activeHopPath.size >= 2) {
                        val pathPositions = topology.activeHopPath.mapNotNull { nodePositions[it] }
                        if (pathPositions.size >= 2) {
                            val segmentCount = pathPositions.size - 1
                            val rawProgress = packetProgress * segmentCount
                            val currentSegment = rawProgress.toInt().coerceIn(0, segmentCount - 1)
                            val segmentProgress = rawProgress - currentSegment

                            val p1 = pathPositions[currentSegment]
                            val p2 = pathPositions[currentSegment + 1]

                            val particleX = p1.x + (p2.x - p1.x) * segmentProgress
                            val particleY = p1.y + (p2.y - p1.y) * segmentProgress

                            // Glowing SOS packet particle
                            drawCircle(
                                color = SignalSosEmber,
                                radius = 7.dp.toPx(),
                                center = Offset(particleX, particleY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(particleX, particleY)
                            )
                        }
                    }

                    // 3. Draw Nodes with Sonar Pulse Waves
                    topology.nodes.forEach { node ->
                        val pos = nodePositions[node.id] ?: Offset(width * 0.5f, height * 0.5f)

                        val nodeColor = when {
                            node.isVictim -> SignalSosEmber
                            node.isGateway -> Color(0xFF00E5FF)
                            else -> SignalSafeTeal
                        }

                        // Pulse Ring
                        drawCircle(
                            color = nodeColor.copy(alpha = pulseAlpha),
                            radius = pulseScale,
                            center = pos,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Outer Node Glow Ring
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.3f),
                            radius = 16.dp.toPx(),
                            center = pos
                        )

                        // Core Node Circle
                        drawCircle(
                            color = nodeColor,
                            radius = 10.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = CanvasNearBlack,
                            radius = 4.dp.toPx(),
                            center = pos
                        )
                    }
                }
            }

            // Topology Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = SignalSosEmber, label = "Victim Node")
                LegendItem(color = SignalSafeTeal, label = "Relay Peers")
                LegendItem(color = Color(0xFF00E5FF), label = "Cloud Gateway")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.LightGray
        )
    }
}
