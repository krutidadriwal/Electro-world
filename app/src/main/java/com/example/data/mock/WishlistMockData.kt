package com.example.data.mock

/**
 * TEMPORARY MOCK DATA.
 * The category/subcategory browse structure now comes from Supabase (see
 * Category/Subcategory in com.example.data.network and GET /api/categories).
 * This file only stands in for the real product catalog -- the actual items
 * within a subcategory -- until that's backed by a database. Delete it and
 * swap call sites over to the real data source when that lands.
 */

data class WishlistItem(
  val id: String,
  val name: String,
  val price: Double,
  val features: List<String>
)

object WishlistMockData {
  // Keyed by subcategory name (see public.subcategories in server/schema.sql)
  // so items line up with whatever subcategory Supabase currently serves.
  private val itemsBySubcategoryName: Map<String, List<WishlistItem>> = mapOf(
    "OLED" to listOf(
      WishlistItem("item_oled_65", "OLED Smart Cinema 65\"", 1299.99, listOf("4K HDR", "120Hz Refresh", "Dolby Atmos", "Voice Remote")),
      WishlistItem("item_oled_55", "OLED Smart Cinema 55\"", 999.99, listOf("4K HDR", "120Hz Refresh", "Dolby Vision"))
    ),
    "QLED" to listOf(
      WishlistItem("item_qled_75", "QLED Ultra Bright 75\"", 1599.99, listOf("Quantum Dot", "8K Upscaling", "Mini-LED Backlight"))
    ),
    "Split AC" to listOf(
      WishlistItem("item_ac_15", "Inverter AC Dual-Cool 1.5T", 499.99, listOf("Inverter Compressor", "Wi-Fi Control", "5 Star Rating"))
    ),
    "Window AC" to listOf(
      WishlistItem("item_ac_window", "Compact Window AC 1T", 329.99, listOf("Low Noise", "Auto Restart"))
    ),
    "Refrigerators" to listOf(
      WishlistItem("item_fridge_350", "Triple-Door Aero Fridge 350L", 899.99, listOf("Frost Free", "Convertible Freezer", "Inverter Compressor"))
    ),
    "Microwaves" to listOf(
      WishlistItem("item_micro_28", "Convection Microwave 28L", 199.99, listOf("Convection + Grill", "Auto Cook Menus"))
    ),
    "Soundbars" to listOf(
      WishlistItem("item_soundbar_51", "EW Soundcore Bar 5.1 Dolby", 179.99, listOf("Dolby Atmos", "Wireless Subwoofer"))
    )
  )

  // Fallback for a category with no subcategories at all, where the category
  // itself is the browse leaf (see WishlistScreen's skip-the-subcategory-step
  // branch). Keyed by the category's icon_key.
  private val itemsByCategoryIconKey: Map<String, List<WishlistItem>> = mapOf(
    "SSH" to listOf(
      WishlistItem("item_soundbar_51", "EW Soundcore Bar 5.1 Dolby", 179.99, listOf("Dolby Atmos", "Wireless Subwoofer"))
    )
  )

  fun itemsForSubcategory(subcategoryName: String): List<WishlistItem> {
    return itemsBySubcategoryName[subcategoryName] ?: emptyList()
  }

  fun itemsForCategory(categoryIconKey: String): List<WishlistItem> {
    return itemsByCategoryIconKey[categoryIconKey.uppercase()] ?: emptyList()
  }
}
