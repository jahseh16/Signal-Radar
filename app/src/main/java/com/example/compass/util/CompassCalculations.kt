package com.example.compass.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object CompassCalculations {

  private const val EARTH_RADIUS_METERS = 6371000.0

  /**
   * Calculates geodesic distance in meters using Haversine formula.
   */
  fun calculateDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)

    val a = sin(dLat / 2) * sin(dLat / 2) +
      cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
  }

  /**
   * Calculates initial bearing (forward azimuth) from point 1 to point 2 in degrees (0..360).
   */
  fun calculateBearing(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)

    val y = sin(dLon) * cos(rLat2)
    val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
    val initialBearingRad = atan2(y, x)
    val initialBearingDeg = Math.toDegrees(initialBearingRad)
    return ((initialBearingDeg + 360.0) % 360.0).toFloat()
  }

  /**
   * Calculates relative angle on compass dial (-180..+180) relative to device heading.
   */
  fun calculateRelativeAngle(
    targetBearing: Float,
    currentAzimuth: Float,
  ): Float {
    var diff = targetBearing - currentAzimuth
    while (diff > 180f) diff -= 360f
    while (diff < -180f) diff += 360f
    return diff
  }

  /**
   * Converts degrees into 16-point cardinal compass directions in Spanish.
   */
  fun getCardinalDirection(azimuthDeg: Float): String {
    val normalized = ((azimuthDeg % 360f) + 360f) % 360f
    return when {
      normalized >= 348.75f || normalized < 11.25f -> "N"
      normalized < 33.75f -> "NNE"
      normalized < 56.25f -> "NE"
      normalized < 78.75f -> "ENE"
      normalized < 101.25f -> "E"
      normalized < 123.75f -> "ESE"
      normalized < 146.25f -> "SE"
      normalized < 168.75f -> "SSE"
      normalized < 191.25f -> "S"
      normalized < 213.75f -> "SSO"
      normalized < 236.25f -> "SO"
      normalized < 258.75f -> "OSO"
      normalized < 281.25f -> "O"
      normalized < 303.75f -> "ONO"
      normalized < 326.25f -> "NO"
      else -> "NNO"
    }
  }

  /**
   * Formats degrees into GPS DDM format (Degrees and Decimal Minutes).
   */
  fun formatCoordinates(latitude: Double, longitude: Double): String {
    val latHemisphere = if (latitude >= 0) "N" else "S"
    val absLat = kotlin.math.abs(latitude)
    val latDeg = absLat.toInt()
    val latMin = (absLat - latDeg) * 60.0

    val lonHemisphere = if (longitude >= 0) "E" else "O"
    val absLon = kotlin.math.abs(longitude)
    val lonDeg = absLon.toInt()
    val lonMin = (absLon - lonDeg) * 60.0

    return "%d°%05.2f' %s  %d°%05.2f' %s".format(
      latDeg, latMin, latHemisphere,
      lonDeg, lonMin, lonHemisphere
    )
  }

  /**
   * Formats distance in natural human language (meters or kilometers).
   */
  fun formatDistanceHuman(distanceMeters: Double): String {
    return if (distanceMeters >= 1000.0) {
      "%.1f km".format(distanceMeters / 1000.0)
    } else {
      "%.0f m".format(distanceMeters)
    }
  }

  /**
   * Formats elevation difference in natural human language (+/- meters, uphill/downhill).
   */
  fun formatElevationDifferenceHuman(diffMeters: Double): String {
    val absDiff = kotlin.math.abs(diffMeters)
    return when {
      absDiff < 1.0 -> "0 m (Mismo nivel)"
      diffMeters > 0 -> "+%.0f m (Subida)".format(diffMeters)
      else -> "-%.0f m (Bajada)".format(absDiff)
    }
  }

  /**
   * Formats terrain inclination / slope in natural human language.
   */
  fun formatSlopeHuman(slopeDeg: Float): String {
    val absSlope = kotlin.math.abs(slopeDeg)
    return when {
      absSlope < 3.0f -> "%.0f° (Nivelado)".format(absSlope)
      absSlope < 12.0f -> "%.0f° (Pendiente suave)".format(absSlope)
      absSlope < 25.0f -> "%.0f° (Pendiente moderada)".format(absSlope)
      else -> "%.0f° (Pendiente fuerte)".format(absSlope)
    }
  }

  /**
   * Returns cardinal direction with full name in Spanish (e.g. "304° NO - Noroeste").
   */
  fun getCardinalDirectionDetailed(azimuthDeg: Float): String {
    val normalized = ((azimuthDeg % 360f) + 360f) % 360f
    return when {
      normalized >= 348.75f || normalized < 11.25f -> "N - Norte"
      normalized < 33.75f -> "NNE - Nor-Noreste"
      normalized < 56.25f -> "NE - Noreste"
      normalized < 78.75f -> "ENE - Este-Noreste"
      normalized < 101.25f -> "E - Este"
      normalized < 123.75f -> "ESE - Este-Sureste"
      normalized < 146.25f -> "SE - Sureste"
      normalized < 168.75f -> "SSE - Sur-Sureste"
      normalized < 191.25f -> "S - Sur"
      normalized < 213.75f -> "SSO - Sur-Suroeste"
      normalized < 236.25f -> "SO - Suroeste"
      normalized < 258.75f -> "OSO - Oeste-Suroeste"
      normalized < 281.25f -> "O - Oeste"
      normalized < 303.75f -> "ONO - Oeste-Noroeste"
      normalized < 326.25f -> "NO - Noroeste"
      else -> "NNO - Nor-Noroeste"
    }
  }

  fun formatBearingHuman(bearingDeg: Float): String {
    val degInt = ((bearingDeg % 360f) + 360f).toInt() % 360
    return "%03d° %s".format(degInt, getCardinalDirectionDetailed(bearingDeg))
  }
}
