package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class NotificationItem(
  val id: String,
  val message: String,
  val imageDriveFileId: String? = null,
  val imageUrl: String? = null,
  val durationMinutes: Int,
  val createdBy: String,
  val createdAt: String,
  val expired: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NotificationsResponse(
  val notifications: List<NotificationItem>
)

@JsonClass(generateAdapter = true)
data class CreateNotificationRequest(
  val message: String,
  val durationMinutes: Int,
  val imageDriveFileId: String? = null,
  val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UploadNotificationImageRequest(
  val fileName: String,
  val mimeType: String,
  val base64Data: String
)

@JsonClass(generateAdapter = true)
data class UploadNotificationImageResponse(
  val driveFileId: String,
  val url: String
)

interface NotificationsApi {
  @GET("api/notifications/list")
  suspend fun list(): NotificationsResponse

  @POST("api/notifications/create")
  suspend fun create(@Body request: CreateNotificationRequest): NotificationItem

  @POST("api/notifications/image")
  suspend fun uploadImage(@Body request: UploadNotificationImageRequest): UploadNotificationImageResponse
}
