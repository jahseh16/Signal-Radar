package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import com.example.ui.theme.BlueBle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi
import com.example.ui.theme.TechDarkBackground
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarView(
  devices: List<ScannedDevice>,
  selectedDevice: ScannedDevice?,
  onDeviceSelected: (ScannedDevice) -> Unit,
  modifier: Modifier = Modifier,
) {
  var highlightedDevice by remember { mutableStateOf(selectedDevice) }

  // Radar sweep animation
  val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "sweep_angle"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBackground)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Legend & Info
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(EmeraldWifi)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Wi-Fi (${devices.count { it.type == DeviceType.WIFI }})",
          style = MaterialTheme.typography.labelSmall,
          color = EmeraldWifi,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(BlueBle)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "BLE (${devices.count { it.type == DeviceType.BLUETOOTH_LE }})",
          style = MaterialTheme.typography.labelSmall,
          color = BlueBle,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
        color = CyanPrimary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "${devices.size} en radio",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = CyanPrimary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    // Interactive Radar Canvas
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      val canvasSize = min(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

      Canvas(
        modifier = Modifier
          .size(canvasSize.dp)
          .testTag("radar_canvas")
          .pointerInput(devices) {
            detectTapGestures { tapOffset ->
              val center = Offset(size.width / 2f, size.height / 2f)
              val maxRadius = (size.width / 2f) * 0.92f

              // Find closest blip to tap
              var closest: ScannedDevice? = null
              var minDistance = Float.MAX_VALUE

              devices.forEach { device ->
                val (blipX, blipY) = calculateBlipPosition(device, center, maxRadius)
                val dist = sqrt((tapOffset.x - blipX) * (tapOffset.x - blipX) + (tapOffset.y - blipY) * (tapOffset.y - blipY))
                if (dist < 40f && dist < minDistance) {
                  minDistance = dist
                  closest = device
                }
              }

              closest?.let {
                highlightedDevice = it
                onDeviceSelected(it)
              }
            }
          }
      ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.width / 2f) * 0.90f
        val ringColor = Color(0xFF1E3A5F)
        val gridColor = Color(0xFF142A45)

        // 1. Radar Concentric Rings (Distance / RSSI tiers)
        val ringFractions = listOf(0.25f, 0.50f, 0.75f, 1.0f)
        ringFractions.forEach { fraction ->
          drawCircle(
            color = ringColor,
            radius = maxRadius * fraction,
            center = center,
            style = Stroke(
              width = 1.5f,
              pathEffect = if (fraction < 1.0f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
            )
          )
        }

        // 2. Crosshair grid lines
        drawLine(
          color = gridColor,
          start = Offset(center.x - maxRadius, center.y),
          end = Offset(center.x + maxRadius, center.y),
          strokeWidth = 1.2f
        )
        drawLine(
          color = gridColor,
          start = Offset(center.x, center.y - maxRadius),
          end = Offset(center.x, center.y + maxRadius),
          strokeWidth = 1.2f
        )

        // Diagonal lines
        val diagOffset = maxRadius * 0.7071f
        drawLine(
          color = gridColor.copy(alpha = 0.5f),
          start = Offset(center.x - diagOffset, center.y - diagOffset),
          end = Offset(center.x + diagOffset, center.y + diagOffset),
          strokeWidth = 1f
        )
        drawLine(
          color = gridColor.copy(alpha = 0.5f),
          start = Offset(center.x - diagOffset, center.y + diagOffset),
          end = Offset(center.x + diagOffset, center.y - diagOffset),
          strokeWidth = 1f
        )

        // 3. Rotating Radar Sweep Beam
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEnd = Offset(
          center.x + (maxRadius * cos(sweepRad)).toFloat(),
          center.y + (maxRadius * sin(sweepRad)).toFloat()
        )

        drawLine(
          brush = Brush.radialGradient(
            colors = listOf(CyanPrimary, CyanPrimary.copy(alpha = 0.3f), Color.Transparent),
            center = center,
            radius = maxRadius
          ),
          start = center,
          end = sweepEnd,
          strokeWidth = 2.5f,
          cap = StrokeCap.Round
        )

        // 4. Center Sonar Emitter Dot
        drawCircle(
          color = CyanPrimary,
          radius = 6f,
          center = center
        )
        drawCircle(
          color = CyanPrimary.copy(alpha = 0.25f),
          radius = 14f,
          center = center
        )

        // 5. Draw Device Blips
        devices.forEach { device ->
          val (blipX, blipY) = calculateBlipPosition(device, center, maxRadius)
          val isSelected = highlightedDevice?.id == device.id
          val blipColor = if (device.type == DeviceType.WIFI) EmeraldWifi else BlueBle

          // Glow halo
          drawCircle(
            color = blipColor.copy(alpha = if (isSelected) 0.5f else 0.22f),
            radius = if (isSelected) 18f else 11f,
            center = Offset(blipX, blipY)
          )

          // Inner solid blip
          drawCircle(
            color = if (isSelected) Color.White else blipColor,
            radius = if (isSelected) 7f else 5f,
            center = Offset(blipX, blipY)
          )

          if (isSelected) {
            drawCircle(
              color = CyanPrimary,
              radius = 24f,
              center = Offset(blipX, blipY),
              style = Stroke(width = 2f)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Bottom Selected Device Highlight Banner or Instruction
    if (highlightedDevice != null) {
      val sel = highlightedDevice!!
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(
            1.dp,
            if (sel.type == DeviceType.WIFI) EmeraldWifi else BlueBle,
            RoundedCornerShape(16.dp)
          ),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  if (sel.type == DeviceType.WIFI) EmeraldWifi.copy(alpha = 0.2f) else BlueBle.copy(alpha = 0.2f)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (sel.type == DeviceType.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (sel.type == DeviceType.WIFI) EmeraldWifi else BlueBle,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = sel.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
              Text(
                text = "MAC: ${sel.macAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${sel.rssi} dBm",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = if (sel.type == DeviceType.WIFI) EmeraldWifi else BlueBle
            )
            Text(
              text = "~${sel.estimatedDistanceMeters} m",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Toca cualquier punto en el radar para inspeccionar la señal",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Maps device RSSI to distance radius, and MAC hash to a deterministic angle.
 */
private fun calculateBlipPosition(
  device: ScannedDevice,
  center: Offset,
  maxRadius: Float
): Pair<Float, Float> {
  // RSSI range typically -30 dBm (immediate contact) to -95 dBm (threshold limit)
  val clampedRssi = device.rssi.coerceIn(-95, -35)
  // Normalized: 1.0 (strongest) down to 0.0 (weakest)
  val strengthFraction = (clampedRssi - (-95)) / 60.0f
  // Invert so stronger is closer to center
  val distanceFraction = (1.0f - strengthFraction).coerceIn(0.12f, 0.94f)
  val radius = maxRadius * distanceFraction

  // Deterministic angle from MAC address hash so blip stays stationary
  val hash = device.macAddress.hashCode()
  val angleDegrees = ((hash % 360) + 360) % 360
  val rad = Math.toRadians(angleDegrees.toDouble())

  val x = (center.x + radius * cos(rad)).toFloat()
  val y = (center.y + radius * sin(rad)).toFloat()

  return x to y
}
