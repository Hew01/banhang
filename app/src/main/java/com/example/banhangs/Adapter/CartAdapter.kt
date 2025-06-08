package com.example.banhangs.Adapter // Or your adapter package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.banhangs.R
import com.example.banhangs.databinding.ViewholderCartBinding // Ensure this layout exists and is correctly named
import com.example.banhangs.Model.CartItemData

class CartAdapter(
    private var cartItems: MutableList<CartItemData>,
    private val onQuantityChanged: (productId: String, newQuantity: Int) -> Unit,
    private val onItemRemoved: (productId: String, currentQuantity: Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ViewholderCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<CartItemData>) {
        cartItems.clear()
        cartItems.addAll(newItems)
        notifyDataSetChanged() // Consider DiffUtil for better performance
    }

    inner class CartViewHolder(private val binding: ViewholderCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItemData) {
            binding.titleTxt.text = cartItem.productName ?: "Product Name"

            // Corrected: Accessing text property of TextView
            binding.feeEachItem.text = "$${String.format("%.2f", cartItem.priceAtOrderTime)}"
            binding.totalEachItem.text = "$${String.format("%.2f", cartItem.totalPrice ?: (cartItem.priceAtOrderTime * cartItem.quantity))}"
            binding.numberItemTxt.text = cartItem.quantity.toString()

            Glide.with(binding.root.context)
                .load(cartItem.mainImageUrl)
                .placeholder(R.drawable.ic_placeholder) // Add a placeholder drawable
                .error(R.drawable.ic_placeholder)       // Add an error placeholder drawable
                .into(binding.pic)

            binding.plusCartBtn.setOnClickListener {
                val newQuantity = cartItem.quantity + 1
                // Potentially add a stock check here if CartItemData includes stock information
                onQuantityChanged(cartItem.productId, newQuantity)
            }

            binding.minusCartBtn.setOnClickListener {
                if (cartItem.quantity > 1) {
                    val newQuantity = cartItem.quantity - 1
                    onQuantityChanged(cartItem.productId, newQuantity)
                } else {
                    // If quantity is 1, pressing minus should remove the item entirely
                    onItemRemoved(cartItem.productId, cartItem.quantity)
                }
            }

            // Optional: If you have a dedicated remove button per item in your viewholder_cart.xml
            // Make sure 'removeItemButton' is the ID in your XML.
            // binding.removeItemButton.setOnClickListener {
            //     onItemRemoved(cartItem.productId, cartItem.quantity)
            // }
        }
    }
}