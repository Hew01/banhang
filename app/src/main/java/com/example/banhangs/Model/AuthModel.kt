package com.example.banhangs.Model

import android.os.Parcelable
import com.example.banhangs.Network.ApiResponse
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// --- Login ---
data class LoginRequest(
    @SerializedName("loginIdentifier")
    val loginIdentifier: String, // This could be email or username

    @SerializedName("password")
    val password: String
)

@Parcelize
data class UserData( // Part of the LoginResponse
    @SerializedName("userId")
    val userId: String,

    @SerializedName("firstName")
    val firstName: String?, // Make nullable if API might not send it

    @SerializedName("lastName")
    val lastName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("passwordHash") // Usually, you don't get the hash back
    val passwordHash: String?, // Consider if you really need this on the client

    @SerializedName("birthday")
    val birthday: String?, // Dates are often Strings, parse them as needed

    @SerializedName("gender")
    val gender: Int?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("phoneNumber")
    val phoneNumber: String?,

    @SerializedName("roleName")
    val roleName: String?,

    @SerializedName("roleId")
    val roleId: Int?,

    @SerializedName("pictureUrl")
    val pictureUrl: String?,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("status")
    val status: Int?
) : Parcelable

data class LoginResponseData( // The "data" object within LoginResponse
    @SerializedName("user")
    val user: UserData,

    @SerializedName("token")
    val token: String // This is important for subsequent authenticated API calls
)

// Specific type alias for Login Response
typealias LoginApiResponse = ApiResponse<LoginResponseData>

// Continue in network/auth/AuthModels.kt or similar

data class UserInformationRequest( // Part of RegisterRequest
    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,


    // IMPORTANT: Clients should NEVER send passwordHash.
    // The server should generate the hash from the plain password.
    // Remove passwordHash from here if your API expects plain password for registration.
    // @SerializedName("passwordHash")
    // val passwordHash: String,

    @SerializedName("birthday")
    val birthday: String?, // Format: "YYYY-MM-DDTHH:mm:ss.sssZ"

    @SerializedName("gender")
    val gender: Int?, // 0 for male, 1 for female, etc. (define this)

    @SerializedName("address")
    val address: String?,

    @SerializedName("phoneNumber")
    val phoneNumber: String?
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String, // This could be email or username

    @SerializedName("password")
    val password: String, // Plain text password

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

)

data class RegisterResponseData( // The "data" object within RegisterResponse
    @SerializedName("userId")
    val userId: String
)

@Parcelize
data class ChangePasswordRequest(
    @SerializedName("currentPassword")
    val currentPassword: String,
    @SerializedName("newPassword")
    val newPassword: String
) : Parcelable

// Specific type alias for Register Response
typealias RegisterApiResponse = ApiResponse<RegisterResponseData>

// --- Categories ---
@Parcelize
data class CategoryData(
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("iconImageUrl") val iconImageUrl: String?
) : Parcelable
typealias CategoriesApiResponse = ApiResponse<List<CategoryModel>>

data class ProductFromCategory(
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("mainImageUrl") val mainImageUrl: String?, // Make nullable if it can be missing
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("price") val price: Double?, // Use Double for price
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("soldCount") val soldCount: Int?,
    @SerializedName("ratedCount") val ratedCount: Int?
)

// This matches the overall API response
data class ProductsByCategoryResponse(
    @SerializedName("partnerCode") val partnerCode: String?,
    @SerializedName("retCode") val retCode: Int?,
    @SerializedName("data") val data: List<ProductFromCategory>?, // List of products
    @SerializedName("statusCode") val statusCode: Int?,
    @SerializedName("systemMessage") val systemMessage: String?
)


// --- Products ---
@Parcelize
data class ProductSummaryData( // For product listings
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("price") val price: Double,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("soldCount") val soldCount: Int?,
    @SerializedName("ratedCount") val ratedCount: Int?
) : Parcelable
typealias ProductsByCategoryApiResponse = ApiResponse<List<ProductSummaryData>>

@Parcelize
data class ProductDetailData(
    @SerializedName("productId") val productId: String,
    @SerializedName("name") val name: String,
    @SerializedName("shortDescription") val shortDescription: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("galleryImageUrls") val galleryImageUrls: List<String>?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("price") val price: Double,
    @SerializedName("soldCount") val soldCount: Int?,
    @SerializedName("salePrice") val salePrice: Double?,
    @SerializedName("saleStart") val saleStart: String?, // Consider Date
    @SerializedName("saleEnd") val saleEnd: String?, // Consider Date
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("brandId") val brandId: String?,
    @SerializedName("brandName") val brandName: String?,
    @SerializedName("ratedCount") val ratedCount: Int?,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("isOnSale") val isOnSale: Boolean?,
    @SerializedName("isFeatured") val isFeatured: Boolean?
) : Parcelable
typealias ProductDetailsApiResponse = ApiResponse<ProductDetailData>

// --- Brands ---
@Parcelize
data class BrandData(
    @SerializedName("brandId") val brandId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("iconImageUrl") val iconImageUrl: String?
) : Parcelable
typealias BrandsApiResponse = ApiResponse<List<BrandData>>
typealias BrandDetailsApiResponse = ApiResponse<BrandData> // Single brand detail

// --- Carts ---
@Parcelize
data class CartItemData(
    @SerializedName("cartItemId") val cartItemId: String,
    @SerializedName("cartId") val cartId: String?, // Assuming cartId might be part of item
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("priceAtOrderTime") val priceAtOrderTime: Double, // API doc says priceAtOrderTime, but for cart it's likely current price
    @SerializedName("discount") val discount: Double?,
    @SerializedName("totalPrice") val totalPrice: Double?
) : Parcelable
typealias CartApiResponse = ApiResponse<List<CartItemData>>

data class AddToCartRequest(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
)

data class RemoveProductFromCartRequest( // For PUT /api/Carts/remove
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int // API doc shows quantity for removal
)

data class UpdateCartItemQuantityRequest( // For PUT /api/Carts (update quantity)
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
)

data class CartItemUpdateRequest(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity")val quantity: Int
)

// Generic response for operations that return { "data": true, ... }
typealias GenericSuccessApiResponse = ApiResponse<Boolean>


// --- Orders ---
@Parcelize
data class OrderItemDetailData( // Item within an order
    @SerializedName("orderItemId") val orderItemId: String,
    @SerializedName("orderId") val orderId: String?,
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("mainImageUrl") val mainImageUrl: String?,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("priceAtOrderTime") val priceAtOrderTime: Double?,
    @SerializedName("discount") val discount: Double?,
    @SerializedName("totalPrice") val totalPrice: Double?
) : Parcelable

data class PlaceOrderRequest(
    // customerId might be inferred by the server from the auth token
    // @SerializedName("customerId") val customerId: String?,
    @SerializedName("shippingAddressId") val shippingAddressId: String?, // If using saved addresses
    @SerializedName("shippingAddress") val shippingAddress: String?, // Or free-form address
    @SerializedName("billingAddressId") val billingAddressId: String?,
    @SerializedName("billingAddress") val billingAddress: String?,
    @SerializedName("shippingMethod") val shippingMethod: Int, // Corresponds to EShippingMethod enum
    @SerializedName("paymentMethod") val paymentMethod: Int,   // Corresponds to EPaymentMethod enum
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<OrderItemRequestData> // Simplified item data for request
)

// Simplified item structure for the PlaceOrderRequest
data class OrderItemRequestData(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
    // Server will fetch price, name etc., at the time of order creation
)

// For POST /api/Orders - Response Data
data class PlaceOrderResponseData(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderCode") val orderCode: String?,
    @SerializedName("status") val status: Int?, // EOrderStatus
    @SerializedName("totalAmount") val totalAmount: Double?,
    @SerializedName("createdAt") val createdAt: String? // "YYYY-MM-DDTHH:mm:ss.sssZ"
    // ... any other relevant fields returned immediately after placing an order
)
typealias PlaceOrderApiResponse = ApiResponse<PlaceOrderResponseData>

// For GET /api/Orders - Response Data (List of orders)
@Parcelize // If passed around
data class OrderSummaryData(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderCode") val orderCode: String?, // User-friendly order identifier
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("customerName") val customerName: String?, // Denormalized for convenience
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("finalAmount") val finalAmount: Double, // After discounts/shipping
    @SerializedName("shippingFee") val shippingFee: Double?,
    @SerializedName("status") val status: Int, // Corresponds to EOrderStatus enum
    @SerializedName("paymentStatus") val paymentStatus: Int?, // Corresponds to EPaymentStatus enum
    @SerializedName("shippingStatus") val shippingStatus: Int?, // Corresponds to EShippingStatus enum
    @SerializedName("createdAt") val createdAt: String, // "YYYY-MM-DDTHH:mm:ss.sssZ"
    @SerializedName("updatedAt") val updatedAt: String? // "YYYY-MM-DDTHH:mm:ss.sssZ"
) : Parcelable
typealias OrdersListApiResponse = ApiResponse<List<OrderSummaryData>>

data class OrderCreateModel(
    @SerializedName("items") val items: List<OrderItemRequestData>,
    @SerializedName("shippingAddress") val shippingAddress: String, // "123 Main St"
    @SerializedName("billingAddress") val billingAddress: String? = null, // Optional, could default to shipping
    @SerializedName("shippingMethodId") val shippingMethodId: String? = null, // Or Int
    @SerializedName("notes") val notes: String? = null)

// For GET /api/Orders/{id} - Response Data (Detailed single order)
@Parcelize // If passed around
data class OrderDetailData(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderCode") val orderCode: String?,
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("customerEmail") val customerEmail: String?,
    @SerializedName("customerPhoneNumber") val customerPhoneNumber: String?,
    @SerializedName("shippingAddress") val shippingAddress: String?, // Could be an object if structured
    @SerializedName("billingAddress") val billingAddress: String?,   // Could be an object
    @SerializedName("shippingMethod") val shippingMethod: Int?, // EShippingMethod
    @SerializedName("shippingFee") val shippingFee: Double?,
    @SerializedName("paymentMethod") val paymentMethod: Int?,   // EPaymentMethod
    @SerializedName("paymentStatus") val paymentStatus: Int?,   // EPaymentStatus
    @SerializedName("transactionId") val transactionId: String?, // From payment gateway
    @SerializedName("subTotal") val subTotal: Double?, // Sum of item prices before discounts/shipping
    @SerializedName("discountAmount") val discountAmount: Double?,
    @SerializedName("totalAmount") val totalAmount: Double?, // subTotal - discountAmount
    @SerializedName("finalAmount") val finalAmount: Double, // totalAmount + shippingFee
    @SerializedName("status") val status: Int, // EOrderStatus
    @SerializedName("shippingStatus") val shippingStatus: Int?, // EShippingStatus
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String, // "YYYY-MM-DDTHH:mm:ss.sssZ"
    @SerializedName("updatedAt") val updatedAt: String?, // "YYYY-MM-DDTHH:mm:ss.sssZ"
    @SerializedName("items") val items: List<OrderItemDetailData> // Re-using the detailed item data class
) : Parcelable
typealias OrderDetailsApiResponse = ApiResponse<OrderDetailData>

// For POST /api/Orders/{id}/cancel - Request Body
data class CancelOrderRequest(
    @SerializedName("reason") val reason: String,
)

@Parcelize
data class ApiCommentModel( // Renamed to avoid conflict if you had a Firebase one
    @SerializedName("commentId") val commentId: String,
    @SerializedName("productId") val productId: String?, // If API provides it
    @SerializedName("userId") val userId: String?,
    @SerializedName("userName") val userName: String?, // Or user's display name
    @SerializedName("userProfileImageUrl") val userProfileImageUrl: String?,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: String?, // e.g., "2023-10-27T10:00:00Z"
    @SerializedName("stars") val stars: Float? // Optional: if comments include ratings
    // Add any other fields your API provides for a comment
) : Parcelable

data class PostCommentRequest(
    @SerializedName("productId") val productId: String,
    @SerializedName("content") val content: String,
    @SerializedName("stars") val stars: Float? // Optional
)

// Assuming a generic success response for posting a comment
// typealias PostCommentApiResponse = ApiResponse<Boolean> // Or it might return the created comment
typealias CommentsListApiResponse = ApiResponse<List<ApiCommentModel>>