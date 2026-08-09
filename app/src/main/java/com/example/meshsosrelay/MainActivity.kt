package com.example.meshsosrelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.meshsosrelay.theme.MeshSosRelayTheme
import com.example.meshsosrelay.ui.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MeshSosRelayTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .systemBarsPadding()
              .imePadding()
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
