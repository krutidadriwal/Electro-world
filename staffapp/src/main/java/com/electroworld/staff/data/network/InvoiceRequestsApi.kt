package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

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

// Admin-only (see requireAdmin in the server route) -- re-send-invoice
// requests are a billing/accounts matter, not general sales-floor triage.
interface InvoiceRequestsApi {
  @GET("api/staff/invoice-requests-list")
  suspend fun list(): InvoiceRequestsResponse
}
