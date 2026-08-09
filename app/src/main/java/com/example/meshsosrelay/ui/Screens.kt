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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.example.meshsosrelay.*
import com.example.meshsosrelay.contract.*
import com.example.meshsosrelay.permissions.*
import com.example.meshsosrelay.sensors.*
import com.example.meshsosrelay.triggers.*
import com.example.meshsosrelay.theme.*
import com.example.meshsosrelay.ui.fake.FakeSosController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.composed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

enum class ElevationLevel {
    Canvas, Card, Raised
}

fun Modifier.premiumElevation(
    level: ElevationLevel,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
): Modifier = this.then(
    when (level) {
        ElevationLevel.Canvas -> Modifier.background(CanvasNearBlack)
        ElevationLevel.Card -> Modifier
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .background(SurfaceNearBlack, shape)
            .border(width = 0.5.dp, color = OnSurfaceOffWhite.copy(alpha = 0.08f), shape = shape)
        ElevationLevel.Raised -> Modifier
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .background(Color(0xFF1B1E23), shape)
            .border(width = 0.5.dp, color = OnSurfaceOffWhite.copy(alpha = 0.15f), shape = shape)
    }
)

fun Modifier.premiumPress(
    onClick: () -> Unit
): Modifier = this.composed {
    val isReducedMotion = isReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressAlpha"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}


class SharedCircleState {
    var isVisible by mutableStateOf(false)
    var targetOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var targetSize by mutableStateOf(160.dp)
    var targetColor by mutableStateOf(SignalSosEmber)
    
    var currentScreen by mutableStateOf("home")
    var sendingProgress by mutableStateOf(1f)
    var hopCount by mutableStateOf(0)
    var isSearching by mutableStateOf(true)
    var scale by mutableStateOf(1f)

    fun updateTarget(
        screen: String,
        offset: androidx.compose.ui.geometry.Offset,
        size: Dp,
        color: Color,
        progress: Float = 1f,
        hops: Int = 0,
        searching: Boolean = true,
        scale: Float = 1f
    ) {
        isVisible = true
        currentScreen = screen
        targetOffset = offset
        targetSize = size
        targetColor = color
        sendingProgress = progress
        hopCount = hops
        isSearching = searching
        this.scale = scale
    }
}

val LocalSharedCircleState = staticCompositionLocalOf<SharedCircleState> {
    error("No SharedCircleState provided")
}

@Composable
fun SharedCirclePlaceholder(
    screen: String,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    hops: Int = 0,
    searching: Boolean = true,
    scale: Float = 1f
) {
    val state = LocalSharedCircleState.current
    Box(
        modifier = modifier
            .size(size)
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    val position = coordinates.positionInWindow()
                    state.updateTarget(
                        screen = screen,
                        offset = position,
                        size = size,
                        color = color,
                        progress = progress,
                        hops = hops,
                        searching = searching,
                        scale = scale
                    )
                }
            }
    )
}


// Extension properties and methods to support fake/debug fields on the seam interface
val SosController.volunteerMode: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    get() = (this as? FakeSosController)?.volunteerMode ?: kotlinx.coroutines.flow.MutableStateFlow(false)

val SosController.deviceRole: kotlinx.coroutines.flow.MutableStateFlow<String>
    get() = (this as? FakeSosController)?.deviceRole ?: (this as? com.example.meshsosrelay.mesh.MeshSosController)?.deviceRole ?: kotlinx.coroutines.flow.MutableStateFlow("observer")

val SosController.soundEnabled: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    get() = (this as? FakeSosController)?.soundEnabled ?: kotlinx.coroutines.flow.MutableStateFlow(false)

val SosController.meshTopology: kotlinx.coroutines.flow.StateFlow<MeshTopology>
    get() = (this as? FakeSosController)?.meshTopology 
        ?: (this as? com.example.meshsosrelay.mesh.MeshSosController)?.meshTopology 
        ?: kotlinx.coroutines.flow.MutableStateFlow(MeshTopology(emptyList(), emptyList(), emptyList()))

val SosController.receivedAlerts: kotlinx.coroutines.flow.StateFlow<List<SosPacket>>
    get() = (this as? FakeSosController)?.receivedAlerts 
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())

fun SosController.cycleDeviceRole() {
    (this as? FakeSosController)?.cycleDeviceRole()
    (this as? com.example.meshsosrelay.mesh.MeshSosController)?.cycleDeviceRole()
}

fun SosController.reset() {
    (this as? FakeSosController)?.reset()
    (this as? com.example.meshsosrelay.mesh.MeshSosController)?.reset()
}

fun SosController.clearReceivedAlerts() {
    (this as? FakeSosController)?.clearReceivedAlerts()
}

fun SosController.populateReceivedAlerts() {
    (this as? FakeSosController)?.populateReceivedAlerts()
}

// Automatically determined by build variant type (true in debug, false in release/demo builds)
val IS_DEBUG_MENU_ENABLED = com.example.meshsosrelay.BuildConfig.DEBUG

@Composable
fun DebugNavigationFooter(
    onNavigate: (NavKey) -> Unit,
    currentRoute: String,
    onReset: (() -> Unit)? = null
) {
    if (!IS_DEBUG_MENU_ENABLED) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .premiumElevation(ElevationLevel.Card, MaterialTheme.shapes.medium)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .premiumPress { onNavigate(key) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (currentRoute == label) CanvasNearBlack else OnSurfaceOffWhite,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (onReset != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = SignalSosEmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumPress(onReset),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Reset Fake Controller Timeline", color = CanvasNearBlack, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AmbientNetworkBackground(
    peerCount: Int,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionEnabled()
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    // Slow, subtle drifting scale for concentric rings
    val radiusMultiplier = if (isReducedMotion) {
        1.0f
    } else {
        val multiplier by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 10000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radiusScale"
        )
        multiplier
    }
    
    // Slow rotation
    val rotationAngle = if (isReducedMotion) {
        0f
    } else {
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 120000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        angle
    }

    // Soft ripple when peer count changes
    val rippleAnim = remember { Animatable(0f) }
    var previousPeerCount by remember { mutableStateOf(peerCount) }
    
    LaunchedEffect(peerCount) {
        if (!isReducedMotion && peerCount != previousPeerCount) {
            previousPeerCount = peerCount
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = EaseOutQuad)
            )
        } else {
            previousPeerCount = peerCount
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = size.center
        val maxDim = size.maxDimension
        
        rotate(rotationAngle, center) {
            val lineSpacing = 48.dp.toPx()
            val gridColor = MutedGray.copy(alpha = 0.04f)
            val nodeColor = MutedGray.copy(alpha = 0.06f)
            val rayCount = 8
            
            // Concentric rings
            for (i in 1..8) {
                drawCircle(
                    color = gridColor,
                    radius = (i * lineSpacing) * radiusMultiplier,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Radial network rays
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

            // Faint nodes at intersection points
            for (i in 2..8 step 2) {
                for (j in 0 until rayCount) {
                    val angle = (360f / rayCount) * j
                    val angleRad = Math.toRadians(angle.toDouble())
                    val dist = (i * lineSpacing) * radiusMultiplier
                    val nx = center.x + (dist * Math.cos(angleRad)).toFloat()
                    val ny = center.y + (dist * Math.sin(angleRad)).toFloat()
                    
                    drawCircle(
                        color = nodeColor,
                        radius = 2.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }
        }

        // Draw soft ripple on peer change (outside rotation)
        if (rippleAnim.value > 0f && rippleAnim.value < 1f && !isReducedMotion) {
            drawCircle(
                color = SignalSafeTeal.copy(alpha = (1f - rippleAnim.value) * 0.12f),
                radius = rippleAnim.value * maxDim * 0.5f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun HomeScreen(
    controller: SosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val meshState by controller.meshState.collectAsState()
    val volunteerMode by controller.volunteerMode.collectAsState()
    val deviceRole by controller.deviceRole.collectAsState()
    val soundOn by controller.soundEnabled.collectAsState()

    var permissionGranted by remember { mutableStateOf(false) }

    HomeScreenContent(
        meshState = meshState,
        volunteerMode = volunteerMode,
        deviceRole = deviceRole,
        soundOn = soundOn,
        permissionGranted = permissionGranted,
        onPermissionGranted = { permissionGranted = true },
        onCycleDeviceRole = { controller.cycleDeviceRole() },
        onToggleSound = { controller.soundEnabled.value = !soundOn },
        onTriggerSos = { controller.trigger(SosDraft("critical", "Emergency manual SOS trigger from HomeScreen")) },
        onReset = { controller.reset() },
        onResetAll = {
            controller.reset()
            permissionGranted = false
        },
        onVolunteerModeChange = { isChecked ->
            controller.volunteerMode.value = isChecked
            if (isChecked) {
                controller.deviceRole.value = "relay"
            } else {
                controller.deviceRole.value = "observer"
            }
        },
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
private fun HomeScreenContent(
    meshState: MeshState,
    volunteerMode: Boolean,
    deviceRole: String,
    soundOn: Boolean,
    permissionGranted: Boolean,
    onPermissionGranted: () -> Unit,
    onCycleDeviceRole: () -> Unit,
    onToggleSound: () -> Unit,
    onTriggerSos: () -> Unit,
    onReset: () -> Unit,
    onResetAll: () -> Unit,
    onVolunteerModeChange: (Boolean) -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
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
        AmbientNetworkBackground(peerCount = peerCount)

        if (!permissionGranted) {
            // High-contrast permission state screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SonarPulse(
                        modifier = Modifier.fillMaxSize(),
                        color = SignalSosEmber,
                        ringCount = 2,
                        durationMillis = 4000
                    )
                    Text(
                        text = "📍",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "BLUETOOTH & LOCATION REQUIRED",
                    style = MaterialTheme.typography.titleMedium,
                    color = SignalSosEmber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Beacon operates completely offline by creating local radio bridges with nearby devices. We require location permissions to discover active peer frequencies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onPermissionGranted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalSosEmber),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(0.7f)
                        .semantics { contentDescription = "Grant mock Bluetooth and Location permissions" }
                ) {
                    Text(
                        text = "GRANT ACCESS",
                        style = MaterialTheme.typography.labelLarge,
                        color = CanvasNearBlack,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        } else {
            // Main Home Screen Layout (sequenced fade/slide in after circle settles)
            val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
            var contentVisible by remember { mutableStateOf(isPreview) }
            LaunchedEffect(Unit) {
                if (!isPreview) {
                    delay(150)
                    contentVisible = true
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { 30 },
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                // Header with settings toggle & live role chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    // Live Mesh Role Chip (calm, ambient indicator)
                    val roleLabel = when (deviceRole) {
                        "victim" -> "Victim Mode"
                        "relay" -> "Relaying"
                        "gateway" -> "Gateway — online"
                        else -> "Standby" // observer
                    }
                    val roleColor = when (deviceRole) {
                        "victim" -> SignalSosEmber
                        "gateway" -> SignalSafeTeal
                        "relay" -> OnSurfaceOffWhite
                        else -> MutedGray
                    }

                    val roleInfiniteTransition = rememberInfiniteTransition(label = "rolePulse")
                    val rolePulseAlpha by roleInfiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "rolePulseAlpha"
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(MaterialTheme.shapes.small)
                            .background(SurfaceNearBlack)
                            .border(1.dp, roleColor.copy(alpha = if (deviceRole == "relay") rolePulseAlpha else 0.3f), MaterialTheme.shapes.small)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onCycleDeviceRole()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(roleColor.copy(alpha = if (deviceRole == "relay") rolePulseAlpha else 1f))
                        )
                        Text(
                            text = roleLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = roleColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "BEACON",
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

                    // 2. Sound Toggle (Muted by default)
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onToggleSound()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .semantics { contentDescription = "Toggle emergency sonar audio alarm sound" }
                    ) {
                        Text(
                            text = if (soundOn) "🔊" else "🔇",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Status banner line
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
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                        slideOutVertically { height -> -height } + fadeOut()
                                    )
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                        slideOutVertically { height -> height } + fadeOut()
                                    )
                                }
                            },
                            label = "peerCountAnimation"
                        ) { targetCount ->
                            Text(
                                text = String.format("%02d", targetCount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
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

                // Central SOS trigger / Relaying state (gated behind shared transition layout placeholders)
                Box(
                    modifier = Modifier.size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (volunteerMode) {
                        SonarPulse(
                            modifier = Modifier.fillMaxSize(),
                            color = SignalSafeTeal,
                            ringCount = 3,
                            durationMillis = 4000
                        )
                        SharedCirclePlaceholder(
                            screen = "relay",
                            size = 160.dp,
                            color = SignalSafeTeal
                        )
                    } else {
                        SonarPulse(
                            modifier = Modifier.fillMaxSize(),
                            color = SignalSosEmber,
                            ringCount = 3,
                            durationMillis = 3000
                        )
                        val scale = remember { Animatable(1f) }
                        SharedCirclePlaceholder(
                            screen = "home",
                            size = 160.dp,
                            color = SignalSosEmber,
                            scale = scale.value,
                            modifier = Modifier
                                .scale(scale.value)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            scope.launch {
                                                scale.animateTo(0.85f, animationSpec = MotionTokens.StiffSpring)
                                            }
                                            tryAwaitRelease()
                                            scope.launch {
                                                scale.animateTo(1f, animationSpec = MotionTokens.SoftSpring)
                                            }
                                        },
                                        onTap = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onTriggerSos()
                                            onNavigate(Sending)
                                        }
                                    )
                                }
                                .semantics { contentDescription = "Double tap to trigger emergency manual SOS broadcast" }
                        )
                    }
                }

                // Secondary controls: I'm safe & Relayed alerts feed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {},
                        border = BorderStroke(1.dp, MutedGray.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceOffWhite),
                        modifier = Modifier
                            .height(48.dp)
                            .premiumPress {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onReset()
                            }
                            .semantics { contentDescription = "Mark status as safe and cancel broadcast" }
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
                            .premiumPress {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onNavigate(ReceivedAlerts)
                            }
                            .padding(8.dp)
                            .semantics { contentDescription = "Inspect offline relayed alerts feed" }
                    )
                }

                // Volunteer Mode Toggle Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .premiumElevation(ElevationLevel.Card, MaterialTheme.shapes.medium)
                        .premiumPress {
                            val isChecked = !volunteerMode
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onVolunteerModeChange(isChecked)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VOLUNTEER MODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalSafeTeal,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Relay others' emergency signals nearby.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceOffWhite
                                )
                            }
                            Switch(
                                checked = volunteerMode,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CanvasNearBlack,
                                    checkedTrackColor = SignalSafeTeal,
                                    uncheckedThumbColor = MutedGray,
                                    uncheckedTrackColor = SurfaceNearBlack
                                )
                            )
                        }
                        HorizontalDivider(color = CanvasNearBlack, thickness = 1.dp)
                        Text(
                            text = "🔒 No account or personal data is collected or shared to act as a relay.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MutedGray
                        )
                    }
                }
            }

            DebugNavigationFooter(
                onNavigate = onNavigate,
                currentRoute = "Home",
                onReset = onResetAll
            )
            }
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
    val isReducedMotion = isReducedMotionEnabled()
    var phase by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isReducedMotion) {
        if (isReducedMotion) return@LaunchedEffect
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000f
            lastTime = now
            
            val ratio = (remainingMillis / 5000f).coerceIn(0f, 1f)
            // Frequency scales smoothly from 0.8Hz (start) to 2.4Hz (end)
            val hz = 2.4f - 1.6f * ratio
            phase = (phase + dt * hz) % 1.0f
            
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val center = size.center
        val maxRadius = size.minDimension / 2
        
        for (i in 0 until ringCount) {
            val progress = if (isReducedMotion) {
                0.4f
            } else {
                (phase - (i.toFloat() / ringCount) + 1.0f) % 1.0f
            }
            
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
    controller: SosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveryState by controller.deliveryState.collectAsState()
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

    SendingScreenContent(
        remainingMillis = remainingMillis,
        onCancel = {
            controller.reset()
            onNavigate(Home)
        },
        onNavigate = onNavigate,
        onReset = { controller.reset() },
        modifier = modifier
    )
}

@Composable
private fun SendingScreenContent(
    remainingMillis: Int,
    onCancel: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionEnabled()
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var contentVisible by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) {
        if (!isPreview) {
            delay(150)
            contentVisible = true
        }
    }

    val progress = (remainingMillis / 5000f).coerceIn(0f, 1f)
    val intensity = 1f - progress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
    ) {
        // Subtle background intensifying tint near center/bottom
        if (!isReducedMotion && intensity > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                SignalSosEmber.copy(alpha = intensity * 0.12f),
                                Color.Transparent
                            ),
                            radius = 1600f
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { 30 },
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                    SharedCirclePlaceholder(
                        screen = "sending",
                        size = 180.dp,
                        color = SignalSosEmber,
                        progress = remainingMillis / 5000f
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
                        onClick = {},
                        border = BorderStroke(1.dp, SignalSosEmber.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalSosEmber),
                        modifier = Modifier
                            .height(54.dp)
                            .width(200.dp)
                            .premiumPress(onCancel)
                    ) {
                        Text(
                            text = "CANCEL",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Sending", onReset = onReset)
            }
        }
    }
}

@Composable
fun StatusScreen(
    controller: SosController,
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

    StatusScreenContent(
        meshState = meshState,
        topology = topology,
        onNavigate = onNavigate,
        onReset = { controller.reset() },
        modifier = modifier
    )
}

@Composable
private fun StatusScreenContent(
    meshState: MeshState,
    topology: MeshTopology,
    onNavigate: (NavKey) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val peerCount = when (val s = meshState) {
        is MeshState.Searching -> s.peers
        is MeshState.InFlight -> s.peers
        else -> 0
    }

    val hopCount = when (val s = meshState) {
        is MeshState.InFlight -> s.hops
        else -> 0
    }

    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var contentVisible by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) {
        if (!isPreview) {
            delay(150)
            contentVisible = true
        }
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { 30 },
        exit = fadeOut(animationSpec = tween(300))
    ) {
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

                // Inner circle placeholder linked to the shared overlay
                SharedCirclePlaceholder(
                    screen = "status",
                    size = 100.dp,
                    color = SignalSosEmber,
                    hops = hopCount,
                    searching = meshState is MeshState.Searching
                )
            }

            // Detailed Topology Path representation
            // Detailed Topology Path representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumElevation(ElevationLevel.Card, MaterialTheme.shapes.large)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ACTIVE ROUTING HOP PATH",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalSafeTeal,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (topology.activeHopPath.isEmpty()) {
                            Text(
                                text = "Establishing secure connection bridges...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray
                            )
                        } else {
                            topology.activeHopPath.forEachIndexed { idx, nodeName ->
                                val isVictim = nodeName == "victim" || nodeName == "node_A"
                                val isGateway = nodeName == "gateway" || nodeName == "node_D"
                                
                                val chipColor = when {
                                    isVictim -> SignalSosEmber
                                    isGateway -> SignalSafeTeal
                                    else -> OnSurfaceOffWhite
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                    )
                                    Text(
                                        text = nodeName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = chipColor,
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
                    .premiumPress {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onNavigate(MeshView)
                    }
                    .padding(8.dp)
            )

            DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Status", onReset = onReset)
        }
    }
}

@Composable
fun DeliveredScreen(
    controller: SosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val meshState by controller.meshState.collectAsState()

    DeliveredScreenContent(
        meshState = meshState,
        onNavigate = onNavigate,
        onReset = { controller.reset() },
        modifier = modifier
    )
}

@Composable
private fun DeliveredScreenContent(
    meshState: MeshState,
    onNavigate: (NavKey) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var contentVisible by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) {
        if (!isPreview) {
            delay(150)
            contentVisible = true
        }
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { 30 },
        exit = fadeOut(animationSpec = tween(300))
    ) {
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

                SharedCirclePlaceholder(
                    screen = "delivered",
                    size = 120.dp,
                    color = SignalSafeTeal
                )
            }

            // Summary Card
            // Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumElevation(ElevationLevel.Card, MaterialTheme.shapes.large)
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
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = SignalSafeTeal),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(0.6f)
                        .premiumPress {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onReset()
                            onNavigate(Home)
                        }
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
                        .premiumPress {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigate(MeshView)
                        }
                        .padding(8.dp)
                )
            }

            DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Delivered", onReset = onReset)
        }
    }
}

@Composable
fun ReceivedAlertsScreen(
    controller: SosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val alerts by controller.receivedAlerts.collectAsState()

    ReceivedAlertsScreenContent(
        alerts = alerts,
        onClearAlerts = { controller.clearReceivedAlerts() },
        onPopulateAlerts = { controller.populateReceivedAlerts() },
        onNavigate = onNavigate,
        onReset = { controller.reset() },
        modifier = modifier
    )
}

@Composable
private fun ReceivedAlertsScreenContent(
    alerts: List<SosPacket>,
    onClearAlerts: () -> Unit,
    onPopulateAlerts: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val isReducedMotion = isReducedMotionEnabled()
    val dotScale by if (isReducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    }

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
                                    onClearAlerts()
                                } else {
                                    onPopulateAlerts()
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
                // Legend wrapping FlowRow - ensures L# items never wrap mid-item and align correctly
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 4, 3, 2, 1).forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(getPriorityColor(level))
                            )
                            Text(
                                text = "L$level ${getPriorityLabel(level)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MutedGray,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Relayed alerts list sorted by highest-priority first
                val sortedAlerts = remember(alerts) {
                    alerts.sortedByDescending { it.priority }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(sortedAlerts) { index, alert ->
                        val priorityColor = getPriorityColor(alert.priority)
                        val priorityLabel = getPriorityLabel(alert.priority)

                        // Staggered entry animation: offset and alpha
                        val animProgress = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(Unit) {
                            if (isReducedMotion) {
                                animProgress.snapTo(1f)
                            } else {
                                delay(index * 60L)
                                animProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 400, easing = EaseOutQuad)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = animProgress.value
                                    translationY = (1f - animProgress.value) * 30.dp.toPx()
                                }
                                .premiumElevation(ElevationLevel.Card, MaterialTheme.shapes.large)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header: Priority Badge + Location
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Priority Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .background(priorityColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "L${alert.priority} · ${priorityLabel.uppercase()}",
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .graphicsLayer {
                                                scaleX = dotScale
                                                scaleY = dotScale
                                            }
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

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Relays", onReset = onReset)
    }
}

@Composable
fun MeshViewScreen(
    controller: SosController,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val topology by controller.meshTopology.collectAsState()
    val meshState by controller.meshState.collectAsState()

    MeshViewScreenContent(
        topology = topology,
        meshState = meshState,
        onNavigate = onNavigate,
        onReset = { controller.reset() },
        modifier = modifier
    )
}

@Composable
private fun MeshViewScreenContent(
    topology: MeshTopology,
    meshState: MeshState,
    onNavigate: (NavKey) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Column {
                Text(text = "PRIORITY", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                val highestPriority = topology.nodes.maxOfOrNull { it.priority } ?: 3
                val pColor = getPriorityColor(highestPriority)
                val pLabel = getPriorityLabel(highestPriority)
                Text(
                    text = "L$highestPriority · ${pLabel.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = pColor,
                    fontWeight = FontWeight.Bold
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

                    val originNode = topology.nodes.find { it.id == hopPath.firstOrNull() }
                    val packetPriority = originNode?.priority ?: 3
                    val packetColor = getPriorityColor(packetPriority)

                    // Draw already-completed segments in solid priority color
                    for (j in 0 until currentSegmentIdx) {
                        val fromPos = getNodePosition(hopPath[j])
                        val toPos = getNodePosition(hopPath[j + 1])
                        drawLine(
                            color = packetColor,
                            start = fromPos,
                            end = toPos,
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }

                    // Draw active segment partially lit up in solid priority color
                    val activeFromPos = getNodePosition(hopPath[currentSegmentIdx])
                    val activeToPos = getNodePosition(hopPath[currentSegmentIdx + 1])
                    val packetPos = Offset(
                        x = activeFromPos.x + (activeToPos.x - activeFromPos.x) * t,
                        y = activeFromPos.y + (activeToPos.y - activeFromPos.y) * t
                    )

                    drawLine(
                        color = packetColor,
                        start = activeFromPos,
                        end = packetPos,
                        strokeWidth = 2.5.dp.toPx()
                    )

                    // Draw the bright glowing packet traveling along route
                    drawCircle(
                        color = packetColor,
                        radius = 8.dp.toPx(),
                        center = packetPos
                    )
                    drawCircle(
                        color = packetColor.copy(alpha = 0.3f),
                        radius = 18.dp.toPx(),
                        center = packetPos
                    )
                }

                // 3. Draw sonar pulses expanding around each node
                topology.nodes.forEach { node ->
                    val pos = getNodePosition(node.id)
                    val nodeColor = when {
                        node.isVictim -> getPriorityColor(node.priority)
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
                            isVictim -> getPriorityColor(node.priority)
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

        DebugNavigationFooter(onNavigate = onNavigate, currentRoute = "Mesh", onReset = onReset)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HomeScreenPreview() {
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            HomeScreenContent(
                meshState = MeshState.Searching(peers = 2),
                volunteerMode = false,
                deviceRole = "observer",
                soundOn = false,
                permissionGranted = true,
                onPermissionGranted = {},
                onCycleDeviceRole = {},
                onToggleSound = {},
                onTriggerSos = {},
                onReset = {},
                onResetAll = {},
                onVolunteerModeChange = {},
                onNavigate = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SendingScreenPreview() {
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            SendingScreenContent(
                remainingMillis = 3500,
                onCancel = {},
                onNavigate = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun StatusScreenPreview() {
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            StatusScreenContent(
                meshState = MeshState.InFlight(peers = 3, hops = 2),
                topology = MeshTopology(
                    nodes = listOf(
                        TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                        TopoNode("peer_b", "Peer B", isRelay = true, nodeRole = "relay", priority = 4),
                        TopoNode("peer_c", "Peer C", isRelay = true, nodeRole = "relay", priority = 5),
                        TopoNode("gateway", "Gateway Phone", isGateway = true, nodeRole = "gateway", priority = 3)
                    ),
                    edges = listOf(
                        TopoEdge("victim", "peer_b"),
                        TopoEdge("peer_b", "peer_c"),
                        TopoEdge("peer_c", "gateway")
                    ),
                    activeHopPath = listOf("victim", "peer_b", "peer_c")
                ),
                onNavigate = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DeliveredScreenPreview() {
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            DeliveredScreenContent(
                meshState = MeshState.Delivered,
                onNavigate = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ReceivedAlertsScreenPreview() {
    val alerts = listOf(
        SosPacket(
            msg_id = "1",
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
            payload = "Severe impact detected.",
            sig = "fake_sig_1",
            priority = 5
        ),
        SosPacket(
            msg_id = "2",
            origin_id = "device_beta",
            created_at = System.currentTimeMillis() - 300000,
            lat = 12.9722,
            lon = 77.5950,
            acc = 15.0f,
            severity = "warn",
            confidence = 0.7f,
            trigger_type = "manual",
            payload = "Sprained ankle on trail.",
            ttl = 5,
            hops = 1,
            sig = "fake_sig_2",
            priority = 4
        )
    )
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            ReceivedAlertsScreenContent(
                alerts = alerts,
                onClearAlerts = {},
                onPopulateAlerts = {},
                onNavigate = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MeshViewScreenPreview() {
    val sharedCircleState = remember { SharedCircleState() }
    CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
        MeshSosRelayTheme {
            MeshViewScreenContent(
                meshState = MeshState.InFlight(peers = 3, hops = 2),
                topology = MeshTopology(
                    nodes = listOf(
                        TopoNode("victim", "My Device (Victim)", isVictim = true, nodeRole = "victim", priority = 3),
                        TopoNode("peer_b", "Peer B", isRelay = true, nodeRole = "relay", priority = 4),
                        TopoNode("peer_c", "Peer C", isRelay = true, nodeRole = "relay", priority = 5),
                        TopoNode("gateway", "Gateway Phone", isGateway = true, nodeRole = "gateway", priority = 3)
                    ),
                    edges = listOf(
                        TopoEdge("victim", "peer_b"),
                        TopoEdge("peer_b", "peer_c"),
                        TopoEdge("peer_c", "gateway")
                    ),
                    activeHopPath = listOf("victim", "peer_b", "peer_c")
                ),
                onNavigate = {},
                onReset = {}
            )
        }
    }
}
