package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class StaffMember(
  val id: String,
  val email: String,
  val role: String,
  // Nullable -- set by hand in Supabase; use displayName for showing it.
  val name: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
) {
  val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: email
}

@JsonClass(generateAdapter = true)
data class StaffListResponse(
  val staff: List<StaffMember>
)

@JsonClass(generateAdapter = true)
data class CreateStaffRequest(
  val email: String,
  val password: String,
  val role: String
)

@JsonClass(generateAdapter = true)
data class UpdateStaffRoleRequest(
  val staffId: String,
  val role: String
)

@JsonClass(generateAdapter = true)
data class DeleteStaffRequest(
  val staffId: String
)

@JsonClass(generateAdapter = true)
data class RegisterStaffDeviceTokenRequest(
  val fcmToken: String
)

interface StaffApi {
  @GET("api/staff/me")
  suspend fun me(): StaffMember

  @GET("api/staff/list")
  suspend fun list(): StaffListResponse

  @POST("api/staff/create")
  suspend fun create(@Body request: CreateStaffRequest): StaffMember

  @PATCH("api/staff/role")
  suspend fun updateRole(@Body request: UpdateStaffRoleRequest): StaffMember

  // Removes the staff member and their login entirely (204, no body).
  @POST("api/staff/remove")
  suspend fun delete(@Body request: DeleteStaffRequest): Response<Unit>

  @POST("api/staff/device-token")
  suspend fun registerDeviceToken(@Body request: RegisterStaffDeviceTokenRequest)
}
