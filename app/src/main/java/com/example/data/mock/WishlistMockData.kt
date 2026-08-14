package com.example.data.mock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * TEMPORARY MOCK DATA.
 * Everything in this file is placeholder content standing in for the real
 * Wishlist database (see Room entities/DAOs once implemented). Delete this
 * file and swap call sites over to the real data source when that lands.
 */

data class WishlistItem(
  val id: String,
  val name: String,
  val price: Double,
  val features: List<String>
)

data class WishlistSubcategory(
  val id: String,
  val name: String,
  val items: List<WishlistItem>
)

data class WishlistCategory(
  val id: String,
  val name: String,
  val icon: ImageVector,
  val subcategories: List<WishlistSubcategory>
)

object WishlistMockData {
  val categories: List<WishlistCategory> = listOf(
    WishlistCategory(
      id = "cat_tv",
      name = "Televisions",
      icon = Icons.Default.Tv,
      subcategories = listOf(
        WishlistSubcategory(
          id = "sub_oled",
          name = "OLED",
          items = listOf(
            WishlistItem("item_oled_65", "OLED Smart Cinema 65\"", 1299.99, listOf("4K HDR", "120Hz Refresh", "Dolby Atmos", "Voice Remote")),
            WishlistItem("item_oled_55", "OLED Smart Cinema 55\"", 999.99, listOf("4K HDR", "120Hz Refresh", "Dolby Vision"))
          )
        ),
        WishlistSubcategory(
          id = "sub_qled",
          name = "QLED",
          items = listOf(
            WishlistItem("item_qled_75", "QLED Ultra Bright 75\"", 1599.99, listOf("Quantum Dot", "8K Upscaling", "Mini-LED Backlight"))
          )
        )
      )
    ),
    WishlistCategory(
      id = "cat_ac",
      name = "Air Conditioners",
      icon = Icons.Default.DeviceThermostat,
      subcategories = listOf(
        WishlistSubcategory(
          id = "sub_split",
          name = "Split AC",
          items = listOf(
            WishlistItem("item_ac_15", "Inverter AC Dual-Cool 1.5T", 499.99, listOf("Inverter Compressor", "Wi-Fi Control", "5 Star Rating"))
          )
        ),
        WishlistSubcategory(
          id = "sub_window",
          name = "Window AC",
          items = listOf(
            WishlistItem("item_ac_window", "Compact Window AC 1T", 329.99, listOf("Low Noise", "Auto Restart"))
          )
        )
      )
    ),
    WishlistCategory(
      id = "cat_kitchen",
      name = "Kitchen Appliances",
      icon = Icons.Default.Kitchen,
      subcategories = listOf(
        WishlistSubcategory(
          id = "sub_fridge",
          name = "Refrigerators",
          items = listOf(
            WishlistItem("item_fridge_350", "Triple-Door Aero Fridge 350L", 899.99, listOf("Frost Free", "Convertible Freezer", "Inverter Compressor"))
          )
        ),
        WishlistSubcategory(
          id = "sub_micro",
          name = "Microwaves",
          items = listOf(
            WishlistItem("item_micro_28", "Convection Microwave 28L", 199.99, listOf("Convection + Grill", "Auto Cook Menus"))
          )
        )
      )
    ),
    WishlistCategory(
      id = "cat_audio",
      name = "Audio",
      icon = Icons.Default.Headphones,
      subcategories = listOf(
        WishlistSubcategory(
          id = "sub_buds",
          name = "Earbuds",
          items = listOf(
            WishlistItem("item_buds_pro", "Sony WF-1000XM5 Active Buds", 199.99, listOf("Active Noise Cancelling", "24hr Battery"))
          )
        ),
        WishlistSubcategory(
          id = "sub_soundbar",
          name = "Soundbars",
          items = listOf(
            WishlistItem("item_soundbar_51", "EW Soundcore Bar 5.1 Dolby", 179.99, listOf("Dolby Atmos", "Wireless Subwoofer"))
          )
        )
      )
    ),
    WishlistCategory(
      id = "cat_fashion",
      name = "Wearables",
      icon = Icons.Default.Checkroom,
      subcategories = listOf(
        WishlistSubcategory(
          id = "sub_watch",
          name = "Smartwatches",
          items = listOf(
            WishlistItem("item_watch_pro", "EW Fit Watch Pro", 149.99, listOf("Heart Rate Monitor", "7 Day Battery", "GPS"))
          )
        )
      )
    )
  )
}
