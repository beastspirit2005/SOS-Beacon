package com.example.meshsosrelay

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.meshsosrelay.ui.fake.FakeSosViewModel
import com.example.meshsosrelay.ui.*
import com.example.meshsosrelay.theme.MotionTokens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.CanvasNearBlack
import com.example.meshsosrelay.theme.OnSurfaceOffWhite
import com.example.meshsosrelay.theme.SignalSafeTeal
import com.example.meshsosrelay.theme.SignalSosEmber
import com.example.meshsosrelay.theme.SurfaceNearBlack
import com.example.meshsosrelay.theme.MutedGray
import kotlin.math.roundToInt

@Composable
fun MainNavigation() {
  // Start stack at Home route
  val backStack = rememberNavBackStack(Home)
  val context = LocalContext.current

  // M-5 fix: inject real GpsLocationManager so SOS packets carry the device's actual location
  val gpsManager = remember { com.example.meshsosrelay.sensors.GpsLocationManager(context) }

  // Runtime Injection: Real MeshSosController with GPS support
  val controller = remember { com.example.meshsosrelay.mesh.MeshSosController(gpsLocationManager = gpsManager) }

  // Clean up the coroutine scope when composition leaves
  DisposableEffect(Unit) {
    onDispose { controller.destroy() }
  }

  val sharedCircleState = remember { SharedCircleState() }
  
  // Detect reduced motion settings (reuses context declared above)
  val isReducedMotion = remember(context) {
    val transitionScale = android.provider.Settings.Global.getFloat(
      context.contentResolver,
      android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
      1f
    )
    transitionScale == 0f
  }

  CompositionLocalProvider(LocalSharedCircleState provides sharedCircleState) {
    Box(modifier = Modifier.fillMaxSize()) {
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
          fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.85f, animationSpec = MotionTokens.SoftSpring) togetherWith
          fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.15f, animationSpec = tween(300))
        },
        entryProvider =
          entryProvider {
            // L-1 fix: removed duplicate entry<Main> — Home is the single entry point
            entry<Home> {
              HomeScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
            entry<Sending> {
              SendingScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
            entry<Status> {
              StatusScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
            entry<Delivered> {
              DeliveredScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
            entry<ReceivedAlerts> {
              ReceivedAlertsScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
            entry<MeshView> {
              MeshViewScreen(
                controller = controller,
                onNavigate = { navKey -> backStack.add(navKey) },
                modifier = Modifier.padding(16.dp)
              )
            }
          },
      )
      
      FloatingSharedCircle(sharedCircleState, isReducedMotion)
    }
  }
}

@Composable
fun FloatingSharedCircle(
    state: SharedCircleState,
    isReducedMotion: Boolean
) {
    if (!state.isVisible) return

    // Spring physics configuration
    val springSpec = remember { MotionTokens.PositionSpring }
    val dpSpringSpec = remember { MotionTokens.DpPositionSpring }
    val colorSpringSpec = remember { MotionTokens.ColorSpring }

    // Animate coordinates
    val animX by animateFloatAsState(targetValue = state.targetOffset.x, animationSpec = if (isReducedMotion) snap() else springSpec, label = "x")
    val animY by animateFloatAsState(targetValue = state.targetOffset.y, animationSpec = if (isReducedMotion) snap() else springSpec, label = "y")
    val animSize by animateDpAsState(targetValue = state.targetSize, animationSpec = if (isReducedMotion) snap() else dpSpringSpec, label = "size")
    val animColor by animateColorAsState(targetValue = state.targetColor, animationSpec = if (isReducedMotion) snap() else colorSpringSpec, label = "color")

    val scaleSpringSpec = remember { MotionTokens.StiffSpring }
    val animScale by animateFloatAsState(
        targetValue = state.scale,
        animationSpec = if (isReducedMotion) snap() else scaleSpringSpec,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(animX.roundToInt(), animY.roundToInt()) }
            .size(animSize)
            .scale(animScale)
            .clip(CircleShape)
            .background(if (state.currentScreen == "status") SurfaceNearBlack else animColor)
            .then(
                if (state.currentScreen == "status" || state.currentScreen == "relay") {
                    Modifier.border(2.dp, animColor, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when (state.currentScreen) {
            "home" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = CanvasNearBlack
                    )
                }
            }
            "relay" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RELAYING",
                        style = MaterialTheme.typography.titleMedium,
                        color = SignalSafeTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray,
                        letterSpacing = 1.sp
                    )
                }
            }
            "sending" -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    drawCircle(
                        color = animColor.copy(alpha = 0.05f),
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        color = animColor,
                        startAngle = -90f,
                        sweepAngle = 360f * state.sendingProgress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                val secondsLeft = (state.sendingProgress * 5f + 0.99f).toInt().coerceIn(1, 5)
                Text(
                    text = secondsLeft.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 72.sp),
                    color = OnSurfaceOffWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            "status" -> {
                if (state.isSearching) {
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
                        Text(
                            text = state.hopCount.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OnSurfaceOffWhite
                        )
                    }
                }
            }
            "delivered" -> {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    color = CanvasNearBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
