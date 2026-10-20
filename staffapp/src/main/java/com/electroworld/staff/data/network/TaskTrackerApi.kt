package com.electroworld.staff.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

// EXPERIMENTAL task tracker -- see TaskTrackerScreen.kt for how to remove it.
// Talks to server/api/tasks/[...path].js, which 404s unless the server's own
// TASK_TRACKER_MODE env var is "true".

@JsonClass(generateAdapter = true)
data class TrackerTask(
  val id: String,
  val title: String,
  val description: String,
  val status: String,
  val priority: String,
  @Json(name = "assigned_to") val assignedTo: String? = null,
  @Json(name = "assigned_to_email") val assignedToEmail: String? = null,
  @Json(name = "assigned_to_name") val assignedToName: String? = null,
  @Json(name = "created_by_email") val createdByEmail: String? = null,
  @Json(name = "created_by_name") val createdByName: String? = null,
  @Json(name = "updated_by_email") val updatedByEmail: String? = null,
  @Json(name = "updated_by_name") val updatedByName: String? = null,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "completed_at") val completedAt: String? = null
) {
  // Name when set in Supabase, else email (same fallback as StaffMember.displayName).
  val assignedToDisplay: String? get() = assignedToName?.takeIf { it.isNotBlank() } ?: assignedToEmail
  val createdByDisplay: String? get() = createdByName?.takeIf { it.isNotBlank() } ?: createdByEmail
}

@JsonClass(generateAdapter = true)
data class TaskListResponse(
  val items: List<TrackerTask>
)

@JsonClass(generateAdapter = true)
data class CreateTaskRequest(
  val title: String,
  val description: String,
  val priority: String,
  val assignedTo: String? = null
)

// Partial update: null fields are dropped by Moshi and left unchanged
// server-side. assignedTo = "" unassigns.
@JsonClass(generateAdapter = true)
data class UpdateTaskRequest(
  val id: String,
  val title: String? = null,
  val description: String? = null,
  val status: String? = null,
  val priority: String? = null,
  val assignedTo: String? = null
)

interface TaskTrackerApi {
  @GET("api/tasks/list")
  suspend fun list(): TaskListResponse

  // Assignee picker options (admins only, for now) -- same { staff: [...] }
  // shape as StaffApi.list, so it reuses StaffListResponse.
  @GET("api/tasks/staff")
  suspend fun assignees(): StaffListResponse

  @POST("api/tasks/create")
  suspend fun create(@Body request: CreateTaskRequest): TrackerTask

  @PATCH("api/tasks/update")
  suspend fun update(@Body request: UpdateTaskRequest): TrackerTask

  @DELETE("api/tasks/delete")
  suspend fun delete(@Query("id") id: String)
}
