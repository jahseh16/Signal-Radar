package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanTab
import com.example.ui.SortMode
import com.example.ui.theme.BlueBle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWifi

@Composable
fun TopControlPanel(
  currentTab: ScanTab,
  isScanning: Boolean,
  wifiCount: Int,
  bleCount: Int,
  searchQuery: String,
  sortMode: SortMode,
  onTabSelected: (ScanTab) -> Unit,
  onToggleScan: () -> Unit,
  onClear: () -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onSortModeChanged: (SortMode) -> Unit,
  onNavigateBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var isSearchVisible by remember { mutableStateOf(false) }
  var isSortMenuExpanded by remember { mutableStateOf(false) }

  // Pulsating animation for active scanning indicator
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Top row: App branding & action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier
                .size(36.dp)
                .testTag("scanner_back_to_dashboard_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar al Dashboard",
                tint = CyanPrimary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          // Radar pulsating icon
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(CyanPrimary.copy(alpha = 0.35f), Color.Transparent)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isScanning) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(CyanPrimary.copy(alpha = 0.4f))
              )
            }
            Icon(
              imageVector = Icons.Default.TrackChanges,
              contentDescription = "Radar Icon",
              tint = CyanPrimary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "SignalRadar",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (isScanning) EmeraldWifi else Color.Gray)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isScanning) "Escaneando en vivo" else "Escaneo pausado",
                style = MaterialTheme.typography.bodySmall,
                color = if (isScanning) EmeraldWifi else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Action Buttons Row (Scan/Stop, Search, Menu)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Search toggle
          IconButton(
            onClick = { isSearchVisible = !isSearchVisible },
            modifier = Modifier.testTag("toggle_search_button")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Buscar",
              tint = if (searchQuery.isNotEmpty() || isSearchVisible) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Sort Menu
          Box {
            IconButton(
              onClick = { isSortMenuExpanded = true },
              modifier = Modifier.testTag("sort_menu_button")
            ) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Ordenar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            DropdownMenu(
              expanded = isSortMenuExpanded,
              onDismissRequest = { isSortMenuExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text("Mayor Señal (dBm)") },
                onClick = {
                  onSortModeChanged(SortMode.STRONGEST_SIGNAL)
                  isSortMenuExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Nombre Alfabético") },
                onClick = {
                  onSortModeChanged(SortMode.NAME_ALPHABETICAL)
                  isSortMenuExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Distancia Estimada") },
                onClick = {
                  onSortModeChanged(SortMode.ESTIMATED_DISTANCE)
                  isSortMenuExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Limpiar Lista") },
                onClick = {
                  onClear()
                  isSortMenuExpanded = false
                }
              )
            }
          }

          // Primary Scan / Stop Button
          ElevatedButton(
            onClick = onToggleScan,
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = if (isScanning) Color(0xFF8B0000) else CyanPrimary,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("toggle_scan_button")
          ) {
            Icon(
              imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (isScanning) "Detener" else "Escanear",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isScanning) "Pausar" else "Escanear",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }

      // Collapsible search bar
      AnimatedVisibility(visible = isSearchVisible || searchQuery.isNotEmpty()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChanged,
          placeholder = { Text("Filtrar por SSID, nombre o MAC...") },
          singleLine = true,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChanged("") }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Limpiar búsqueda"
                )
              }
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("search_text_input")
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // THE 3 PRIMARY CONTROL BUTTONS REQUIRED BY USER:
      // 1. Botón 'Redes Wi-Fi'
      // 2. Botón 'Dispositivos Bluetooth'
      // 3. Botón 'Modo Mapa/Radar'
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NavFilterButton(
          title = "Redes Wi-Fi",
          icon = Icons.Default.Wifi,
          badgeCount = wifiCount,
          activeColor = EmeraldWifi,
          isSelected = currentTab == ScanTab.WIFI,
          testTag = "tab_button_wifi",
          onClick = { onTabSelected(ScanTab.WIFI) },
          modifier = Modifier.weight(1f)
        )

        NavFilterButton(
          title = "Dispositivos Bluetooth",
          icon = Icons.Default.Bluetooth,
          badgeCount = bleCount,
          activeColor = BlueBle,
          isSelected = currentTab == ScanTab.BLUETOOTH,
          testTag = "tab_button_bluetooth",
          onClick = { onTabSelected(ScanTab.BLUETOOTH) },
          modifier = Modifier.weight(1f)
        )

        NavFilterButton(
          title = "Modo Mapa/Radar",
          icon = Icons.Default.TrackChanges,
          badgeCount = null,
          activeColor = CyanPrimary,
          isSelected = currentTab == ScanTab.RADAR,
          testTag = "tab_button_radar",
          onClick = { onTabSelected(ScanTab.RADAR) },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun NavFilterButton(
  title: String,
  icon: ImageVector,
  badgeCount: Int?,
  activeColor: Color,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val backgroundColor = if (isSelected) {
    activeColor.copy(alpha = 0.18f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  }

  val borderColor = if (isSelected) activeColor else Color.Transparent

  Box(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(backgroundColor)
      .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 6.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )

        if (badgeCount != null && badgeCount > 0) {
          Spacer(modifier = Modifier.width(4.dp))
          Badge(
            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            contentColor = Color.White
          ) {
            Text(
              text = "$badgeCount",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          fontSize = 11.sp
        ),
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }
  }
}
