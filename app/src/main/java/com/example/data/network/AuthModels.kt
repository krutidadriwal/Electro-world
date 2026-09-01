package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPinRequest(
  val idToken: String,
  val pin: String,
  val name: String? = null,
  val howHeardAboutUs: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
  val phone: String,
  val pin: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  val token: String,
  val phone: String,
  val name: String
)
