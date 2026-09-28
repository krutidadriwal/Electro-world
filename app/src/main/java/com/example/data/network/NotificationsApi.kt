package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

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
  @GET("api/notifications/active")
  suspend fun getActiveNotifications(): ActiveNotificationsResponse
}
