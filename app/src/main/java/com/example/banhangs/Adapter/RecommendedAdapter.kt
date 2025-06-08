package com.example.banhangs.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.Activity.DetailActivity
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.databinding.ViewholderRecommendedBinding

class RecommendedAdapter(val items: MutableList<ProductDetailsModel>) :
    RecyclerView.Adapter<RecommendedAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderRecommendedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewholderRecommendedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            titleTxt.text = item.name
            priceTxt.text = "$${item.price}"
            ratingTxt.text = item.averageRating.toString()

            // Kiểm tra nếu picUrl không rỗng để tránh lỗi
            if (item.galleryImageUrls?.isNotEmpty() == true) {
                Glide.with(holder.itemView.context)
                    .load(item.galleryImageUrls?.get(0)) // Lấy hình đầu tiên trong danh sách
                    .into(pic)
            }

            // Xử lý sự kiện click vào item
            root.setOnClickListener {
                val intent = Intent(holder.itemView.context, DetailActivity::class.java).apply {
                    putExtra("object", item)
                    putExtra("itemKey", item.productId)
                }
                ContextCompat.startActivity(holder.itemView.context, intent , null)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
