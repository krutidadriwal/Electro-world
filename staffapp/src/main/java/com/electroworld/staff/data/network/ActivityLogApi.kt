package com.electroworld.staff.data.network

import com.electroworld.staff.data.toTitleCase
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

// One thing a staff member did, as a plain sentence -- staffName +
// description reads e.g. "Ravi changed Washing Machine complaint to Closed
// on 2026-10-26". customerPhone is null for work not done for a customer
// (broadcasts, staff accounts, price list).
@JsonClass(generateAdapter = true)
data class ActivityLogEntry(
  val id: String,
  // Null once the staff member has been deleted; staffName is still kept.
  @Json(name = "staff_id") val staffId: String? = null,
  @Json(name = "staff_name") val rawStaffName: String,
  @Json(name = "work_type") val workType: String,
  val description: String,
  @Json(name = "customer_phone") val customerPhone: String? = null,
  @Json(name = "customer_name") val rawCustomerName: String? = null,
  @Json(name = "created_at") val createdAt: String
) {
  // Title Case however the names are stored -- see Names.kt.
  val staffName: String get() = rawStaffName.toTitleCase()
  val customerName: String? get() = rawCustomerName?.toTitleCase()
}

@JsonClass(generateAdapter = true)
data class ActivityLogResponse(
  val items: List<ActivityLogEntry>,
  // Pass back as `before` for the next (older) page; null on the last page.
  val nextCursor: String? = null
)

interface ActivityLogApi {
  // Every filter is optional; from/to are ISO-8601 timestamps.
  @GET("api/staff/activity-log")
  suspend fun list(
    @Query("staffId") staffId: String? = null,
    @Query("workType") workType: String? = null,
    @Query("from") from: String? = null,
    @Query("to") to: String? = null,
    @Query("before") before: String? = null
  ): ActivityLogResponse
}
