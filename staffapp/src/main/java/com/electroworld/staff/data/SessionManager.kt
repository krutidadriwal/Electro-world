package com.electroworld.staff.data

import android.content.Context

class SessionManager(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  val isLoggedIn: Boolean
    get() = accessToken.isNotBlank()

  val accessToken: String
    get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""

  val refreshToken: String
    get() = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""

  val staffId: String
    get() = prefs.getString(KEY_STAFF_ID, "") ?: ""

  val email: String
    get() = prefs.getString(KEY_EMAIL, "") ?: ""

  val role: String
    get() = prefs.getString(KEY_ROLE, "") ?: ""

  val isAdmin: Boolean
    get() = role == "admin"

  fun saveTokens(accessToken: String, refreshToken: String) {
    prefs.edit()
      .putString(KEY_ACCESS_TOKEN, accessToken)
      .putString(KEY_REFRESH_TOKEN, refreshToken)
      .apply()
  }

  fun saveProfile(staffId: String, email: String, role: String) {
    prefs.edit()
      .putString(KEY_STAFF_ID, staffId)
      .putString(KEY_EMAIL, email)
      .putString(KEY_ROLE, role)
      .apply()
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_NAME = "electro_world_staff_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_STAFF_ID = "staff_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"
  }
}
