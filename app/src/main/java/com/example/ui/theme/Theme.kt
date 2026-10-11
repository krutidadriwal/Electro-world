package com.example.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One scheme, used whatever the device's light/dark setting: the palette in
// Color.kt is a dark one by design (a deep base that makes the golds pop)
// and has no light surfaces to build a second scheme from.
//
// Every role Material3's scheme builder accepts is filled in explicitly --
// left unset, several (primaryContainer, secondaryContainer, error, outline,
// ...) fall back to Material3's own baseline colors, which are NOT in the
// palette (its default error is red, several containers are purple-tinted).
//
// How the roles are used across the screens:
//   background / surface   page base / cards, modals and top bars
//   surfaceVariant         insets *inside* a card (chips, rows), so it's the
//                          base tone again rather than a third dark shade
//   primary                actions and key highlights (Vivid Amber)
//   secondary              icons, secondary accents and badges (Pale Gold)
//   onBackground/onSurface primary text (Crisp Paper)
//   outline                muted text and borders (Platinum Muted)
private val AppColorScheme = darkColorScheme(
  primary = VividAmber,
  onPrimary = ObsidianCharcoal,
  primaryContainer = VividAmber,
  onPrimaryContainer = ObsidianCharcoal,
  secondary = PaleGold,
  onSecondary = ObsidianCharcoal,
  secondaryContainer = DeepSlate,
  onSecondaryContainer = PaleGold,
  tertiary = PaleGold,
  onTertiary = ObsidianCharcoal,
  tertiaryContainer = DeepSlate,
  onTertiaryContainer = CrispPaper,
  background = ObsidianCharcoal,
  onBackground = CrispPaper,
  surface = DeepSlate,
  onSurface = CrispPaper,
  surfaceVariant = ObsidianCharcoal,
  onSurfaceVariant = PlatinumMuted,
  // Left at its default (`primary`), Card/Surface blend this into the
  // surface color based on elevation, tinting every card instead of
  // showing the flat palette color.
  surfaceTint = Color.Transparent,
  // The palette has no red -- errors and warnings use the action gold.
  error = VividAmber,
  onError = ObsidianCharcoal,
  errorContainer = DeepSlate,
  onErrorContainer = CrispPaper,
  outline = PlatinumMuted,
  outlineVariant = PlatinumMuted,
  inverseSurface = CrispPaper,
  inverseOnSurface = ObsidianCharcoal,
  inversePrimary = VividAmber,
  scrim = ObsidianCharcoal
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = AppColorScheme, typography = Typography, content = content)
}

// Shared card color for every screen -- change the color here once rather
// than per screen.
@Composable
fun appCardColors(): CardColors = CardDefaults.cardColors(containerColor = DeepSlate)
