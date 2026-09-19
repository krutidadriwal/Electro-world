package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class StaffMember(
  val id: String,
  val email: String,
  val role: String,
  val createdAt: String? = null
)

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

interface StaffApi {
  @GET("api/staff/me")
  suspend fun me(): StaffMember

  @GET("api/staff/list")
  suspend fun list(): StaffListResponse

  @POST("api/staff/create")
  suspend fun create(@Body request: CreateStaffRequest): StaffMember

  @PATCH("api/staff/role")
  suspend fun updateRole(@Body request: UpdateStaffRoleRequest): StaffMember
}
