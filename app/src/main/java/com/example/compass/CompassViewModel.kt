package com.example.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.compass.sensors.CompassSensorData
import com.example.compass.sensors.OrientationSensorManager
import com.example.compass.service.BacktrackService
import com.example.compass.util.CompassCalculations
import com.example.data.local.AppDatabase
import com.example.data.local.CompassTrackPointEntity
import com.example.data.local.CompassWaypointEntity
import com.example.location.UserLocation
import com.example.location.UserLocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WaypointDisplayItem(
  val entity: CompassWaypointEntity,
  val distanceMeters: Double,
  val bearingDeg: Float,
  val relativeAngleDeg: Float, // -180..+180 relative to current user heading
  val elevationDifferenceMeters: Double, // Desnivel = altitudActual - altitudWaypoint
)

data class CompassUiState(
  val sensorData: CompassSensorData = CompassSensorData(),
  val userLocation: UserLocation? = null,
  val waypoints: List<WaypointDisplayItem> = emptyList(),
  val isRecordingBacktrack: Boolean = false,
  val breadcrumbPoints: List<CompassTrackPointEntity> = emptyList(),
  val totalBacktrackDistanceMeters: Double = 0.0,
  val selectedWaypoint: WaypointDisplayItem? = null,
  val isAddWaypointDialogOpen: Boolean = false,
  val isWaypointsListSheetOpen: Boolean = false,
  val is3DMode: Boolean = true, // Toggle entre Vista 3D (Perspectiva Inclinada) y 2D (Plana)
  val zoomRangeMeters: Float = 250f, // Escala de radar (50m a 2500m)
)

class CompassViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val compassDao = AppDatabase.getDatabase(appContext).compassDao()
  private val orientationManager = OrientationSensorManager(appContext)
  private val locationTracker = UserLocationTracker(appContext)

  val sensorData: StateFlow<CompassSensorData> = orientationManager.sensorData
  val userLocation: StateFlow<UserLocation?> = locationTracker.currentLocation
  val isRecordingBacktrack: StateFlow<Boolean> = BacktrackService.isRecordingFlow
  val liveTrackPoints: StateFlow<List<CompassTrackPointEntity>> = BacktrackService.liveTrackPoints

  private val _rawWaypoints = compassDao.getAllWaypoints().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private val _selectedWaypoint = MutableStateFlow<WaypointDisplayItem?>(null)
  val selectedWaypoint: StateFlow<WaypointDisplayItem?> = _selectedWaypoint.asStateFlow()

  private val _isAddWaypointDialogOpen = MutableStateFlow(false)
  val isAddWaypointDialogOpen: StateFlow<Boolean> = _isAddWaypointDialogOpen.asStateFlow()

  private val _isWaypointsListSheetOpen = MutableStateFlow(false)
  val isWaypointsListSheetOpen: StateFlow<Boolean> = _isWaypointsListSheetOpen.asStateFlow()

  private val _is3DMode = MutableStateFlow(true)
  val is3DMode: StateFlow<Boolean> = _is3DMode.asStateFlow()

  private val _zoomRangeMeters = MutableStateFlow(250f)
  val zoomRangeMeters: StateFlow<Float> = _zoomRangeMeters.asStateFlow()

  // Combined UI state
  val uiState: StateFlow<CompassUiState> = combine(
    sensorData,
    userLocation,
    _rawWaypoints,
    isRecordingBacktrack,
    liveTrackPoints,
    _selectedWaypoint,
    _isAddWaypointDialogOpen,
    _isWaypointsListSheetOpen,
    _is3DMode,
    _zoomRangeMeters
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val sensor = args[0] as CompassSensorData
    val loc = args[1] as? UserLocation
    @Suppress("UNCHECKED_CAST")
    val rawWp = args[2] as List<CompassWaypointEntity>
    val recording = args[3] as Boolean
    @Suppress("UNCHECKED_CAST")
    val tracks = args[4] as List<CompassTrackPointEntity>
    val selWp = args[5] as? WaypointDisplayItem
    val addDialog = args[6] as Boolean
    val listSheet = args[7] as Boolean
    val mode3D = args[8] as Boolean
    val zoom = args[9] as Float

    val currentAlt = sensor.barometricAltitudeMeters ?: (loc?.altitude ?: 0.0)

    val computedWaypoints = rawWp.map { entity ->
      val dist = if (loc != null) {
        CompassCalculations.calculateDistanceMeters(
          loc.latitude, loc.longitude,
          entity.latitude, entity.longitude
        )
      } else 0.0

      val bearing = if (loc != null) {
        CompassCalculations.calculateBearing(
          loc.latitude, loc.longitude,
          entity.latitude, entity.longitude
        )
      } else 0f

      val relative = CompassCalculations.calculateRelativeAngle(bearing, sensor.azimuth)
      val elevationDiff = currentAlt - entity.altitude

      WaypointDisplayItem(
        entity = entity,
        distanceMeters = dist,
        bearingDeg = bearing,
        relativeAngleDeg = relative,
        elevationDifferenceMeters = elevationDiff
      )
    }

    // Recompute selected waypoint with current distance and elevation difference
    val updatedSelected = if (selWp != null) {
      computedWaypoints.find { it.entity.id == selWp.entity.id }
    } else null

    // Compute total distance along track points
    var totalTrackDist = 0.0
    for (i in 0 until tracks.size - 1) {
      val p1 = tracks[i]
      val p2 = tracks[i + 1]
      totalTrackDist += CompassCalculations.calculateDistanceMeters(
        p1.latitude, p1.longitude,
        p2.latitude, p2.longitude
      )
    }

    CompassUiState(
      sensorData = sensor,
      userLocation = loc,
      waypoints = computedWaypoints,
      isRecordingBacktrack = recording,
      breadcrumbPoints = tracks,
      totalBacktrackDistanceMeters = totalTrackDist,
      selectedWaypoint = updatedSelected,
      isAddWaypointDialogOpen = addDialog,
      isWaypointsListSheetOpen = listSheet,
      is3DMode = mode3D,
      zoomRangeMeters = zoom
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    CompassUiState()
  )

  init {
    orientationManager.start()
    locationTracker.startTracking()
  }

  override fun onCleared() {
    super.onCleared()
    orientationManager.stop()
    locationTracker.stopTracking()
  }

  fun toggle3DMode() {
    _is3DMode.value = !_is3DMode.value
  }

  fun zoomIn() {
    val current = _zoomRangeMeters.value
    val next = when {
      current > 1000f -> 1000f
      current > 500f -> 500f
      current > 250f -> 250f
      current > 100f -> 100f
      current > 50f -> 50f
      else -> 50f
    }
    _zoomRangeMeters.value = next
  }

  fun zoomOut() {
    val current = _zoomRangeMeters.value
    val next = when {
      current < 100f -> 100f
      current < 250f -> 250f
      current < 500f -> 500f
      current < 1000f -> 1000f
      current < 2500f -> 2500f
      else -> 2500f
    }
    _zoomRangeMeters.value = next
  }

  fun setZoomRange(rangeMeters: Float) {
    _zoomRangeMeters.value = rangeMeters.coerceIn(50f, 2500f)
  }

  fun toggleBacktrack() {
    if (BacktrackService.isRecordingFlow.value) {
      BacktrackService.stopService(appContext)
    } else {
      BacktrackService.startService(appContext)
    }
  }

  fun openAddWaypointDialog() {
    _isAddWaypointDialogOpen.value = true
  }

  fun closeAddWaypointDialog() {
    _isAddWaypointDialogOpen.value = false
  }

  fun openWaypointsListSheet() {
    _isWaypointsListSheetOpen.value = true
  }

  fun closeWaypointsListSheet() {
    _isWaypointsListSheetOpen.value = false
  }

  fun selectWaypoint(item: WaypointDisplayItem?) {
    _selectedWaypoint.value = item
  }

  fun saveWaypoint(name: String, colorHex: String, iconType: String = "PIN") {
    val loc = userLocation.value ?: return
    val fusedAlt = sensorData.value.barometricAltitudeMeters ?: loc.altitude
    viewModelScope.launch {
      val entity = CompassWaypointEntity(
        name = name.ifBlank { "Punto ${System.currentTimeMillis() % 1000}" },
        latitude = loc.latitude,
        longitude = loc.longitude,
        altitude = fusedAlt,
        colorHex = colorHex,
        iconType = iconType,
        accuracy = loc.accuracy
      )
      val newId = compassDao.insertWaypoint(entity)
      closeAddWaypointDialog()
    }
  }

  fun deleteWaypoint(id: Long) {
    viewModelScope.launch {
      compassDao.deleteWaypointById(id)
      if (_selectedWaypoint.value?.entity?.id == id) {
        _selectedWaypoint.value = null
      }
    }
  }

  fun clearTrackHistory() {
    viewModelScope.launch {
      compassDao.clearAllTrackPoints()
    }
  }
}
