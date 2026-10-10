package com.example.data.model

data class TacticalMarker(
  val id: String,
  val name: String,
  val bssidMac: String,
  val lat: Double,
  val lon: Double,
  val rssi: Int,
  val type: DeviceType,
  val isOnline: Boolean, // true -> Verde (En Vivo), false -> Gris (Offline / Histórico)
  val lastSeenTimestamp: Long = System.currentTimeMillis(),
  val distanceMeters: Double = 0.0,
  val frequency: Int? = null,
  val capabilities: String? = null
) {
  val statusLabel: String
    get() = if (isOnline) "En Vivo" else "Histórico (Offline)"
}
