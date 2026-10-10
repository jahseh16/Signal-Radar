package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScannerMainScreen
import com.example.ui.ScannerViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.screens.CameraArScreen
import com.example.ui.screens.Compass3DScreen
import com.example.ui.screens.TacticalToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalBackground

enum class AppScreen {
  DASHBOARD,
  SCANNER,
  COMPASS_3D,
  CAMERA_AR,
  TACTICAL_TOOLS
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ScannerViewModel = viewModel()
        val userLocation by viewModel.userLocation.collectAsState()
        val wifiCount by viewModel.wifiCount.collectAsState()
        val bleCount by viewModel.bleCount.collectAsState()
        val savedCount by viewModel.savedNetworksCount.collectAsState()

        var currentScreen by rememberSaveable { mutableStateOf(AppScreen.DASHBOARD) }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = TacticalBackground
        ) {
          when (currentScreen) {
            AppScreen.DASHBOARD -> {
              DashboardScreen(
                onNavigateToScanner = { currentScreen = AppScreen.SCANNER },
                onNavigateToCompass = { currentScreen = AppScreen.COMPASS_3D },
                onNavigateToCameraAr = { currentScreen = AppScreen.CAMERA_AR },
                onNavigateToTacticalTools = { currentScreen = AppScreen.TACTICAL_TOOLS },
                savedNetworksCount = savedCount,
                wifiCount = wifiCount,
                bleCount = bleCount
              )
            }
            AppScreen.SCANNER -> {
              ScannerMainScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
              )
            }
            AppScreen.COMPASS_3D -> {
              Compass3DScreen(
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                userLocation = userLocation
              )
            }
            AppScreen.CAMERA_AR -> {
              CameraArScreen(
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
              )
            }
            AppScreen.TACTICAL_TOOLS -> {
              TacticalToolsScreen(
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
              )
            }
          }
        }
      }
    }
  }
}
