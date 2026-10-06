package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

@JsonClass(generateAdapter = true)
data class InvoiceRequestItem(
  val id: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  val description: String,
  val status: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class InvoiceRequestsResponse(
  val items: List<InvoiceRequestItem>
)

@JsonClass(generateAdapter = true)
data class InvoiceRequestStatusRequest(
  val invoiceRequestId: String,
  val status: String
)

@JsonClass(generateAdapter = true)
data class InvoiceRequestStatusResponse(
  val id: String,
  val status: String,
  // Absent when status == "closed" -- that response deletes the row instead
  // of updating it (see WishlistStatusResponse for the same shape, used for
  // "purchased").
  @Json(name = "updated_at") val updatedAt: String? = null,
  @Json(name = "resolved_at") val resolvedAt: String? = null
)

// Admin-only (see requireAdmin in the server route) -- re-send-invoice
// requests are a billing/accounts matter, not general sales-floor triage.
interface InvoiceRequestsApi {
  @GET("api/staff/invoice-requests-list")
  suspend fun list(): InvoiceRequestsResponse

  @PATCH("api/staff/invoice-requests-status")
  suspend fun updateStatus(@Body request: InvoiceRequestStatusRequest): InvoiceRequestStatusResponse
}
