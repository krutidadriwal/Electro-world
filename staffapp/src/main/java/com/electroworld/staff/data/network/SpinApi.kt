package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

// Prize stock: a prize is only on the customer's wheel / winnable while
// quantity > 0.
@JsonClass(generateAdapter = true)
data class SpinPrize(
  val id: String,
  val name: String,
  val quantity: Int
)

// What staff pick from when unlocking a spin. An empty mappedPrizeIds means
// the purchase can win any in-stock prize.
@JsonClass(generateAdapter = true)
data class SpinPurchaseItem(
  val id: String,
  val name: String,
  val mappedPrizeIds: List<String>
)

// One unlocked spin. usedAt/prizeName are null until the customer plays it;
// deliveredAt is null until staff mark the won prize as handed over.
@JsonClass(generateAdapter = true)
data class SpinGrant(
  val id: String,
  val phone: String,
  val customerName: String? = null,
  val boughtItem: String,
  val createdAt: String,
  val usedAt: String? = null,
  val prizeName: String? = null,
  val grantedByEmail: String? = null,
  val deliveredAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SpinOverviewResponse(
  val prizes: List<SpinPrize>,
  val purchaseItems: List<SpinPurchaseItem>,
  val grants: List<SpinGrant>,
  val pendingCount: Int,
  val playedCount: Int,
  val deliveredCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class CreateSpinPrizeRequest(
  val name: String,
  val quantity: Int
)

@JsonClass(generateAdapter = true)
data class UpdateSpinPrizeRequest(
  val id: String,
  val quantity: Int
)

// id is null when creating, set when editing an existing item.
@JsonClass(generateAdapter = true)
data class SaveSpinPurchaseItemRequest(
  val id: String? = null,
  val name: String,
  val mappedPrizeIds: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateSpinGrantRequest(
  val phone: String,
  val purchaseItemId: String
)

@JsonClass(generateAdapter = true)
data class UndoSpinGrantRequest(
  val id: String
)

@JsonClass(generateAdapter = true)
data class DeliverSpinGrantRequest(
  val id: String,
  val action: String = "deliver"
)

@JsonClass(generateAdapter = true)
data class DeliverSpinGrantResponse(
  val id: String,
  val deliveredAt: String
)

@JsonClass(generateAdapter = true)
data class CreateSpinGrantResponse(
  val id: String
)

interface SpinApi {
  @GET("api/staff/spin-overview")
  suspend fun overview(): SpinOverviewResponse

  @POST("api/staff/spin-prize")
  suspend fun createPrize(@Body request: CreateSpinPrizeRequest): SpinPrize

  @PATCH("api/staff/spin-prize")
  suspend fun updatePrize(@Body request: UpdateSpinPrizeRequest): SpinPrize

  @DELETE("api/staff/spin-prize")
  suspend fun deletePrize(@Query("id") id: String)

  @POST("api/staff/spin-purchase-item")
  suspend fun createPurchaseItem(@Body request: SaveSpinPurchaseItemRequest): SpinPurchaseItem

  @PATCH("api/staff/spin-purchase-item")
  suspend fun updatePurchaseItem(@Body request: SaveSpinPurchaseItemRequest): SpinPurchaseItem

  @DELETE("api/staff/spin-purchase-item")
  suspend fun deletePurchaseItem(@Query("id") id: String)

  // Unlocks one spin for the customer with this (10-digit) phone.
  @POST("api/staff/spin-grant")
  suspend fun unlock(@Body request: CreateSpinGrantRequest): CreateSpinGrantResponse

  // Reverses a played spin: prize back into stock, spin playable again.
  // Refused once the prize has been delivered.
  @PATCH("api/staff/spin-grant")
  suspend fun undoWin(@Body request: UndoSpinGrantRequest)

  // Marks a won prize as handed over to the customer, stamped with now.
  @PATCH("api/staff/spin-grant")
  suspend fun markDelivered(@Body request: DeliverSpinGrantRequest): DeliverSpinGrantResponse

  // Takes away a spin that hasn't been played yet.
  @DELETE("api/staff/spin-grant")
  suspend fun revoke(@Query("id") id: String)
}
