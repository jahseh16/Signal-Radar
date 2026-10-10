package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compass_waypoints")
data class CompassWaypointEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "latitude")
  val latitude: Double,
  @ColumnInfo(name = "longitude")
  val longitude: Double,
  @ColumnInfo(name = "altitude")
  val altitude: Double = 0.0,
  @ColumnInfo(name = "color_hex")
  val colorHex: String = "#00D2FF",
  @ColumnInfo(name = "icon_type")
  val iconType: String = "PIN",
  @ColumnInfo(name = "accuracy")
  val accuracy: Float = 0f,
  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),
)
