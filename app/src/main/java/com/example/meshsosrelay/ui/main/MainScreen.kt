package com.example.meshsosrelay.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.example.meshsosrelay.theme.MeshSosRelayTheme
import com.example.meshsosrelay.ui.ThemePreviewScreen

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  ThemePreviewScreen(modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MeshSosRelayTheme { 
    ThemePreviewScreen()
  }
}
