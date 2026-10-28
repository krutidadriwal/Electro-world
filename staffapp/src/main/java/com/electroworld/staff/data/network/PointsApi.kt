package com.electroworld.staff.data.network

import com.electroworld.staff.data.toTitleCase
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// One earned batch of points with something left on it. expiresOn is
// "YYYY-MM-DD" (store time zone).
@JsonClass(generateAdapter = true)
data class PointsLot(
  val points: Int,
  val expiresOn: String
)

@JsonClass(generateAdapter = true)
data class PointsNextExpiry(
  val points: Int,
  val expiresOn: String
)

// balance only counts unexpired points; lots are in the order a redemption
// takes from them (soonest expiry first).
@JsonClass(generateAdapter = true)
data class PointsCustomerResponse(
  @Json(name = "name") val rawName: String,
  val balance: Int,
  val minBalanceToRedeem: Int,
  val nextExpiry: PointsNextExpiry? = null,
  val lots: List<PointsLot>
) {
  // Title Case however the name is stored -- see Names.kt.
  val name: String get() = rawName.toTitleCase()
}

@JsonClass(generateAdapter = true)
data class RedeemPointsRequest(
  val phone: String,
  val points: Int
)

@JsonClass(generateAdapter = true)
data class RedeemPointsResponse(
  val redeemed: Int,
  val balance: Int
)

// Admin-only. points-customer 404s when the phone has no account.
interface PointsApi {
  @GET("api/staff/points-customer")
  suspend fun customer(@Query("phone") phone: String): PointsCustomerResponse

  @POST("api/staff/points-redeem")
  suspend fun redeem(@Body request: RedeemPointsRequest): RedeemPointsResponse
}
