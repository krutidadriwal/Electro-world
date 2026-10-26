package com.electroworld.staff.data.share

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.electroworld.staff.data.network.PriceListItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

// One item picked for a quotation. Prices are a snapshot from when it was
// added, refreshed from the live list whenever the price list loads (see
// refreshPrices), so a quote never shows a stale price after a sync.
@JsonClass(generateAdapter = true)
data class QuoteLine(
  val itemName: String,
  val category: String,
  val groupName: String,
  val mrp: Double,
  val finalPrice: Double
)

// The quotation pick list: device-only, kept in SharedPreferences so it
// survives the app being closed. No quantities or totals -- a quotation is
// just a set of items with their MRP and price. Keyed by item name (unique
// in the Tally export). `lines` is Compose state, so screens reading it
// recompose on change.
object QuoteCart {
  private const val PREFS_NAME = "electro_world_quote_cart"
  private const val KEY_LINES = "lines"

  private var prefs: SharedPreferences? = null
  private val adapter = Moshi.Builder().build()
    .adapter<List<QuoteLine>>(Types.newParameterizedType(List::class.java, QuoteLine::class.java))

  val lines = mutableStateListOf<QuoteLine>()

  fun init(context: Context) {
    if (prefs != null) return
    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs?.getString(KEY_LINES, null)
    lines.addAll(saved?.let { runCatching { adapter.fromJson(it) }.getOrNull() }.orEmpty())
  }

  fun contains(itemName: String): Boolean = lines.any { it.itemName == itemName }

  fun toggle(item: PriceListItem) {
    if (contains(item.itemName)) remove(item.itemName)
    else {
      lines.add(QuoteLine(item.itemName, item.category, item.groupName, item.mrp, item.finalPrice))
      save()
    }
  }

  fun remove(itemName: String) {
    if (lines.removeAll { it.itemName == itemName }) save()
  }

  fun clear() {
    lines.clear()
    save()
  }

  // Items no longer in the list keep their last known price.
  fun refreshPrices(items: List<PriceListItem>) {
    val byName = items.associateBy { it.itemName }
    var changed = false
    lines.forEachIndexed { index, line ->
      val fresh = byName[line.itemName] ?: return@forEachIndexed
      if (fresh.finalPrice != line.finalPrice || fresh.mrp != line.mrp) {
        lines[index] = line.copy(finalPrice = fresh.finalPrice, mrp = fresh.mrp)
        changed = true
      }
    }
    if (changed) save()
  }

  private fun save() {
    prefs?.edit()?.putString(KEY_LINES, adapter.toJson(lines.toList()))?.apply()
  }
}
