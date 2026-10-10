package com.example.navigation

import com.example.data.model.ScannedDevice
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class NavigationMode {
  STREETS,
  DIRECT
}

enum class InstructionType {
  STRAIGHT,
  TURN_RIGHT,
  TURN_LEFT,
  ARRIVED,
  DIRECT_BEARING
}

data class GeoPoint(val lat: Double, val lon: Double)

data class RouteInstruction(
  val text: String,
  val distanceMeters: Double,
  val type: InstructionType
)

data class SignalRoute(
  val mode: NavigationMode,
  val waypoints: List<GeoPoint>,
  val instructions: List<RouteInstruction>,
  val totalDistanceMeters: Double,
  val currentRssi: Int,
  val targetName: String
)

data class StreetSegment(
  val name: String,
  val start: GeoPoint,
  val end: GeoPoint
)

object SignalRoutingEngine {

  private const val EARTH_RADIUS = 6371000.0 // meters

  /**
   * Calculates the geo coordinates of a target signal given the observer's location,
   * the estimated RSSI distance, and a deterministic bearing based on device MAC.
   */
  fun calculateTargetCoordinates(
    observerLat: Double,
    observerLon: Double,
    device: ScannedDevice
  ): GeoPoint {
    val distanceMeters = device.estimatedDistanceMeters.coerceIn(3.0, 150.0)

    // Deterministic bearing from MAC address (0 to 359 degrees)
    val hash = device.macAddress.hashCode()
    val bearingDegrees = ((hash % 360) + 360) % 360
    val bearingRad = Math.toRadians(bearingDegrees.toDouble())

    val deltaLat = (distanceMeters * cos(bearingRad)) / EARTH_RADIUS
    val deltaLon = (distanceMeters * sin(bearingRad)) / (EARTH_RADIUS * cos(Math.toRadians(observerLat)))

    return GeoPoint(
      lat = observerLat + Math.toDegrees(deltaLat),
      lon = observerLon + Math.toDegrees(deltaLon)
    )
  }

  /**
   * Generates local street network around reference coordinate.
   */
  fun getLocalStreets(centerLat: Double, centerLon: Double): List<StreetSegment> {
    val deltaMeters = 60.0 // Grid cell width ~60m
    val dLat = Math.toDegrees(deltaMeters / EARTH_RADIUS)
    val dLon = Math.toDegrees(deltaMeters / (EARTH_RADIUS * cos(Math.toRadians(centerLat))))

    val streets = mutableListOf<StreetSegment>()

    // North-South Avenues
    val avenueNames = listOf(
      "Av. de las Ondas",
      "Paseo de la Señal",
      "Av. Central Radio",
      "Paseo del Espectro",
      "Av. Wi-Fi 6"
    )

    for (i in -2..2) {
      val lon = centerLon + (i * dLon)
      val name = avenueNames.getOrElse(i + 2) { "Avenida ${i + 3}" }
      streets.add(
        StreetSegment(
          name = name,
          start = GeoPoint(centerLat - (2.5 * dLat), lon),
          end = GeoPoint(centerLat + (2.5 * dLat), lon)
        )
      )
    }

    // East-West Streets
    val streetNames = listOf(
      "Calle Frecuencia 5G",
      "Calle del Router",
      "Calle Antena Central",
      "Calle Baliza BLE",
      "Calle Satélite"
    )

    for (j in -2..2) {
      val lat = centerLat + (j * dLat)
      val name = streetNames.getOrElse(j + 2) { "Calle ${j + 3}" }
      streets.add(
        StreetSegment(
          name = name,
          start = GeoPoint(lat, centerLon - (2.5 * dLon)),
          end = GeoPoint(lat, centerLon + (2.5 * dLon))
        )
      )
    }

    return streets
  }

  /**
   * Builds the navigation route between user and target.
   */
  fun calculateRoute(
    userLat: Double,
    userLon: Double,
    targetLat: Double,
    targetLon: Double,
    device: ScannedDevice,
    mode: NavigationMode
  ): SignalRoute {
    val directDistance = calculateDistance(userLat, userLon, targetLat, targetLon)

    if (mode == NavigationMode.DIRECT) {
      // Direct Mode / Open Field
      val bearing = calculateBearing(userLat, userLon, targetLat, targetLon).roundToInt()
      val cardinal = getCardinalDirection(bearing)
      val instructions = if (directDistance < 5.0) {
        listOf(
          RouteInstruction(
            text = "¡Estás sobre la señal! Dispositivo objetivo a menos de 5m.",
            distanceMeters = directDistance,
            type = InstructionType.ARRIVED
          )
        )
      } else {
        listOf(
          RouteInstruction(
            text = "Avanza en línea recta ${directDistance.roundToInt()}m rumbo $bearing° ($cardinal) hacia '${device.displayName}'",
            distanceMeters = directDistance,
            type = InstructionType.DIRECT_BEARING
          )
        )
      }

      return SignalRoute(
        mode = NavigationMode.DIRECT,
        waypoints = listOf(GeoPoint(userLat, userLon), GeoPoint(targetLat, targetLon)),
        instructions = instructions,
        totalDistanceMeters = directDistance,
        currentRssi = device.rssi,
        targetName = device.displayName
      )
    }

    // Modo Calles: Calculate Manhattan / Street Grid Polyline
    // Route from user -> (userLat, targetLon) or (targetLat, userLon) -> target
    val waypoints = mutableListOf<GeoPoint>()
    val instructions = mutableListOf<RouteInstruction>()

    waypoints.add(GeoPoint(userLat, userLon))

    // Determine street corner waypoint
    // Step 1: Walk along latitude or longitude
    val cornerPoint = GeoPoint(userLat, targetLon)
    val leg1Dist = calculateDistance(userLat, userLon, cornerPoint.lat, cornerPoint.lon)
    val leg2Dist = calculateDistance(cornerPoint.lat, cornerPoint.lon, targetLat, targetLon)

    waypoints.add(cornerPoint)
    waypoints.add(GeoPoint(targetLat, targetLon))

    val leg1Bearing = calculateBearing(userLat, userLon, cornerPoint.lat, cornerPoint.lon)
    val leg2Bearing = calculateBearing(cornerPoint.lat, cornerPoint.lon, targetLat, targetLon)

    val leg1Name = if (userLon < targetLon) "Calle hacia el Este" else "Calle hacia el Oeste"
    val leg2Name = if (userLat < targetLat) "Avenida Norte" else "Avenida Sur"

    if (leg1Dist >= 4.0) {
      instructions.add(
        RouteInstruction(
          text = "Camina ${leg1Dist.roundToInt()}m por $leg1Name",
          distanceMeters = leg1Dist,
          type = InstructionType.STRAIGHT
        )
      )
    }

    val turnType = if (isTurnRight(leg1Bearing, leg2Bearing)) InstructionType.TURN_RIGHT else InstructionType.TURN_LEFT
    val turnText = if (turnType == InstructionType.TURN_RIGHT) "Gira a la derecha" else "Gira a la izquierda"

    if (leg2Dist >= 4.0) {
      instructions.add(
        RouteInstruction(
          text = "$turnText en la esquina hacia $leg2Name y avanza ${leg2Dist.roundToInt()}m",
          distanceMeters = leg2Dist,
          type = turnType
        )
      )
    }

    instructions.add(
      RouteInstruction(
        text = "Señal '${device.displayName}' al frente a ${device.estimatedDistanceMeters}m (RSSI: ${device.rssi} dBm)",
        distanceMeters = device.estimatedDistanceMeters,
        type = InstructionType.ARRIVED
      )
    )

    val totalDistance = leg1Dist + leg2Dist

    return SignalRoute(
      mode = NavigationMode.STREETS,
      waypoints = waypoints,
      instructions = instructions,
      totalDistanceMeters = totalDistance,
      currentRssi = device.rssi,
      targetName = device.displayName
    )
  }

  fun calculateRouteForMarker(
    userLat: Double,
    userLon: Double,
    marker: com.example.data.model.TacticalMarker,
    mode: NavigationMode
  ): SignalRoute {
    val directDistance = calculateDistance(userLat, userLon, marker.lat, marker.lon)

    if (mode == NavigationMode.DIRECT) {
      val bearing = calculateBearing(userLat, userLon, marker.lat, marker.lon).roundToInt()
      val cardinal = getCardinalDirection(bearing)
      val instructions = if (directDistance < 5.0) {
        listOf(
          RouteInstruction(
            text = "¡Estás sobre la señal! '${marker.name}' a menos de 5m.",
            distanceMeters = directDistance,
            type = InstructionType.ARRIVED
          )
        )
      } else {
        listOf(
          RouteInstruction(
            text = "Avanza en línea recta ${directDistance.roundToInt()}m rumbo $bearing° ($cardinal) hacia '${marker.name}'",
            distanceMeters = directDistance,
            type = InstructionType.DIRECT_BEARING
          )
        )
      }

      return SignalRoute(
        mode = NavigationMode.DIRECT,
        waypoints = listOf(GeoPoint(userLat, userLon), GeoPoint(marker.lat, marker.lon)),
        instructions = instructions,
        totalDistanceMeters = directDistance,
        currentRssi = marker.rssi,
        targetName = marker.name
      )
    }

    val waypoints = mutableListOf<GeoPoint>()
    val instructions = mutableListOf<RouteInstruction>()

    waypoints.add(GeoPoint(userLat, userLon))
    val cornerPoint = GeoPoint(userLat, marker.lon)
    val leg1Dist = calculateDistance(userLat, userLon, cornerPoint.lat, cornerPoint.lon)
    val leg2Dist = calculateDistance(cornerPoint.lat, cornerPoint.lon, marker.lat, marker.lon)

    waypoints.add(cornerPoint)
    waypoints.add(GeoPoint(marker.lat, marker.lon))

    val leg1Bearing = calculateBearing(userLat, userLon, cornerPoint.lat, cornerPoint.lon)
    val leg2Bearing = calculateBearing(cornerPoint.lat, cornerPoint.lon, marker.lat, marker.lon)

    val leg1Name = if (userLon < marker.lon) "Calle hacia el Este" else "Calle hacia el Oeste"
    val leg2Name = if (userLat < marker.lat) "Avenida Norte" else "Avenida Sur"

    if (leg1Dist >= 4.0) {
      instructions.add(
        RouteInstruction(
          text = "Camina ${leg1Dist.roundToInt()}m por $leg1Name",
          distanceMeters = leg1Dist,
          type = InstructionType.STRAIGHT
        )
      )
    }

    val turnType = if (isTurnRight(leg1Bearing, leg2Bearing)) InstructionType.TURN_RIGHT else InstructionType.TURN_LEFT
    val turnText = if (turnType == InstructionType.TURN_RIGHT) "Gira a la derecha" else "Gira a la izquierda"

    if (leg2Dist >= 4.0) {
      instructions.add(
        RouteInstruction(
          text = "$turnText en la esquina hacia $leg2Name y avanza ${leg2Dist.roundToInt()}m",
          distanceMeters = leg2Dist,
          type = turnType
        )
      )
    }

    instructions.add(
      RouteInstruction(
        text = "Llegando a '${marker.name}' (${if (marker.isOnline) "En vivo: ${marker.rssi} dBm" else "Último registro: ${marker.rssi} dBm"})",
        distanceMeters = directDistance,
        type = InstructionType.ARRIVED
      )
    )

    return SignalRoute(
      mode = NavigationMode.STREETS,
      waypoints = waypoints,
      instructions = instructions,
      totalDistanceMeters = leg1Dist + leg2Dist,
      currentRssi = marker.rssi,
      targetName = marker.name
    )
  }

  fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (EARTH_RADIUS * c * 10).roundToInt() / 10.0
  }

  fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    val theta = atan2(y, x)
    return (Math.toDegrees(theta) + 360) % 360
  }

  private fun isTurnRight(b1: Double, b2: Double): Boolean {
    val diff = (b2 - b1 + 360) % 360
    return diff in 1.0..179.0
  }

  private fun getCardinalDirection(bearing: Int): String {
    return when (bearing) {
      in 338..360, in 0..22 -> "Norte"
      in 23..67 -> "Noreste"
      in 68..112 -> "Este"
      in 113..157 -> "Sureste"
      in 158..202 -> "Sur"
      in 203..247 -> "Suroeste"
      in 248..292 -> "Oeste"
      in 293..337 -> "Noroeste"
      else -> "Norte"
    }
  }
}
