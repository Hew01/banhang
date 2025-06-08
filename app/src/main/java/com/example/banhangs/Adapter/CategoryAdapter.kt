package com.example.banhangs.Adapter // Or your actual adapter package

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.Activity.ListItemsActivity // Activity to open on category click
import com.example.banhangs.Model.CategoryModel         // Your CategoryModel
import com.example.banhangs.R
import com.example.banhangs.databinding.ViewhoderCategoryBinding // Your ViewBinding class for the item layout

class CategoryAdapter(private var items: MutableList<CategoryModel>) : // Use private var for items
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = -1
    private var lastSelectedPosition = -1
    private val handler = Handler(Looper.getMainLooper()) // Reusable handler
    private var clickRunnable: Runnable? = null // To manage delayed clicks

    inner class ViewHolder(val binding: ViewhoderCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewhoderCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position] // Use currentItem for clarity

        holder.binding.titleTxt.text = currentItem.name

        Glide.with(holder.itemView.context)
            .load(currentItem.iconImageUrl)
            .placeholder(R.drawable.ic_placeholder) // Add a placeholder
            .error(R.drawable.ic_error_placeholder)   // Add an error placeholder
            .into(holder.binding.pic)

        val context = holder.itemView.context

        if (selectedPosition == position) {
            holder.binding.pic.setBackgroundResource(0) // Or a specific selected background for the image itself
            holder.binding.mainLayout.setBackgroundResource(R.drawable.green_button_bg) // Background for the whole item
            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            )
            holder.binding.titleTxt.visibility = View.VISIBLE
            holder.binding.titleTxt.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.binding.pic.setBackgroundResource(R.drawable.grey_bg) // Background for the image when not selected
            holder.binding.mainLayout.setBackgroundResource(0) // No background for the whole item or a default one
            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
            )
            holder.binding.titleTxt.visibility = View.GONE // Or View.INVISIBLE if you want to keep space
            // titleTxt color when not selected (might not be visible if GONE)
            // holder.binding.titleTxt.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        holder.binding.root.setOnClickListener {
            // Cancel any pending click runnable
            clickRunnable?.let { handler.removeCallbacks(it) }

            val newPosition = holder.adapterPosition // Use adapterPosition for safety
            if (newPosition != RecyclerView.NO_POSITION) {
                lastSelectedPosition = selectedPosition
                selectedPosition = newPosition

                // Notify changes for the old and new selected items
                if (lastSelectedPosition != -1) {
                    notifyItemChanged(lastSelectedPosition)
                }
                notifyItemChanged(selectedPosition)

                // Create a new runnable for the delayed action
                clickRunnable = Runnable {
                    val intent = Intent(context, ListItemsActivity::class.java).apply {
                        // Pass category ID and title to the ListItemsActivity
                        // Ensure ListItemsActivity expects "categoryId" and "categoryTitle"
                        putExtra("categoryId", currentItem.categoryId)
                        putExtra("categoryTitle", currentItem.name)
                    }
                    ContextCompat.startActivity(context, intent, null)
                }
                handler.postDelayed(clickRunnable!!, 300) // Reduced delay for better UX, adjust as needed
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // Method to update the list of categories in the adapter
    fun updateCategories(newCategories: List<CategoryModel>) {
        items.clear()
        items.addAll(newCategories)
        selectedPosition = -1 // Reset selection when data changes
        lastSelectedPosition = -1
        notifyDataSetChanged() // Or use DiffUtil for better performance with large lists
    }
    fun updateData(newItems: List<CategoryModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}