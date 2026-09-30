package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class WishlistItem(
  val id: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "subcategory_name") val subcategoryName: String?,
  val status: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "status_updated_at") val statusUpdatedAt: String?,
  @Json(name = "last_called_at") val lastCalledAt: String?
)

@JsonClass(generateAdapter = true)
data class WishlistResponse(
  val items: List<WishlistItem>
)

@JsonClass(generateAdapter = true)
data class WishlistStatusRequest(
  val wishlistItemId: String,
  val status: String
)

@JsonClass(generateAdapter = true)
data class WishlistStatusResponse(
  val id: String,
  val status: String,
  @Json(name = "status_updated_at") val statusUpdatedAt: String
)

@JsonClass(generateAdapter = true)
data class WishlistCallRequest(
  val wishlistItemId: String
)

@JsonClass(generateAdapter = true)
data class WishlistCallResponse(
  val id: String,
  @Json(name = "called_at") val calledAt: String
)

interface WishlistApi {
  @GET("api/staff/wishlist-list")
  suspend fun list(): WishlistResponse

  @PATCH("api/staff/wishlist-status")
  suspend fun updateStatus(@Body request: WishlistStatusRequest): WishlistStatusResponse

  @POST("api/staff/wishlist-call")
  suspend fun logCall(@Body request: WishlistCallRequest): WishlistCallResponse
}
