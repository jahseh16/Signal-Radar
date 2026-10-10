package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import com.example.data.model.SignalTier
import com.example.ui.theme.BlueBle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi
import com.example.ui.theme.SignalExcellent
import com.example.ui.theme.SignalFair
import com.example.ui.theme.SignalGood
import com.example.ui.theme.SignalWeak

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
  device: ScannedDevice,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onNavigateToSignal: ((ScannedDevice) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val (signalColor, signalQuality) = when (device.signalTier) {
    SignalTier.EXCELLENT -> SignalExcellent to "Excelente (Cercano)"
    SignalTier.GOOD -> SignalGood to "Buena Señal"
    SignalTier.FAIR -> SignalFair to "Señal Media"
    SignalTier.WEAK -> SignalWeak to "Señal Débil"
  }

  val typeColor = if (device.type == DeviceType.WIFI) EmeraldWifi else BlueBle
  val typeLabel = if (device.type == DeviceType.WIFI) "Red Wi-Fi" else "Dispositivo Bluetooth LE"

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = modifier.testTag("device_detail_sheet")
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)
    ) {
      item {
        // Header with Icon, Name, and Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(typeColor.copy(alpha = 0.2f))
              .border(1.5.dp, typeColor, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (device.type == DeviceType.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
              contentDescription = null,
              tint = typeColor,
              modifier = Modifier.size(26.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = device.displayName,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = typeLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = typeColor
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Signal Strength Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, signalColor.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Potencia de Señal (RSSI)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = signalQuality,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = signalColor
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Text(
                text = "${device.rssi} dBm",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                color = signalColor
              )
              Text(
                text = "Distancia aprox: ~${device.estimatedDistanceMeters} m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Action: Navigate towards this signal on the Map
        if (onNavigateToSignal != null) {
          Button(
            onClick = {
              onNavigateToSignal(device)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (device.type == DeviceType.WIFI) EmeraldWifi else CyanPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("navigate_to_signal_button")
          ) {
            Icon(
              imageVector = Icons.Default.NearMe,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.surface,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Trazar Ruta en el Mapa hacia esta Señal",
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.surface
            )
          }

          Spacer(modifier = Modifier.height(18.dp))
        }

        // Details grid
        Text(
          text = "Parámetros Técnicos",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // MAC / BSSID Item
        TechnicalRow(
          icon = Icons.Default.Info,
          label = if (device.type == DeviceType.WIFI) "BSSID (Dirección MAC)" else "MAC Address BLE",
          value = device.macAddress,
          isMonospace = true,
          onCopy = {
            clipboardManager.setText(AnnotatedString(device.macAddress))
            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
          }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Wi-Fi Specific properties
        if (device.type == DeviceType.WIFI) {
          device.bandLabel?.let { band ->
            TechnicalRow(
              icon = Icons.Default.Speed,
              label = "Banda / Canal",
              value = band
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }

          device.frequency?.let { freq ->
            TechnicalRow(
              icon = Icons.Default.NetworkCheck,
              label = "Frecuencia Central",
              value = "$freq MHz"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }

          device.capabilities?.let { caps ->
            TechnicalRow(
              icon = Icons.Default.Security,
              label = "Capacidades de Seguridad",
              value = caps
            )
          }
        }

        // Bluetooth Specific properties
        if (device.type == DeviceType.BLUETOOTH_LE) {
          device.txPower?.let { tx ->
            TechnicalRow(
              icon = Icons.Default.Speed,
              label = "Potencia Transmisión (TxPower)",
              value = "$tx dBm"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }

          device.isConnectable?.let { conn ->
            TechnicalRow(
              icon = Icons.Default.NetworkCheck,
              label = "Estado de Conexión",
              value = if (conn) "Conectable (GATT Server)" else "Sólo Publicidad (Beacon)"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }

          if (device.serviceUuids.isNotEmpty()) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
              Text(
                text = "UUIDs de Servicios GATT Advertised (${device.serviceUuids.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              device.serviceUuids.forEach { uuid ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                ) {
                  Text(
                    text = uuid,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Close button
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Text("Cerrar Detalles", color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }
  }
}

@Composable
private fun TechnicalRow(
  icon: ImageVector,
  label: String,
  value: String,
  isMonospace: Boolean = false,
  onCopy: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = value,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    if (onCopy != null) {
      IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Copiar",
          tint = CyanPrimary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
