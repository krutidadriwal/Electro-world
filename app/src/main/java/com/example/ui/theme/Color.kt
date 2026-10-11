package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// The ONLY 6 colors allowed anywhere in this app -- the single (dark) scheme
// in Theme.kt is built exclusively from these. Prefer
// MaterialTheme.colorScheme.* in screens; reference these directly only for
// fixed brand artwork (e.g. the Spin to Win wheel).
//
// This palette is the customer app's own: the staff app
// (staffapp/.../ui/theme/Color.kt) still uses the earlier 8-swatch set.

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

// The two dark tones need Paper text for contrast; the golds, Paper and
// Platinum all read fine with Obsidian text.
fun contrastTextColor(background: Color): Color =
  if (background == ObsidianCharcoal || background == DeepSlate) CrispPaper else ObsidianCharcoal
