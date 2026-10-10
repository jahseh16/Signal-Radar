package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import com.example.data.model.SignalTier
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BlueBle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi
import com.example.ui.theme.RedSignal
import com.example.ui.theme.SignalExcellent
import com.example.ui.theme.SignalFair
import com.example.ui.theme.SignalGood
import com.example.ui.theme.SignalWeak

@Composable
fun DeviceCard(
  device: ScannedDevice,
  onClick: () -> Unit,
  onNavigateToSignal: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val (signalColor, signalText) = when (device.signalTier) {
    SignalTier.EXCELLENT -> SignalExcellent to "Excelente"
    SignalTier.GOOD -> SignalGood to "Buena"
    SignalTier.FAIR -> SignalFair to "Media"
    SignalTier.WEAK -> SignalWeak to "Débil"
  }

  val typeColor = if (device.type == DeviceType.WIFI) EmeraldWifi else BlueBle
  val typeLabel = if (device.type == DeviceType.WIFI) "Wi-Fi" else "Bluetooth BLE"

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(18.dp))
      .border(
        width = 1.dp,
        color = typeColor.copy(alpha = 0.25f),
        shape = RoundedCornerShape(18.dp)
      )
      .clickable(onClick = onClick)
      .testTag("device_card_${device.id}"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Top header: Type badge, Band/Tx, and RSSI meter badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Type Badge with Icon
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = typeColor.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (device.type == DeviceType.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
              contentDescription = typeLabel,
              tint = typeColor,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = typeLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = typeColor
            )

            // Frequency or BLE connectable badge
            device.bandLabel?.let { band ->
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• $band",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // RSSI Pill Badge
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = signalColor.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, signalColor.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            SignalIcon(device.rssi, signalColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${device.rssi} dBm",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              ),
              color = signalColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Main Device Name
      Text(
        text = device.displayName,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(6.dp))

      // MAC Address row with copy button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "MAC: ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = device.macAddress,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(device.macAddress))
              Toast.makeText(context, "MAC copiada al portapapeles", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copiar MAC",
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Estimated Distance
        Text(
          text = "~${device.estimatedDistanceMeters} m",
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Wi-Fi Security or BLE service info footer if present
      if (!device.capabilities.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          val isSecure = !device.capabilities.contains("ESS") || device.capabilities.contains("WPA")
          Icon(
            imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            tint = if (isSecure) MaterialTheme.colorScheme.onSurfaceVariant else AmberSignal,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = formatCapabilities(device.capabilities),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      } else if (device.serviceUuids.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "${device.serviceUuids.size} Servicios GATT detectados",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Quick Navigate Button to Trace Route
      if (onNavigateToSignal != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          FilledTonalButton(
            onClick = onNavigateToSignal,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = typeColor.copy(alpha = 0.15f),
              contentColor = typeColor
            )
          ) {
            Icon(
              imageVector = Icons.Default.NearMe,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Navegar hacia la señal",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SignalIcon(rssi: Int, tint: Color) {
  val icon = when {
    rssi >= -60 -> Icons.Default.NetworkWifi
    rssi >= -72 -> Icons.Default.NetworkWifi3Bar
    rssi >= -84 -> Icons.Default.NetworkWifi2Bar
    else -> Icons.Default.NetworkWifi1Bar
  }
  Icon(
    imageVector = icon,
    contentDescription = "Signal Level",
    tint = tint,
    modifier = Modifier.size(14.dp)
  )
}

private fun formatCapabilities(raw: String): String {
  val clean = raw.replace("[", " ").replace("]", " ").trim()
  return when {
    clean.contains("WPA3") -> "WPA3 Personal / Enterprise"
    clean.contains("WPA2") -> "WPA2 Personal"
    clean.contains("WPA") -> "WPA Personal"
    clean.contains("WEP") -> "WEP (Inseguro)"
    clean.isBlank() || clean == "ESS" -> "Red Abierta (Sin contraseña)"
    else -> clean
  }
}
