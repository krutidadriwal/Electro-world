package com.example.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
  val phone: String,
  val fcmToken: String
)

interface DeviceTokenApi {
  @POST("api/device-tokens")
  suspend fun registerToken(@Body request: RegisterDeviceTokenRequest)
}
