package com.example.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiScanner(private val context: Context) {

  private val tag = "WifiScanner"
  private val wifiManager: WifiManager? =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

  private val _wifiDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
  val wifiDevices: StateFlow<List<ScannedDevice>> = _wifiDevices.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private var isReceiverRegistered = false

  private val wifiScanReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
      if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        Log.d(tag, "Scan results available from hardware, success=$success")
        retrieveScanResults()
      }
    }
  }

  fun isWifiEnabled(): Boolean {
    return try {
      wifiManager?.isWifiEnabled == true
    } catch (e: Exception) {
      false
    }
  }

  fun hasPermission(): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val wifiState = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_WIFI_STATE
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocation && wifiState
  }

  fun startScan(): Boolean {
    if (!hasPermission()) {
      _statusMessage.value = "Permisos de ubicación y Wi-Fi requeridos"
      return false
    }

    if (wifiManager == null) {
      _statusMessage.value = "Hardware Wi-Fi no disponible"
      return false
    }

    if (!isWifiEnabled()) {
      _statusMessage.value = "El Wi-Fi está desactivado"
      return false
    }

    registerReceiver()
    _isScanning.value = true

    // Check cached hardware scan results immediately
    retrieveScanResults()

    return try {
      @Suppress("DEPRECATION")
      val started = wifiManager.startScan()
      if (!started) {
        _statusMessage.value = "Escaneo Wi-Fi en espera de respuesta del chip"
      }
      true
    } catch (e: SecurityException) {
      Log.e(tag, "SecurityException starting Wi-Fi scan", e)
      _statusMessage.value = "Error de seguridad al iniciar escaneo Wi-Fi"
      false
    } catch (e: Exception) {
      Log.e(tag, "Exception starting Wi-Fi scan", e)
      _statusMessage.value = "Error al iniciar escaneo Wi-Fi: ${e.message}"
      false
    }
  }

  fun stopScan() {
    _isScanning.value = false
    unregisterReceiver()
  }

  fun clear() {
    _wifiDevices.value = emptyList()
  }

  private fun registerReceiver() {
    if (!isReceiverRegistered) {
      try {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
          context.registerReceiver(wifiScanReceiver, intentFilter)
        }
        isReceiverRegistered = true
      } catch (e: Exception) {
        Log.e(tag, "Error registering wifi receiver", e)
      }
    }
  }

  private fun unregisterReceiver() {
    if (isReceiverRegistered) {
      try {
        context.unregisterReceiver(wifiScanReceiver)
      } catch (e: Exception) {
        Log.w(tag, "Error unregistering wifi receiver", e)
      } finally {
        isReceiverRegistered = false
      }
    }
  }

  private fun retrieveScanResults() {
    if (!hasPermission() || wifiManager == null) return

    try {
      val results: List<ScanResult> = wifiManager.scanResults ?: emptyList()
      Log.d(tag, "Retrieved ${results.size} real Wi-Fi networks from hardware")

      val mappedDevices = results.map { result ->
        @Suppress("DEPRECATION")
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          result.wifiSsid?.toString()?.replace("\"", "") ?: result.SSID ?: ""
        } else {
          result.SSID ?: ""
        }

        ScannedDevice(
          id = result.BSSID ?: "wifi_${result.hashCode()}",
          name = if (ssid.isBlank() || ssid == "<unknown ssid>") "Red Oculta" else ssid,
          macAddress = result.BSSID ?: "00:00:00:00:00:00",
          rssi = result.level,
          type = DeviceType.WIFI,
          timestamp = result.timestamp / 1000,
          frequency = result.frequency,
          capabilities = result.capabilities,
        )
      }

      _wifiDevices.value = mappedDevices.sortedByDescending { it.rssi }
      _statusMessage.value = null
    } catch (e: SecurityException) {
      Log.e(tag, "Permission denied retrieving scan results", e)
      _statusMessage.value = "Permiso denegado al leer resultados Wi-Fi"
    } catch (e: Exception) {
      Log.e(tag, "Error retrieving scan results", e)
    }
  }
}
