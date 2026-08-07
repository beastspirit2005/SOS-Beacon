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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    val view = LocalView.current

    // Auto-navigate to Delivered screen when delivery state is Notified
    LaunchedEffect(deliveryState) {
        if (deliveryState is DeliveryState.Notified) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onNavigate(Delivered)
        }
    }

    val peerCount = when (val s = meshState) {
        is MeshState.Searching -> s.peers
        is MeshState.InFlight -> s.peers
        else -> 0
    }

    val hopCount = when (val s = meshState) {
        is MeshState.InFlight -> s.hops
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = if (meshState is MeshState.Searching) "REACHING PEERS" else "RELAYING SOS ALERT",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSosEmber,
                letterSpacing = 4.sp
            )
            Text(
                text = if (meshState is MeshState.Searching) "Broadcasting search query..." else "Packet propagating through mesh",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Live concentric wave showing physical hopping motion
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Faster pulsing in flight, slower when searching
            val pulseSpeed = if (meshState is MeshState.InFlight) 1800 else 3000
            SonarPulse(
                modifier = Modifier.fillMaxSize(),
                color = SignalSosEmber,
                ringCount = 3,
                durationMillis = pulseSpeed
            )

            // Inner circle displaying current hops count or search symbol
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SurfaceNearBlack)
                    .border(2.dp, SignalSosEmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (meshState is MeshState.Searching) {
                    Text(
                        text = "📡",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HOP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray
                        )
                        AnimatedContent(
                            targetState = hopCount,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "hopCountAnimation"
                        ) { count ->
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 36.sp),
                                color = SignalSosEmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Detailed Topology Path representation
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ACTIVE ROUTE PATH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    letterSpacing = 1.sp
                )

                // Simple path elements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topology.activeHopPath.forEachIndexed { idx, nodeId ->
                        val node = topology.nodes.find { it.id == nodeId }
                        if (node != null) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (node.isVictim) SignalSosEmber else if (node.isGateway) SignalSafeTeal else CanvasNearBlack)
                                    .border(
                                        width = 1.dp,
                                        color = if (node.isVictim) SignalSosEmber else if (node.isGateway) SignalSafeTeal else MutedGray.copy(alpha = 0.3f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = node.label.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (node.isVictim || node.isGateway) CanvasNearBlack else OnSurfaceOffWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (idx < topology.activeHopPath.size - 1) {
                                Text(
                                    text = "➔",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedGray
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = CanvasNearBlack, thickness = 1.dp)

                // Realtime connection statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PEERS COVERED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray
                        )
                        Text(
                            text = String.format("%02d", peerCount),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                            color = OnSurfaceOffWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MAPPED HOPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray
                        )
                        Text(
                            text = String.format("%02d", hopCount),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                            color = OnSurfaceOffWhite
                        )
                    }
                }
            }
        }

        // Redirect links
        Text(
            text = "VIEW DETAILED TOPOLOGY GRAPH",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
            letterSpacing = 2.sp,
            modifier = Modifier
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onNavigate(MeshView)
                }
                .padding(8.dp)
        )

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Status", onReset = { controller.reset() })
    }
}

@Composable
fun DeliveredScreen(
    controller: FakeSosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val meshState by controller.meshState.collectAsState()

    val peerCount = when (val s = meshState) {
        is MeshState.Searching -> s.peers
        is MeshState.InFlight -> s.peers
        else -> 3
    }

    val hopCount = when (val s = meshState) {
        is MeshState.InFlight -> s.hops
        else -> 2
    }

    // Color transition from alert Ember to safe Teal
    val colorAnim = remember { Animatable(SignalSosEmber) }
    val scaleAnim = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        // Play success haptic beat
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        
        // Parallel animations: color transition + soft spring settle
        launch {
            colorAnim.animateTo(
                targetValue = SignalSafeTeal,
                animationSpec = tween(durationMillis = 1000, easing = EaseInOutQuad)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = MotionTokens.SoftSpring
            )
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
        // Screen Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "DELIVERY CONFIRMED",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 4.sp
            )
            Text(
                text = "Responders notified · location sent",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Settling Circle Beacon & Checkmark
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Calm expanding teal sonar rings
            SonarPulse(
                modifier = Modifier.fillMaxSize(),
                color = SignalSafeTeal,
                ringCount = 2,
                durationMillis = 4000
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim.value)
                    .clip(CircleShape)
                    .background(colorAnim.value),
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

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TRANSMISSION SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HOPS TRAVERSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray
                        )
                        Text(
                            text = String.format("%02d", hopCount),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                            color = SignalSafeTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PEERS ENGAGED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray
                        )
                        Text(
                            text = String.format("%02d", peerCount),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                            color = SignalSafeTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = CanvasNearBlack, thickness = 1.dp)

                Text(
                    text = "Your SOS packet successfully reached an internet-connected Gateway node. SMS alert dispatched to responders with active location coordinates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceOffWhite
                )
            }
        }

        // Action Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    controller.reset()
                    onNavigate(Home)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalSafeTeal),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.height(48.dp).fillMaxWidth(0.6f)
            ) {
                Text(
                    text = "DONE",
                    style = MaterialTheme.typography.labelLarge,
                    color = CanvasNearBlack,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "VIEW TRANSMISSION ROUTE",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onNavigate(MeshView)
                    }
                    .padding(8.dp)
            )
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
    val view = LocalView.current

    // Pulsing dot animation for active relay status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header Row: Count & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RELAY NETWORK",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalSafeTeal,
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "You're carrying",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceOffWhite
                        )
                        AnimatedContent(
                            targetState = alerts.size,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "alertsCount"
                        ) { count ->
                            Text(
                                text = String.format("%02d", count),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (count > 0) SignalSosEmber else SignalSafeTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceOffWhite
                        )
                    }
                }

                // Inline Feed Toggle controls for hackathon presentation
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (alerts.isNotEmpty()) "CLEAR" else "POPULATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, MutedGray.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (alerts.isNotEmpty()) {
                                    controller.clearReceivedAlerts()
                                } else {
                                    controller.populateReceivedAlerts()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (alerts.isEmpty()) {
                // Standby Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Slow ambient search pulse
                        SonarPulse(
                            modifier = Modifier.fillMaxSize(),
                            color = SignalSafeTeal,
                            ringCount = 2,
                            durationMillis = 6000
                        )
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(SurfaceNearBlack)
                                .border(1.dp, SignalSafeTeal.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛰",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No alerts nearby",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceOffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your phone is on standby, ready to relay offline signals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, end = 32.dp)
                    )
                }
            } else {
                // Relayed alerts list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(alerts) { alert ->
                        val severityColor = when (alert.severity) {
                            "critical" -> SignalSosEmber
                            "warning", "warn" -> Color(0xFFFFB703) // Amber
                            else -> SignalSafeTeal
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceNearBlack),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header: Severity Badge + Location
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Severity Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .background(severityColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = alert.severity.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CanvasNearBlack,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Coarse Location (approx coordinates) - never precise details
                                    val approxLat = Math.round(alert.lat * 1000.0) / 1000.0
                                    val approxLon = Math.round(alert.lon * 1000.0) / 1000.0
                                    Text(
                                        text = String.format("GPS: %.3f°, %.3f°", approxLat, approxLon),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedGray
                                    )
                                }

                                // Secondary Info: Device ID Hash, Hops, Time Elapsed
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SOURCE ROUTE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedGray
                                        )
                                        Text(
                                            text = alert.origin_id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnSurfaceOffWhite
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val timeDiff = (System.currentTimeMillis() - alert.created_at) / 1000
                                        val timeText = if (timeDiff < 60) "Just now" else "${timeDiff / 60}m ago"
                                        
                                        Text(
                                            text = "HOPS: ${alert.hops} · RECEIVED: $timeText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedGray
                                        )
                                    }
                                }

                                HorizontalDivider(color = CanvasNearBlack, thickness = 1.dp)

                                // Active Relaying status dot
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SignalSafeTeal.copy(alpha = dotAlpha))
                                    )
                                    Text(
                                        text = "ACTIVELY RELAYING SIGNAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SignalSafeTeal,
                                        letterSpacing = 1.sp
                                    )
                                }
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
    // =========================================================================
    // SEAM FOR SYSTEMS TEAM:
    // This topology flow is driven by the controller interface flow.
    // When the real systems core is ready, this state flow can be replaced by the
    // systems team's Flow<MeshTopology> without requiring any layout changes.
    // =========================================================================
    val topology by controller.meshTopology.collectAsState()
    val meshState by controller.meshState.collectAsState()
    val view = LocalView.current

    val peerCount = when (val s = meshState) {
        is MeshState.Searching -> s.peers
        is MeshState.InFlight -> s.peers
        else -> 3
    }

    val hopCount = when (val s = meshState) {
        is MeshState.InFlight -> s.hops
        else -> 2
    }

    val isGatewayOnline = topology.nodes.any { it.isGateway }

    // Animations for idle pulse dots, dash line phase, and packet pulse progress
    val infiniteTransition = rememberInfiniteTransition(label = "meshAnimations")
    
    val nodePingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "nodePing"
    )

    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )

    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packetProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Screen Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "MESH TOPOLOGY GRAPH",
                style = MaterialTheme.typography.labelLarge,
                color = SignalSafeTeal,
                letterSpacing = 4.sp
            )
            Text(
                text = "Live peer-to-peer route map",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Live high-tech HUD summary bar (monospace numerals)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(SurfaceNearBlack)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "PEERS", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                Text(
                    text = String.format("%02d", peerCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceOffWhite
                )
            }
            Column {
                Text(text = "HOPS", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                Text(
                    text = String.format("%02d", hopCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceOffWhite
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "GATEWAY", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                Text(
                    text = if (isGatewayOnline) "ONLINE" else "STANDBY",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isGatewayOnline) SignalSafeTeal else SignalSosEmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Living interactive topology map inside BoxWithConstraints
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .clip(MaterialTheme.shapes.large)
                .background(SurfaceNearBlack)
                .border(1.dp, MutedGray.copy(alpha = 0.15f), MaterialTheme.shapes.large)
        ) {
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            // Dynamic coordinate translator helper
            fun getNodePosition(id: String): Offset {
                val relative = when (id) {
                    "victim" -> Offset(0.18f, 0.50f)
                    "peer_b" -> Offset(0.42f, 0.30f)
                    "peer_c" -> Offset(0.62f, 0.70f)
                    "gateway" -> Offset(0.85f, 0.50f)
                    else -> {
                        val hash = id.hashCode()
                        val rx = 0.25f + 0.5f * (Math.abs(hash % 100) / 100f)
                        val ry = 0.20f + 0.6f * (Math.abs((hash / 100) % 100) / 100f)
                        Offset(rx, ry)
                    }
                }
                return Offset(relative.x * widthPx, relative.y * heightPx)
            }

            // Canvas drawing layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw inactive topology background connections (flowing dashed lines)
                topology.edges.forEach { edge ->
                    val fromPos = getNodePosition(edge.fromNodeId)
                    val toPos = getNodePosition(edge.toNodeId)
                    
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), dashPhase)
                    drawLine(
                        color = MutedGray.copy(alpha = 0.15f),
                        start = fromPos,
                        end = toPos,
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // 2. Draw active packet propagation segments along hop path
                val hopPath = topology.activeHopPath
                if (hopPath.size >= 2) {
                    val numSegments = hopPath.size - 1
                    val segmentProgress = packetProgress * numSegments
                    val currentSegmentIdx = segmentProgress.toInt().coerceIn(0, numSegments - 1)
                    val t = segmentProgress - currentSegmentIdx

                    // Draw already-completed segments in solid ember
                    for (j in 0 until currentSegmentIdx) {
                        val fromPos = getNodePosition(hopPath[j])
                        val toPos = getNodePosition(hopPath[j + 1])
                        drawLine(
                            color = SignalSosEmber,
                            start = fromPos,
                            end = toPos,
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }

                    // Draw active segment partially lit up in solid ember
                    val activeFromPos = getNodePosition(hopPath[currentSegmentIdx])
                    val activeToPos = getNodePosition(hopPath[currentSegmentIdx + 1])
                    val packetPos = Offset(
                        x = activeFromPos.x + (activeToPos.x - activeFromPos.x) * t,
                        y = activeFromPos.y + (activeToPos.y - activeFromPos.y) * t
                    )

                    drawLine(
                        color = SignalSosEmber,
                        start = activeFromPos,
                        end = packetPos,
                        strokeWidth = 2.5.dp.toPx()
                    )

                    // Draw the bright glowing packet traveling along route
                    drawCircle(
                        color = SignalSosEmber,
                        radius = 8.dp.toPx(),
                        center = packetPos
                    )
                    drawCircle(
                        color = SignalSosEmber.copy(alpha = 0.3f),
                        radius = 18.dp.toPx(),
                        center = packetPos
                    )
                }

                // 3. Draw sonar pulses expanding around each node
                topology.nodes.forEach { node ->
                    val pos = getNodePosition(node.id)
                    val nodeColor = when {
                        node.isVictim -> SignalSosEmber
                        node.isGateway -> SignalSafeTeal
                        else -> OnSurfaceOffWhite
                    }

                    // expanding outer ring
                    val ringRadius = 26.dp.toPx() * nodePingProgress
                    val ringAlpha = (1f - nodePingProgress) * 0.3f
                    drawCircle(
                        color = nodeColor.copy(alpha = ringAlpha),
                        radius = 6.dp.toPx() + ringRadius,
                        center = pos,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // solid core circle
                    drawCircle(
                        color = nodeColor,
                        radius = 6.dp.toPx(),
                        center = pos
                    )
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.2f),
                        radius = 12.dp.toPx(),
                        center = pos
                    )
                }
            }

            // HTML-styled absolute overlay text labels for nodes
            topology.nodes.forEach { node ->
                val pos = getNodePosition(node.id)
                val isVictim = node.isVictim
                val isGateway = node.isGateway
                
                val labelWidth = 100.dp
                val densityVal = density.density
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = (pos.x / densityVal - 50).dp,
                            y = (pos.y / densityVal + 14).dp
                        )
                        .width(labelWidth)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceNearBlack.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.label.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = when {
                            isVictim -> SignalSosEmber
                            isGateway -> SignalSafeTeal
                            else -> MutedGray
                        },
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Done button to navigate back
        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onNavigate(Home)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceNearBlack),
            border = BorderStroke(1.dp, MutedGray.copy(alpha = 0.3f)),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "DISMISS MAP",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceOffWhite,
                letterSpacing = 1.5.sp
            )
        }

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Mesh", onReset = { controller.reset() })
    }
}
