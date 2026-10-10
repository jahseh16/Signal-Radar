package com.example.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalBackground
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalNeonBlue
import com.example.ui.theme.TacticalNeonGreen

@Composable
fun DashboardScreen(
  onNavigateToScanner: () -> Unit,
  onNavigateToCompass: () -> Unit,
  onNavigateToCameraAr: () -> Unit,
  onNavigateToTacticalTools: () -> Unit,
  savedNetworksCount: Int,
  wifiCount: Int,
  bleCount: Int,
  modifier: Modifier = Modifier,
) {
  // Pulsating animation for tactical radar header
  val infiniteTransition = rememberInfiniteTransition(label = "dash_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // 1. TACTICAL HEADER & TELEMETRY STATUS
    Surface(
      color = TacticalCard,
      shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
      border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp)
      ) {
        // Telemetry system line
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TacticalNeonGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SYS_ONLINE // MODO TÁCTICO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              ),
              color = TacticalNeonGreen
            )
          }

          Text(
            text = "GPS: ALTA PRECISIÓN",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            ),
            color = TacticalNeonBlue
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Branding row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(TacticalNeonGreen.copy(alpha = 0.25f), Color.Transparent)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(TacticalNeonGreen.copy(alpha = 0.35f))
            )
            Icon(
              imageVector = Icons.Default.TrackChanges,
              contentDescription = null,
              tint = TacticalNeonGreen,
              modifier = Modifier.size(30.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "SIGNALRADAR",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
              ),
              color = Color.White
            )
            Text(
              text = "Centro de Operaciones Tácticas y Radiofrecuencia",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Telemetry Pills (Base de datos SQLite, Wi-Fi en vivo, BLE)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TelemetryPill(
            icon = Icons.Default.Storage,
            label = "DB Guardados",
            value = "$savedNetworksCount",
            tint = TacticalNeonGreen,
            modifier = Modifier.weight(1f)
          )
          TelemetryPill(
            icon = Icons.Default.Wifi,
            label = "Wi-Fi Vivo",
            value = "$wifiCount",
            tint = TacticalNeonGreen,
            modifier = Modifier.weight(1f)
          )
          TelemetryPill(
            icon = Icons.Default.GpsFixed,
            label = "Bluetooth",
            value = "$bleCount",
            tint = TacticalNeonBlue,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section title
    PaddingBox(horizontal = 20.dp) {
      Text(
        text = "MÓDULOS DE CAMPO DISPONIBLES",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = Color(0xFF64748B)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. TACTICAL 2-COLUMN GRID OF CARDS (LAS 4 TARJETAS REQUERIDAS)
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .testTag("dashboard_grid")
    ) {
      // TARJETA 1 (Señales & Radar)
      item {
        TacticalCardItem(
          title = "Radar & Escáner de Señales",
          description = "Redes Wi-Fi, Dispositivos Bluetooth y Modo Mapa/Radar",
          badge = "EN VIVO",
          icon = Icons.Default.TrackChanges,
          accentColor = TacticalNeonGreen,
          testTag = "card_scanner_signals",
          onClick = onNavigateToScanner
        )
      }

      // TARJETA 2 (Topografía 3D)
      item {
        TacticalCardItem(
          title = "Brújula 3D & Altímetro",
          description = "Orientación en grados, inclinación y metros sobre el nivel del mar",
          badge = "3D INERCIAL",
          icon = Icons.Default.Explore,
          accentColor = TacticalNeonBlue,
          testTag = "card_compass_3d",
          onClick = onNavigateToCompass
        )
      }

      // TARJETA 3 (Cámara AR)
      item {
        TacticalCardItem(
          title = "Medidor de Suelo (Cámara)",
          description = "Escáner de suelo y distancias en metros",
          badge = "VISIÓN AR",
          icon = Icons.Default.CameraAlt,
          accentColor = TacticalNeonBlue,
          testTag = "card_camera_ar",
          onClick = onNavigateToCameraAr
        )
      }

      // TARJETA 4 (Herramientas Extra)
      item {
        TacticalCardItem(
          title = "Herramientas Tácticas",
          description = "Opciones de inspección y campo",
          badge = "UTILIDADES",
          icon = Icons.Default.Construction,
          accentColor = TacticalNeonGreen,
          testTag = "card_tactical_tools",
          onClick = onNavigateToTacticalTools
        )
      }

      // Footer notice span
      item(span = { GridItemSpan(2) }) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = TacticalCard.copy(alpha = 0.6f),
          border = BorderStroke(1.dp, Color(0xFF1E293B)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = TacticalNeonGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Base de datos Room activa para guardado automático de redes (War-walking).",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TacticalCardItem(
  title: String,
  description: String,
  badge: String,
  icon: ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .height(190.dp)
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = TacticalCard),
    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.85f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top row: Icon with glowing container & Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp
            ),
            color = accentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      // Bottom: Title & Description
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 18.sp
          ),
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            lineHeight = 15.sp
          ),
          color = Color(0xFF94A3B8),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun TelemetryPill(
  icon: ImageVector,
  label: String,
  value: String,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF070B14),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = Color(0xFF64748B)
        )
        Text(
          text = value,
          style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          ),
          color = Color.White
        )
      }
    }
  }
}

@Composable
private fun PaddingBox(horizontal: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
  Box(modifier = Modifier.padding(horizontal = horizontal)) {
    content()
  }
}
