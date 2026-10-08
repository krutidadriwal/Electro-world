package com.electroworld.staff.ui.theme

import androidx.compose.ui.graphics.Color

// The ONLY 8 colors allowed anywhere in this app -- both the light and dark
// schemes in Theme.kt are built exclusively from these. No other file
// should reference these directly; use MaterialTheme.colorScheme.* instead,
// so colors actually switch with the active theme (see HomeScreen.kt /
// CustomerLookupStatus.kt, which do exactly that).
val Swatch1Charcoal = Color(0xFF3B3430)
val Swatch2Paper = Color(0xFFFCFCFC)
val Swatch3Amber = Color(0xFFE9A515)
val Swatch4Taupe = Color(0xFF959188)
val Swatch5Pearl = Color(0xFFDBD8D2)
val Swatch6DeepAmber = Color(0xFFD99912)
val Swatch7PaleGold = Color(0xFFF0CF8C)
val Swatch8Sand = Color(0xFFBFA995)

// All 8 swatches together, for call sites that intentionally need the raw
// palette rather than a theme role -- e.g. PriceListScreen.kt's per-row card
// colors and CustomerPriceScreen's randomized background.
val AllSwatches = listOf(
  Swatch1Charcoal, Swatch2Paper, Swatch3Amber, Swatch4Taupe,
  Swatch5Pearl, Swatch6DeepAmber, Swatch7PaleGold, Swatch8Sand
)

// Per explicit design rule (CustomerPriceScreen's randomized background):
// Charcoal and Taupe are the two swatches dark/muted enough to need Paper
// text for contrast; every other swatch reads fine with Charcoal text.
fun contrastTextColor(background: Color): Color =
  if (background == Swatch1Charcoal || background == Swatch4Taupe) Swatch2Paper else Swatch1Charcoal
