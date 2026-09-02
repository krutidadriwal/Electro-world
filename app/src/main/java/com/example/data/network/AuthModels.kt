package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOtpRequest(
  val phone: String
)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(
  val sent: Boolean
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
  val phone: String,
  val code: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
  val verificationToken: String
)

@JsonClass(generateAdapter = true)
data class SetPinRequest(
  val verificationToken: String,
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
