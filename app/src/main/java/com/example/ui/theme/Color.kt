package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Electro World Color Palette (Gold & Espresso) -- kept identical to
// staffapp/src/main/java/com/electroworld/staff/ui/theme/Color.kt so both
// apps read as the same brand.
val GoldPrimary = Color(0xFFC6A15B)       // Gold accent
val GoldSecondary = Color(0xFFC6A15B)     // Gold accent
val GoldLight = Color(0xFFDBC28C)         // Lighter tint of GoldPrimary
val GoldDark = Color(0xFF8F7641)          // Darker shade of GoldPrimary
val Espresso = Color(0xFF2A1018)          // Card/tile surfaces
val EspressoLight = Color(0xFF4A2733)     // Lighter shade, for borders/elevated elements
val SlateBackground = Color(0xFF1E2022)   // Screen background (not a "tab" surface)
val SlateSurface = Espresso
val SlateSurfaceVariant = EspressoLight
val OnSlateText = Color(0xFFEEEEEE)       // Off-white readable text
val OnSlateTextSecondary = Color(0xFFAAAAAA) // Soft gray body text
val AlertRed = Color(0xFF6E1734)          // Error/alert accent
val SuccessGreen = Color(0xFF81C784)      // Success verification color
val GoldGradientStart = GoldLight
val GoldGradientEnd = GoldDark

