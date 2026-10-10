package com.example.compass.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.CompassTrackPointEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BacktrackService : Service() {

  companion object {
    private const val TAG = "BacktrackService"
    const val CHANNEL_ID = "signal_radar_backtrack_channel"
    const val NOTIFICATION_ID = 8842

    const val ACTION_START = "ACTION_START_BACKTRACK"
    const val ACTION_STOP = "ACTION_STOP_BACKTRACK"

    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

    private val _liveTrackPoints = MutableStateFlow<List<CompassTrackPointEntity>>(emptyList())
    val liveTrackPoints: StateFlow<List<CompassTrackPointEntity>> = _liveTrackPoints.asStateFlow()

    private val _currentSessionKey = MutableStateFlow<String?>(null)
    val currentSessionKey: StateFlow<String?> = _currentSessionKey.asStateFlow()

    fun startService(context: Context) {
      val intent = Intent(context, BacktrackService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, BacktrackService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var fusedClient: FusedLocationProviderClient? = null
  private var locationCallback: LocationCallback? = null
  private var activeSessionId: String = ""
  private val kalmanFilter = com.example.compass.util.GpsKalmanFilter()
  private var lastRecordedLat = 0.0
  private var lastRecordedLon = 0.0

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    fusedClient = LocationServices.getFusedLocationProviderClient(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        startRecording()
      }
      ACTION_STOP -> {
        stopRecording()
        stopSelf()
      }
    }
    return START_STICKY
  }

  private fun startRecording() {
    activeSessionId = "SESSION_${System.currentTimeMillis()}"
    _currentSessionKey.value = activeSessionId
    _isRecordingFlow.value = true
    _liveTrackPoints.value = emptyList()
    kalmanFilter.reset()
    lastRecordedLat = 0.0
    lastRecordedLon = 0.0

    startForeground(NOTIFICATION_ID, buildNotification("Grabando ruta GPS (Migas de Pan)..."))

    startLocationUpdates()
  }

  @SuppressLint("MissingPermission")
  private fun startLocationUpdates() {
    val fine = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fine) {
      Log.w(TAG, "Location permission missing for BacktrackService")
      stopRecording()
      stopSelf()
      return
    }

    try {
      val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
        .setMinUpdateIntervalMillis(1000L)
        .setMinUpdateDistanceMeters(1.0f)
        .build()

      locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          val loc = result.lastLocation ?: return

          // Filter with 2D Kalman filter
          val filtered = kalmanFilter.process(
            rawLat = loc.latitude,
            rawLon = loc.longitude,
            rawAccuracyMeters = loc.accuracy,
            timeMs = System.currentTimeMillis()
          )

          // Distance gate: require at least 1.0 meter displacement from last recorded point
          if (lastRecordedLat != 0.0 && lastRecordedLon != 0.0) {
            val dist = com.example.compass.util.CompassCalculations.calculateDistanceMeters(
              lastRecordedLat, lastRecordedLon,
              filtered.latitude, filtered.longitude
            )
            if (dist < 1.0) return // Skip tiny jitter while standing still
          }

          lastRecordedLat = filtered.latitude
          lastRecordedLon = filtered.longitude

          val point = CompassTrackPointEntity(
            sessionKey = activeSessionId,
            latitude = filtered.latitude,
            longitude = filtered.longitude,
            altitude = if (loc.hasAltitude()) loc.altitude else 0.0,
            accuracy = filtered.accuracy,
            timestamp = System.currentTimeMillis()
          )

          // Update live stream and write to Room
          val updated = _liveTrackPoints.value + point
          _liveTrackPoints.value = updated

          serviceScope.launch {
            try {
              AppDatabase.getDatabase(applicationContext).compassDao().insertTrackPoint(point)
            } catch (e: Exception) {
              Log.e(TAG, "Error saving track point to Room", e)
            }
          }

          updateNotification("Migas registradas: ${updated.size} puntos")
        }
      }

      fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start location updates in service", e)
    }
  }

  private fun stopRecording() {
    _isRecordingFlow.value = false
    try {
      locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error removing location updates", e)
    }
  }

  override fun onDestroy() {
    stopRecording()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "SignalRadar Backtrack",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Registro continuo de coordenadas y trayecto para retorno"
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(contentText: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("SignalRadar // Backtrack Activo")
      .setContentText(contentText)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  private fun updateNotification(text: String) {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    manager?.notify(NOTIFICATION_ID, buildNotification(text))
  }
}
