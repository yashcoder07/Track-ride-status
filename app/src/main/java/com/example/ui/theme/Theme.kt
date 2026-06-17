package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF8FD7A4),         // Soothing sage mint green
    onPrimary = Color(0xFF0D3520),
    secondary = Color(0xFFADC2B4),
    onSecondary = Color(0xFF1A2E23),
    background = Color(0xFF121815),      // Deep forest charcoal to prevent eye strain
    onBackground = Color(0xFFE1EAE4),
    surface = Color(0xFF1B221E),
    onSurface = Color(0xFFE1EAE4),
    surfaceVariant = Color(0xFF28322B),
    onSurfaceVariant = Color(0xFFBAC7BF),
    outline = Color(0xFF869389)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ProfessionalPrimary,
    onPrimary = ProfessionalOnPrimary,
    primaryContainer = ProfessionalPrimaryContainer,
    onPrimaryContainer = ProfessionalOnPrimaryContainer,
    secondary = ProfessionalSecondary,
    onSecondary = ProfessionalOnSecondary,
    tertiary = ProfessionalTertiary,
    onTertiary = ProfessionalOnTertiary,
    background = ProfessionalBackground,
    onBackground = ProfessionalOnBackground,
    surface = ProfessionalSurface,
    onSurface = ProfessionalOnSurface,
    surfaceVariant = ProfessionalSurfaceVariant,
    onSurfaceVariant = ProfessionalOnBackground,
    outline = ProfessionalOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color so our custom hand-crafted palette is loaded
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
