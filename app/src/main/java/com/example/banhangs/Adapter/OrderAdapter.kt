package com.example.banhangs.Adapter

import android.content.res.ColorStateList // Import this
import android.util.Log // For logging errors
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat // Import this
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.Model.OrderData
import com.example.banhangs.R
import com.example.banhangs.databinding.ViewholderOrderBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class OrderAdapter(private var orders: MutableList<OrderData> = mutableListOf()) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    // For date formatting
    private val inputApiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val outputDisplayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val priceFormatter = DecimalFormat("#,### đ")

    init {
        inputApiFormat.timeZone = TimeZone.getTimeZone("UTC")
        outputDisplayFormat.timeZone = TimeZone.getDefault()
    }

    private val diffCallback = object : DiffUtil.ItemCallback<OrderData>() {
        override fun areItemsTheSame(oldItem: OrderData, newItem: OrderData): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: OrderData, newItem: OrderData): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun updateOrders(newOrders: List<OrderData>) {
        differ.submitList(newOrders.toList()) // Submit a copy to be safe
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ViewholderOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = differ.currentList[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int = differ.currentList.size

    inner class OrderViewHolder(private val binding: ViewholderOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderData) {
            binding.orderIdTxt.text = itemView.context.getString(R.string.order_id_prefix, order.orderId)

            try {
                order.createdAt?.let {
                    val date = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse(it)
                    } catch (e1: Exception) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.parse(it)
                        } catch (e2: Exception) {
                            inputApiFormat.parse(it)
                        }
                    }
                    binding.orderDateTxt.text = itemView.context.getString(R.string.order_date_prefix, outputDisplayFormat.format(date!!))
                } ?: run {
                    binding.orderDateTxt.text = itemView.context.getString(R.string.order_date_prefix, "N/A")
                }
            } catch (e: Exception) {
                binding.orderDateTxt.text = itemView.context.getString(R.string.order_date_prefix, "Invalid Date")
                Log.e("OrderAdapter", "Date parsing error for ${order.createdAt}: ${e.message}")
            }

            binding.orderTotalTxt.text = itemView.context.getString(R.string.order_total_prefix, priceFormatter.format(order.finalAmount ?: 0.0))

            // --- START OF MODIFICATION for status appearance ---
            val statusString = getOrderStatusText(order.orderStatus, itemView.context) // Pass context
            binding.orderStatusTxt.text = statusString // Set the text directly
            setOrderStatusAppearance(order.orderStatus)
            // --- END OF MODIFICATION for status appearance ---

            if (!order.items.isNullOrEmpty()) {
                val firstItem = order.items[0]
                binding.firstItemNameTxt.text = firstItem.productName ?: "Item Details Unavailable"
                Glide.with(itemView.context)
                    .load(firstItem.mainImageUrl)
                    .placeholder(R.drawable.rounded_background)
                    .error(R.drawable.rounded_background)
                    .into(binding.firstItemImg)
                binding.firstItemImg.visibility = View.VISIBLE
                binding.firstItemNameTxt.visibility = View.VISIBLE

                if (order.items.size > 1) {
                    binding.moreItemsTxt.text = itemView.context.getString(R.string.more_items_format, order.items.size - 1)
                    binding.moreItemsTxt.visibility = View.VISIBLE
                } else {
                    binding.moreItemsTxt.visibility = View.GONE
                }

            } else {
                binding.firstItemNameTxt.text = "No items in this order"
                // Consider setting a placeholder or hiding the ImageView if no image
                binding.firstItemImg.setImageResource(R.drawable.rounded_background)
                binding.firstItemImg.visibility = View.VISIBLE // Or View.GONE
                binding.moreItemsTxt.visibility = View.GONE
            }

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "Clicked on order: ${order.orderId}", Toast.LENGTH_SHORT).show()
                // TODO: Implement click listener to navigate to OrderDetailActivity
                // Example:
                // val intent = Intent(itemView.context, OrderDetailActivity::class.java)
                // intent.putExtra("ORDER_ID", order.orderId)
                // itemView.context.startActivity(intent)
            }
        }

        // --- ADD THIS HELPER METHOD INSIDE OrderViewHolder ---
        private fun setOrderStatusAppearance(status: Int?) {
            val context = itemView.context
            var backgroundColorRes = R.color.status_unknown_bg
            var textColorRes = R.color.status_unknown_text

            when (status) {
                0 -> { // Pending
                    backgroundColorRes = R.color.status_pending_bg
                    textColorRes = R.color.status_pending_text
                }
                1 -> { // Processing
                    backgroundColorRes = R.color.status_processing_bg
                    textColorRes = R.color.status_processing_text
                }
                2 -> { // Delivering
                    backgroundColorRes = R.color.status_delivering_bg
                    textColorRes = R.color.status_delivering_text
                }
                3 -> { // Completed
                    backgroundColorRes = R.color.status_completed_bg
                    textColorRes = R.color.status_completed_text
                }
                4 -> { // Canceled
                    backgroundColorRes = R.color.status_canceled_bg
                    textColorRes = R.color.status_canceled_text
                }
                5 -> { // Refunded
                    backgroundColorRes = R.color.status_refunded_bg
                    textColorRes = R.color.status_refunded_text
                }
                6 -> { //Failed
                    backgroundColorRes = R.color.status_failed_bg
                    textColorRes = R.color.status_failed_text
                }
            }
            // Apply the background tint and text color
            // Make sure your binding.orderStatusTxt has a background drawable that supports tinting
            // (e.g., a <shape> drawable). If not, you might need to set a background resource
            // that is a shape drawable.
            // Example: binding.orderStatusTxt.setBackgroundResource(R.drawable.status_badge_background)
            // where status_badge_background is a simple shape drawable.
            binding.orderStatusTxt.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, backgroundColorRes))
            binding.orderStatusTxt.setTextColor(ContextCompat.getColor(context, textColorRes))
        }
    } // End of OrderViewHolder

    // Helper function to convert status code to readable text
    // This should align with your EOrderStatus enum
    // --- MODIFIED to accept Context and use string resources ---
    private fun getOrderStatusText(status: Int?, context: android.content.Context): String {
        return when (status) {
            0 -> context.getString(R.string.status_pending)
            1 -> context.getString(R.string.status_processing)
            2 -> context.getString(R.string.status_delivering)
            3 -> context.getString(R.string.status_completed)
            4 -> context.getString(R.string.status_canceled)
            5 -> context.getString(R.string.status_refunded)
            6 -> context.getString(R.string.status_failed)
            else -> context.getString(R.string.status_unknown)
        }
    }
}