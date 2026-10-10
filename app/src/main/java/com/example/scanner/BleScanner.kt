package com.example.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class BleScanner(private val context: Context) {

  private val tag = "BleScanner"
  private val bluetoothManager: BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter?
    get() = bluetoothManager?.adapter

  private val _bleDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
  val bleDevices: StateFlow<List<ScannedDevice>> = _bleDevices.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val discoveredDevices = ConcurrentHashMap<String, ScannedDevice>()
  private var bleScanner: BluetoothLeScanner? = null

  private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
      result?.let { processScanResult(it) }
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
      results?.forEach { processScanResult(it) }
    }

    override fun onScanFailed(errorCode: Int) {
      Log.e(tag, "BLE Scan failed with errorCode: $errorCode")
      val msg = when (errorCode) {
        SCAN_FAILED_ALREADY_STARTED -> "El escaneo ya está iniciado"
        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Error de registro en el servicio Bluetooth"
        SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE no soportado en este dispositivo"
        SCAN_FAILED_INTERNAL_ERROR -> "Error interno del controlador Bluetooth"
        else -> "Fallo en escaneo BLE (código: $errorCode)"
      }
      _statusMessage.value = msg
      _isScanning.value = false
    }
  }

  fun isBluetoothEnabled(): Boolean {
    return try {
      bluetoothAdapter?.isEnabled == true
    } catch (e: Exception) {
      false
    }
  }

  fun hasPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val scanPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_SCAN
      ) == PackageManager.PERMISSION_GRANTED

      val connectPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED

      val locationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

      return scanPermission && connectPermission && locationPermission
    } else {
      val location = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

      val bt = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH
      ) == PackageManager.PERMISSION_GRANTED

      val btAdmin = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_ADMIN
      ) == PackageManager.PERMISSION_GRANTED

      return location && bt && btAdmin
    }
  }

  @SuppressLint("MissingPermission")
  fun startScan(): Boolean {
    if (!hasPermission()) {
      _statusMessage.value = "Permisos de Bluetooth y ubicación requeridos"
      return false
    }

    val adapter = bluetoothAdapter
    if (adapter == null) {
      _statusMessage.value = "Hardware Bluetooth no disponible"
      return false
    }

    if (!adapter.isEnabled) {
      _statusMessage.value = "El Bluetooth está desactivado"
      return false
    }

    bleScanner = adapter.bluetoothLeScanner
    if (bleScanner == null) {
      _statusMessage.value = "Escáner BLE no inicializado"
      return false
    }

    return try {
      val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

      bleScanner?.startScan(null, settings, scanCallback)
      _isScanning.value = true
      _statusMessage.value = null
      true
    } catch (e: SecurityException) {
      Log.e(tag, "SecurityException starting BLE scan", e)
      _statusMessage.value = "Permiso de Bluetooth insuficiente"
      false
    } catch (e: Exception) {
      Log.e(tag, "Exception starting BLE scan", e)
      _statusMessage.value = "Error al iniciar escaneo BLE: ${e.message}"
      false
    }
  }

  @SuppressLint("MissingPermission")
  fun stopScan() {
    if (_isScanning.value) {
      try {
        bleScanner?.stopScan(scanCallback)
      } catch (e: Exception) {
        Log.w(tag, "Error stopping BLE scan", e)
      } finally {
        _isScanning.value = false
      }
    }
  }

  fun clear() {
    discoveredDevices.clear()
    _bleDevices.value = emptyList()
  }

  @SuppressLint("MissingPermission")
  private fun processScanResult(result: ScanResult) {
    try {
      val device = result.device
      val address = device.address ?: return
      val name = try {
        device.name ?: result.scanRecord?.deviceName ?: ""
      } catch (e: SecurityException) {
        result.scanRecord?.deviceName ?: ""
      }

      val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val p = result.txPower
        if (p != ScanResult.TX_POWER_NOT_PRESENT) p else result.scanRecord?.txPowerLevel
      } else {
        result.scanRecord?.txPowerLevel
      }

      val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

      val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        result.isConnectable
      } else {
        null
      }

      val scannedDevice = ScannedDevice(
        id = address,
        name = name,
        macAddress = address,
        rssi = result.rssi,
        type = DeviceType.BLUETOOTH_LE,
        timestamp = System.currentTimeMillis(),
        txPower = txPower,
        serviceUuids = serviceUuids,
        isConnectable = isConnectable
      )

      discoveredDevices[address] = scannedDevice
      _bleDevices.value = discoveredDevices.values.sortedByDescending { it.rssi }
    } catch (e: Exception) {
      Log.e(tag, "Error processing BLE result", e)
    }
  }
}
