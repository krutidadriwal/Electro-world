package com.example.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApi {
  @POST("api/user")
  suspend fun createOrUpdateUser(@Body request: CreateUserRequest): UserResponse

  @GET("api/user")
  suspend fun getUser(@Query("phone") phone: String): UserResponse
}
