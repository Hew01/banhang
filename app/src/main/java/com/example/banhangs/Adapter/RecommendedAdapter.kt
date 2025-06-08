// In com/example/banhangs/Adapter/RecommendedAdapter.kt
package com.example.banhangs.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.Model.ProductDetailsModel // Or ProductSummaryData if that's what you use here
import com.example.banhangs.databinding.ViewholderRecommendedBinding // Assuming this is your item layout binding

class RecommendedAdapter(private var items: MutableList<ProductDetailsModel>) :
    RecyclerView.Adapter<RecommendedAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderRecommendedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.binding.titleTxt.text = currentItem.name
        holder.binding.priceTxt.text = String.format("$%.2f", currentItem.price) // Example formatting
        // holder.binding.ratingTxt.text = currentItem.averageRating.toString() // If you have rating

        Glide.with(holder.itemView.context)
            .load(currentItem.mainImageUrl) // Assuming mainImageUrl is the field
            // .placeholder(R.drawable.placeholder_image) // Optional placeholder
            // .error(R.drawable.error_image) // Optional error image
            .into(holder.binding.pic)

        // Handle item click if needed
        holder.itemView.setOnClickListener {
            // Intent to ProductDetailActivity, pass currentItem.productId or the object
        }
    }

    override fun getItemCount(): Int = items.size

    // THIS IS THE IMPORTANT METHOD
    fun updateData(newItems: List<ProductDetailsModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // For simplicity. Consider DiffUtil for better performance.
    }

    class ViewHolder(val binding: ViewholderRecommendedBinding) : RecyclerView.ViewHolder(binding.root)
}