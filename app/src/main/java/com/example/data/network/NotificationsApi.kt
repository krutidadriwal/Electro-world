package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ActiveNotification(
  val id: String,
  val message: String,
  @Json(name = "image_url") val imageUrl: String? = null,
  @Json(name = "duration_minutes") val durationMinutes: Int,
  @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ActiveNotificationsResponse(
  val notifications: List<ActiveNotification>
)

interface NotificationsApi {
  // With phone, the customer's own notifications (e.g. points expiry
  // reminders) come back mixed in with the broadcasts, newest first.
  @GET("api/notifications/active")
  suspend fun getActiveNotifications(@Query("phone") phone: String? = null): ActiveNotificationsResponse
}
