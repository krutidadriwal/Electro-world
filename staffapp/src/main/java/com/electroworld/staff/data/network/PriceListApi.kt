package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class PriceListItem(
  val category: String,
  @Json(name = "group_name") val groupName: String,
  @Json(name = "item_name") val itemName: String,
  @Json(name = "final_price") val finalPrice: Double,
  val mrp: Double,
  // Only populated for admins -- the server omits this column entirely for
  // employees, so it's absent rather than present-but-zero for them.
  @Json(name = "cost_price") val costPrice: Double? = null,
  @Json(name = "stock_label") val stockLabel: String
)

@JsonClass(generateAdapter = true)
data class PriceListResponse(
  val items: List<PriceListItem>
)

@JsonClass(generateAdapter = true)
data class SyncPriceListResponse(
  val count: Int,
  @Json(name = "syncedAt") val syncedAt: String
)

@JsonClass(generateAdapter = true)
data class CategoryImage(
  val category: String,
  @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryImagesResponse(
  val categories: List<CategoryImage>
)

@JsonClass(generateAdapter = true)
data class UploadCategoryImageRequest(
  val category: String,
  val fileName: String,
  val mimeType: String,
  val base64Data: String
)

@JsonClass(generateAdapter = true)
data class UploadCategoryImageResponse(
  val category: String,
  @Json(name = "image_url") val imageUrl: String
)

interface PriceListApi {
  @GET("api/staff/price-list-list")
  suspend fun list(): PriceListResponse

  @POST("api/staff/price-list-sync")
  suspend fun sync(): SyncPriceListResponse

  @GET("api/staff/price-list-category-images")
  suspend fun categoryImages(): CategoryImagesResponse

  @POST("api/staff/price-list-category-image")
  suspend fun uploadCategoryImage(@Body request: UploadCategoryImageRequest): UploadCategoryImageResponse
}
