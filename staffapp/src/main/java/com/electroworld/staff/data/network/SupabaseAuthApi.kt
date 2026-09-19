package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SupabasePasswordLoginRequest(
  val email: String,
  val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshRequest(
  @Suppress("PropertyName") val refresh_token: String
)

@JsonClass(generateAdapter = true)
data class SupabaseTokenResponse(
  @Suppress("PropertyName") val access_token: String,
  @Suppress("PropertyName") val refresh_token: String
)

@JsonClass(generateAdapter = true)
data class SupabaseErrorResponse(
  @Suppress("PropertyName") val error_description: String? = null,
  val msg: String? = null
)

// Talks to Supabase's GoTrue REST API directly -- deliberately not the
// official Supabase Kotlin SDK, to keep this app's dependency footprint
// consistent with :app (plain Retrofit calls, manual session storage).
interface SupabaseAuthApi {
  @POST("auth/v1/token")
  suspend fun login(
    @Query("grant_type") grantType: String = "password",
    @Body request: SupabasePasswordLoginRequest
  ): SupabaseTokenResponse

  @POST("auth/v1/token")
  suspend fun refresh(
    @Query("grant_type") grantType: String = "refresh_token",
    @Body request: SupabaseRefreshRequest
  ): SupabaseTokenResponse
}
