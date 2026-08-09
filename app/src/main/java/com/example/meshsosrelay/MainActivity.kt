package com.example.meshsosrelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.meshsosrelay.theme.MeshSosRelayTheme
import com.example.meshsosrelay.triggers.SosForegroundService
import com.example.meshsosrelay.ui.SplashScreen
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // L-3 fix: Service start moved to Screens.kt (after permission is granted)
    // to prevent SecurityException on startup.

    enableEdgeToEdge()
    setContent {
      MeshSosRelayTheme {
        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
              delay(1500)
              showSplash = false
            }
            
            if (showSplash) {
              SplashScreen()
            } else {
              MainNavigation()
            }
          }
        }
      }
    }
  }
}
