package com.example.banhangs.Network

// In a new file, e.g., network/ApiService.kt
import com.example.banhangs.Model.AddToCartRequest
import com.example.banhangs.Model.ApiCommentModel
import com.example.banhangs.Model.CancelOrderRequest
import com.example.banhangs.Model.CartItemData
import com.example.banhangs.Model.CartItemUpdateRequest
import com.example.banhangs.Model.CategoriesApiResponse
import com.example.banhangs.Model.ChangePasswordRequest
import com.example.banhangs.Model.CommentModel
import com.example.banhangs.Model.CommentsListApiResponse
import com.example.banhangs.Model.GenericSuccessApiResponse
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.LoginApiResponse
import com.example.banhangs.Model.LoginRequest
import com.example.banhangs.Model.OrderCreateModel
import com.example.banhangs.Model.OrderData
import com.example.banhangs.Model.OrderDetailsApiResponse
import com.example.banhangs.Model.OrdersApiResponse
import com.example.banhangs.Model.PlaceOrderRequest
import com.example.banhangs.Model.PlaceOrderResponseData
import com.example.banhangs.Model.PostCommentRequest
import com.example.banhangs.Model.ProductDetailsApiResponse
import com.example.banhangs.Model.ProductsByCategoryApiResponse
import com.example.banhangs.Model.ProductsByCategoryResponse
import com.example.banhangs.Model.RegisterApiResponse
import com.example.banhangs.Model.RegisterRequest
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.Model.UserData
import com.example.banhangs.Model.UserInformationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/Authentication/login") // Ensure this path is correct relative to your BASE_URL
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginApiResponse>

    @POST("api/Authentication/register") // Ensure this path is correct
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterApiResponse>

    @GET("api/products/{id}") // Or your actual path, e.g., "api/Product/GetProductDetails"
    suspend fun getProductDetails(@Path("id") productId: String): Response<ApiResponse<ProductDetailsModel>>
    // Where ProductDetailsApiResponse is:
    // typealias ProductDetailsApiResponse = ApiResponse<ProductDetailData>
    // And ProductDetailData is the DTO class from your API.

    @GET("api/products/search") // <<<< YOUR ACTUAL SEARCH ENDPOINT PATH
    suspend fun searchProductsByName(
        @Query("search_term") searchTerm: String // <<<< YOUR ACTUAL QUERY PARAMETER NAME FOR THE SEARCH TERM
    ): Response<List<ProductDetailsApiResponse>> // Assuming the API returns a list of items

    @GET("api/Products/recommended/{userId}") // << YOUR ACTUAL ENDPOINT FOR RECOMMENDED ITEMS
    suspend fun getRecommendedItems(@Path("userId") userId: String): Response<List<ProductDetailsModel>>

    @GET("api/categories") // << YOUR ACTUAL ENDPOINT FOR CATEGORIES
    suspend fun getCategories(): Response<CategoriesApiResponse>

    @GET("api/Products/category/{categoryId}") // Corrected endpoint
    suspend fun getProductsByCategoryId(
        @Path("categoryId") categoryId: String
    ): Response<ProductsByCategoryResponse>

    @GET("api/banners")    // << YOUR ACTUAL ENDPOINT FOR BANNERS
    suspend fun getBanners(): Response<List<SliderModel>>

    // --- Carts ---

    @GET("api/Carts/{userId}") // Example endpoint
    suspend fun getCart(@Path("userId") userId: String): Response<ApiResponse<List<CartItemData>>>

    @POST("api/Carts/{userId}") // As per your specification
    suspend fun addItemToCart(
        @Path("userId") userId: String,
        @Body itemDetails: AddToCartRequest
    ): Response<GenericSuccessApiResponse>

    // Example: update quantity, might be POST or PUT
    @PUT("api/Carts/{userId}") // Or use @Body if sending more data
    suspend fun updateCartItemQuantity(
        @Path("userId") userId: String,
        @Body itemUpdateRequest: CartItemUpdateRequest
    ): Response<GenericSuccessApiResponse>

    @PUT("api/Carts/clear/{userId}") // Example endpoint
    suspend fun clearCart(@Path("userId") userId: String): Response<GenericSuccessApiResponse>

    @PUT("api/Carts/remove/{userId}") // Example endpoint
    suspend fun removeCartItem(@Path("userId") userId: String, @Body itemUpdateRequest: CartItemUpdateRequest): Response<GenericSuccessApiResponse>


    // If placeOrder and verifyAndPlaceOrder logic moves to CartRepository:
    @POST("api/orders/create")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body orderRequest: PlaceOrderRequest // Define OrderRequest model
    ): Response<PlaceOrderResponseData> // Define OrderConfirmation model


    /**
     * Fetch all comments for a specific product.
     * Endpoint: GET api/Comments/product/{id}
     * Authorization: Optional
     */
    @GET("api/Comments/product/{id}")
    suspend fun getProductComments(
        @Path("id") productId: String,
    ): Response<ApiResponse<List<ApiCommentModel>>> // Assuming ApiCommentResponseModel is your detailed comment model for this response

    /**
     * Add a new comment to a product.
     * Endpoint: POST api/Comments
     * Authorization: Required
     */
    @POST("api/Comments")
    suspend fun addComment(
        @Header("Authorization") token: String, // Token is required
        @Body commentCreateModel: PostCommentRequest
    ): Response<ApiResponse<ApiCommentModel>> // data field is the comment ID (String)

    /**
     * Delete a comment.
     * Endpoint: DELETE api/Comments/{id}
     * Authorization: Required
     */
    @DELETE("api/Comments/{id}")
    suspend fun deleteComment(
        @Path("id") commentId: String,
        @Header("Authorization") token: String // Token is required
    ): Response<GenericSuccessApiResponse> // data field is Boolean

    @GET("api/Users")
    suspend fun getUserDetails(
        @Header("Authorization") token: String,
    ): Response<ApiResponse<UserData>>

    @PUT("api/Users/update-information/{id}")
    suspend fun updateUserDetails(
        @Header("Authorization") token: String, // Add if endpoint requires auth, though description says not
        @Path("id") userId: String,
        @Body userUpdateRequest: UserInformationRequest
    ): Response<GenericSuccessApiResponse>

    @PUT("api/Users/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String, // JWT token is required
        @Body changePasswordRequest: ChangePasswordRequest
    ): Response<GenericSuccessApiResponse>

    @POST("api/auth/logout") // <<<< ADJUST TO YOUR ACTUAL LOGOUT ENDPOINT AND HTTP METHOD
    suspend fun logoutUserApi(
        @Header("Authorization") token: String
    ): Response<GenericSuccessApiResponse>

    @POST("api/orders/create")
    suspend fun createGenericOrder(
        @Header("Authorization") token: String,
        @Body orderRequest: PlaceOrderRequest // Your existing PlaceOrderRequest
    ): Response<PlaceOrderResponseData>

    @GET("api/Orders/user/{userId}")
    suspend fun getOrdersByUserId(
        @Path("userId") userId: String,
        @Header("Authorization") token: String? // Assuming optional for now
    ): Response<ApiResponse<List<OrderData>>>

    /**
     * Cancel a specific order for a user.
     * Endpoint: POST api/Orders/{id}/cancel/{userId}
     * Authorization: Required
     */
    @POST("api/Orders/{id}/cancel/{userId}")
    suspend fun cancelOrder(
        @Path("id") orderId: String,
        @Path("userId") userId: String, // Server should ideally validate this userId against the token
        @Header("Authorization") token: String, // Required
        @Body cancelOrderModel: CancelOrderRequest
    ): Response<ApiResponse<Boolean>>

    /**
     * Create a Cash-on-Delivery (COD) order.
     * Endpoint: POST api/Orders/cod-order/{userId}
     * Authorization: Required
     */
    @POST("api/Orders/cod-order/{userId}")
    suspend fun createCodOrder(
        @Path("userId") userId: String, // Server should ideally validate this userId against the token
        @Header("Authorization") token: String, // Required
        @Body orderCreateModel: OrderCreateModel // Assuming same model as COD
    ): Response<ApiResponse<String>> // data field is the created order ID (String)

    /**
     * Create a pre-paid online order.
     * Endpoint: POST api/Orders/pre-pay-order/{userId}
     * Authorization: Required
     */
    @POST("api/Orders/pre-pay-order/{userId}")
    suspend fun createPrePayOrder(
        @Path("userId") userId: String, // Server should ideally validate this userId against the token
        @Header("Authorization") token: String, // Required
        @Body orderCreateModel: OrderCreateModel // Assuming same model as COD
    ): Response<ApiResponse<String>>

}