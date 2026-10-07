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

// FloatingActionButton defaults to primaryContainer/onPrimaryContainer --
// left unset, it falls back to Material3's baseline purple-ish tone. Set
// explicitly (even though this app has no FABs today) so it stays on-brand
// red/gold if one's ever added, and to match staffapp's theme.
//
// surfaceTint is also set to Transparent -- left at its default (`primary`,
// i.e. gold), Card/Surface blend it into the surface color based on
// elevation, washing cards with a gold tint instead of pure dark espresso.
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
    onSurfaceVariant = OnSlateTextSecondary,
    error = AlertRed,
    primaryContainer = AlertRed,
    onPrimaryContainer = OnSlateText,
    surfaceTint = Color.Transparent
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
    onSurfaceVariant = OnSlateTextSecondary,
    error = AlertRed,
    primaryContainer = AlertRed,
    onPrimaryContainer = OnSlateText,
    surfaceTint = Color.Transparent
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
