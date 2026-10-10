package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "redes_detectadas",
  indices = [Index(value = ["bssid_mac"], unique = true)]
)
data class ScannedNetworkEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  @ColumnInfo(name = "ssid")
  val ssid: String,
  @ColumnInfo(name = "bssid_mac")
  val bssidMac: String,
  @ColumnInfo(name = "latitud")
  val latitud: Double,
  @ColumnInfo(name = "longitud")
  val longitud: Double,
  @ColumnInfo(name = "ultimo_rssi")
  val ultimoRssi: Int,
  @ColumnInfo(name = "fecha_registro")
  val fechaRegistro: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "tipo")
  val tipo: String = "WIFI", // "WIFI" o "BLUETOOTH"
  @ColumnInfo(name = "frecuencia")
  val frecuencia: Int? = null,
  @ColumnInfo(name = "capacidades")
  val capacidades: String? = null
)
