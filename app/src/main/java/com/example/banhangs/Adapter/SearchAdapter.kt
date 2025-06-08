// SearchAdapter.kt
package com.example.banhangs.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.R

class SearchAdapter(private var items: MutableList<ProductDetailsModel>) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageView)
        val title: TextView = itemView.findViewById(R.id.textViewTitle)
        val price: TextView = itemView.findViewById(R.id.textViewPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.price.text = "đ${item.price}"

        if (item.galleryImageUrls?.isNotEmpty() == true) {
            Glide.with(holder.itemView.context)
                .load(item.galleryImageUrls?.get(0))
                .into(holder.image)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<ProductDetailsModel>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}