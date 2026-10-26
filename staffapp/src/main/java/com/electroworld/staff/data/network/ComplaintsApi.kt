package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ComplaintItem(
  val id: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "subcategory_name") val subcategoryName: String?,
  // Null on complaints made before companies were added.
  @Json(name = "company_name") val companyName: String? = null,
  val description: String,
  val address: String,
  @Json(name = "contact_phone") val contactPhone: String?,
  @Json(name = "invoice_file_name") val invoiceFileName: String?,
  val status: String,
  @Json(name = "complaint_number") val complaintNumber: String?,
  @Json(name = "technician_name") val technicianName: String?,
  @Json(name = "technician_mobile") val technicianMobile: String?,
  @Json(name = "blocked_reason") val blockedReason: String?,
  @Json(name = "cancellation_reason") val cancellationReason: String?,
  @Json(name = "closed_date") val closedDate: String?,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class ComplaintsResponse(
  val items: List<ComplaintItem>
)

// referenceNumber/technicianName/technicianMobile/reason/closedDate are each
// only required for the matching status -- see statusChangeFieldsError in
// api/staff/[...path].js, and ServiceRequestStatus.kt's
// ServiceRequestStatusDialog for which one each status collects.
@JsonClass(generateAdapter = true)
data class ComplaintStatusRequest(
  val complaintId: String,
  val status: String,
  val referenceNumber: String? = null,
  val technicianName: String? = null,
  val technicianMobile: String? = null,
  val reason: String? = null,
  val closedDate: String? = null
)

@JsonClass(generateAdapter = true)
data class ComplaintStatusResponse(
  val id: String,
  val status: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class ComplaintAddRequest(
  val phone: String,
  val customerName: String,
  val categoryIconKey: String,
  val subcategoryId: String? = null,
  val companyId: String,
  val description: String,
  val address: String,
  val contactPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class ComplaintAddResponse(
  val id: String
)

interface ComplaintsApi {
  @GET("api/staff/complaints-list")
  suspend fun list(): ComplaintsResponse

  @POST("api/staff/complaints-add")
  suspend fun add(@Body request: ComplaintAddRequest): ComplaintAddResponse

  @PATCH("api/staff/complaints-status")
  suspend fun updateStatus(@Body request: ComplaintStatusRequest): ComplaintStatusResponse
}
