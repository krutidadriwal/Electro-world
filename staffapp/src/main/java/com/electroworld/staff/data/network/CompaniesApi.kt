package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// A product brand (LG, Samsung...) -- the Company dropdown on the add
// complaint / installation dialogs. Managed by admins on Catalog Setup.
@JsonClass(generateAdapter = true)
data class Company(
  val id: String,
  val name: String
)

@JsonClass(generateAdapter = true)
data class CompaniesResponse(
  val companies: List<Company>
)

@JsonClass(generateAdapter = true)
data class AddCompanyRequest(
  val name: String
)

interface CompaniesApi {
  @GET("api/staff/companies")
  suspend fun list(): CompaniesResponse

  // Admin only. 409 if a company with that name (ignoring case) exists.
  @POST("api/staff/companies")
  suspend fun add(@Body request: AddCompanyRequest): Company

  // Admin only (204, no body).
  @DELETE("api/staff/companies")
  suspend fun delete(@Query("id") id: String): Response<Unit>
}
