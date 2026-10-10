package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserLocation(
  val latitude: Double,
  val longitude: Double,
  val altitude: Double = 0.0,
  val accuracy: Float = 0f,
  val bearing: Float = 0f,
  val speed: Float = 0f,
  val isRealGps: Boolean = false,
)

class UserLocationTracker(private val context: Context) {

  private val tag = "UserLocationTracker"

  private val _currentLocation = MutableStateFlow<UserLocation?>(null)
  val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

  private var fusedClient: FusedLocationProviderClient? = null
  private var fusedCallback: LocationCallback? = null
  private var locationManager: LocationManager? = null
  private var locationListener: LocationListener? = null
  private var isTracking = false

  init {
    try {
      fusedClient = LocationServices.getFusedLocationProviderClient(context)
      locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      checkLastKnownLocation()
    } catch (e: Exception) {
      Log.e(tag, "Error initializing location providers", e)
    }
  }

  fun hasPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  private fun checkLastKnownLocation() {
    if (!hasPermission()) return

    try {
      fusedClient?.lastLocation?.addOnSuccessListener { loc ->
        loc?.let { handleNewLocation(it) }
      }

      val mgr = locationManager
      if (mgr != null && _currentLocation.value == null) {
        val lastGps = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val best = lastGps ?: lastNet
        best?.let { handleNewLocation(it) }
      }
    } catch (e: Exception) {
      Log.w(tag, "Error checking last known location", e)
    }
  }

  @SuppressLint("MissingPermission")
  fun startTracking() {
    if (isTracking || !hasPermission()) return

    isTracking = true
    checkLastKnownLocation()

    try {
      val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
        .setMinUpdateIntervalMillis(1000L)
        .setMinUpdateDistanceMeters(0.5f)
        .build()

      fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { handleNewLocation(it) }
        }
      }

      fusedClient?.requestLocationUpdates(request, fusedCallback!!, Looper.getMainLooper())
    } catch (e: Exception) {
      Log.w(tag, "Fused location error, trying LocationManager", e)
      fallbackToLocationManager()
    }
  }

  @SuppressLint("MissingPermission")
  private fun fallbackToLocationManager() {
    try {
      locationListener = LocationListener { location ->
        handleNewLocation(location)
      }

      val mgr = locationManager ?: return
      if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        mgr.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          1500L,
          0.5f,
          locationListener!!,
          Looper.getMainLooper()
        )
      } else if (mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        mgr.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER,
          2000L,
          1f,
          locationListener!!,
          Looper.getMainLooper()
        )
      }
    } catch (e: Exception) {
      Log.e(tag, "LocationManager fallback failed", e)
    }
  }

  fun stopTracking() {
    isTracking = false
    try {
      fusedCallback?.let { fusedClient?.removeLocationUpdates(it) }
      locationListener?.let { locationManager?.removeUpdates(it) }
    } catch (e: Exception) {
      Log.w(tag, "Error stopping location updates", e)
    }
  }

  private fun handleNewLocation(location: Location) {
    _currentLocation.value = UserLocation(
      latitude = location.latitude,
      longitude = location.longitude,
      altitude = if (location.hasAltitude()) location.altitude else 0.0,
      accuracy = location.accuracy,
      bearing = location.bearing,
      speed = location.speed,
      isRealGps = true
    )
  }
}
