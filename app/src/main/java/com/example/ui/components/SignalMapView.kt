package com.example.ui.components

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.preference.PreferenceManager
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.DeviceType
import com.example.data.model.TacticalMarker
import com.example.location.UserLocation
import com.example.navigation.InstructionType
import com.example.navigation.NavigationMode
import com.example.navigation.SignalRoute
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi
import com.example.ui.theme.TechDarkBackground
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignalMapView(
  userLocation: UserLocation?,
  markers: List<TacticalMarker>,
  targetMarker: TacticalMarker?,
  inspectedMarker: TacticalMarker?,
  route: SignalRoute?,
  navigationMode: NavigationMode,
  onMarkerSelected: (TacticalMarker) -> Unit,
  onNavigateToMarker: (TacticalMarker) -> Unit,
  onModeChanged: (NavigationMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var hasCenteredUser by remember { mutableStateOf(false) }

  val onlineCount = markers.count { it.isOnline }
  val offlineCount = markers.count { !it.isOnline }

  // Lifecycle management for OSMDroid MapView
  DisposableEffect(Unit) {
    onDispose {
      mapViewRef?.onDetach()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBackground)
  ) {
    if (userLocation == null) {
      // Waiting for real GPS fix from hardware sensor
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(CyanPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.GpsNotFixed,
              contentDescription = null,
              tint = CyanPrimary,
              modifier = Modifier.size(38.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Buscando satélites GPS...",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Esperando coordenadas exactas del chip GPS para calibrar el mapa táctico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      // 1. Native 100% Kotlin OSMDroid MapView (inflated from R.layout.layout_tactical_map)
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("native_osm_map_view"),
        factory = { ctx ->
          // Initialize OSMDroid configuration
          Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
          Configuration.getInstance().userAgentValue = ctx.packageName

          val view = LayoutInflater.from(ctx).inflate(R.layout.layout_tactical_map, null, false)
          val osmMapView = view.findViewById<MapView>(R.id.osm_map_view)

          // Configure dark tactical theme & behavior
          osmMapView.setTileSource(TileSourceFactory.MAPNIK)
          osmMapView.setMultiTouchControls(true)
          osmMapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
          osmMapView.setBackgroundColor(android.graphics.Color.parseColor("#070B14"))

          // Apply tactical dark inverted color filter matrix
          val tacticalDarkMatrix = ColorMatrix().apply {
            // Negative inversion + contrast for night military/tactical aesthetics
            val invertMatrix = floatArrayOf(
              -0.85f, 0.0f, 0.0f, 0.0f, 225f,
              0.0f, -0.85f, 0.0f, 0.0f, 225f,
              0.0f, 0.0f, -0.85f, 0.0f, 235f,
              0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
            set(invertMatrix)
          }
          osmMapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(tacticalDarkMatrix))

          // Initial position
          osmMapView.controller.setZoom(17.5)
          osmMapView.controller.setCenter(GeoPoint(userLocation.latitude, userLocation.longitude))

          mapViewRef = osmMapView
          view
        },
        update = { _ ->
          val mapView = mapViewRef ?: return@AndroidView

          // Center on user if initial
          if (!hasCenteredUser) {
            mapView.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
            hasCenteredUser = true
          }

          // Clear previous markers & polylines from overlayManager
          mapView.overlays.removeAll { it is Marker || it is Polyline }

          // A. Native Marker: User GPS Live Location (Bright Blue Glowing Dot)
          val userGeo = GeoPoint(userLocation.latitude, userLocation.longitude)
          val userMarker = Marker(mapView).apply {
            position = userGeo
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_marker_user_gps)
            title = "Tu Ubicación GPS"
            snippet = "Precisión: ±${userLocation.accuracy.toInt()}m"
          }
          mapView.overlays.add(userMarker)

          // B. Native Markers: Each detected network (Verde En Vivo vs Gris Offline/Histórico)
          markers.forEach { markerItem ->
            val markerGeo = GeoPoint(markerItem.lat, markerItem.lon)
            val isTarget = targetMarker?.bssidMac.equals(markerItem.bssidMac, ignoreCase = true)
            val isInspected = inspectedMarker?.bssidMac.equals(markerItem.bssidMac, ignoreCase = true)

            val osmMarker = Marker(mapView).apply {
              position = markerGeo
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = ContextCompat.getDrawable(
                context,
                if (markerItem.isOnline) R.drawable.ic_marker_wifi_online else R.drawable.ic_marker_wifi_offline
              )
              title = markerItem.name
              snippet = "${markerItem.rssi} dBm • ${if (markerItem.isOnline) "En Vivo" else "Histórico"} (~${markerItem.distanceMeters.toInt()}m)"
              setOnMarkerClickListener { _, _ ->
                onMarkerSelected(markerItem)
                true
              }
            }
            mapView.overlays.add(osmMarker)
          }

          // C. Native Polyline: Route between User GPS and Target Marker
          if (targetMarker != null && route != null) {
            val polyline = Polyline(mapView).apply {
              outlinePaint.color = if (navigationMode == NavigationMode.DIRECT) {
                android.graphics.Color.parseColor("#00D2FF") // Neon Cyan for Direct line
              } else {
                android.graphics.Color.parseColor("#00E676") // Neon Green for Streets mode
              }
              outlinePaint.strokeWidth = 10f
              outlinePaint.strokeCap = Paint.Cap.ROUND
              outlinePaint.strokeJoin = Paint.Join.ROUND

              // Convert route waypoints to OSMDroid GeoPoints
              val geoPoints = route.waypoints.map { GeoPoint(it.lat, it.lon) }
              setPoints(geoPoints)
            }
            mapView.overlays.add(polyline)
          }

          mapView.invalidate()
        }
      )

      // 2. Tactical Map Header: Legend Counters & Mode Selector
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
          .align(Alignment.TopCenter)
      ) {
        // Stats bar: Verde (En Vivo) vs Gris (Histórico)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Online green badge
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF00E676))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "En Vivo: $onlineCount",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF00E676)
              )

              Spacer(modifier = Modifier.width(16.dp))

              // Offline gray badge
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF94A3B8))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Históricos: $offlineCount",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF94A3B8)
              )
            }

            // Total indicator
            Surface(
              color = CyanPrimary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "Total: ${markers.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CyanPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Route HUD if route is calculated
        if (route != null && targetMarker != null) {
          val primaryInstruction = route.instructions.firstOrNull()?.text ?: "Navegando..."
          val instrType = route.instructions.firstOrNull()?.type ?: InstructionType.STRAIGHT

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(
                    if (navigationMode == NavigationMode.STREETS) EmeraldWifi.copy(alpha = 0.2f) else CyanPrimary.copy(alpha = 0.2f)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = when (instrType) {
                    InstructionType.TURN_RIGHT -> Icons.Default.TurnRight
                    InstructionType.TURN_LEFT -> Icons.Default.TurnRight
                    InstructionType.ARRIVED -> Icons.Default.CheckCircle
                    InstructionType.DIRECT_BEARING -> Icons.Default.NearMe
                    InstructionType.STRAIGHT -> Icons.Default.Straight
                  },
                  contentDescription = null,
                  tint = if (navigationMode == NavigationMode.STREETS) EmeraldWifi else CyanPrimary,
                  modifier = Modifier.size(24.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = primaryInstruction,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Distancia: ${route.totalDistanceMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = CyanPrimary
                  )
                  Text(
                    text = " • ${if (targetMarker.isOnline) "RSSI en vivo: ${targetMarker.rssi} dBm" else "Último RSSI: ${targetMarker.rssi} dBm"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Modo Calles vs Línea Directa Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = navigationMode == NavigationMode.STREETS,
              onClick = { onModeChanged(NavigationMode.STREETS) },
              label = { Text("Modo Calles") },
              leadingIcon = {
                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(14.dp))
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EmeraldWifi.copy(alpha = 0.2f),
                selectedLabelColor = EmeraldWifi,
                selectedLeadingIconColor = EmeraldWifi
              ),
              modifier = Modifier.testTag("chip_mode_streets")
            )

            FilterChip(
              selected = navigationMode == NavigationMode.DIRECT,
              onClick = { onModeChanged(NavigationMode.DIRECT) },
              label = { Text("Línea Directa") },
              leadingIcon = {
                Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp))
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                selectedLabelColor = CyanPrimary,
                selectedLeadingIconColor = CyanPrimary
              ),
              modifier = Modifier.testTag("chip_mode_direct")
            )
          }
        }
      }

      // 3. Floating Native Map Controls (Zoom In, Zoom Out, Re-center GPS)
      Column(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          shadowElevation = 4.dp
        ) {
          Column {
            IconButton(
              onClick = { mapViewRef?.controller?.zoomIn() },
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            IconButton(
              onClick = { mapViewRef?.controller?.zoomOut() },
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          shadowElevation = 4.dp
        ) {
          IconButton(
            onClick = {
              mapViewRef?.controller?.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
            },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar GPS", tint = CyanPrimary)
          }
        }
      }

      // 4. Bottom Tactical Marker Inspection Card: 'Navegar hacia esta señal'
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
          .align(Alignment.BottomCenter)
      ) {
        val activeInspection = inspectedMarker ?: targetMarker
        if (activeInspection != null) {
          val isOnline = activeInspection.isOnline
          val statusColor = if (isOnline) Color(0xFF00E676) else Color(0xFF94A3B8)

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("inspected_marker_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
            ) {
              // Top info row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (activeInspection.type == DeviceType.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
                      contentDescription = null,
                      tint = statusColor,
                      modifier = Modifier.size(22.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Column {
                    Text(
                      text = activeInspection.name,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "MAC: ${activeInspection.bssidMac}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                      ),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                // Status Pill: Verde (En Vivo) vs Gris (Histórico)
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = statusColor.copy(alpha = 0.15f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isOnline) "EN VIVO" else "OFFLINE",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = statusColor
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Telemetry stats row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = if (isOnline) "Potencia Actual" else "Última Potencia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "${activeInspection.rssi} dBm",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    ),
                    color = statusColor
                  )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "Distancia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "~${activeInspection.distanceMeters.toInt()} m",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = if (isOnline) "Detectado" else "Fecha Registro",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = if (isOnline) "Ahora" else formatTimestamp(activeInspection.lastSeenTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // The required action button: 'Navegar hacia esta señal'
              Button(
                onClick = { onNavigateToMarker(activeInspection) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isOnline) EmeraldWifi else CyanPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("navigate_to_marker_button")
              ) {
                Icon(
                  imageVector = Icons.Default.NearMe,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.surface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Navegar hacia esta señal",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.surface
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
  return sdf.format(date)
}
