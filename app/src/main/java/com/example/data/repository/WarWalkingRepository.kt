package com.example.data.repository

import com.example.data.local.ScannedNetworkDao
import com.example.data.local.ScannedNetworkEntity
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import kotlinx.coroutines.flow.Flow

class WarWalkingRepository(private val dao: ScannedNetworkDao) {

  val allSavedNetworks: Flow<List<ScannedNetworkEntity>> = dao.getAllNetworks()
  val savedCount: Flow<Int> = dao.getNetworksCount()

  suspend fun saveOrUpdateDevice(device: ScannedDevice, userLat: Double, userLon: Double) {
    val existing = dao.getNetworkByBssid(device.macAddress)

    val entity = if (existing != null) {
      existing.copy(
        ssid = if (device.name.isNotBlank() && device.name != "<unknown ssid>") device.name else existing.ssid,
        ultimoRssi = device.rssi,
        fechaRegistro = System.currentTimeMillis(),
        // Update GPS coordinates if current reading is stronger than previously recorded
        latitud = if (device.rssi > existing.ultimoRssi) userLat else existing.latitud,
        longitud = if (device.rssi > existing.ultimoRssi) userLon else existing.longitud,
        frecuencia = device.frequency ?: existing.frecuencia,
        capacidades = device.capabilities ?: existing.capacidades
      )
    } else {
      ScannedNetworkEntity(
        ssid = device.displayName,
        bssidMac = device.macAddress,
        latitud = userLat,
        longitud = userLon,
        ultimoRssi = device.rssi,
        fechaRegistro = System.currentTimeMillis(),
        tipo = if (device.type == DeviceType.WIFI) "WIFI" else "BLUETOOTH",
        frecuencia = device.frequency,
        capacidades = device.capabilities
      )
    }

    dao.upsertNetwork(entity)
  }

  suspend fun clearAll() {
    dao.clearAll()
  }
}
