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
    @SerializedName("orderId") val orderId: String, // Matches "ORD-0022"
    @SerializedName("id") val internalId: String?, // Matches "68484ab07b44b2d92ca201e4", renamed to avoid conflict if you use 'id' elsewhere

    @SerializedName("customerId") val customerId: String?,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("customerPhoneNumber") val customerPhoneNumber: String?, // Corrected casing
    @SerializedName("customerEmail") val customerEmail: String?,

    @SerializedName("orderStatus") val orderStatus: Int?,
    @SerializedName("shippingAddress") val shippingAddress: String?,
    @SerializedName("totalPrice") val totalPrice: Double?,
    @SerializedName("discountAmount") val discountAmount: Double?,
    @SerializedName("shippingCharge") val shippingCharge: Double?,
    @SerializedName("finalAmount") val finalAmount: Double?,
    @SerializedName("paymentMethod") val paymentMethod: Int?,

    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,

    @SerializedName("items") val items: List<OrderItemData>?, // Assuming items can exist, though not in this sample

    @SerializedName("orderType") val orderType: Int?,

    // Fields from JSON not originally in OrderData
    @SerializedName("note") val note: String?,
    @SerializedName("invoiceId") val invoiceId: String?,
    @SerializedName("paymentId") val paymentId: String?,
    @SerializedName("qrCodeId") val qrCodeId: String?,
    @SerializedName("entityStatus") val entityStatus: Int?,
    @SerializedName("createdBy") val createdBy: String?,
    @SerializedName("updatedBy") val updatedBy: String?,

    // Optional fields that were in OrderData but not in this specific JSON sample
    // These will be null if not present in the JSON, which is fine.
    @SerializedName("shippingDetailId") val shippingDetailId: String?,
    @SerializedName("shipperName") val shipperName: String?,
    @SerializedName("trackingNumber") val trackingNumber: String?,
    // @SerializedName("status") val status: Int?, // Decide if you need this or if orderStatus is enough
    @SerializedName("shippedDate") val shippedDate: String?,
    @SerializedName("estimatedArrival") val estimatedArrival: String?,
    @SerializedName("shippingNote") val shippingNote: String?,
    @SerializedName("failureCount") val failureCount: Int?
)


// Typealias for the API response structure for a list of orders
typealias OrdersApiResponse = ApiResponse<List<OrderData>>