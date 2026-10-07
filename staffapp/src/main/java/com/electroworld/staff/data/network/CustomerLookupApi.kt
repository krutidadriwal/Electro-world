package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class CustomerLookupResponse(
  val name: String
)

// 404s (not a 200 with a null name) when the phone has no account yet --
// used by the Wishlist/Complaints/Installations "add" dialogs to auto-fill
// the customer name once a full phone number is entered.
interface CustomerLookupApi {
  @GET("api/staff/customer-lookup")
  suspend fun lookup(@Query("phone") phone: String): CustomerLookupResponse
}

// Shared by all three "add" dialogs -- null (not an exception) both for "no
// account" (404) and for a network failure, since either way the dialog's
// only sane fallback is the same: leave the name field open for staff to
// type.
suspend fun lookupCustomerName(phone: String): String? = try {
  NetworkModule.customerLookupApi.lookup(phone).name
} catch (e: Exception) {
  null
}
