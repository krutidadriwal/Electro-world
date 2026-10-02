package com.electroworld.staff.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class Subcategory(
  val id: String,
  val name: String
)

@JsonClass(generateAdapter = true)
data class Category(
  val iconKey: String,
  val name: String,
  val canInstall: Boolean,
  val canDemo: Boolean,
  val subcategories: List<Subcategory>
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
  val categories: List<Category>
)

// Public, unauthenticated catalog data (same /api/categories the customer
// app uses) -- reused here so staff can pick a category when manually
// logging a walk-in customer's wishlist interest.
interface CatalogApi {
  @GET("api/categories")
  suspend fun categories(): CategoriesResponse
}
