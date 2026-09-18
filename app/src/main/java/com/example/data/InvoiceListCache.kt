package com.example.data

import android.content.Context
import com.example.data.network.InvoiceFile
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Persists the last-known invoice list (metadata only, not the PDF bytes --
 * see [InvoiceFileCache] for that) per phone number, so the Invoices UI has
 * something to show immediately -- including fully offline -- while a fresh
 * copy is fetched from the server in the background.
 */
class InvoiceListCache(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val adapter: JsonAdapter<List<InvoiceFile>> =
    Moshi.Builder().build().adapter(Types.newParameterizedType(List::class.java, InvoiceFile::class.java))

  fun get(phone: String): List<InvoiceFile>? {
    val json = prefs.getString(key(phone), null) ?: return null
    return try {
      adapter.fromJson(json)
    } catch (e: Exception) {
      null
    }
  }

  fun save(phone: String, invoices: List<InvoiceFile>) {
    prefs.edit().putString(key(phone), adapter.toJson(invoices)).apply()
  }

  private fun key(phone: String) = "invoices_$phone"

  companion object {
    private const val PREFS_NAME = "electro_world_invoice_list_cache"
  }
}
