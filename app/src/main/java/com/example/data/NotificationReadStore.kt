package com.example.data

import android.content.Context

// Tracks which notification ids the user has opened, purely as a local
// "seen" marker for greying out list items -- not synced to the server,
// so it's fine to key this per-device rather than per-phone.
class NotificationReadStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isRead(id: String): Boolean = prefs.getStringSet(KEY_READ_IDS, emptySet())?.contains(id) == true

  fun markRead(id: String) {
    val current = prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    if (id in current) return
    prefs.edit().putStringSet(KEY_READ_IDS, current + id).apply()
  }

  companion object {
    private const val PREFS_NAME = "electro_world_notification_reads"
    private const val KEY_READ_IDS = "read_ids"
  }
}
