package com.example.banhangs.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.OrderModel
import com.example.banhangs.R
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private val orders: List<OrderModel>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTxt: TextView = itemView.findViewById(R.id.dateTxt)
        val itemsRecyclerView: RecyclerView = itemView.findViewById(R.id.itemsRecyclerView)
        val totalTxt: TextView = itemView.findViewById(R.id.totalTxt)
        val statusTxt: TextView = itemView.findViewById(R.id.statusTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(order.timestamp))
        holder.dateTxt.text = "Ngày: $date"
        // Sử dụng NumberFormat để định dạng tổng tiền cho nhất quán với CartActivity
        val formatter = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0
        holder.totalTxt.text = "Tổng: ${formatter.format(order.total.toLong())}$"
        holder.statusTxt.text = "Trạng thái: ${order.status}"

        // Hiển thị danh sách sản phẩm trong đơn hàng
        holder.itemsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.VERTICAL, false)
        holder.itemsRecyclerView.adapter = OrderItemAdapter(order.items)
    }

    override fun getItemCount(): Int = orders.size
}

class OrderItemAdapter(private val items: List<ProductDetailsModel>) : RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder>() {

    class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.titleTxt)
        val priceTxt: TextView = itemView.findViewById(R.id.priceTxt)
        val quantityTxt: TextView = itemView.findViewById(R.id.quantityTxt)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_product, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        val item = items[position]
        holder.titleTxt.text = item.title
        // Sử dụng NumberFormat để định dạng giá
        val formatter = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0
        holder.priceTxt.text = "Giá: ${formatter.format(item.price)}$"
        holder.quantityTxt.text = "Số lượng: ${item.numberInCart}"
        // Thêm placeholder cho Glide
        Glide.with(holder.itemView.context)
            .load(if (item.picUrl.isNotEmpty()) item.picUrl[0] else R.drawable.placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = items.size
}