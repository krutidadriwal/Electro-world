package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

// The one server call in price-list sharing: records who shared to whom
// (not what). The image itself never leaves the phone except via the share
// sheet. See ShareHistory.kt for the offline retry.
@JsonClass(generateAdapter = true)
data class ShareLogRequest(
  val customerPhone: String,
  val customerName: String,
  // ISO-8601, so a log retried later still records when the share happened.
  val sharedAt: String
)

interface ShareLogApi {
  @POST("api/staff/price-share-log")
  suspend fun log(@Body request: ShareLogRequest)
}
