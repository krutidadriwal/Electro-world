package com.electroworld.staff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Both schemes below are built exclusively from the 8 swatches in Color.kt,
// and every role Material3's scheme builders accept is filled in explicitly
// -- left unset, several (primaryContainer, secondaryContainer, error,
// outline, ...) fall back to Material3's own baseline colors, which are NOT
// in the 8-color palette (its default error is red, several containers are
// purple-tinted).
//
// Swatch6 (deep amber) is used for secondary/tabs/badges/initials in BOTH
// schemes, so that specific brand element stays consistent regardless of
// device theme.

private val LightColorScheme = lightColorScheme(
  primary = Swatch3Amber,
  onPrimary = Swatch1Charcoal,
  primaryContainer = Swatch6DeepAmber,
  onPrimaryContainer = Swatch1Charcoal,
  secondary = Swatch6DeepAmber,
  onSecondary = Swatch1Charcoal,
  secondaryContainer = Swatch7PaleGold,
  onSecondaryContainer = Swatch1Charcoal,
  tertiary = Swatch7PaleGold,
  onTertiary = Swatch1Charcoal,
  tertiaryContainer = Swatch8Sand,
  onTertiaryContainer = Swatch1Charcoal,
  background = Swatch2Paper,
  onBackground = Swatch1Charcoal,
  surface = Swatch5Pearl,
  onSurface = Swatch1Charcoal,
  surfaceVariant = Swatch8Sand,
  onSurfaceVariant = Swatch4Taupe,
  // Left at its default (`primary`), Card/Surface blend this into the
  // surface color based on elevation, tinting every card instead of
  // showing the flat swatch color.
  surfaceTint = Color.Transparent,
  error = Swatch1Charcoal,
  onError = Swatch2Paper,
  errorContainer = Swatch8Sand,
  onErrorContainer = Swatch1Charcoal,
  outline = Swatch4Taupe,
  outlineVariant = Swatch8Sand,
  inverseSurface = Swatch1Charcoal,
  inverseOnSurface = Swatch2Paper,
  inversePrimary = Swatch7PaleGold,
  scrim = Swatch1Charcoal
)

// Charcoal is the palette's only dark swatch, so it carries the background
// here -- there's no second dark tone for elevated surfaces, so Taupe
// stands in for cards (lighter than the background, same role elevation
// normally plays). Text (onBackground/onSurface/onSurfaceVariant) is Paper
// throughout dark mode, by explicit request -- not contrast-derived from
// Taupe the way the light scheme's text is.
private val DarkColorScheme = darkColorScheme(
  primary = Swatch3Amber,
  onPrimary = Swatch1Charcoal,
  primaryContainer = Swatch6DeepAmber,
  onPrimaryContainer = Swatch1Charcoal,
  secondary = Swatch6DeepAmber,
  onSecondary = Swatch1Charcoal,
  secondaryContainer = Swatch7PaleGold,
  onSecondaryContainer = Swatch1Charcoal,
  tertiary = Swatch7PaleGold,
  onTertiary = Swatch1Charcoal,
  tertiaryContainer = Swatch8Sand,
  onTertiaryContainer = Swatch1Charcoal,
  background = Swatch1Charcoal,
  onBackground = Swatch2Paper,
  surface = Swatch4Taupe,
  onSurface = Swatch2Paper,
  surfaceVariant = Swatch8Sand,
  onSurfaceVariant = Swatch2Paper,
  surfaceTint = Color.Transparent,
  error = Swatch1Charcoal,
  onError = Swatch2Paper,
  errorContainer = Swatch8Sand,
  onErrorContainer = Swatch1Charcoal,
  outline = Swatch8Sand,
  outlineVariant = Swatch8Sand,
  inverseSurface = Swatch5Pearl,
  inverseOnSurface = Swatch1Charcoal,
  inversePrimary = Swatch6DeepAmber,
  scrim = Swatch1Charcoal
)

@Composable
fun StaffAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Shared row/list card color for every tab EXCEPT Price List (which sets its
// own per-row colors by category/group/item -- see PriceListScreen.kt). Pass
// this to every other screen's Card(colors = ...) so a future color change
// only has to happen here, not once per tab.
@Composable
fun staffCardColors(): CardColors = CardDefaults.cardColors(containerColor = Swatch8Sand.copy(alpha = 0.3f))
