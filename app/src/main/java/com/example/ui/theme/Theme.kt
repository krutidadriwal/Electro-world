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

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldLight,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = SlateBackground,
    onSecondary = SlateBackground,
    onBackground = OnSlateText,
    onSurface = OnSlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = OnSlateTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldDark,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = SlateBackground,
    onSecondary = SlateBackground,
    onBackground = OnSlateText,
    onSurface = OnSlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = OnSlateTextSecondary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force brand dark theme to match Electro World style
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
