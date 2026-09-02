package com.example.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
  @POST("api/auth/send-otp")
  suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

  @POST("api/auth/verify-otp")
  suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

  @POST("api/auth/set-pin")
  suspend fun setPin(@Body request: SetPinRequest): AuthResponse

  @POST("api/auth/login")
  suspend fun login(@Body request: LoginRequest): AuthResponse
}
