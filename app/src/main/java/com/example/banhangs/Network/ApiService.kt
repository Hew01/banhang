package com.example.banhangs.Network

// In a new file, e.g., network/ApiService.kt
import com.example.banhangs.Model.AddToCartRequest
import com.example.banhangs.Model.ApiCommentModel
import com.example.banhangs.Model.CartApiResponse
import com.example.banhangs.Model.CategoriesApiResponse
import com.example.banhangs.Model.CategoryModel
import com.example.banhangs.Model.ChangePasswordRequest
import com.example.banhangs.Model.CommentModel
import com.example.banhangs.Model.CommentsListApiResponse
import com.example.banhangs.Model.GenericSuccessApiResponse
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.LoginApiResponse
import com.example.banhangs.Model.LoginRequest
import com.example.banhangs.Model.OrdersApiResponse
import com.example.banhangs.Model.PlaceOrderRequest
import com.example.banhangs.Model.PlaceOrderResponseData
import com.example.banhangs.Model.PostCommentRequest
import com.example.banhangs.Model.ProductDetailsApiResponse
import com.example.banhangs.Model.ProductsByCategoryApiResponse
import com.example.banhangs.Model.ProductsByCategoryResponse
import com.example.banhangs.Model.RegisterApiResponse
import com.example.banhangs.Model.RegisterRequest
import com.example.banhangs.Model.RemoveProductFromCartRequest
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.Model.UpdateCartItemQuantityRequest
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

    @GET("api/products/category/{categoryId}") // Or your actual path, e.g., "api/Product/GetProductsByCategory"
    suspend fun getProductsByCategory(@Path("categoryId") categoryId: String): Response<ProductsByCategoryApiResponse>
    // Where ProductsByCategoryApiResponse is:
    // typealias ProductsByCategoryApiResponse = ApiResponse<List<ProductSummaryData>>

    @GET("api/items/recommended") // << YOUR ACTUAL ENDPOINT FOR RECOMMENDED ITEMS
    suspend fun getRecommendedItems(): Response<List<ProductDetailsModel>>

    @GET("api/categories") // << YOUR ACTUAL ENDPOINT FOR CATEGORIES
    suspend fun getCategories(): Response<CategoriesApiResponse>

    @GET("api/Products/category/{categoryId}") // Corrected endpoint
    suspend fun getProductsByCategoryId(
        @Path("categoryId") categoryId: String
    ): Response<ProductsByCategoryResponse>

    @GET("api/banners")    // << YOUR ACTUAL ENDPOINT FOR BANNERS
    suspend fun getBanners(): Response<List<SliderModel>>

    @GET("api/items/{itemId}")
    suspend fun getItemDetails(@Path("itemId") itemId: String): Response<ProductDetailsModel>

    @GET("api/items/{itemId}/comments")
    suspend fun getItemComments(@Path("itemId") itemId: String): Response<List<CommentModel>>

    @POST("api/items/{itemId}/comments")
    suspend fun postComment(
        @Path("itemId") itemId: String,
        @Body commentRequest: CommentRequest // Your CommentRequest data class
    ): Response<CommentModel> // Or whatever your API returns for a new comment

    // --- Carts ---

    @GET("api/Carts") // Example endpoint
    suspend fun getCart(@Header("Authorization") token: String): Response<GenericSuccessApiResponse>

    @POST("api/Carts") // As per your specification
    suspend fun addItemToCart(
        @Header("Authorization") token: String,
        @Body itemDetails: AddToCartRequest
    ): Response<GenericSuccessApiResponse>

    // Example: update quantity, might be POST or PUT
    @PUT("api/Carts") // Or use @Body if sending more data
    suspend fun updateCartItemQuantity(
        @Header("Authorization") token: String,
        @Path("productId") productId: String,
        @Query("quantity") newQuantity: Int // Or send as part of a request body
    ): Response<GenericSuccessApiResponse> // Generic success/failure response

    @DELETE("api/Carts/remove") // Example endpoint
    suspend fun removeCartItem(
        @Header("Authorization") token: String,
        @Path("productId") productId: String
    ): Response<GenericSuccessApiResponse>

    @DELETE("api/Carts/clear") // Example endpoint
    suspend fun clearCart(@Header("Authorization") token: String): Response<GenericSuccessApiResponse>

    // If placeOrder and verifyAndPlaceOrder logic moves to CartRepository:
    @POST("api/orders/create")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body orderRequest: PlaceOrderRequest // Define OrderRequest model
    ): Response<PlaceOrderResponseData> // Define OrderConfirmation model


    @GET("api/products/{productId}/comments") // Example endpoint
    suspend fun getProductComments(@Path("productId") productId: String): Response<CommentsListApiResponse>

    @POST("api/products/{productId}/comments") // Example endpoint
    suspend fun postProductComment(
        @Path("productId") productId: String,
        @Body commentRequest: PostCommentRequest
    ): Response<ApiResponse<ApiCommentModel>> // Assuming API returns the created comment
    // Or: Response<GenericSuccessApiResponse> if it just returns success:true

    @GET("api/Orders")
    suspend fun getOrders(@Header("Authorization") token: String): Response<OrdersApiResponse>

    @GET("api/Users/{id}") // Endpoint to get user details by ID
    suspend fun getUserDetails(
        @Header("Authorization") token: String, // Assuming this endpoint also requires auth
        @Path("id") userId: String
    ): Response<ApiResponse<UserData>> // Or Response<UserData> if API returns it directly

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
}