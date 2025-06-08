package com.example.banhangs.Model // Or your model package

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize // If you need to pass it between activities/fragments
data class CommentModel(
    // Fields that your API *returns* for a comment
    @SerializedName("commentId") // The unique ID of the comment (e.g., "cmt_123") - API might generate this
    val id: String? = null, // Or use 'commentId' as the variable name

    @SerializedName("itemId") // The ID of the item this comment belongs to
    val itemId: String? = null, // API might include this for context

    @SerializedName("userId") // The ID of the user who posted the comment
    val userId: String? = null,

    @SerializedName("userName") // The display name of the user
    val userName: String? = null,

    @SerializedName("userProfilePicUrl") // URL for the user's profile picture (optional)
    val userProfilePicUrl: String? = null,

    @SerializedName("commentText") // The actual text content of the comment
    val text: String = "", // Or 'commentText'

    @SerializedName("timestamp") // When the comment was created/updated (e.g., Unix timestamp or ISO 8601 string)
    val timestamp: Long? = null, // Or String, depending on API format

    @SerializedName("rating") // If comments can include a star rating for the item (optional)
    val rating: Double? = null

    // Add any other fields your API provides for a comment.
    // For example, if comments can be edited:
    // @SerializedName("edited")
    // val edited: Boolean? = false,
    // @SerializedName("lastEditedTimestamp")
    // val lastEditedTimestamp: Long? = null
) : Parcelable