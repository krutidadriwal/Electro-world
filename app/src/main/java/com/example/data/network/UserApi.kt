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

  @GET("api/invoices")
  suspend fun getInvoices(@Query("phone") phone: String): InvoicesResponse

  @GET("api/complaints")
  suspend fun getComplaints(@Query("phone") phone: String): ComplaintsResponse

  @POST("api/complaints")
  suspend fun createComplaint(@Body request: CreateComplaintRequest): Complaint

  @GET("api/categories")
  suspend fun getCategories(): CategoriesResponse

  @GET("api/installations")
  suspend fun getInstallations(@Query("phone") phone: String): InstallationsResponse

  @POST("api/installations")
  suspend fun createInstallation(@Body request: CreateInstallationRequest): Installation

  @GET("api/wishlist")
  suspend fun getWishlist(@Query("phone") phone: String): WishlistResponse

  @POST("api/wishlist")
  suspend fun confirmWishlist(@Body request: ConfirmWishlistRequest): WishlistResponse
}
