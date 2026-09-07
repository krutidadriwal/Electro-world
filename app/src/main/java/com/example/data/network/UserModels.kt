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
data class UpdateUserRequest(
  val phone: String,
  val name: String
)

@JsonClass(generateAdapter = true)
data class DeleteUserRequest(
  val phone: String
)

@JsonClass(generateAdapter = true)
data class DeleteUserResponse(
  val deleted: Boolean
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

@JsonClass(generateAdapter = true)
data class CreateComplaintRequest(
  val phone: String,
  val invoiceFileId: String? = null,
  val invoiceFileName: String? = null,
  val categoryIconKey: String,
  val subcategoryId: String? = null,
  val description: String,
  val address: String,
  val contactPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class Complaint(
  val id: String,
  @Json(name = "invoice_file_id") val invoiceFileId: String?,
  @Json(name = "invoice_file_name") val invoiceFileName: String?,
  @Json(name = "category_icon_key") val categoryIconKey: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "subcategory_id") val subcategoryId: String?,
  @Json(name = "subcategory_name") val subcategoryName: String?,
  val description: String,
  val address: String,
  @Json(name = "contact_phone") val contactPhone: String?,
  val status: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class ComplaintsResponse(
  val complaints: List<Complaint>
)

// Shared category/subcategory taxonomy, configurable in Supabase and used by
// both Register Complaint (product being complained about) and Wishlist
// (browse structure).
@JsonClass(generateAdapter = true)
data class Subcategory(
  val id: String,
  val name: String
)

// iconKey is the category's unique identifier (primary key in Supabase), not
// just a display hint -- it's what's sent back as categoryIconKey when filing
// a complaint.
@JsonClass(generateAdapter = true)
data class Category(
  val iconKey: String,
  val name: String,
  val canInstall: Boolean = false,
  val canDemo: Boolean = false,
  val subcategories: List<Subcategory>
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
  val categories: List<Category>
)

@JsonClass(generateAdapter = true)
data class CreateInstallationRequest(
  val phone: String,
  val invoiceFileId: String? = null,
  val invoiceFileName: String? = null,
  val categoryIconKey: String,
  val itemName: String,
  val wantsDemo: Boolean,
  val wantsInstallation: Boolean,
  val address: String,
  val contactPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class Installation(
  val id: String,
  @Json(name = "invoice_file_id") val invoiceFileId: String?,
  @Json(name = "invoice_file_name") val invoiceFileName: String?,
  @Json(name = "category_icon_key") val categoryIconKey: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "item_name") val itemName: String,
  @Json(name = "wants_demo") val wantsDemo: Boolean,
  @Json(name = "wants_installation") val wantsInstallation: Boolean,
  val address: String,
  @Json(name = "contact_phone") val contactPhone: String?,
  val status: String,
  @Json(name = "created_at") val createdAt: String,
  @Json(name = "updated_at") val updatedAt: String,
  @Json(name = "resolved_at") val resolvedAt: String?
)

@JsonClass(generateAdapter = true)
data class InstallationsResponse(
  val installations: List<Installation>
)

// A saved wishlist entry: either a whole category (subcategoryId null) or a
// specific subcategory within one.
@JsonClass(generateAdapter = true)
data class WishlistItemDto(
  val id: String,
  @Json(name = "category_icon_key") val categoryIconKey: String,
  @Json(name = "category_name") val categoryName: String,
  @Json(name = "subcategory_id") val subcategoryId: String?,
  @Json(name = "subcategory_name") val subcategoryName: String?,
  @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class WishlistResponse(
  val items: List<WishlistItemDto>
)

@JsonClass(generateAdapter = true)
data class ConfirmWishlistRequest(
  val phone: String,
  val categoryIconKey: String,
  val categoryStarred: Boolean,
  val subcategoryIds: List<String>
)
