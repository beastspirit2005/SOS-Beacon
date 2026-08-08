package com.example.meshsosrelay.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.meshsosrelay.*
import com.example.meshsosrelay.contract.*
import com.example.meshsosrelay.permissions.PermissionManager
import com.example.meshsosrelay.permissions.PermissionStatusCard
import com.example.meshsosrelay.theme.*
import com.example.meshsosrelay.ui.fake.FakeSosController
import kotlinx.coroutines.launch

@Composable
fun DebugNavigationFooter(
    onNavigate: (NavKey) -> Unit,
    currentRoute: String,
    onReset: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "NAVIGATION MENU (CURRENT: $currentRoute)",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair(Home, "Home"),
                    Pair(Sending, "Sending"),
                    Pair(Status, "Status"),
                    Pair(Delivered, "Delivered"),
                    Pair(ReceivedAlerts, "Relays"),
                    Pair(MeshView, "Mesh")
                ).forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (currentRoute == label) SignalSafeTeal else CanvasNearBlack)
                            .clickable { onNavigate(key) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (currentRoute == label) CanvasNearBlack else OnSurfaceOffWhite,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (onReset != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = SignalSosEmber),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Reset Controller Timeline", color = CanvasNearBlack, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AmbientNetworkBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val radiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radiusScale"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = size.center
        val maxDim = size.maxDimension

        rotate(rotationAngle, center) {
            val lineSpacing = 48.dp.toPx()
            val gridColor = MutedGray.copy(alpha = 0.04f)

            for (i in 1..8) {
                drawCircle(
                    color = gridColor,
                    radius = (i * lineSpacing) * radiusMultiplier,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            val rayCount = 8
            for (i in 0 until rayCount) {
                val angle = (360f / rayCount) * i
                val angleRad = Math.toRadians(angle.toDouble())
                val endX = center.x + (maxDim * Math.cos(angleRad)).toFloat()
                val endY = center.y + (maxDim * Math.sin(angleRad)).toFloat()
                drawLine(
                    color = gridColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val meshState by controller.meshState.collectAsState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current

    val peerCount = when (val state = meshState) {
        is MeshState.Searching -> state.peers
        is MeshState.InFlight -> state.peers
        is MeshState.Delivered -> 3
        else -> 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
    ) {
        AmbientNetworkBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header & Permissions Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "MESH SOS RELAY",
                    style = MaterialTheme.typography.labelMedium,
                    color = SignalSafeTeal,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "OFFLINE EMERGENCY NETWORK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                PermissionStatusCard()
            }

            // Mesh Health Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(SurfaceNearBlack)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (peerCount > 0) SignalSafeTeal else SignalSosEmber)
                )
                if (peerCount > 0) {
                    Text(
                        text = "Mesh active · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceOffWhite
                    )
                    AnimatedContent(
                        targetState = peerCount,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "peerCountAnimation"
                    ) { targetCount ->
                        Text(
                            text = String.format("%02d", targetCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = SignalSafeTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = " peers nearby",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceOffWhite
                    )
                } else {
                    Text(
                        text = "Searching for peers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray
                    )
                }
            }

            // Central Breathing SOS Button
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                SonarPulse(
                    modifier = Modifier.fillMaxSize(),
                    color = SignalSosEmber,
                    ringCount = 3,
                    durationMillis = 3000
                )

                val scale = remember { Animatable(1f) }

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(SignalSosEmber)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    scope.launch {
                                        scale.animateTo(0.85f, animationSpec = MotionTokens.StiffSpring)
                                    }
                                    tryAwaitRelease()
                                    scope.launch {
                                        scale.animateTo(1f, animationSpec = MotionTokens.SoftSpring)
                                    }
                                },
                                onTap = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    controller.trigger(SosDraft("critical", "Emergency manual SOS trigger from HomeScreen"))
                                    onNavigate(Sending)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SOS",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = CanvasNearBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ONE-TAP SEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = CanvasNearBlack.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Secondary Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            controller.reset()
                        },
                        border = BorderStroke(1.dp, MutedGray.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceOffWhite),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("I'M SAFE", style = MaterialTheme.typography.labelMedium, color = OnSurfaceOffWhite)
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigate(ReceivedAlerts)
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceNearBlack),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("RELAY ALERTS", style = MaterialTheme.typography.labelMedium, color = SignalSafeTeal)
                    }
                }
            }

            DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Home", onReset = { controller.reset() })
        }
    }
}

@Composable
fun SendingScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveryState by controller.deliveryState.collectAsState()
    val meshState by controller.meshState.collectAsState()

    LaunchedEffect(deliveryState, meshState) {
        if (meshState is MeshState.InFlight) {
            onNavigate(Status)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "CANCEL WINDOW COUNTDOWN",
            style = MaterialTheme.typography.labelLarge,
            color = SignalSosEmber,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            SonarPulse(
                modifier = Modifier.fillMaxSize(),
                color = SignalSosEmber,
                ringCount = 3,
                durationMillis = 1800
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(SurfaceNearBlack)
                    .border(2.dp, SignalSosEmber, CircleShape)
                    .clickable {
                        controller.reset()
                        onNavigate(Home)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CANCEL",
                        style = MaterialTheme.typography.titleMedium,
                        color = SignalSosEmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sending in 5s...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PACKET STAGING & GPS ATTACH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
                )
                Text(
                    text = "Acquiring Fused Location GPS coordinates and computing HMAC-SHA256 signature...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceOffWhite
                )
            }
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Sending", onReset = { controller.reset() })
    }
}

@Composable
fun StatusScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val meshState by controller.meshState.collectAsState()
    val deliveryState by controller.deliveryState.collectAsState()
    val topology by controller.meshTopology.collectAsState()

    LaunchedEffect(deliveryState) {
        if (deliveryState is DeliveryState.Notified) {
            onNavigate(Delivered)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "BROADCASTING IN MESH",
            style = MaterialTheme.typography.labelLarge,
            color = SignalSosEmber,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Embedded Topology Canvas (Money Shot)
        TopologyCanvas(
            topology = topology,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PEERS NEARBY", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                    Text(
                        text = when (val s = meshState) {
                            is MeshState.Searching -> s.peers.toString()
                            is MeshState.InFlight -> s.peers.toString()
                            else -> "0"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
                        color = SignalSafeTeal
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("HOPS ELAPSED", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                    Text(
                        text = when (val s = meshState) {
                            is MeshState.InFlight -> s.hops.toString()
                            else -> "0"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
                        color = SignalSafeTeal
                    )
                }
            }
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Status", onReset = { controller.reset() })
    }
}

@Composable
fun DeliveredScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "DELIVERY CONFIRMED",
            style = MaterialTheme.typography.labelLarge,
            color = SignalSafeTeal,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            SonarPulse(
                modifier = Modifier.fillMaxSize(),
                color = SignalSafeTeal,
                ringCount = 3,
                durationMillis = 2400
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SignalSafeTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    color = CanvasNearBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EGRESS CONFIRMATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
                )
                Text(
                    text = "Your SOS packet successfully reached a connected Gateway device. Emergency dispatch notifications have been acknowledged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceOffWhite
                )
            }
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Delivered", onReset = { controller.reset() })
    }
}

@Composable
fun ReceivedAlertsScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val alerts by controller.receivedAlerts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "INCOMING RELAY ALERTS FEED",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(alerts) { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEVICE: ${alert.origin_id}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceOffWhite
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (alert.severity == "critical") SignalSosEmber else SignalSafeTeal)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = alert.severity.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CanvasNearBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = alert.payload,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "GPS: ${alert.lat}, ${alert.lon}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGray
                                )
                                Text(
                                    text = "HOPS: ${alert.hops}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGray
                                )
                            }
                        }
                    }
                }
            }
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Relays", onReset = { controller.reset() })
    }
}

@Composable
fun MeshViewScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val topology by controller.meshTopology.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MESH TOPOLOGY VISUALIZER",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            // Render live topology graph money shot canvas
            TopologyCanvas(topology = topology)
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Mesh", onReset = { controller.reset() })
    }
}
