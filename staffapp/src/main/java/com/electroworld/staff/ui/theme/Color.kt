package com.electroworld.staff.ui.theme

import androidx.compose.ui.graphics.Color

// The ONLY 6 colors allowed anywhere in this app -- the same palette as the
// customer app (app/.../ui/theme/Color.kt), so both apps look like one
// brand. The single (dark) scheme in Theme.kt is built exclusively from
// these. Prefer MaterialTheme.colorScheme.* in screens; reference these
// directly only where a screen needs raw palette colors rather than a theme
// role (PriceListScreen's row tones and CustomerPriceScreen's randomized
// background).

// Dominant dark (base): the page background everything else sits on.
val ObsidianCharcoal = Color(0xFF1A1715)

// Surface / card (layer 1): cards, modals, top bars -- lifted off the base.
val DeepSlate = Color(0xFF2D2825)

// Primary gold (action): primary buttons and key highlights.
val VividAmber = Color(0xFFFFB800)

// Secondary gold (muted): secondary borders, icons, supporting badge text.
val PaleGold = Color(0xFFF0CF8C)

// High-contrast light (text): titles and body text on dark surfaces.
val CrispPaper = Color(0xFFFCFCFC)

// Muted support (text): captions, hints, placeholders, hairline borders.
val PlatinumMuted = Color(0xFFA8A39D)

// Status colors -- the one exception to the 6-color palette above, used
// only to tint Complaints/Installations/Wishlist cards by status (see
// statusCardColors in Theme.kt): red = needs attention or lost, yellow =
// in progress, green = done/won.
val StatusRed = Color(0xFFD3524E)
val StatusYellow = Color(0xFFE2A13B)
val StatusGreen = Color(0xFF6B9A76)

// All 6 together, for CustomerPriceScreen's randomized full-screen background.
val AllSwatches = listOf(ObsidianCharcoal, DeepSlate, VividAmber, PaleGold, CrispPaper, PlatinumMuted)

// The two dark tones need Paper text for contrast; the golds, Paper and
// Platinum all read fine with Obsidian text.
fun contrastTextColor(background: Color): Color =
  if (background == ObsidianCharcoal || background == DeepSlate) CrispPaper else ObsidianCharcoal
