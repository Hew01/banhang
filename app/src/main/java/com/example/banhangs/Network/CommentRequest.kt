package com.example.banhangs.Network // Or your model/request package

import com.google.gson.annotations.SerializedName

data class CommentRequest(
    @SerializedName("commentText") // The key your API expects for the comment content
    val commentText: String,

    // Optional: If users can also submit a rating with their comment
    // @SerializedName("rating")
    // val rating: Double? = null
)