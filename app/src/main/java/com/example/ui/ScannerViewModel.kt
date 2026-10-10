package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import com.example.data.model.TacticalMarker
import com.example.data.repository.WarWalkingRepository
import com.example.location.UserLocation
import com.example.location.UserLocationTracker
import com.example.navigation.GeoPoint
import com.example.navigation.NavigationMode
import com.example.navigation.SignalRoute
import com.example.navigation.SignalRoutingEngine
import com.example.scanner.BleScanner
import com.example.scanner.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScanTab {
  WIFI,
  BLUETOOTH,
  RADAR
}

enum class RadarSubView {
  MAP_NAVIGATION,
  SONAR_RADAR
}

enum class SortMode {
  STRONGEST_SIGNAL,
  NAME_ALPHABETICAL,
  ESTIMATED_DISTANCE
}

data class ScannerUiState(
  val currentTab: ScanTab = ScanTab.RADAR,
  val radarSubView: RadarSubView = RadarSubView.MAP_NAVIGATION,
  val isScanning: Boolean = false,
  val searchQuery: String = "",
  val sortMode: SortMode = SortMode.STRONGEST_SIGNAL,
  val selectedDevice: ScannedDevice? = null,
  val targetDevice: ScannedDevice? = null,
  val targetMarker: TacticalMarker? = null,
  val inspectedMarker: TacticalMarker? = null,
  val navigationMode: NavigationMode = NavigationMode.STREETS,
  val hasPermissions: Boolean = false,
  val isWifiHardwareEnabled: Boolean = true,
  val isBluetoothHardwareEnabled: Boolean = true,
  val infoMessage: String? = null,
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

  val wifiScanner = WifiScanner(application)
  val bleScanner = BleScanner(application)
  val locationTracker = UserLocationTracker(application)

  private val database = AppDatabase.getDatabase(application)
  val warWalkingRepository = WarWalkingRepository(database.scannedNetworkDao())

  private val _uiState = MutableStateFlow(ScannerUiState())
  val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

  val userLocation: StateFlow<UserLocation?> = locationTracker.currentLocation

  private var scanLoopJob: Job? = null

  // Real Wi-Fi and BLE lists with filtering & sorting for tab lists
  val visibleDevices: StateFlow<List<ScannedDevice>> = combine(
    wifiScanner.wifiDevices,
    bleScanner.bleDevices,
    _uiState
  ) { wifiList, bleList, state ->
    val baseList = when (state.currentTab) {
      ScanTab.WIFI -> wifiList
      ScanTab.BLUETOOTH -> bleList
      ScanTab.RADAR -> wifiList + bleList
    }

    val filtered = if (state.searchQuery.isBlank()) {
      baseList
    } else {
      val query = state.searchQuery.trim().lowercase()
      baseList.filter {
        it.displayName.lowercase().contains(query) ||
            it.macAddress.lowercase().contains(query) ||
            (it.capabilities?.lowercase()?.contains(query) == true)
      }
    }

    when (state.sortMode) {
      SortMode.STRONGEST_SIGNAL -> filtered.sortedByDescending { it.rssi }
      SortMode.NAME_ALPHABETICAL -> filtered.sortedBy { it.displayName.lowercase() }
      SortMode.ESTIMATED_DISTANCE -> filtered.sortedBy { it.estimatedDistanceMeters }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val wifiCount: StateFlow<Int> = wifiScanner.wifiDevices
    .combine(MutableStateFlow(Unit)) { list, _ -> list.size }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val bleCount: StateFlow<Int> = bleScanner.bleDevices
    .combine(MutableStateFlow(Unit)) { list, _ -> list.size }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val savedNetworksCount: StateFlow<Int> = warWalkingRepository.savedCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  // Combined Tactical Map Markers: Online (Verde) vs Offline Histórico (Gris)
  val tacticalMarkers: StateFlow<List<TacticalMarker>> = combine(
    warWalkingRepository.allSavedNetworks,
    wifiScanner.wifiDevices,
    bleScanner.bleDevices,
    userLocation
  ) { savedList, liveWifi, liveBle, loc ->
    val liveMap = mutableMapOf<String, ScannedDevice>()
    liveWifi.forEach { liveMap[it.macAddress.lowercase()] = it }
    liveBle.forEach { liveMap[it.macAddress.lowercase()] = it }

    val markers = mutableListOf<TacticalMarker>()
    val seenBssids = mutableSetOf<String>()

    // 1. Online Markers (Verde en vivo)
    liveMap.values.forEach { liveDev ->
      val bssidKey = liveDev.macAddress.lowercase()
      seenBssids.add(bssidKey)

      val coords = if (loc != null) {
        SignalRoutingEngine.calculateTargetCoordinates(loc.latitude, loc.longitude, liveDev)
      } else {
        GeoPoint(0.0, 0.0)
      }

      val dist = if (loc != null && coords.lat != 0.0) {
        liveDev.estimatedDistanceMeters
      } else 0.0

      markers.add(
        TacticalMarker(
          id = liveDev.id,
          name = liveDev.displayName,
          bssidMac = liveDev.macAddress,
          lat = coords.lat,
          lon = coords.lon,
          rssi = liveDev.rssi,
          type = liveDev.type,
          isOnline = true,
          lastSeenTimestamp = liveDev.timestamp,
          distanceMeters = dist,
          frequency = liveDev.frequency,
          capabilities = liveDev.capabilities
        )
      )
    }

    // 2. Offline / Historical Markers (Gris) from Room Database
    savedList.forEach { saved ->
      val bssidKey = saved.bssidMac.lowercase()
      if (!seenBssids.contains(bssidKey)) {
        val dist = if (loc != null) {
          SignalRoutingEngine.calculateDistance(loc.latitude, loc.longitude, saved.latitud, saved.longitud)
        } else 0.0

        markers.add(
          TacticalMarker(
            id = "saved_${saved.id}",
            name = saved.ssid,
            bssidMac = saved.bssidMac,
            lat = saved.latitud,
            lon = saved.longitud,
            rssi = saved.ultimoRssi,
            type = if (saved.tipo == "WIFI") DeviceType.WIFI else DeviceType.BLUETOOTH_LE,
            isOnline = false,
            lastSeenTimestamp = saved.fechaRegistro,
            distanceMeters = dist,
            frequency = saved.frecuencia,
            capabilities = saved.capacidades
          )
        )
      }
    }

    markers
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dynamic Route calculation for target marker (Online or Offline)
  val currentRoute: StateFlow<SignalRoute?> = combine(
    userLocation,
    _uiState
  ) { loc, state ->
    if (loc == null) return@combine null

    val marker = state.targetMarker
    if (marker != null) {
      return@combine SignalRoutingEngine.calculateRouteForMarker(
        userLat = loc.latitude,
        userLon = loc.longitude,
        marker = marker,
        mode = state.navigationMode
      )
    }

    val targetDev = state.targetDevice ?: return@combine null
    val targetCoords = SignalRoutingEngine.calculateTargetCoordinates(loc.latitude, loc.longitude, targetDev)
    SignalRoutingEngine.calculateRoute(
      userLat = loc.latitude,
      userLon = loc.longitude,
      targetLat = targetCoords.lat,
      targetLon = targetCoords.lon,
      device = targetDev,
      mode = state.navigationMode
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  init {
    updateHardwareAndPermissionState()
    locationTracker.startTracking()
    observeRealSensorRssiUpdatesAndAutoSave()
  }

  /**
   * Listens for incoming real sensor frames to update target RSSI in real time
   * and automatically saves each discovered network to the Room database with GPS coordinates (War-walking).
   */
  private fun observeRealSensorRssiUpdatesAndAutoSave() {
    viewModelScope.launch {
      wifiScanner.wifiDevices.collect { realWifiList ->
        val loc = userLocation.value
        // War-walking auto-save: persist every discovered network with instant GPS coordinates
        if (loc != null) {
          realWifiList.forEach { dev ->
            warWalkingRepository.saveOrUpdateDevice(dev, loc.latitude, loc.longitude)
          }
        }

        // Live target update
        val currentTarget = _uiState.value.targetDevice
        if (currentTarget != null && currentTarget.type == DeviceType.WIFI) {
          val matching = realWifiList.find { it.macAddress.equals(currentTarget.macAddress, ignoreCase = true) }
          if (matching != null && matching.rssi != currentTarget.rssi) {
            _uiState.value = _uiState.value.copy(targetDevice = matching)
          }
        }

        val currentMarker = _uiState.value.targetMarker
        if (currentMarker != null && currentMarker.isOnline && currentMarker.type == DeviceType.WIFI) {
          val matching = realWifiList.find { it.macAddress.equals(currentMarker.bssidMac, ignoreCase = true) }
          if (matching != null && matching.rssi != currentMarker.rssi) {
            _uiState.value = _uiState.value.copy(
              targetMarker = currentMarker.copy(
                rssi = matching.rssi,
                distanceMeters = matching.estimatedDistanceMeters
              )
            )
          }
        }
      }
    }

    viewModelScope.launch {
      bleScanner.bleDevices.collect { realBleList ->
        val loc = userLocation.value
        // War-walking auto-save for BLE devices
        if (loc != null) {
          realBleList.forEach { dev ->
            warWalkingRepository.saveOrUpdateDevice(dev, loc.latitude, loc.longitude)
          }
        }

        val currentTarget = _uiState.value.targetDevice
        if (currentTarget != null && currentTarget.type == DeviceType.BLUETOOTH_LE) {
          val matching = realBleList.find { it.macAddress.equals(currentTarget.macAddress, ignoreCase = true) }
          if (matching != null && matching.rssi != currentTarget.rssi) {
            _uiState.value = _uiState.value.copy(targetDevice = matching)
          }
        }

        val currentMarker = _uiState.value.targetMarker
        if (currentMarker != null && currentMarker.isOnline && currentMarker.type == DeviceType.BLUETOOTH_LE) {
          val matching = realBleList.find { it.macAddress.equals(currentMarker.bssidMac, ignoreCase = true) }
          if (matching != null && matching.rssi != currentMarker.rssi) {
            _uiState.value = _uiState.value.copy(
              targetMarker = currentMarker.copy(
                rssi = matching.rssi,
                distanceMeters = matching.estimatedDistanceMeters
              )
            )
          }
        }
      }
    }
  }

  fun updateHardwareAndPermissionState() {
    val wifiOk = wifiScanner.hasPermission()
    val bleOk = bleScanner.hasPermission()
    val locOk = locationTracker.hasPermission()
    val allGranted = wifiOk && bleOk && locOk

    val wifiOn = wifiScanner.isWifiEnabled()
    val btOn = bleScanner.isBluetoothEnabled()

    _uiState.value = _uiState.value.copy(
      hasPermissions = allGranted,
      isWifiHardwareEnabled = wifiOn,
      isBluetoothHardwareEnabled = btOn,
      infoMessage = when {
        !allGranted -> "Concede los permisos requeridos para activar los sensores de radio y GPS."
        !wifiOn && !btOn -> "Activa el Wi-Fi y Bluetooth para comenzar el escaneo."
        !wifiOn -> "El Wi-Fi está desactivado en el teléfono."
        !btOn -> "El Bluetooth está desactivado en el teléfono."
        else -> null
      }
    )

    if (locOk) {
      locationTracker.startTracking()
    }
  }

  fun onPermissionsResult(granted: Boolean) {
    updateHardwareAndPermissionState()
    if (granted) {
      locationTracker.startTracking()
      startScanning()
    }
  }

  fun selectTab(tab: ScanTab) {
    _uiState.value = _uiState.value.copy(currentTab = tab)
  }

  fun setRadarSubView(subView: RadarSubView) {
    _uiState.value = _uiState.value.copy(radarSubView = subView)
  }

  fun setSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun setSortMode(mode: SortMode) {
    _uiState.value = _uiState.value.copy(sortMode = mode)
  }

  fun selectDevice(device: ScannedDevice?) {
    _uiState.value = _uiState.value.copy(selectedDevice = device)
  }

  fun inspectMarker(marker: TacticalMarker?) {
    _uiState.value = _uiState.value.copy(inspectedMarker = marker)
  }

  fun navigateToMarker(marker: TacticalMarker) {
    _uiState.value = _uiState.value.copy(
      targetMarker = marker,
      inspectedMarker = marker,
      currentTab = ScanTab.RADAR,
      radarSubView = RadarSubView.MAP_NAVIGATION,
      infoMessage = "Navegando hacia '${marker.name}' (${if (marker.isOnline) "En vivo" else "Histórico"})"
    )
  }

  fun setTargetDevice(device: ScannedDevice?) {
    val marker = device?.let {
      val loc = userLocation.value
      val coords = if (loc != null) SignalRoutingEngine.calculateTargetCoordinates(loc.latitude, loc.longitude, it) else GeoPoint(0.0, 0.0)
      TacticalMarker(
        id = it.id,
        name = it.displayName,
        bssidMac = it.macAddress,
        lat = coords.lat,
        lon = coords.lon,
        rssi = it.rssi,
        type = it.type,
        isOnline = true,
        lastSeenTimestamp = it.timestamp,
        distanceMeters = it.estimatedDistanceMeters,
        frequency = it.frequency,
        capabilities = it.capabilities
      )
    }

    _uiState.value = _uiState.value.copy(
      targetDevice = device,
      targetMarker = marker,
      inspectedMarker = marker,
      currentTab = ScanTab.RADAR,
      radarSubView = RadarSubView.MAP_NAVIGATION,
      infoMessage = if (device != null) "Navegando hacia '${device.displayName}'" else null
    )
  }

  fun setNavigationMode(mode: NavigationMode) {
    _uiState.value = _uiState.value.copy(navigationMode = mode)
  }

  fun toggleScanning() {
    if (_uiState.value.isScanning) {
      stopScanning()
    } else {
      startScanning()
    }
  }

  fun startScanning() {
    updateHardwareAndPermissionState()

    val wifiStarted = wifiScanner.startScan()
    val bleStarted = bleScanner.startScan()
    locationTracker.startTracking()

    _uiState.value = _uiState.value.copy(
      isScanning = true,
      infoMessage = "Escaneando señales de radio y guardando en base de datos local..."
    )

    scanLoopJob?.cancel()
    scanLoopJob = viewModelScope.launch {
      while (_uiState.value.isScanning) {
        delay(12000)
        if (_uiState.value.isScanning) {
          wifiScanner.startScan()
        }
      }
    }
  }

  fun stopScanning() {
    scanLoopJob?.cancel()
    scanLoopJob = null
    wifiScanner.stopScan()
    bleScanner.stopScan()
    _uiState.value = _uiState.value.copy(
      isScanning = false,
      infoMessage = "Escaneo pausado"
    )
  }

  fun clearAll() {
    bleScanner.clear()
    wifiScanner.clear()
    _uiState.value = _uiState.value.copy(
      targetDevice = null,
      targetMarker = null,
      selectedDevice = null,
      inspectedMarker = null,
      infoMessage = "Dispositivos en vivo limpiados"
    )
  }

  override fun onCleared() {
    super.onCleared()
    stopScanning()
    locationTracker.stopTracking()
  }
}
