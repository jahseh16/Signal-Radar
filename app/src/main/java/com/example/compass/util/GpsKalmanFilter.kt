package com.example.compass.util

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 2D Kalman Filter for smoothing noisy GPS coordinates and eliminating drift.
 * Specifically tuned for pedestrian, running, and tactical navigation.
 */
class GpsKalmanFilter(
  private val qMetresPerSecond: Float = 2.5f // Process noise (typical walking/running speed variability)
) {
  private var timeStampMs: Long = 0
  private var latitude: Double = 0.0
  private var longitude: Double = 0.0
  private var variance: Float = -1f // -1 means uninitialized

  val isInitialized: Boolean
    get() = variance >= 0f

  fun reset() {
    variance = -1f
    timeStampMs = 0
  }

  /**
   * Processes a raw GPS fix and returns a filtered, smoothed coordinate.
   * [rawLat] raw GPS latitude
   * [rawLon] raw GPS longitude
   * [rawAccuracyMeters] GPS accuracy reported by FusedLocationProviderClient
   * [timeMs] timestamp in milliseconds
   */
  fun process(
    rawLat: Double,
    rawLon: Double,
    rawAccuracyMeters: Float,
    timeMs: Long
  ): FilteredCoordinate {
    var accuracy = rawAccuracyMeters
    if (accuracy < 1f) accuracy = 1f

    if (variance < 0f) {
      // First fix: initialize state
      timeStampMs = timeMs
      latitude = rawLat
      longitude = rawLon
      variance = accuracy * accuracy
      return FilteredCoordinate(latitude, longitude, accuracy)
    }

    val timeDeltaMs = timeMs - timeStampMs
    if (timeDeltaMs > 0) {
      // 1. Prediction step: process noise increases variance proportional to time elapsed
      val timeDeltaSec = timeDeltaMs / 1000f
      variance += timeDeltaSec * qMetresPerSecond * qMetresPerSecond
      timeStampMs = timeMs
    }

    // Outlier rejection: if position jumps impossibly far in a short time, damp or re-anchor
    val approxDistMeters = estimateDistanceMeters(latitude, longitude, rawLat, rawLon)
    if (timeDeltaMs in 1..4000 && approxDistMeters > 50.0 && accuracy > 12f) {
      // Reject noisy jump
      return FilteredCoordinate(latitude, longitude, sqrt(variance))
    }

    // 2. Update step: calculate Kalman gain
    val measurementVariance = accuracy * accuracy
    val kalmanGain = variance / (variance + measurementVariance)

    // Apply Kalman update
    latitude += kalmanGain * (rawLat - latitude)
    longitude += kalmanGain * (rawLon - longitude)

    // Update state variance
    variance = (1f - kalmanGain) * variance

    return FilteredCoordinate(
      latitude = latitude,
      longitude = longitude,
      accuracy = sqrt(variance)
    )
  }

  private fun estimateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val meanLat = Math.toRadians((lat1 + lat2) / 2.0)
    val x = dLon * cos(meanLat)
    val y = dLat
    return 6371000.0 * sqrt(x * x + y * y)
  }
}

data class FilteredCoordinate(
  val latitude: Double,
  val longitude: Double,
  val accuracy: Float
)
