package com.example.banhangs.Adapter // Or your adapter package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.R
import com.example.banhangs.databinding.ViewholderCommentBinding // Create this layout
import com.example.banhangs.Model.ApiCommentModel
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParseException // Import for ParseException


class CommentAdapter(private var comments: MutableList<ApiCommentModel>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ViewholderCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<ApiCommentModel>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged() // Consider DiffUtil for better performance
    }

    fun addComment(comment: ApiCommentModel) {
        comments.add(0, comment) // Add to the top
        notifyItemInserted(0)
        // Consider scrolling to the top if you add a new comment
    }


    inner class CommentViewHolder(private val binding: ViewholderCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: ApiCommentModel) {
            binding.userNameTxt.text = comment.userName ?: "Anonymous"
            binding.commentTxt.text = comment.commentText

            // Format date (example)
            binding.dateTxt.text = comment.createdAt?.let { formatDisplayDate(it) } ?: "Just now"

            // Load user image (example)
            Glide.with(binding.root.context)
                .load(comment.userProfileImageUrl)
                .placeholder(R.drawable.profile_pic_placeholder) // Add a placeholder drawable
                .error(R.drawable.profile_pic_placeholder) // Error placeholder
                .circleCrop()
                .into(binding.pic)

            // Handle rating if your comment model and layout have it
            // binding.ratingBar.rating = comment.rating ?: 0f
            // binding.ratingBar.visibility = if (comment.rating != null) View.VISIBLE else View.GONE
        }

        private fun formatDisplayDate(dateString: String): String {
            // Input format from API: "YYYY-MM-DDTHH:mm:ss.sssZ" or similar ISO 8601
            // Output format desired: "MMM dd, yyyy" or "HH:mm MMM dd"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Assuming API sends UTC
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

            return try {
                val date: Date = inputFormat.parse(dateString) ?: return dateString
                outputFormat.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
                dateString // Fallback to original string if parsing fails
            }
        }
    }
}