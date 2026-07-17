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

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    secondary = ImmersiveSecondary,
    tertiary = ImmersiveTertiary,
    background = ImmersiveBackground,
    surface = ImmersiveSurface,
    surfaceVariant = ImmersiveSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F1115),
    onTertiary = Color(0xFF0F1115),
    onBackground = ImmersiveTextMain,
    onSurface = ImmersiveTextMain,
    onSurfaceVariant = ImmersiveTextMuted,
    outline = ImmersiveBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false, // Force custom theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = ImmersiveColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

