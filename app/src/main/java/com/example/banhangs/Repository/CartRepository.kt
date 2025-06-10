package com.example.banhangs.Repository

// ... other imports ...
import com.example.banhangs.Model.AddToCartRequest
import com.example.banhangs.Model.CartItemData
import com.example.banhangs.Model.CartItemUpdateRequest
import com.example.banhangs.Network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.text.isNullOrBlank

class CartRepository(
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    // Helper to get both token and userId, failing if either is missing
    private suspend fun getAuthDetails(): Pair<String, String>? = coroutineScope {
        val tokenDeferred = async { userPreferencesRepository.getUserToken() }
        val userIdDeferred = async { userPreferencesRepository.getUserId() } // Assuming this exists

        val token = tokenDeferred.await()
        val userId = userIdDeferred.await()

        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            null
        } else {
            Pair("Bearer $token", userId)
        }
    }


    suspend fun getCartItems(): Result<List<CartItemData>> {
        val authDetails = getAuthDetails()
            ?: return Result.failure(Exception("User not authenticated or userId missing to fetch cart."))
        val (bearerToken, userId) = authDetails

        return withContext(Dispatchers.IO) {
            try {
                // Assuming apiService.getCart now takes userId
                val response = apiService.getCart(userId)
                if (response.isSuccessful) {
                    val cartApiResponse = response.body()
                    if (cartApiResponse != null && cartApiResponse.retCode == 0 && cartApiResponse.data != null) {
                        Result.success(cartApiResponse.data)
                    } else {
                        Result.failure(Exception(cartApiResponse?.systemMessage ?: "Failed to fetch cart items"))
                    }
                } else {
                    Result.failure(Exception("Error fetching cart: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                // It's generally better not to cast with 'as' if you can ensure type safety through function signatures
                // The compiler should infer this type if the try-block returns Result<List<CartItemData>>
                Result.failure(Exception("Network error fetching cart: ${e.message}", e))
            }
        }
    }

    suspend fun addItemToCart(productId: String, quantity: Int): Result<Unit> {
        val authDetails = getAuthDetails()
            ?: return Result.failure(Exception("User not authenticated or userId missing to add item. (Status Code: 424 expected)"))
        val (bearerToken, userId) = authDetails

        return withContext(Dispatchers.IO) {
            try {
                val itemDetails = AddToCartRequest(productId = productId, quantity = quantity)
                // Assuming apiService.addItemToCart now takes userId and the token in header
                // If userId is part of AddToCartRequest, it's already there.
                // If it's a path param or separate header, adjust apiService call.
                // For this example, let's assume it's part of the API path or handled by interceptor with token
                val response = apiService.addItemToCart(userId, itemDetails)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(apiResponse?.systemMessage ?: "Failed to add item to cart. (NoExitData)"))
                    }
                } else {
                    Result.failure(Exception("Error adding item to cart: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error adding item to cart: ${e.message}", e))
            }
        }
    }

    suspend fun updateItemQuantity(productId: String, newQuantity: Int): Result<Unit> {
        val authDetails = getAuthDetails()
            ?: return Result.failure(Exception("User not authenticated or userId missing to update cart quantity."))
        val (bearerToken, userId) = authDetails


        val itemUpdateRequest = CartItemUpdateRequest(productId = productId, quantity = newQuantity)

        return withContext(Dispatchers.IO) {
            try {
                // Pass userId to the apiService.updateCartItemQuantity
                val response = apiService.updateCartItemQuantity(userId, itemUpdateRequest)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(apiResponse?.systemMessage ?: "Failed to update quantity: Backend error or data false"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Error updating quantity: ${response.code()} - ${response.message()}" +
                            if (!errorBody.isNullOrBlank()) ". Error Body: $errorBody" else ""
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error updating quantity: ${e.message}", e))
            }
        }
    }
    /**
     * Removes a product entirely from the cart.
     * This calls the backend's "remove" endpoint.
     * @param productId The ID of the product to remove.
     * @param quantityToRemove The quantity of the item to remove. To remove the item line,
     *                         this should be the current quantity of the item in the cart.
     *                         Your C# endpoint [HttpPut("remove/{userId}")] expects this in the body.
     */
    suspend fun removeItemFromCart(productId: String, quantityToRemove: Int): Result<Unit> {
        val authDetails = getAuthDetails()
            ?: return Result.failure(Exception("User not authenticated or userId missing to remove item."))
        val (bearerToken, userId) = authDetails

        // This model matches the CartItemUpdateModel expected by your C# RemoveProduct endpoint
        val removeItemRequest = CartItemUpdateRequest(productId = productId, quantity = quantityToRemove)

        return withContext(Dispatchers.IO) {
            try {
                // Ensure apiService.removeCartItem takes (token, userId, body)
                // and maps to [HttpPut("remove/{userId}")]
                val response = apiService.removeCartItem(userId, removeItemRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) { // Assuming data: true for success
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(apiResponse?.systemMessage ?: "Failed to remove item (API error)"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Error removing item: ${response.code()} - ${response.message()}. Body: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error removing item: ${e.message}", e))
            }
        }
    }
    suspend fun clearCart(): Result<Unit> {
        val authDetails = getAuthDetails()
            ?: return Result.failure(Exception("User not authenticated or userId missing to clear cart."))
        val (bearerToken, userId) = authDetails

        return withContext(Dispatchers.IO) {
            try {
                // Assuming apiService.clearCart now takes userId (and token in header)
                val response = apiService.clearCart(userId)
                if (response.isSuccessful && response.body()?.retCode == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.systemMessage ?: "Failed to clear cart"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error clearing cart: ${e.message}", e))
            }
        }
    }
}