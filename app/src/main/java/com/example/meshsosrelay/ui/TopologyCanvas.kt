package com.example.meshsosrelay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.CanvasNearBlack
import com.example.meshsosrelay.theme.SignalSafeTeal
import com.example.meshsosrelay.theme.SignalSosEmber
import com.example.meshsosrelay.theme.SurfaceNearBlack
import com.example.meshsosrelay.theme.getPriorityColor
import com.example.meshsosrelay.theme.getPriorityLabel

@Composable
fun TopologyCanvas(
    topology: MeshTopology,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        val transitionScale = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        transitionScale == 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "topologyCanvas")

    // Slow idle sonar ping
    val pulseScale by if (isReducedMotion) {
        remember { mutableStateOf(16f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = 35f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nodePulse"
        )
    }

    val pulseAlpha by if (isReducedMotion) {
        remember { mutableStateOf(0.4f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nodeAlpha"
        )
    }

    // Faint shimmer/dash flow for edges
    val shimmerPhase by if (isReducedMotion) {
        remember { mutableStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerPhase"
        )
    }

    // Animated packet progress along active hop path (0.0 to 1.0)
    val packetProgress by if (isReducedMotion) {
        remember { mutableStateOf(0.5f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Restart
            ),
            label = "packetProgress"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .premiumElevation(ElevationLevel.Card, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE MESH TOPOLOGY",
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

                    // 1. Dynamic layout positioning of nodes based on ID and role
                    val nodePositions = mutableMapOf<String, Offset>()
                    val relayNodes = topology.nodes.filter { !it.isVictim && !it.isGateway && it.nodeRole != "victim" && it.nodeRole != "gateway" }
                    
                    topology.nodes.forEach { node ->
                        val pos = when {
                            node.isVictim || node.nodeRole == "victim" || node.id == "victim" || node.id == "node_A" -> {
                                Offset(width * 0.18f, height * 0.50f)
                            }
                            node.isGateway || node.nodeRole == "gateway" || node.id == "gateway" || node.id == "node_D" -> {
                                Offset(width * 0.82f, height * 0.50f)
                            }
                            else -> {
                                val index = relayNodes.indexOf(node)
                                if (index == 0 || node.id == "peer_b" || node.id == "node_B") {
                                    Offset(width * 0.50f, height * 0.28f)
                                } else {
                                    Offset(width * 0.50f, height * 0.72f)
                                }
                            }
                        }
                        nodePositions[node.id] = pos
                    }

                    // Draw Background Grid
                    val gridColor = Color.White.copy(alpha = 0.03f)
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

                    // 2. Draw Edges (Network Links)
                    topology.edges.forEach { edge ->
                        val startPos = nodePositions[edge.fromNodeId] ?: Offset(width * 0.3f, height * 0.5f)
                        val endPos = nodePositions[edge.toNodeId] ?: Offset(width * 0.7f, height * 0.5f)

                        val isActiveLink = topology.activeHopPath.contains(edge.fromNodeId) &&
                                topology.activeHopPath.contains(edge.toNodeId)

                        // Thin solid baseline
                        drawLine(
                            color = if (isActiveLink) SignalSafeTeal.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            start = startPos,
                            end = endPos,
                            strokeWidth = 1.dp.toPx()
                        )

                        // Faint animated flow dash
                        drawLine(
                            color = if (isActiveLink) SignalSafeTeal.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            start = startPos,
                            end = endPos,
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 18f), -shimmerPhase)
                        )
                    }

                    // 3. Draw Traveling Packet Pulse with Fading Trail along Hop Path
                    if (topology.activeHopPath.size >= 2) {
                        val pathPositions = topology.activeHopPath.mapNotNull { nodePositions[it] }
                        if (pathPositions.size >= 2) {
                            // Extract packet priority from the origin/victim node
                            val victimNode = topology.nodes.firstOrNull { it.isVictim || it.nodeRole == "victim" }
                            val packetPriority = victimNode?.priority ?: 3
                            val pulseColor = getPriorityColor(packetPriority)

                            // Helper function to calculate position on multi-segment path
                            fun getPositionOnPath(path: List<Offset>, progress: Float): Offset {
                                val segmentCount = path.size - 1
                                val rawProgress = progress.coerceIn(0f, 1f) * segmentCount
                                val currentSegment = rawProgress.toInt().coerceIn(0, segmentCount - 1)
                                val segmentProgress = rawProgress - currentSegment
                                val p1 = path[currentSegment]
                                val p2 = path[currentSegment + 1]
                                return Offset(
                                    p1.x + (p2.x - p1.x) * segmentProgress,
                                    p1.y + (p2.y - p1.y) * segmentProgress
                                )
                            }

                            // Draw fading trail
                            val trailOffsets = listOf(0.04f, 0.08f, 0.12f)
                            trailOffsets.forEachIndexed { index, offset ->
                                val trailProgress = packetProgress - offset
                                if (trailProgress >= 0f) {
                                    val trailPos = getPositionOnPath(pathPositions, trailProgress)
                                    val trailAlpha = 0.4f / (index + 1)
                                    val trailRadius = (6 - index * 1.5f).coerceAtLeast(2f).dp.toPx()
                                    drawCircle(
                                        color = pulseColor.copy(alpha = trailAlpha),
                                        radius = trailRadius,
                                        center = trailPos
                                    )
                                }
                            }

                            // Main bright pulse
                            val mainPos = getPositionOnPath(pathPositions, packetProgress)
                            drawCircle(
                                color = pulseColor.copy(alpha = 0.2f),
                                radius = 12.dp.toPx(),
                                center = mainPos
                            )
                            drawCircle(
                                color = pulseColor,
                                radius = 7.dp.toPx(),
                                center = mainPos
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = mainPos
                            )
                        }
                    }

                    // 4. Draw Nodes with Sonar Pulse Waves
                    topology.nodes.forEach { node ->
                        val pos = nodePositions[node.id] ?: Offset(width * 0.5f, height * 0.5f)

                        // Mappings: victim = priority-based color, gateway = teal, relay = white, observer = dim
                        val nodeColor = when (node.nodeRole) {
                            "victim" -> getPriorityColor(node.priority)
                            "gateway" -> SignalSafeTeal
                            "relay" -> Color.White.copy(alpha = 0.8f)
                            "observer" -> Color.Gray.copy(alpha = 0.4f)
                            else -> {
                                if (node.isVictim) getPriorityColor(node.priority)
                                else if (node.isGateway) SignalSafeTeal
                                else Color.White.copy(alpha = 0.8f)
                            }
                        }

                        // Pulse Ring
                        drawCircle(
                            color = nodeColor.copy(alpha = pulseAlpha),
                            radius = pulseScale,
                            center = pos,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Outer glowing aura
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.15f),
                            radius = 16.dp.toPx(),
                            center = pos
                        )

                        // Core Node Circle
                        drawCircle(
                            color = nodeColor,
                            radius = 9.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = CanvasNearBlack,
                            radius = 3.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // 5. Compact Monospace HUD Overlay
                val isGatewayOnline = topology.nodes.any { it.isGateway }
                val totalHops = if (topology.activeHopPath.isNotEmpty()) topology.activeHopPath.size - 1 else 0
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(SurfaceNearBlack.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PEERS:   ${topology.nodes.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.LightGray
                        )
                        Text(
                            text = "HOPS:    $totalHops",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.LightGray
                        )
                        Text(
                            text = "GATEWAY: ${if (isGatewayOnline) "ONLINE" else "OFFLINE"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isGatewayOnline) SignalSafeTeal else SignalSosEmber
                        )
                    }
                }
            }

            // Topology Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = SignalSosEmber, label = "Origin (Victim)")
                LegendItem(color = Color.White.copy(alpha = 0.8f), label = "Relay")
                LegendItem(color = SignalSafeTeal, label = "Gateway")
                LegendItem(color = Color.Gray.copy(alpha = 0.4f), label = "Observer")
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
