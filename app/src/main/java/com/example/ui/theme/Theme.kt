package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF002230),
    primaryContainer = Color(0xFF00384D),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = EmeraldWifi,
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF005328),
    onSecondaryContainer = Color(0xFF8CFFA9),
    tertiary = BlueBle,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF002E7A),
    onTertiaryContainer = Color(0xFFD6E3FF),
    background = TechDarkBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = TechDarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = RedSignal,
    onError = Color.White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TechLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAEDFF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = TechLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8FCD3),
    onSecondaryContainer = Color(0xFF00210B),
    tertiary = TechLightTertiary,
    onTertiary = Color.White,
    background = TechLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = TechLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = TechLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFD32F2F),
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent cyber/radar branding
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Use tech dark mode by default for radar/scanner look or LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
