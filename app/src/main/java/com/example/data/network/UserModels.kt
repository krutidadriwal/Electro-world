package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
  val name: String,
  val phone: String,
  val howHeardAboutUs: String? = null
)

@JsonClass(generateAdapter = true)
data class UserResponse(
  val phone: String,
  val name: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "last_login_at") val lastLoginAt: String
)

@JsonClass(generateAdapter = true)
data class InvoiceFile(
  val id: String,
  val name: String,
  val createdAt: String?,
  val sizeBytes: Long?
)

@JsonClass(generateAdapter = true)
data class InvoicesResponse(
  val invoices: List<InvoiceFile>
)
