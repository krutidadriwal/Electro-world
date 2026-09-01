package com.example.data

import android.content.Context

class SessionManager(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  val isLoggedIn: Boolean
    get() = prefs.getBoolean(KEY_LOGGED_IN, false)

  val userName: String
    get() = prefs.getString(KEY_NAME, "") ?: ""

  val userPhone: String
    get() = prefs.getString(KEY_PHONE, "") ?: ""

  val sessionToken: String
    get() = prefs.getString(KEY_TOKEN, "") ?: ""

  fun saveSession(name: String, phone: String, token: String) {
    prefs.edit()
      .putBoolean(KEY_LOGGED_IN, true)
      .putString(KEY_NAME, name)
      .putString(KEY_PHONE, phone)
      .putString(KEY_TOKEN, token)
      .apply()
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_NAME = "electro_world_session"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_NAME = "user_name"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_TOKEN = "session_token"
  }
}
