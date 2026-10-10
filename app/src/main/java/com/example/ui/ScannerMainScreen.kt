package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeviceCard
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.PermissionsBanner
import com.example.ui.components.RadarView
import com.example.ui.components.SignalMapView
import com.example.ui.components.TopControlPanel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi
import com.example.ui.theme.TechDarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerMainScreen(
  viewModel: ScannerViewModel,
  onNavigateBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val visibleDevices by viewModel.visibleDevices.collectAsState()
  val tacticalMarkers by viewModel.tacticalMarkers.collectAsState()
  val wifiCount by viewModel.wifiCount.collectAsState()
  val bleCount by viewModel.bleCount.collectAsState()
  val userLocation by viewModel.userLocation.collectAsState()
  val currentRoute by viewModel.currentRoute.collectAsState()
  val savedCount by viewModel.savedNetworksCount.collectAsState()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  BackHandler {
    if (uiState.selectedDevice != null) {
      viewModel.selectDevice(null)
    } else if (uiState.inspectedMarker != null) {
      viewModel.inspectMarker(null)
    } else {
      onNavigateBack?.invoke()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = TechDarkBackground,
    topBar = {
      TopControlPanel(
        currentTab = uiState.currentTab,
        isScanning = uiState.isScanning,
        wifiCount = wifiCount,
        bleCount = bleCount,
        searchQuery = uiState.searchQuery,
        sortMode = uiState.sortMode,
        onTabSelected = { viewModel.selectTab(it) },
        onToggleScan = { viewModel.toggleScanning() },
        onClear = { viewModel.clearAll() },
        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
        onSortModeChanged = { viewModel.setSortMode(it) },
        onNavigateBack = onNavigateBack
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Permission Banner (if not yet granted)
      PermissionsBanner(
        hasPermissions = uiState.hasPermissions,
        onPermissionsUpdated = { viewModel.onPermissionsResult(it) }
      )

      // Optional status banner
      AnimatedVisibility(visible = uiState.infoMessage != null) {
        uiState.infoMessage?.let { msg ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // MAIN CONTENT: MAP/RADAR VIEW vs LIST VIEW (RECYCLERVIEW / LAZYCOLUMN)
      when (uiState.currentTab) {
        ScanTab.RADAR -> {
          Column(modifier = Modifier.fillMaxSize()) {
            // View Switcher: Mapa Táctico Oscuro vs Sonar Radar 360°
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              FilterChip(
                selected = uiState.radarSubView == RadarSubView.MAP_NAVIGATION,
                onClick = { viewModel.setRadarSubView(RadarSubView.MAP_NAVIGATION) },
                label = { Text("Mapa Táctico Oscuro (${tacticalMarkers.size})") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                  selectedLabelColor = CyanPrimary,
                  selectedLeadingIconColor = CyanPrimary
                ),
                modifier = Modifier.testTag("chip_view_map")
              )

              FilterChip(
                selected = uiState.radarSubView == RadarSubView.SONAR_RADAR,
                onClick = { viewModel.setRadarSubView(RadarSubView.SONAR_RADAR) },
                label = { Text("Sonar 360°") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldWifi.copy(alpha = 0.2f),
                  selectedLabelColor = EmeraldWifi,
                  selectedLeadingIconColor = EmeraldWifi
                ),
                modifier = Modifier.testTag("chip_view_sonar")
              )
            }

            if (uiState.radarSubView == RadarSubView.MAP_NAVIGATION) {
              // Tactical Dark Map with Online Green & Offline Gray markers, and route polyline!
              SignalMapView(
                userLocation = userLocation,
                markers = tacticalMarkers,
                targetMarker = uiState.targetMarker,
                inspectedMarker = uiState.inspectedMarker,
                route = currentRoute,
                navigationMode = uiState.navigationMode,
                onMarkerSelected = { viewModel.inspectMarker(it) },
                onNavigateToMarker = { viewModel.navigateToMarker(it) },
                onModeChanged = { viewModel.setNavigationMode(it) },
                modifier = Modifier.weight(1f)
              )
            } else {
              // Sonar Radar 360° View
              RadarView(
                devices = visibleDevices,
                selectedDevice = uiState.selectedDevice ?: uiState.targetDevice,
                onDeviceSelected = {
                  viewModel.selectDevice(it)
                  viewModel.setTargetDevice(it)
                },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        ScanTab.WIFI, ScanTab.BLUETOOTH -> {
          if (visibleDevices.isEmpty()) {
            EmptyStateView(
              tab = uiState.currentTab,
              isScanning = uiState.isScanning,
              onStartScan = { viewModel.startScanning() },
              modifier = Modifier.weight(1f)
            )
          } else {
            // Header stats
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (uiState.currentTab == ScanTab.WIFI) {
                  "Redes Wi-Fi Detectadas (${visibleDevices.size})"
                } else {
                  "Dispositivos BLE Detectados (${visibleDevices.size})"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Text(
                text = "Orden: ${
                  when (uiState.sortMode) {
                    SortMode.STRONGEST_SIGNAL -> "Señal dBm"
                    SortMode.NAME_ALPHABETICAL -> "Nombre"
                    SortMode.ESTIMATED_DISTANCE -> "Distancia"
                  }
                }",
                style = MaterialTheme.typography.labelSmall,
                color = CyanPrimary
              )
            }

            // Real hardware list of detected devices
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("devices_list")
            ) {
              items(
                items = visibleDevices,
                key = { it.id }
              ) { device ->
                DeviceCard(
                  device = device,
                  onClick = { viewModel.selectDevice(device) },
                  onNavigateToSignal = {
                    viewModel.setTargetDevice(device)
                  }
                )
              }
              item {
                Spacer(modifier = Modifier.height(24.dp))
              }
            }
          }
        }
      }
    }

    // Detail Bottom Sheet
    uiState.selectedDevice?.let { device ->
      DeviceDetailSheet(
        device = device,
        sheetState = sheetState,
        onDismiss = { viewModel.selectDevice(null) },
        onNavigateToSignal = { target ->
          viewModel.setTargetDevice(target)
        }
      )
    }
  }
}

@Composable
private fun EmptyStateView(
  tab: ScanTab,
  isScanning: Boolean,
  onStartScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isWifi = tab == ScanTab.WIFI
  val icon = if (isWifi) Icons.Default.WifiFind else Icons.Default.Bluetooth
  val title = if (isWifi) "No se detectaron redes Wi-Fi" else "No se detectaron dispositivos BLE"
  val desc = if (isScanning) {
    "El chip de radio está escaneando el entorno físico... Esperando señales reales."
  } else {
    "Presiona 'Escanear' o asegúrate de que el ${if (isWifi) "Wi-Fi" else "Bluetooth"} y la ubicación estén encendidos en el teléfono."
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isWifi) EmeraldWifi else CyanPrimary,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = desc,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = onStartScan,
        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isScanning) "Reescanear" else "Escanear Ahora",
          color = MaterialTheme.colorScheme.surface,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
