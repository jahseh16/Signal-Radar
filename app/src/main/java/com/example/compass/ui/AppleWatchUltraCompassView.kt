package com.example.compass.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.compass.WaypointDisplayItem
import com.example.compass.util.CompassCalculations
import com.example.data.local.CompassTrackPointEntity
import com.example.location.UserLocation
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val METERS_PER_DEGREE_LAT = 111132.92

@Composable
fun AppleWatchUltraCompassView(
  azimuthDeg: Float,
  terrainSlopeDeg: Float, // Inclinación del terreno (grados)
  lateralTiltDeg: Float,  // Inclinación lateral (grados)
  userLocation: UserLocation?,
  waypoints: List<WaypointDisplayItem>,
  breadcrumbPoints: List<CompassTrackPointEntity>,
  isRecordingBacktrack: Boolean,
  selectedWaypoint: WaypointDisplayItem?,
  is3DMode: Boolean,
  zoomRangeMeters: Float, // Escala del radar (50m a 2500m)
  onWaypointClicked: (WaypointDisplayItem) -> Unit,
  onZoomGesture: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Animación de pulso táctico para balizas y camino activo
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_trail")
  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  // Perspectiva 3D inclinada (~42°) en el modo 3D
  val perspectiveX = if (is3DMode) {
    42f + (terrainSlopeDeg.coerceIn(-12f, 12f) * 0.25f)
  } else {
    0f
  }

  val perspectiveY = if (is3DMode) {
    -lateralTiltDeg.coerceIn(-12f, 12f) * 0.2f
  } else {
    0f
  }

  // Altura del mástil vertical en píxeles para la proyección 3D del Pin
  val mastHeightPx = if (is3DMode) 32.dp else 0.dp

  // Pinceles de texto para renderizado nítido en el Canvas
  val cardinalPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = 38f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = android.graphics.Color.WHITE
    }
  }

  val northCardinalPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = 42f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = android.graphics.Color.parseColor("#FF3B30") // Rojo táctico Apple Ultra
    }
  }

  val degreeLabelPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = 22f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
      color = android.graphics.Color.parseColor("#94A3B8")
    }
  }

  val waypointLabelPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = android.graphics.Color.parseColor("#00D2FF")
    }
  }

  val radarScalePaint = remember {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.LEFT
      textSize = 22f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      color = android.graphics.Color.parseColor("#00FF41")
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .graphicsLayer {
        rotationX = perspectiveX
        rotationY = perspectiveY
        cameraDistance = 14f * density
      }
      .testTag("apple_watch_ultra_compass"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
        // Interacción gestual directa: Pinch-to-zoom (pellizcar para acercar o alejar el radar)
        .pointerInput(zoomRangeMeters) {
          detectTransformGestures { _, _, zoom, _ ->
            if (zoom != 1f && zoom > 0.05f) {
              val newRange = zoomRangeMeters / zoom
              onZoomGesture(newRange)
            }
          }
        }
        // Detección de toque para seleccionar waypoints en el radar o en el bisel
        .pointerInput(waypoints, azimuthDeg, zoomRangeMeters) {
          detectTapGestures { tapOffset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = kotlin.math.min(size.width, size.height).toFloat()
            val dialRadius = (minDim / 2f) * 0.84f

            val clicked = waypoints.minByOrNull { wp ->
              val angleRad = Math.toRadians((wp.bearingDeg - azimuthDeg - 90f).toDouble())
              val markerX = center.x + (dialRadius * cos(angleRad)).toFloat()
              val markerY = center.y + (dialRadius * sin(angleRad)).toFloat()
              val dist = sqrt((tapOffset.x - markerX) * (tapOffset.x - markerX) + (tapOffset.y - markerY) * (tapOffset.y - markerY))
              dist
            }

            if (clicked != null) {
              val angleRad = Math.toRadians((clicked.bearingDeg - azimuthDeg - 90f).toDouble())
              val markerX = center.x + (dialRadius * cos(angleRad)).toFloat()
              val markerY = center.y + (dialRadius * sin(angleRad)).toFloat()
              val dist = sqrt((tapOffset.x - markerX) * (tapOffset.x - markerX) + (tapOffset.y - markerY) * (tapOffset.y - markerY))
              if (dist <= 65f) {
                onWaypointClicked(clicked)
              }
            }
          }
        }
    ) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val maxRadius = size.minDimension / 2f

      // El dial y bisel exterior se mantienen estrictamente FIJOS en tamaño
      val outerBezelRadius = maxRadius * 0.96f
      val dialRadius = maxRadius * 0.84f
      val innerGridRadius = maxRadius * 0.62f
      val innerCoreRadius = maxRadius * 0.32f

      // 1. BISEL TÁCTICO EXTERIOR (Fondo oscuro premium #05070A)
      drawCircle(
        color = Color(0xFF05070A),
        radius = outerBezelRadius,
        center = center
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFF070B12), Color(0xFF10192A)),
          center = center,
          radius = outerBezelRadius
        ),
        radius = outerBezelRadius,
        center = center
      )
      drawCircle(
        color = Color(0xFF1E293B),
        radius = outerBezelRadius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
      )

      // 2. REJILLA INTERNA DEL RADAR (Se escala con el zoom)
      drawCircle(
        color = Color(0xFF00FF41).copy(alpha = 0.08f),
        radius = innerGridRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )
      drawCircle(
        color = Color(0xFF00FF41).copy(alpha = 0.12f),
        radius = innerCoreRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )

      // Retículas cardinales
      drawLine(
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        start = Offset(center.x - innerGridRadius, center.y),
        end = Offset(center.x + innerGridRadius, center.y),
        strokeWidth = 1.dp.toPx()
      )
      drawLine(
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        start = Offset(center.x, center.y - innerGridRadius),
        end = Offset(center.x, center.y + innerGridRadius),
        strokeWidth = 1.dp.toPx()
      )

      // Indicador de Escala de Radar discreto en el Canvas
      val scaleText = "Alcance: ${zoomRangeMeters.toInt()}m"
      drawContext.canvas.nativeCanvas.drawText(
        scaleText,
        center.x - innerGridRadius + 8f,
        center.y + innerGridRadius - 10f,
        radarScalePaint
      )

      // 3. CAMINITO GRABADO (BREADCRUMB TRAIL) - Trazo fino (2.5dp) y estilizado
      if (breadcrumbPoints.size >= 2 && userLocation != null) {
        drawFineBreadcrumbTrail(
          center = center,
          maxRadius = innerGridRadius,
          userLat = userLocation.latitude,
          userLon = userLocation.longitude,
          headingDeg = azimuthDeg,
          points = breadcrumbPoints,
          zoomRangeMeters = zoomRangeMeters,
          pulseGlow = pulseGlow
        )
      }

      // 4. LÍNEA GUÍA HACIA EL WAYPOINT SELECCIONADO
      if (selectedWaypoint != null && userLocation != null) {
        drawWaypointGuidanceLine(
          center = center,
          dialRadius = dialRadius,
          azimuthDeg = azimuthDeg,
          waypoint = selectedWaypoint,
          pulseGlow = pulseGlow
        )
      }

      // 5. ANILLO GIRATORIO DE LA BRÚJULA (FIJO EN TAMAÑO, ROTA CON EL AZIMUT)
      drawRotatingDial(
        center = center,
        dialRadius = dialRadius,
        outerBezelRadius = outerBezelRadius,
        azimuthDeg = azimuthDeg,
        cardinalPaint = cardinalPaint,
        northCardinalPaint = northCardinalPaint,
        degreeLabelPaint = degreeLabelPaint
      )

      // 6. PROYECCIÓN 3D DE WAYPOINTS: PIN CON MÁSTIL VERTICAL
      draw3DWaypointsWithMasts(
        center = center,
        dialRadius = dialRadius,
        innerGridRadius = innerGridRadius,
        mastHeight = mastHeightPx.toPx(),
        is3DMode = is3DMode,
        azimuthDeg = azimuthDeg,
        waypoints = waypoints,
        selectedWaypoint = selectedWaypoint,
        zoomRangeMeters = zoomRangeMeters,
        labelPaint = waypointLabelPaint,
        pulseGlow = pulseGlow
      )

      // 7. HORIZONTE ARTIFICIAL / MIRA CENTRAL
      drawCenterHorizonReticle(
        center = center,
        terrainSlopeDeg = terrainSlopeDeg,
        lateralTiltDeg = lateralTiltDeg,
        isFocusZoomIn = zoomRangeMeters <= 100f,
        selectedWaypoint = selectedWaypoint,
        pulseGlow = pulseGlow
      )

      // 8. FLECHA DE APICE SUPERIOR (POINTER NORTE)
      val triangleHeight = 22f
      val triangleWidth = 24f
      val pointerPath = Path().apply {
        moveTo(center.x, center.y - outerBezelRadius + 4f)
        lineTo(center.x - triangleWidth / 2f, center.y - outerBezelRadius + triangleHeight)
        lineTo(center.x + triangleWidth / 2f, center.y - outerBezelRadius + triangleHeight)
        close()
      }
      drawPath(
        path = pointerPath,
        color = Color(0xFFFF3B30) // Rojo táctico
      )
    }
  }
}

private fun DrawScope.drawRotatingDial(
  center: Offset,
  dialRadius: Float,
  outerBezelRadius: Float,
  azimuthDeg: Float,
  cardinalPaint: Paint,
  northCardinalPaint: Paint,
  degreeLabelPaint: Paint,
) {
  for (deg in 0 until 360 step 2) {
    val visualAngleDeg = (deg - azimuthDeg - 90f)
    val rad = Math.toRadians(visualAngleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    val isCardinal = deg % 90 == 0
    val isMajor = deg % 30 == 0
    val isMedium = deg % 10 == 0

    val tickLength = when {
      isCardinal -> 24f
      isMajor -> 18f
      isMedium -> 12f
      else -> 6f
    }

    val tickColor = when {
      deg == 0 -> Color(0xFFFF3B30) // Norte en rojo táctico
      isMajor -> Color.White.copy(alpha = 0.9f)
      isMedium -> Color(0xFF64748B)
      else -> Color(0xFF334155)
    }

    val startX = center.x + (dialRadius - tickLength) * cosA
    val startY = center.y + (dialRadius - tickLength) * sinA
    val endX = center.x + dialRadius * cosA
    val endY = center.y + dialRadius * sinA

    drawLine(
      color = tickColor,
      start = Offset(startX, startY),
      end = Offset(endX, endY),
      strokeWidth = if (isMajor) 2.5f else 1.5f,
      cap = StrokeCap.Round
    )

    // Letras cardinales
    if (isCardinal) {
      val letterRadius = dialRadius - 44f
      val textX = center.x + letterRadius * cosA
      val textY = center.y + letterRadius * sinA + 14f

      val text = when (deg) {
        0 -> "N"
        90 -> "E"
        180 -> "S"
        270 -> "O"
        else -> ""
      }

      val paintToUse = if (deg == 0) northCardinalPaint else cardinalPaint
      drawContext.canvas.nativeCanvas.drawText(text, textX, textY, paintToUse)
    } else if (isMajor) {
      val numRadius = dialRadius - 38f
      val textX = center.x + numRadius * cosA
      val textY = center.y + numRadius * sinA + 8f
      drawContext.canvas.nativeCanvas.drawText("$deg°", textX, textY, degreeLabelPaint)
    }
  }
}

/**
 * Renderiza el caminito con trazo fino (2.5dp) y estilizado en rojo neón.
 */
private fun DrawScope.drawFineBreadcrumbTrail(
  center: Offset,
  maxRadius: Float,
  userLat: Double,
  userLon: Double,
  headingDeg: Float,
  points: List<CompassTrackPointEntity>,
  zoomRangeMeters: Float,
  pulseGlow: Float,
) {
  if (points.isEmpty()) return

  val cosUserLat = cos(Math.toRadians(userLat))
  val metersPerDegLon = METERS_PER_DEGREE_LAT * cosUserLat

  // Escala métrica acorde al nivel de zoom
  val pixelsPerMeter = maxRadius / zoomRangeMeters

  val headingRad = Math.toRadians(headingDeg.toDouble())
  val cosHeading = cos(headingRad).toFloat()
  val sinHeading = sin(headingRad).toFloat()

  val trailPath = Path()
  var hasStarted = false

  val neonRed = Color(0xFFFF3B30)
  val fineStrokeWidth = 2.5.dp.toPx()

  points.forEach { pt ->
    val dNorthMeters = (pt.latitude - userLat) * METERS_PER_DEGREE_LAT
    val dEastMeters = (pt.longitude - userLon) * metersPerDegLon

    // Rotación orientada hacia el rumbo actual
    val localX = (dEastMeters * cosHeading - dNorthMeters * sinHeading).toFloat()
    val localY = -(dEastMeters * sinHeading + dNorthMeters * cosHeading).toFloat()

    val screenX = center.x + localX * pixelsPerMeter
    val screenY = center.y + localY * pixelsPerMeter

    val distFromCenter = sqrt((screenX - center.x) * (screenX - center.x) + (screenY - center.y) * (screenY - center.y))
    if (distFromCenter <= maxRadius * 1.05f) {
      if (!hasStarted) {
        trailPath.moveTo(screenX, screenY)
        hasStarted = true
      } else {
        trailPath.lineTo(screenX, screenY)
      }
    }
  }

  // Halo sutil exterior
  drawPath(
    path = trailPath,
    color = neonRed.copy(alpha = 0.35f * pulseGlow),
    style = Stroke(width = fineStrokeWidth * 1.8f, cap = StrokeCap.Round)
  )
  // Línea fina precisa principal
  drawPath(
    path = trailPath,
    color = neonRed,
    style = Stroke(width = fineStrokeWidth, cap = StrokeCap.Round)
  )

  // Marcador del punto de partida
  val first = points.first()
  val firstDNorth = (first.latitude - userLat) * METERS_PER_DEGREE_LAT
  val firstDEast = (first.longitude - userLon) * metersPerDegLon
  val firstLocalX = (firstDEast * cosHeading - firstDNorth * sinHeading).toFloat()
  val firstLocalY = -(firstDEast * sinHeading + firstDNorth * cosHeading).toFloat()
  val firstX = center.x + firstLocalX * pixelsPerMeter
  val firstY = center.y + firstLocalY * pixelsPerMeter

  val distFromCenter = sqrt((firstX - center.x) * (firstX - center.x) + (firstY - center.y) * (firstY - center.y))
  if (distFromCenter <= maxRadius) {
    drawCircle(
      color = Color(0xFFFF9500),
      radius = 5.dp.toPx(),
      center = Offset(firstX, firstY)
    )
    drawCircle(
      color = Color.White,
      radius = 2.5.dp.toPx(),
      center = Offset(firstX, firstY)
    )
  }
}

/**
 * Dibuja cada Waypoint en 3D como un Pin con mástil vertical perpendicular y cabeza flotante.
 */
private fun DrawScope.draw3DWaypointsWithMasts(
  center: Offset,
  dialRadius: Float,
  innerGridRadius: Float,
  mastHeight: Float,
  is3DMode: Boolean,
  azimuthDeg: Float,
  waypoints: List<WaypointDisplayItem>,
  selectedWaypoint: WaypointDisplayItem?,
  zoomRangeMeters: Float,
  labelPaint: Paint,
  pulseGlow: Float,
) {
  val pixelsPerMeter = innerGridRadius / zoomRangeMeters

  waypoints.forEach { wp ->
    val visualAngleDeg = (wp.bearingDeg - azimuthDeg - 90f)
    val rad = Math.toRadians(visualAngleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    val isSelected = selectedWaypoint?.entity?.id == wp.entity.id
    val baseColor = try {
      Color(android.graphics.Color.parseColor(wp.entity.colorHex))
    } catch (_: Exception) {
      Color(0xFF00D2FF)
    }

    // 1. Posición en el bisel exterior (Indicador de rumbo angular fijo)
    val bezelMarkerRadius = dialRadius + 14f
    val bezelX = center.x + bezelMarkerRadius * cosA
    val bezelY = center.y + bezelMarkerRadius * sinA

    drawCircle(
      color = baseColor.copy(alpha = if (isSelected) pulseGlow else 0.45f),
      radius = if (isSelected) 14f else 9f,
      center = Offset(bezelX, bezelY)
    )
    drawCircle(
      color = if (isSelected) Color.White else baseColor,
      radius = if (isSelected) 7f else 5f,
      center = Offset(bezelX, bezelY)
    )

    // 2. Proyección 3D en la rejilla del radar (Pin con mástil vertical)
    val radarDist = (wp.distanceMeters * pixelsPerMeter).toFloat()
    if (radarDist <= innerGridRadius * 0.98f) {
      val baseX = center.x + radarDist * cosA
      val baseY = center.y + radarDist * sinA

      // En vista 3D: el mástil se eleva perpendicularmente hacia arriba (pantalla -Y)
      val effectiveMast = if (is3DMode) mastHeight else 0f
      val topX = baseX
      val topY = baseY - effectiveMast

      // 2.1 Base de anclaje (Z=0) con sombra elíptica
      if (is3DMode) {
        drawOval(
          color = Color.Black.copy(alpha = 0.5f),
          topLeft = Offset(baseX - 10f, baseY - 4f),
          size = androidx.compose.ui.geometry.Size(20f, 8f)
        )
      }
      drawCircle(
        color = baseColor.copy(alpha = 0.6f),
        radius = 4f,
        center = Offset(baseX, baseY)
      )

      // 2.2 Mástil vertical perpendicular
      if (effectiveMast > 0f) {
        drawLine(
          color = baseColor.copy(alpha = 0.85f),
          start = Offset(baseX, baseY),
          end = Offset(topX, topY),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round
        )
      }

      // 2.3 Cabeza de Pin / Baliza flotante en la cima del mástil
      drawCircle(
        color = baseColor.copy(alpha = if (isSelected) pulseGlow else 0.45f),
        radius = if (isSelected) 15f else 10f,
        center = Offset(topX, topY)
      )
      drawCircle(
        color = if (isSelected) Color.White else baseColor,
        radius = if (isSelected) 7.5f else 5.5f,
        center = Offset(topX, topY)
      )

      // Si está seleccionado, mostrar etiqueta flotante sobre el Pin
      if (isSelected) {
        val distText = CompassCalculations.formatDistanceHuman(wp.distanceMeters)
        labelPaint.color = baseColor.toArgb()
        drawContext.canvas.nativeCanvas.drawText(
          "${wp.entity.name} ($distText)",
          topX,
          topY - 14f,
          labelPaint
        )
      }
    } else if (isSelected) {
      // Si está fuera del rango visible del radar pero seleccionado, etiqueta en el bisel
      val tagRadius = dialRadius + 38f
      val tagX = center.x + tagRadius * cosA
      val tagY = center.y + tagRadius * sinA + 8f

      val distFormatted = CompassCalculations.formatDistanceHuman(wp.distanceMeters)
      labelPaint.color = baseColor.toArgb()
      drawContext.canvas.nativeCanvas.drawText(
        "${wp.entity.name} ($distFormatted)",
        tagX,
        tagY,
        labelPaint
      )
    }
  }
}

private fun DrawScope.drawWaypointGuidanceLine(
  center: Offset,
  dialRadius: Float,
  azimuthDeg: Float,
  waypoint: WaypointDisplayItem,
  pulseGlow: Float,
) {
  val visualAngleDeg = (waypoint.bearingDeg - azimuthDeg - 90f)
  val rad = Math.toRadians(visualAngleDeg.toDouble())
  val cosA = cos(rad).toFloat()
  val sinA = sin(rad).toFloat()

  val targetX = center.x + dialRadius * cosA
  val targetY = center.y + dialRadius * sinA

  // Línea guía cian punteada hacia el objetivo
  drawLine(
    color = Color(0xFF00D2FF).copy(alpha = 0.75f * pulseGlow),
    start = center,
    end = Offset(targetX, targetY),
    strokeWidth = 2.dp.toPx(),
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
    cap = StrokeCap.Round
  )
}

private fun DrawScope.drawCenterHorizonReticle(
  center: Offset,
  terrainSlopeDeg: Float,
  lateralTiltDeg: Float,
  isFocusZoomIn: Boolean,
  selectedWaypoint: WaypointDisplayItem?,
  pulseGlow: Float,
) {
  val ringRadius = if (isFocusZoomIn) 36f else 26f
  drawCircle(
    color = Color(0xFF00FF41).copy(alpha = 0.6f),
    radius = ringRadius,
    center = center,
    style = Stroke(width = 2.dp.toPx())
  )

  // Burbuja de nivel de terreno
  val bubbleMaxOffset = ringRadius * 0.75f
  val bubbleOffsetX = (lateralTiltDeg.coerceIn(-30f, 30f) / 30f) * bubbleMaxOffset
  val bubbleOffsetY = (-terrainSlopeDeg.coerceIn(-30f, 30f) / 30f) * bubbleMaxOffset
  val bubbleCenter = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)

  val isLevel = kotlin.math.abs(terrainSlopeDeg) < 2.5f && kotlin.math.abs(lateralTiltDeg) < 2.5f
  val bubbleColor = if (isLevel) Color(0xFF00FF41) else Color(0xFF00D2FF)

  drawCircle(
    color = bubbleColor.copy(alpha = if (isLevel) pulseGlow else 0.8f),
    radius = if (isLevel) 7.5f else 5.5f,
    center = bubbleCenter
  )

  if (isLevel) {
    drawLine(
      color = Color(0xFF00FF41).copy(alpha = 0.85f),
      start = Offset(center.x - 8f, center.y),
      end = Offset(center.x + 8f, center.y),
      strokeWidth = 2f
    )
    drawLine(
      color = Color(0xFF00FF41).copy(alpha = 0.85f),
      start = Offset(center.x, center.y - 8f),
      end = Offset(center.x, center.y + 8f),
      strokeWidth = 2f
    )
  }

  // En modo de enfoque cerrado (Zoom In ≤ 100m), flecha de rumbo directa al objetivo
  if (isFocusZoomIn && selectedWaypoint != null) {
    val angleRad = Math.toRadians((selectedWaypoint.relativeAngleDeg - 90f).toDouble())
    val arrowLen = 48f
    val tipX = center.x + arrowLen * cos(angleRad).toFloat()
    val tipY = center.y + arrowLen * sin(angleRad).toFloat()

    drawLine(
      color = Color(0xFF00D2FF),
      start = center,
      end = Offset(tipX, tipY),
      strokeWidth = 3.5.dp.toPx(),
      cap = StrokeCap.Round
    )
  }
}
