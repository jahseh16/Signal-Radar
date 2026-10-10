package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalBackground
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalNeonBlue
import com.example.ui.theme.TacticalNeonGreen

private data class TacticalToolItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val status: String,
  val accentColor: Color,
)

@Composable
fun TacticalToolsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onNavigateBack() }

  var stealthMode by remember { mutableStateOf(false) }

  val tools = listOf(
    TacticalToolItem(
      id = "ping_analyzer",
      title = "Ping & Diagnóstico de Paquetes",
      subtitle = "Latencia ICMP y análisis de pérdida en redes enlazadas",
      icon = Icons.Default.NetworkPing,
      status = "LISTO",
      accentColor = TacticalNeonGreen
    ),
    TacticalToolItem(
      id = "spectrum_rf",
      title = "Analizador de Espectro RF",
      subtitle = "Distribución de canales Wi-Fi 2.4 GHz y 5.8 GHz",
      icon = Icons.Default.CellTower,
      status = "LISTO",
      accentColor = TacticalNeonBlue
    ),
    TacticalToolItem(
      id = "export_warwalk",
      title = "Exportar Registros War-Walking",
      subtitle = "Generar archivo KML / CSV con todas las balizas georreferenciadas",
      icon = Icons.Default.FileDownload,
      status = "DISPONIBLE",
      accentColor = TacticalNeonGreen
    ),
    TacticalToolItem(
      id = "strobe_light",
      title = "Faro Estroboscópico SOS",
      subtitle = "Baliza visual intermitente con el flash de la cámara táctica",
      icon = Icons.Default.FlashlightOn,
      status = "STANDBY",
      accentColor = TacticalNeonBlue
    ),
    TacticalToolItem(
      id = "velocity_meter",
      title = "Velocímetro de Patrulla",
      subtitle = "Medición de velocidad de marcha en tiempo real por GPS Doppler",
      icon = Icons.Default.Speed,
      status = "0.0 KM/H",
      accentColor = TacticalNeonGreen
    )
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // 1. TOP BAR
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
          modifier = Modifier.testTag("tactical_tools_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Regresar al Dashboard",
            tint = TacticalNeonGreen
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Text(
            text = "HERRAMIENTAS TÁCTICAS",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = Color.White
          )
          Text(
            text = "Opciones de Inspección, Análisis y Operaciones de Campo",
            style = MaterialTheme.typography.bodySmall,
            color = TacticalNeonGreen
          )
        }
      }
    }

    // 2. STEALTH MODE TOGGLE
    Card(
      colors = CardDefaults.cardColors(containerColor = TacticalCard),
      border = BorderStroke(1.dp, if (stealthMode) TacticalNeonGreen else Color(0xFF1B2A3D)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = if (stealthMode) TacticalNeonGreen else Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Modo Sigilo (Stealth)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
            Text(
              text = if (stealthMode) "Transmisiones BLE y Wi-Fi inactivas (solo escucha pasiva)" else "Modo estándar activo",
              style = MaterialTheme.typography.bodySmall,
              color = if (stealthMode) TacticalNeonGreen else Color(0xFF94A3B8)
            )
          }
        }

        Switch(
          checked = stealthMode,
          onCheckedChange = { stealthMode = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.Black,
            checkedTrackColor = TacticalNeonGreen,
            uncheckedThumbColor = Color.Gray,
            uncheckedTrackColor = Color(0xFF1E293B)
          )
        )
      }
    }

    // 3. TOOLS LIST
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(tools, key = { it.id }) { item ->
        Card(
          colors = CardDefaults.cardColors(containerColor = TacticalCard),
          border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_card_${item.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = item.accentColor.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.4f)),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = item.accentColor,
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = item.accentColor.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.35f))
            ) {
              Text(
                text = item.status,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp
                ),
                color = item.accentColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
