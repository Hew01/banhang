package com.example.banhangs.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategoryModel(
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("iconImageUrl") val iconImageUrl: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    companion object : Parceler<CategoryModel> {

        override fun CategoryModel.write(parcel: Parcel, flags: Int) {
            parcel.writeString(categoryId)
            parcel.writeString(name)
            parcel.writeString(iconImageUrl)
        }

        override fun create(parcel: Parcel): CategoryModel {
            return CategoryModel(parcel)
        }
    }
}
