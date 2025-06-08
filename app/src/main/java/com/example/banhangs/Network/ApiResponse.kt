package com.example.banhangs.Network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("partnerCode") val partnerCode: String?,
    @SerializedName("retCode") val retCode: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("statusCode") val statusCode: Int?,
    @SerializedName("systemMessage") val systemMessage: String?
)