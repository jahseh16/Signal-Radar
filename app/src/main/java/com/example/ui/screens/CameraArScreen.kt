package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalBackground
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalNeonBlue
import com.example.ui.theme.TacticalNeonGreen

@Composable
fun CameraArScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onNavigateBack() }

  var simulatedDistance by remember { mutableStateOf(2.45f) }
  var isCalibrating by remember { mutableStateOf(false) }

  val infiniteTransition = rememberInfiniteTransition(label = "ar_reticle")
  val laserAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_alpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // 1. TOP APP BAR
    Surface(
      color = TacticalCard,
      border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("camera_ar_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Regresar al Dashboard",
            tint = TacticalNeonBlue
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Text(
            text = "MEDIDOR DE SUELO (CÁMARA)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = Color.White
          )
          Text(
            text = "Escáner Óptico de Suelo y Distancias en Metros",
            style = MaterialTheme.typography.bodySmall,
            color = TacticalNeonBlue
          )
        }
      }
    }

    // 2. AR CAMERA VIEWPORT PLACEHOLDER & RETICLE
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(16.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF060910))
        .border(1.5.dp, TacticalNeonBlue.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
      contentAlignment = Alignment.Center
    ) {
      // Perspective Floor Grid Lines Canvas
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Horizontal perspective floor lines
        val floorStartY = h * 0.55f
        for (i in 1..6) {
          val y = floorStartY + (h - floorStartY) * (i / 6.5f)
          drawLine(
            color = TacticalNeonBlue.copy(alpha = 0.25f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
          )
        }

        // Perspective diagonal lines converging towards horizon
        val horizonCenter = Offset(w / 2f, floorStartY)
        val numDiagonals = 6
        for (i in 0..numDiagonals) {
          val bottomX = w * (i.toFloat() / numDiagonals)
          drawLine(
            color = TacticalNeonBlue.copy(alpha = 0.2f),
            start = horizonCenter,
            end = Offset(bottomX, h),
            strokeWidth = 1.5f
          )
        }
      }

      // Center Laser Reticle HUD
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(100.dp)
            .border(2.dp, TacticalNeonGreen.copy(alpha = laserAlpha), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = null,
            tint = TacticalNeonGreen.copy(alpha = laserAlpha),
            modifier = Modifier.size(48.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = TacticalCard.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, TacticalNeonGreen.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Straighten,
              contentDescription = null,
              tint = TacticalNeonGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DISTANCIA AL SUELO: %.2f m".format(simulatedDistance),
              style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              color = TacticalNeonGreen
            )
          }
        }
      }

      // Top corner telemetry HUD badge
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp),
        contentAlignment = Alignment.TopStart
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = TacticalCard.copy(alpha = 0.8f),
          border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = null,
              tint = TacticalNeonBlue,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SENSOR ÓPTICO // ARCore STANDBY",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              ),
              color = Color.White
            )
          }
        }
      }
    }

    // 3. TELEMETRY & CONTROLS FOOTER
    Surface(
      color = TacticalCard,
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      border = BorderStroke(1.dp, Color(0xFF1B2A3D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
            border = BorderStroke(1.dp, TacticalNeonBlue.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Plano de Suelo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
              Text("Detectado", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TacticalNeonGreen)
            }
          }

          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
            border = BorderStroke(1.dp, TacticalNeonBlue.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Inclinación Óptica", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
              Text("32.5°", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            isCalibrating = !isCalibrating
            simulatedDistance = if (simulatedDistance == 2.45f) 3.12f else 2.45f
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = TacticalNeonBlue
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("ar_calibrate_button")
        ) {
          Icon(
            imageVector = Icons.Default.GridOn,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isCalibrating) "RECALIBRANDO MALLA DE SUELO..." else "CALIBRAR MEDIDOR DE SUELO",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = Color.Black
          )
        }
      }
    }
  }
}
