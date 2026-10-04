package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

@JsonClass(generateAdapter = true)
data class ComplaintItem(
  val id: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "subcategory_name") val subcategoryName: String?,
  val description: String,
  val address: String,
  @Json(name = "contact_phone") val contactPhone: String?,
  @Json(name = "invoice_file_name") val invoiceFileName: String?,
  val status: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class ComplaintsResponse(
  val items: List<ComplaintItem>
)

@JsonClass(generateAdapter = true)
data class ComplaintStatusRequest(
  val complaintId: String,
  val status: String
)

@JsonClass(generateAdapter = true)
data class ComplaintStatusResponse(
  val id: String,
  val status: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

interface ComplaintsApi {
  @GET("api/staff/complaints-list")
  suspend fun list(): ComplaintsResponse

  @PATCH("api/staff/complaints-status")
  suspend fun updateStatus(@Body request: ComplaintStatusRequest): ComplaintStatusResponse
}
