package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.compass.CompassViewModel
import com.example.compass.WaypointDisplayItem
import com.example.compass.ui.AppleWatchUltraCompassView
import com.example.compass.util.CompassCalculations
import com.example.location.UserLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Compass3DScreen(
  onNavigateBack: () -> Unit,
  userLocation: UserLocation? = null,
  viewModel: CompassViewModel = viewModel(),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  BackHandler {
    if (uiState.selectedWaypoint != null) {
      viewModel.selectWaypoint(null)
    } else {
      onNavigateBack()
    }
  }

  // Animación de pulso para la grabación activa de camino
  val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
  val recScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "rec_scale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF05070A)) // Estética táctica oscura premium
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // 1. BARRA SUPERIOR TÁCTICA
    Surface(
      color = Color(0xFF0A0F17),
      border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("compass_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Regresar al menú principal",
              tint = Color(0xFF00D2FF)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          Column {
            Text(
              text = "BRÚJULA & NAVEGACIÓN",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = Color.White
            )
            Text(
              text = if (uiState.is3DMode) "Vista 3D con Perspectiva Inclinada" else "Vista 2D Cenital Plana",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color(0xFF00FF41)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Botón Toggle para alternar entre Vista 2D y Vista 3D
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (uiState.is3DMode) Color(0xFF10283E) else Color(0xFF131D2A),
            border = BorderStroke(1.dp, if (uiState.is3DMode) Color(0xFF00D2FF) else Color(0xFF24364D)),
            modifier = Modifier
              .clickable { viewModel.toggle3DMode() }
              .testTag("toggle_3d_mode_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = "Alternar perspectiva",
                tint = if (uiState.is3DMode) Color(0xFF00D2FF) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (uiState.is3DMode) "3D" else "2D",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold
                ),
                color = if (uiState.is3DMode) Color(0xFF00D2FF) else Color(0xFF94A3B8)
              )
            }
          }

          // Botón Lista de Puntos Guardados (Waypoints en Room)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0C1320),
            border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
            modifier = Modifier
              .clickable { viewModel.openWaypointsListSheet() }
              .testTag("open_waypoints_sheet_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Puntos de referencia",
                tint = Color(0xFF00D2FF),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${uiState.waypoints.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold
                ),
                color = Color.White
              )
            }
          }
        }
      }
    }

    // 2. ENCABEZADO DE RUMBO CON PUNTO CARDINAL EN ESPAÑOL CLARO
    val azimuthInt = ((uiState.sensorData.azimuth % 360f) + 360f) % 360f
    val cardinalDetailed = CompassCalculations.getCardinalDirectionDetailed(azimuthInt)

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = "%03d°".format(azimuthInt.toInt()),
          style = MaterialTheme.typography.displayMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
          ),
          color = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = cardinalDetailed,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = if (cardinalDetailed.startsWith("N ")) Color(0xFFFF3B30) else Color(0xFF00D2FF)
        )
      }

      // Indicador de grabación de camino activa
      if (uiState.isRecordingBacktrack) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFFF3B30).copy(alpha = 0.2f),
          border = BorderStroke(1.dp, Color(0xFFFF3B30))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .scale(recScale)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "GRABANDO CAMINO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
              ),
              color = Color(0xFFFF3B30)
            )
          }
        }
      }
    }

    // 3. RADAR 3D / 2D CON GESTOS DIRECTOS PINCH-TO-ZOOM (SIN BOTONES +/-)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.Center
    ) {
      // Vista principal del radar: dial exterior fijo y escala interna mediante pellizco
      AppleWatchUltraCompassView(
        azimuthDeg = uiState.sensorData.azimuth,
        terrainSlopeDeg = uiState.sensorData.pitch,
        lateralTiltDeg = uiState.sensorData.roll,
        userLocation = uiState.userLocation ?: userLocation,
        waypoints = uiState.waypoints,
        breadcrumbPoints = uiState.breadcrumbPoints,
        isRecordingBacktrack = uiState.isRecordingBacktrack,
        selectedWaypoint = uiState.selectedWaypoint,
        is3DMode = uiState.is3DMode,
        zoomRangeMeters = uiState.zoomRangeMeters,
        onWaypointClicked = { wp ->
          viewModel.selectWaypoint(wp)
        },
        onZoomGesture = { newZoom ->
          viewModel.setZoomRange(newZoom)
        }
      )

      // TARJETA FLOTANTE CON INFORMACIÓN COMPLETA DEL WAYPOINT SELECCIONADO
      androidx.compose.animation.AnimatedVisibility(
        visible = uiState.selectedWaypoint != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        uiState.selectedWaypoint?.let { wp ->
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09111C)),
            border = BorderStroke(1.5.dp, Color(0xFF00D2FF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              // Cabecera: Icono, Nombre y botón de cierre
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = getIconForType(wp.entity.iconType),
                    contentDescription = null,
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = wp.entity.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }

                IconButton(
                  onClick = { viewModel.selectWaypoint(null) },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Cerrar selección", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Métricas clave: Distancia, Desnivel y Rumbo en lenguaje humano
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Distancia al objetivo
                Column(modifier = Modifier.weight(1f)) {
                  Text("Distancia al objetivo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                  Text(
                    text = CompassCalculations.formatDistanceHuman(wp.distanceMeters),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = Color(0xFF00D2FF)
                  )
                }

                // Desnivel
                Column(modifier = Modifier.weight(1f)) {
                  Text("Desnivel", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                  Text(
                    text = CompassCalculations.formatElevationDifferenceHuman(wp.elevationDifferenceMeters),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = if (wp.elevationDifferenceMeters > 0) Color(0xFFFF9500) else Color(0xFF00FF41)
                  )
                }

                // Rumbo exacto en español
                Column(modifier = Modifier.weight(1f)) {
                  Text("Rumbo hacia el punto", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                  Text(
                    text = "%03d° %s".format(wp.bearingDeg.toInt(), CompassCalculations.getCardinalDirection(wp.bearingDeg)),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Altitud sobre el nivel del mar registrada en el momento exacto
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Altitud registrada: %.0f m sobre el nivel del mar".format(wp.entity.altitude),
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = Color(0xFF94A3B8)
                )

                if (wp.entity.accuracy > 0) {
                  Text(
                    text = "Precisión: ±%.1f m".format(wp.entity.accuracy),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Color(0xFF00FF41)
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. TELEMETRÍA CONTINUA EN LENGUAJE HUMANO (BARÓMETRO NATIVO EN TIEMPO REAL)
    val curLoc = uiState.userLocation ?: userLocation

    // Lectura continua de altitud vía barómetro nativo (Sensor.TYPE_PRESSURE)
    val altitudSobreNivelDelMar = uiState.sensorData.barometricAltitudeMeters
      ?: (curLoc?.altitude ?: 0.0)

    Surface(
      color = Color(0xFF0A0F17),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Altitud sobre el nivel del mar (actualizada al instante por presión atmosférica)
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF05080E)),
            border = BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Height, contentDescription = null, tint = Color(0xFF00FF41), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Altitud sobre el nivel del mar", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 9.sp)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "%.0f m".format(altitudSobreNivelDelMar),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = Color.White
              )
              Text(
                text = if (uiState.sensorData.pressureHpa != null) "Sensor barométrico: %.1f hPa".format(uiState.sensorData.pressureHpa) else "Medición satelital",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = Color(0xFF64748B)
              )
            }
          }

          // Inclinación del terreno
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF05080E)),
            border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Landscape, contentDescription = null, tint = Color(0xFF00D2FF), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inclinación del terreno", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 9.sp)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = CompassCalculations.formatSlopeHuman(uiState.sensorData.pitch),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
              Text(
                text = "Ángulo de pendiente frontal",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = Color(0xFF64748B)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Línea de Coordenadas Geográficas
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (curLoc != null) {
              "Coordenadas: ${CompassCalculations.formatCoordinates(curLoc.latitude, curLoc.longitude)}"
            } else {
              "Coordenadas: Buscando satélites..."
            },
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            ),
            color = Color(0xFF94A3B8)
          )

          if (curLoc != null && curLoc.accuracy > 0) {
            Text(
              text = "Precisión: ±%.1f m".format(curLoc.accuracy),
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              ),
              color = Color(0xFF00FF41)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. BOTONES DE ACCIÓN PRINCIPALES
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Botón 'Marcar Punto Actual'
          Button(
            onClick = {
              if (curLoc == null) {
                Toast.makeText(context, "Esperando coordenadas GPS precisas...", Toast.LENGTH_SHORT).show()
              } else {
                viewModel.openAddWaypointDialog()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E2235)),
            border = BorderStroke(1.dp, Color(0xFF00D2FF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("btn_mark_current_point")
          ) {
            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color(0xFF00D2FF), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "MARCAR PUNTO",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
              color = Color(0xFF00D2FF)
            )
          }

          // Botón 'Grabar Camino' (Backtrack con trazo fino de alta precisión)
          Button(
            onClick = {
              viewModel.toggleBacktrack()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (uiState.isRecordingBacktrack) Color(0xFF380C0C) else Color(0xFF0D2818)
            ),
            border = BorderStroke(1.dp, if (uiState.isRecordingBacktrack) Color(0xFFFF3B30) else Color(0xFF00FF41)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("btn_record_route_backtrack")
          ) {
            Icon(
              imageVector = if (uiState.isRecordingBacktrack) Icons.Default.Stop else Icons.Default.Route,
              contentDescription = null,
              tint = if (uiState.isRecordingBacktrack) Color(0xFFFF3B30) else Color(0xFF00FF41),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.isRecordingBacktrack) "DETENER RUTA" else "GRABAR CAMINO",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
              color = if (uiState.isRecordingBacktrack) Color(0xFFFF3B30) else Color(0xFF00FF41)
            )
          }
        }
      }
    }
  }

  // DIÁLOGO PARA MARCAR PUNTO ACTUAL
  if (uiState.isAddWaypointDialogOpen) {
    AddWaypointDialog(
      onDismiss = { viewModel.closeAddWaypointDialog() },
      onSave = { name, colorHex, iconType ->
        viewModel.saveWaypoint(name, colorHex, iconType)
        Toast.makeText(context, "Punto guardado en la base de datos", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // MODAL BOTTOM SHEET: PUNTOS GUARDADOS CON DATOS COMPLETOS
  if (uiState.isWaypointsListSheetOpen) {
    ModalBottomSheet(
      onDismissRequest = { viewModel.closeWaypointsListSheet() },
      sheetState = sheetState,
      containerColor = Color(0xFF0A0F17)
    ) {
      WaypointsListContent(
        waypoints = uiState.waypoints,
        selectedWaypoint = uiState.selectedWaypoint,
        onSelect = { wp ->
          viewModel.selectWaypoint(wp)
          viewModel.closeWaypointsListSheet()
        },
        onDelete = { wp ->
          viewModel.deleteWaypoint(wp.entity.id)
        }
      )
    }
  }
}

@Composable
private fun AddWaypointDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, colorHex: String, iconType: String) -> Unit,
) {
  var waypointName by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf("#00D2FF") }
  var selectedIconType by remember { mutableStateOf("CAMP") }

  val iconPresets = listOf(
    "CAMP" to ("Campamento" to Icons.Default.Cabin),
    "CAR" to ("Coche aparcado" to Icons.Default.DirectionsCar),
    "WATER" to ("Fuente de agua" to Icons.Default.WaterDrop),
    "PEAK" to ("Mirador / Cima" to Icons.Default.Terrain),
    "PIN" to ("Punto general" to Icons.Default.Place)
  )

  val colorOptions = listOf(
    "#00D2FF" to "Azul cian",
    "#00FF41" to "Verde neón",
    "#FF9500" to "Naranja táctico",
    "#FF3B30" to "Rojo alerta",
    "#AF52DE" to "Púrpura"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0A0F17),
    title = {
      Text(
        text = "MARCAR PUNTO ACTUAL",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Guarda tus coordenadas exactas con la altitud sobre el nivel del mar capturada por el barómetro para proyectar el punto en 3D.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = waypointName,
          onValueChange = { waypointName = it },
          label = { Text("Nombre del punto") },
          placeholder = { Text("ej. Coche aparcado") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00D2FF),
            unfocusedBorderColor = Color(0xFF1E293B),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_waypoint_name")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Tipo de punto:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          iconPresets.forEach { (type, pair) ->
            val (_, icon) = pair
            val isSelected = selectedIconType == type
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) Color(0xFF162A3D) else Color(0xFF0D1420))
                .border(1.dp, if (isSelected) Color(0xFF00D2FF) else Color(0xFF1B2A3D), RoundedCornerShape(10.dp))
                .clickable {
                  selectedIconType = type
                  if (waypointName.isBlank()) waypointName = pair.first
                },
              contentAlignment = Alignment.Center
            ) {
              Icon(icon, contentDescription = pair.first, tint = if (isSelected) Color(0xFF00D2FF) else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Color del Pin:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          colorOptions.forEach { (hex, _) ->
            val color = Color(android.graphics.Color.parseColor(hex))
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (selectedColorHex == hex) 3.dp else 1.dp,
                  color = if (selectedColorHex == hex) Color.White else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { selectedColorHex = hex },
              contentAlignment = Alignment.Center
            ) {
              if (selectedColorHex == hex) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(waypointName.trim().ifBlank { "Punto guardado" }, selectedColorHex, selectedIconType)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF)),
        modifier = Modifier.testTag("btn_confirm_save_waypoint")
      ) {
        Text("GUARDAR PUNTO", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCELAR", color = Color(0xFF94A3B8))
      }
    }
  )
}

@Composable
private fun WaypointsListContent(
  waypoints: List<WaypointDisplayItem>,
  selectedWaypoint: WaypointDisplayItem?,
  onSelect: (WaypointDisplayItem) -> Unit,
  onDelete: (WaypointDisplayItem) -> Unit,
) {
  val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 10.dp)
  ) {
    Text(
      text = "PUNTOS GUARDADOS",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
      color = Color.White
    )
    Text(
      text = "Selecciona un punto para ver el mástil 3D y la guía de dirección directa",
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFF00D2FF)
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (waypoints.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No tienes puntos guardados.\nPresiona 'Marcar Punto' para registrar uno.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF64748B),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(380.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(waypoints, key = { it.entity.id }) { wp ->
          val isSelected = selectedWaypoint?.entity?.id == wp.entity.id
          val color = try {
            Color(android.graphics.Color.parseColor(wp.entity.colorHex))
          } catch (_: Exception) {
            Color(0xFF00D2FF)
          }

          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF060910)),
            border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF00D2FF) else Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(wp) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = getIconForType(wp.entity.iconType),
                      contentDescription = null,
                      tint = color,
                      modifier = Modifier.size(20.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Text(
                      text = wp.entity.name,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White
                    )
                    Text(
                      text = "Distancia: ${CompassCalculations.formatDistanceHuman(wp.distanceMeters)}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = Color(0xFF00D2FF)
                    )
                  }
                }

                IconButton(onClick = { onDelete(wp) }) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar punto",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Métricas completas en español claro: Rumbo, Desnivel, Altitud y Coordenadas
              Text(
                text = "Rumbo: ${CompassCalculations.formatBearingHuman(wp.bearingDeg)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
              )
              Text(
                text = "Desnivel: ${CompassCalculations.formatElevationDifferenceHuman(wp.elevationDifferenceMeters)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (wp.elevationDifferenceMeters > 0) Color(0xFFFF9500) else Color(0xFF00FF41)
              )
              Text(
                text = "Altitud registrada: %.0f m sobre el nivel del mar".format(wp.entity.altitude),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
              )
              Text(
                text = "Coordenadas: ${CompassCalculations.formatCoordinates(wp.entity.latitude, wp.entity.longitude)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                color = Color(0xFF64748B)
              )
              Text(
                text = "Registrado el ${dateFormatter.format(Date(wp.entity.createdAt))}${if (wp.entity.accuracy > 0) " (Precisión: ±%.1fm)".format(wp.entity.accuracy) else ""}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color(0xFF64748B)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

private fun getIconForType(iconType: String): ImageVector {
  return when (iconType) {
    "CAR" -> Icons.Default.DirectionsCar
    "WATER" -> Icons.Default.WaterDrop
    "PEAK" -> Icons.Default.Terrain
    "CAMP" -> Icons.Default.Cabin
    else -> Icons.Default.Place
  }
}
