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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.meshsosrelay.*
import com.example.meshsosrelay.contract.*
import com.example.meshsosrelay.theme.*
import com.example.meshsosrelay.ui.fake.FakeSosController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
                text = "DEBUG NAVIGATION MENU (CURRENT: $currentRoute)",
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
                    Text("Reset Fake Controller Timeline", color = CanvasNearBlack, style = MaterialTheme.typography.labelMedium)
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
            
            // Concentric circles
            for (i in 1..8) {
                drawCircle(
                    color = gridColor,
                    radius = (i * lineSpacing) * radiusMultiplier,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Radial network rays
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
    val deliveryState by controller.deliveryState.collectAsState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
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
        // Ambient network lines
        AmbientNetworkBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
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
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Animated Status Line in Teal (Mesh Health)
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

            // Central breathing SOS action button
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sonar pulse rings breathing every 3s
                SonarPulse(
                    modifier = Modifier.fillMaxSize(),
                    color = SignalSosEmber,
                    ringCount = 3,
                    durationMillis = 3000
                )

                // Stiff spring button scaling
                val scale = remember { Animatable(1f) }


                Box(
                    modifier = Modifier
                        .size(160.dp)
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
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                            color = CanvasNearBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TAP & HOLD",
                            style = MaterialTheme.typography.labelSmall,
                            color = CanvasNearBlack.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Secondary controls: I'm safe & Relayed alerts link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        controller.reset()
                    },
                    border = BorderStroke(1.dp, MutedGray.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceOffWhite),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "I'M SAFE",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        color = OnSurfaceOffWhite
                    )
                }

                Text(
                    text = "VIEW RELAYED ALERTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedGray,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigate(ReceivedAlerts)
                        }
                        .padding(8.dp)
                )
            }

            DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Home", onReset = { controller.reset() })
        }
    }
}

@Composable
fun AcceleratingSonarPulse(
    modifier: Modifier = Modifier,
    color: Color = SignalSosEmber,
    remainingMillis: Int,
    ringCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val animFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "pulseAnim"
    )

    val timeMs = System.currentTimeMillis()
    val ratio = (remainingMillis / 5000f).coerceIn(0f, 1f)
    val duration = (800 + (1600 * ratio)).toLong()

    Canvas(modifier = modifier) {
        val center = size.center
        val maxRadius = size.minDimension / 2
        
        for (i in 0 until ringCount) {
            val offsetTime = timeMs - (i * (duration / ringCount))
            val rawProgress = (offsetTime % duration) / duration.toFloat()
            val progress = rawProgress.coerceIn(0f, 1f)
            
            val currentRadius = maxRadius * progress
            val alpha = (1f - progress) * 0.4f
            
            drawCircle(
                color = color,
                radius = currentRadius,
                alpha = alpha,
                style = Stroke(width = 2.dp.toPx())
            )
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
    val view = LocalView.current

    var remainingMillis by remember { mutableStateOf(5000) }

    LaunchedEffect(Unit) {
        var lastTick = 5
        val startTime = System.currentTimeMillis()
        while (remainingMillis > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = (5000 - elapsed).coerceAtLeast(0).toInt()
            remainingMillis = remaining
            
            val currentSecond = (remaining + 999) / 1000
            if (currentSecond < lastTick && currentSecond >= 1) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                lastTick = currentSecond
            }
            
            delay(16)
        }
        onNavigate(Status)
    }

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "SENDING SOS",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSosEmber,
                letterSpacing = 4.sp
            )
            Text(
                text = "BROADCAST STAGING",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            AcceleratingSonarPulse(
                modifier = Modifier.fillMaxSize(),
                color = SignalSosEmber,
                remainingMillis = remainingMillis,
                ringCount = 3
            )

            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = SignalSosEmber.copy(alpha = 0.05f),
                    style = Stroke(width = 8.dp.toPx())
                )
                
                drawArc(
                    color = SignalSosEmber,
                    startAngle = -90f,
                    sweepAngle = 360f * (remainingMillis / 5000f),
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            val secondsLeft = (remainingMillis + 999) / 1000
            Text(
                text = secondsLeft.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 72.sp),
                color = OnSurfaceOffWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Emergency alert will broadcast automatically unless cancelled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            OutlinedButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    controller.reset()
                    onNavigate(Home)
                },
                border = BorderStroke(1.dp, SignalSosEmber.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalSosEmber),
                modifier = Modifier.height(54.dp).width(200.dp)
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
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

    // Automatically navigate to delivered once notified
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
            text = "BROADCASTING ACTIVE",
            style = MaterialTheme.typography.labelLarge,
            color = SignalSosEmber,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        // Topology path card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MESH HOP ROUTING PATH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
                )

                // Simple Visual Path representation
                topology.activeHopPath.forEachIndexed { index, nodeId ->
                    val node = topology.nodes.find { it.id == nodeId }
                    if (node != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (node.isVictim) SignalSosEmber else if (node.isGateway) SignalSafeTeal else MutedGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CanvasNearBlack
                                )
                            }
                            Text(
                                text = node.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceOffWhite
                            )
                        }
                        
                        if (index < topology.activeHopPath.size - 1) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 13.dp)
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(MutedGray)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("PEERS NEARBY", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        Text(
                            text = when (val s = meshState) {
                                is MeshState.Searching -> s.peers.toString()
                                is MeshState.InFlight -> s.peers.toString()
                                else -> "0"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                            color = SignalSafeTeal
                        )
                    }
                    Column {
                        Text("HOPS ELAPSED", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        Text(
                            text = when (val s = meshState) {
                                is MeshState.InFlight -> s.hops.toString()
                                else -> "0"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                            color = SignalSafeTeal
                        )
                    }
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

        // Success checkmark circle
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
                    text = "Your SOS packet successfully reached a connected Gateway device. An SMS alert has been dispatched to emergency responders.",
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
                text = "INCOMING RELAY ALERTS",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
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
                text = "MESH TOPOLOGY STATUS",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Nodes listing
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ACTIVE NODES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray
                    )
                    topology.nodes.forEach { node ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (node.isVictim) SignalSosEmber else if (node.isGateway) SignalSafeTeal else MutedGray)
                            )
                            Text(
                                text = "${node.label} (${node.id})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceOffWhite
                            )
                        }
                    }
                }
            }

            // Edges listing
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TOPOLOGY EDGES (CONNECTIONS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray
                    )
                    if (topology.edges.isEmpty()) {
                        Text(
                            text = "No connections active. Searching for peers...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray
                        )
                    } else {
                        topology.edges.forEach { edge ->
                            Text(
                                text = "Link: ${edge.fromNodeId} ⟷ ${edge.toNodeId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceOffWhite
                            )
                        }
                    }
                }
            }
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Mesh", onReset = { controller.reset() })
    }
}
