package com.electroworld.staff.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

// Same scheme as the customer app (app/.../ui/theme/Theme.kt), role for
// role, so the two apps look alike. One scheme, used whatever the device's
// light/dark setting: the palette in Color.kt is a dark one by design (a
// deep base that makes the golds pop) and has no light surfaces to build a
// second scheme from.
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
private val StaffColorScheme = darkColorScheme(
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
fun StaffAppTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = StaffColorScheme, typography = Typography, content = content)
}

// Shared row/list card color for every tab EXCEPT Price List (which sets its
// own row tones -- see PriceListScreen.kt). Pass this to every other
// screen's Card(colors = ...) so a future color change only has to happen
// here, not once per tab.
@Composable
fun staffCardColors(): CardColors = CardDefaults.cardColors(containerColor = DeepSlate)

// staffCardColors, tinted with a status color (StatusRed/Yellow/Green) --
// a wash of it over the normal card color, faint enough that the usual
// light text stays readable. null means no status color: the plain card.
@Composable
fun statusCardColors(statusColor: Color?): CardColors =
  if (statusColor == null) {
    staffCardColors()
  } else {
    CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.28f).compositeOver(DeepSlate))
  }
