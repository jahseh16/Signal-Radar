package com.example

import com.example.compass.util.CompassCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassCalculationsTest {

  @Test
  fun testBearingCalculationCardinalDirections() {
    val northBearing = CompassCalculations.calculateBearing(0.0, 0.0, 1.0, 0.0)
    assertEquals(0f, northBearing, 0.5f)
    assertEquals("N", CompassCalculations.getCardinalDirection(northBearing))
    assertEquals("N - Norte", CompassCalculations.getCardinalDirectionDetailed(northBearing))

    val eastBearing = CompassCalculations.calculateBearing(0.0, 0.0, 0.0, 1.0)
    assertEquals(90f, eastBearing, 0.5f)
    assertEquals("E", CompassCalculations.getCardinalDirection(eastBearing))
    assertEquals("E - Este", CompassCalculations.getCardinalDirectionDetailed(eastBearing))

    val southBearing = CompassCalculations.calculateBearing(0.0, 0.0, -1.0, 0.0)
    assertEquals(180f, southBearing, 0.5f)
    assertEquals("S", CompassCalculations.getCardinalDirection(southBearing))
    assertEquals("S - Sur", CompassCalculations.getCardinalDirectionDetailed(southBearing))

    val westBearing = CompassCalculations.calculateBearing(0.0, 0.0, 0.0, -1.0)
    assertEquals(270f, westBearing, 0.5f)
    assertEquals("O", CompassCalculations.getCardinalDirection(westBearing))
    assertEquals("O - Oeste", CompassCalculations.getCardinalDirectionDetailed(westBearing))
  }

  @Test
  fun testDetailedBearingSpanishFormatting() {
    val northwest = CompassCalculations.getCardinalDirectionDetailed(304f)
    assertEquals("NO - Noroeste", northwest)

    val formattedBearing = CompassCalculations.formatBearingHuman(304f)
    assertEquals("304° NO - Noroeste", formattedBearing)
  }

  @Test
  fun testDistanceCalculation() {
    val madridLat = 40.4168
    val madridLon = -3.7038
    val bcnLat = 41.3879
    val bcnLon = 2.1699

    val distMeters = CompassCalculations.calculateDistanceMeters(madridLat, madridLon, bcnLat, bcnLon)
    assertTrue("Distance between Madrid and Barcelona should be ~505 km", distMeters in 500000.0..510000.0)

    val formattedKm = CompassCalculations.formatDistanceHuman(distMeters)
    assertTrue("Formatted distance ends with km and contains 505", formattedKm.endsWith("km") && formattedKm.contains("505"))

    val formattedMeters = CompassCalculations.formatDistanceHuman(450.0)
    assertEquals("450 m", formattedMeters)
  }

  @Test
  fun testElevationDifferenceFormatting() {
    val uphill = CompassCalculations.formatElevationDifferenceHuman(35.0)
    assertEquals("+35 m (Subida)", uphill)

    val downhill = CompassCalculations.formatElevationDifferenceHuman(-12.0)
    assertEquals("-12 m (Bajada)", downhill)

    val flat = CompassCalculations.formatElevationDifferenceHuman(0.2)
    assertEquals("0 m (Mismo nivel)", flat)
  }

  @Test
  fun testRelativeAngleOnBezel() {
    val rel1 = CompassCalculations.calculateRelativeAngle(30f, 300f)
    assertEquals(90f, rel1, 0.1f)

    val rel2 = CompassCalculations.calculateRelativeAngle(0f, 45f)
    assertEquals(-45f, rel2, 0.1f)
  }

  @Test
  fun testCoordinateFormatting() {
    val formatted = CompassCalculations.formatCoordinates(40.4168, -3.7038)
    assertTrue("Formatted coordinate contains N", formatted.contains("N"))
    assertTrue("Formatted coordinate contains O", formatted.contains("O"))
  }
}
