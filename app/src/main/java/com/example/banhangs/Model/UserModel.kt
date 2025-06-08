package com.example.banhangs.Model

import com.google.gson.annotations.SerializedName

data class UserModel(
    @SerializedName("userId") val userId: String,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("email") val email: String?,
    // ... other fields from the "user" object in login response
    @SerializedName("pictureUrl") val pictureUrl: String?
)