package com.example.banhangs.Model

import android.os.Parcelable // Standard Parcelable import
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize // Import for @Parcelize

@Parcelize // Add this annotation
data class ProductDetailsModel(
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("shortDescription") val shortDescription: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("galleryImageUrls") val galleryImageUrls: List<String>?, // Keep as List<String>?
    @SerializedName("stock") val stock: Int?,
    @SerializedName("price") val price: Double, // This is correct
    @SerializedName("soldCount") val soldCount: Int?,
    @SerializedName("salePrice") val salePrice: Double?,
    @SerializedName("saleStart") val saleStart: String?, // Consider converting to Date/Timestamp if needed for logic
    @SerializedName("saleEnd") val saleEnd: String?,   // Consider converting to Date/Timestamp
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("brandId") val brandId: String?,
    @SerializedName("brandName") val brandName: String?,
    @SerializedName("ratedCount") val ratedCount: Int?,
    @SerializedName("averageRating") val averageRating: Double?, // This is correct
    @SerializedName("isOnSale") val isOnSale: Boolean?,
    @SerializedName("isFeatured") val isFeatured: Boolean?

    // --- Fields that were in your manual Parcelable but not in the data class properties ---
    // Decide if these are truly part of ProductDetailsModel from the API,
    // or if they are transient UI state or belong elsewhere.
    // If they are part of the API response for product details, add them here with @SerializedName.
    // If not, they should not be part of this core model.

    // var numberInCart: Int = 0, // Example: If you track this locally after fetching
    // var showRecommended: Boolean = false // Example: UI state
) : Parcelable {
    // With @Parcelize, you NO LONGER NEED the manual constructor,
    // writeToParcel(), describeContents(), or the CREATOR companion object.
    // The @Parcelize annotation generates all of that for you.
}