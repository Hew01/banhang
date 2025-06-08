package com.example.banhangs.Model
import com.example.banhangs.Network.ApiResponse
import com.google.gson.annotations.SerializedName

data class OrderItemData(
    @SerializedName("orderItemId") val orderItemId: String?,
    @SerializedName("orderId") val orderId: String?,
    @SerializedName("productId") val productId: String?,
    @SerializedName("productName") val productName: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("priceAtOrderTime") val priceAtOrderTime: Double?,
    @SerializedName("discount") val discount: Double?,
    @SerializedName("totalPrice") val totalPrice: Double?
)

// Main model for an order
data class OrderData(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("customerPhonenumber") val customerPhonenumber: String?,
    @SerializedName("customerEmail") val customerEmail: String?,
    @SerializedName("orderStatus") val orderStatus: Int?, // Corresponds to EOrderStatus enum
    @SerializedName("shippingAddress") val shippingAddress: String?,
    @SerializedName("totalPrice") val totalPrice: Double?,
    @SerializedName("discountAmount") val discountAmount: Double?,
    @SerializedName("shippingCharge") val shippingCharge: Double?,
    @SerializedName("finalAmount") val finalAmount: Double?,
    @SerializedName("paymentMethod") val paymentMethod: Int?, // Corresponds to EPaymentMethod enum
    @SerializedName("createdAt") val createdAt: String?, // Consider parsing to Date
    @SerializedName("updatedAt") val updatedAt: String?, // Consider parsing to Date
    @SerializedName("items") val items: List<OrderItemData>?,
    @SerializedName("orderType") val orderType: Int?, // Corresponds to EOrderType enum
    @SerializedName("shippingDetailId") val shippingDetailId: String?,
    @SerializedName("shipperName") val shipperName: String?,
    @SerializedName("trackingNumber") val trackingNumber: String?,
    @SerializedName("status") val status: Int?, // This seems redundant with orderStatus, clarify if needed
    @SerializedName("shippedDate") val shippedDate: String?, // Consider parsing to Date
    @SerializedName("estimatedArrival") val estimatedArrival: String?, // Consider parsing to Date
    @SerializedName("shippingNote") val shippingNote: String?,
    @SerializedName("failureCount") val failureCount: Int?
)

// Typealias for the API response structure for a list of orders
typealias OrdersApiResponse = ApiResponse<List<OrderData>>