package com.example.meshsosrelay

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.meshsosrelay.ui.fake.FakeSosViewModel
import com.example.meshsosrelay.ui.*

@Composable
fun MainNavigation() {
  // Start stack at Home route
  val backStack = rememberNavBackStack(Home)
  val viewModel: FakeSosViewModel = viewModel()
  val controller = viewModel.controller

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          HomeScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Home> {
          HomeScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Sending> {
          SendingScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Status> {
          StatusScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Delivered> {
          DeliveredScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<ReceivedAlerts> {
          ReceivedAlertsScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<MeshView> {
          MeshViewScreen(
            controller = controller,
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      },
  )
}
