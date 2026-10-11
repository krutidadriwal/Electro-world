package com.example.data

import android.content.Context
import com.example.data.network.Category
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Persists the last-known category/subcategory list (global, not per-phone --
 * unlike [InvoiceListCache]), so forms that need it (Register Complaint,
 * Installation Request) can render their category dropdown immediately while
 * a fresh copy is fetched from the server in the background.
 */
class CategoryCache(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val adapter: JsonAdapter<List<Category>> =
    Moshi.Builder().build().adapter(Types.newParameterizedType(List::class.java, Category::class.java))

  fun get(): List<Category>? {
    val json = prefs.getString(KEY, null) ?: return null
    return try {
      adapter.fromJson(json)
    } catch (e: Exception) {
      null
    }
  }

  fun save(categories: List<Category>) {
    prefs.edit().putString(KEY, adapter.toJson(categories)).apply()
  }

  companion object {
    private const val PREFS_NAME = "electro_world_category_cache"
    private const val KEY = "categories"
  }
}
