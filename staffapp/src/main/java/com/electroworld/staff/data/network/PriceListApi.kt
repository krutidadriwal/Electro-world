package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class PriceListItem(
  val category: String,
  @Json(name = "group_name") val groupName: String,
  @Json(name = "item_name") val itemName: String,
  @Json(name = "final_price") val finalPrice: Double,
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

interface PriceListApi {
  @GET("api/staff/price-list/list")
  suspend fun list(): PriceListResponse

  @POST("api/staff/price-list/sync")
  suspend fun sync(): SyncPriceListResponse
}
