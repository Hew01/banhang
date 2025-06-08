// In com/example/banhangs/Adapter/SliderAdapter.kt
package com.example.banhangs.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.banhangs.Model.SliderModel // Your SliderModel
import com.example.banhangs.databinding.ViewholderSliderBinding // Assuming this is your item layout binding

class SliderAdapter(
    private var items: MutableList<SliderModel>,
    private val viewPager2: ViewPager2 // If you need it for auto-scroll or other interactions
) : RecyclerView.Adapter<SliderAdapter.ViewHolder>() {

    // ... (onCreateViewHolder, onBindViewHolder, getItemCount similar to RecommendedAdapter,
    //      but using SliderModel and ViewholderSliderBinding)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl) // Assuming SliderModel has 'imageUrl'
            .into(holder.binding.imageSlide) // Assuming your binding has 'imageSlide'
    }

    override fun getItemCount(): Int = items.size

    // THIS IS THE IMPORTANT METHOD
    fun updateData(newItems: List<SliderModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ViewholderSliderBinding) : RecyclerView.ViewHolder(binding.root)
}