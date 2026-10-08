package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class InstallationItem(
  val id: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "item_name") val itemName: String,
  @Json(name = "wants_demo") val wantsDemo: Boolean,
  @Json(name = "wants_installation") val wantsInstallation: Boolean,
  val address: String,
  @Json(name = "contact_phone") val contactPhone: String?,
  @Json(name = "invoice_file_name") val invoiceFileName: String?,
  val status: String,
  @Json(name = "installation_number") val installationNumber: String?,
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
data class InstallationsResponse(
  val items: List<InstallationItem>
)

// See the matching comment on ComplaintStatusRequest -- same per-status
// required-field pattern, shared ServiceRequestStatusDialog.
@JsonClass(generateAdapter = true)
data class InstallationStatusRequest(
  val installationId: String,
  val status: String,
  val referenceNumber: String? = null,
  val technicianName: String? = null,
  val technicianMobile: String? = null,
  val reason: String? = null,
  val closedDate: String? = null
)

@JsonClass(generateAdapter = true)
data class InstallationStatusResponse(
  val id: String,
  val status: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class InstallationAddRequest(
  val phone: String,
  val customerName: String,
  val categoryIconKey: String,
  val itemName: String,
  val wantsDemo: Boolean,
  val wantsInstallation: Boolean,
  val address: String,
  val contactPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class InstallationAddResponse(
  val id: String
)

interface InstallationsApi {
  @GET("api/staff/installations-list")
  suspend fun list(): InstallationsResponse

  @POST("api/staff/installations-add")
  suspend fun add(@Body request: InstallationAddRequest): InstallationAddResponse

  @PATCH("api/staff/installations-status")
  suspend fun updateStatus(@Body request: InstallationStatusRequest): InstallationStatusResponse
}
