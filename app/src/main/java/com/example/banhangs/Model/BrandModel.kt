package com.example.banhangs.Model

import android.os.Parcelable // Standard Parcelable import
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize // Import for @Parcelize

@Parcelize // This annotation handles the Parcelable implementation
data class BrandModel(
    @SerializedName("brandId") val brandId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?, // Nullable if API might not send it
    @SerializedName("slug") val slug: String?,               // Nullable
    @SerializedName("iconImageUrl") val iconImageUrl: String? // Nullable
) : Parcelable // Declare that it implements Parcelable