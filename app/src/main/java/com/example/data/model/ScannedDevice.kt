package com.example.data.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class DeviceType {
  WIFI,
  BLUETOOTH_LE
}

enum class SignalTier {
  EXCELLENT,
  GOOD,
  FAIR,
  WEAK
}

data class ScannedDevice(
  val id: String,
  val name: String,
  val macAddress: String,
  val rssi: Int,
  val type: DeviceType,
  val timestamp: Long = System.currentTimeMillis(),
  val frequency: Int? = null,
  val capabilities: String? = null,
  val txPower: Int? = null,
  val serviceUuids: List<String> = emptyList(),
  val isConnectable: Boolean? = null,
) {
  val displayName: String
    get() = if (name.isNotBlank() && name != "<unknown ssid>") name else if (type == DeviceType.WIFI) "Red Wi-Fi Oculta" else "Dispositivo BLE Sin Nombre"

  val signalTier: SignalTier
    get() = when {
      rssi >= -60 -> SignalTier.EXCELLENT
      rssi >= -72 -> SignalTier.GOOD
      rssi >= -84 -> SignalTier.FAIR
      else -> SignalTier.WEAK
    }

  /**
   * Approximate distance in meters based on Log-Distance Path Loss Model.
   * txPower or default -59 dBm at 1 meter. Path loss exponent n ~ 2.4 (indoor environment).
   */
  val estimatedDistanceMeters: Double
    get() {
      val measuredPower = txPower?.toDouble() ?: if (type == DeviceType.WIFI) -45.0 else -59.0
      val ratio = (measuredPower - rssi) / (10.0 * 2.4)
      val distance = 10.0.pow(ratio)
      return (distance * 10).roundToInt() / 10.0
    }

  val bandLabel: String?
    get() {
      if (type != DeviceType.WIFI || frequency == null) return null
      return when (frequency) {
        in 2400..2500 -> "2.4 GHz (Ch ${getWifiChannel(frequency)})"
        in 4900..5900 -> "5 GHz (Ch ${getWifiChannel(frequency)})"
        in 5925..7125 -> "6 GHz (Wi-Fi 6E)"
        else -> "${frequency} MHz"
      }
    }

  companion object {
    fun getWifiChannel(frequency: Int): Int {
      return when {
        frequency in 2412..2484 -> if (frequency == 2484) 14 else (frequency - 2407) / 5
        frequency in 5170..5825 -> (frequency - 5000) / 5
        else -> 0
      }
    }
  }
}
