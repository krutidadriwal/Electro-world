package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class UnreadableInvoiceItem(
  @Json(name = "file_id") val fileId: String,
  @Json(name = "invoice_name") val invoiceName: String,
  @Json(name = "customer_phone") val customerPhone: String,
  @Json(name = "customer_name") val customerName: String,
  @Json(name = "processed_at") val processedAt: String
)

@JsonClass(generateAdapter = true)
data class UnreadableInvoicesResponse(
  val items: List<UnreadableInvoiceItem>
)

@JsonClass(generateAdapter = true)
data class UnreadableInvoiceRetryRequest(
  val fileId: String
)

@JsonClass(generateAdapter = true)
data class UnreadableInvoiceRetryResponse(
  val fileId: String,
  // "credited", "unreadable" or "cleared" -- see unreadableInvoicesRetry in
  // server/api/staff/[...path].js.
  val outcome: String,
  val points: Int = 0
)

// Admin-only (see requireAdmin in the server route): invoices whose total
// couldn't be read off the PDF, so earned the customer no purchase points.
interface UnreadableInvoicesApi {
  @GET("api/staff/unreadable-invoices-list")
  suspend fun list(): UnreadableInvoicesResponse

  @POST("api/staff/unreadable-invoices-retry")
  suspend fun retry(@Body request: UnreadableInvoiceRetryRequest): UnreadableInvoiceRetryResponse
}
