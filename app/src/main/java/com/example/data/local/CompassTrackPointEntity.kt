package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compass_track_points")
data class CompassTrackPointEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  @ColumnInfo(name = "session_key")
  val sessionKey: String,
  @ColumnInfo(name = "latitude")
  val latitude: Double,
  @ColumnInfo(name = "longitude")
  val longitude: Double,
  @ColumnInfo(name = "altitude")
  val altitude: Double = 0.0,
  @ColumnInfo(name = "accuracy")
  val accuracy: Float = 0f,
  @ColumnInfo(name = "timestamp")
  val timestamp: Long = System.currentTimeMillis(),
)
