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

@Composable
fun MainNavigation() {
  // Start stack at Home route
  val backStack = rememberNavBackStack(Home)
  
  // Runtime Injection: Providing the real MeshSosController instead of FakeSosController
  val controller = remember { com.example.meshsosrelay.mesh.MeshSosController() }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    transitionSpec = {
      fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.85f, animationSpec = MotionTokens.SoftSpring) togetherWith
      fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.15f, animationSpec = tween(300))
    },
    entryProvider =
      entryProvider {
        entry<Main> {
          HomeScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.padding(16.dp)
          )
        }
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
}
