package com.example.banhangs.Repository

// Correctly import UserPreferencesRepository if it's in the same package or add full path
// Assuming UserPreferencesRepository is in com.example.banhangs.Repository
// Use the CartItemData from your Model file
import com.example.banhangs.Model.AddToCartRequest
import com.example.banhangs.Model.CartItemData
// Assuming your API returns a structure like ApiResponse<List<CartItemData>> for getCart
import com.example.banhangs.Model.CartApiResponse // Your typealias for ApiResponse<List<CartItemData>>
// Generic API response for updates/removals
import com.example.banhangs.Model.GenericSuccessApiResponse // Your typealias for ApiResponse<Boolean> or similar for success
import com.example.banhangs.Network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class CartRepository(
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private suspend fun getAuthToken(): String? {
        // Assuming getUserToken() in UserPreferencesRepository returns a Flow<String?>
        // If it's a suspend fun returning String?, then just call it directly.
        return userPreferencesRepository.getUserToken()
    }

    suspend fun getCartItems(): Result<List<CartItemData>> {
        val token = getAuthToken()
        if (token.isNullOrEmpty()) { // Check for null or empty
            return Result.failure(Exception("User not authenticated to fetch cart."))
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCart("Bearer $token")
                if (response.isSuccessful) {
                    val cartApiResponse = response.body()
                    // Check against your CartApiResponse structure
                    if (cartApiResponse != null && cartApiResponse.retCode == 0 && cartApiResponse.data != null) {
                        Result.success(cartApiResponse.data) // data is List<CartItemData>
                    } else {
                        Result.failure(Exception(cartApiResponse?.systemMessage ?: "Failed to fetch cart items"))
                    }
                } else {
                    Result.failure(Exception("Error fetching cart: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error fetching cart: ${e.message}", e))
            } as Result<List<CartItemData>>
        }
    }

    suspend fun addItemToCart(productId: String, quantity: Int): Result<Unit> {
        val token = getAuthToken()
        if (token.isNullOrEmpty()) {
            // Corresponds to "424 FailedDependency if user not authenticated"
            return Result.failure(Exception("User not authenticated. (Status Code: 424 expected)"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val itemDetails = AddToCartRequest(productId = productId, quantity = quantity)
                val response = apiService.addItemToCart("Bearer $token", itemDetails)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    // Success: "200 OK"
                    // Failure: "200 OK with NoExitData if operation fails" (means retCode != 0 or data is false/null)
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(apiResponse?.systemMessage ?: "Failed to add item to cart. (NoExitData)"))
                    }
                } else {
                    // Handle other HTTP error codes if necessary, though your spec focuses on 200 OK
                    Result.failure(Exception("Error adding item to cart: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error adding item to cart: ${e.message}", e))
            }
        }
    }

    suspend fun updateItemQuantity(productId: String, newQuantity: Int): Result<Unit> {
        val token = getAuthToken()
        if (token.isNullOrEmpty()) return Result.failure(Exception("User not authenticated."))

        return withContext(Dispatchers.IO) {
            try {
                // Assuming your apiService.updateCartItemQuantity expects a token, productId, and newQuantity
                // And returns a Response<GenericSuccessApiResponse> or similar
                val response = apiService.updateCartItemQuantity("Bearer $token", productId, newQuantity)

                if (response.isSuccessful && response.body()?.retCode == 0) {
                    // If your GenericSuccessApiResponse's data field is Boolean, you might check response.body()?.data == true
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.systemMessage ?: "Failed to update quantity"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error updating quantity: ${e.message}", e))
            }
        }
    }

    suspend fun removeItemFromCart(productId: String): Result<Unit> {
        val token = getAuthToken()
        if (token.isNullOrEmpty()) return Result.failure(Exception("User not authenticated."))

        return withContext(Dispatchers.IO) {
            try {
                // Assuming your apiService.removeCartItem expects token and productId
                // And returns a Response<GenericSuccessApiResponse> or similar
                val response = apiService.removeCartItem("Bearer $token", productId)
                if (response.isSuccessful && response.body()?.retCode == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.systemMessage ?: "Failed to remove item"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error removing item: ${e.message}", e))
            }
        }
    }

    suspend fun clearCart(): Result<Unit> {
        val token = getAuthToken()
        if (token.isNullOrEmpty()) return Result.failure(Exception("User not authenticated."))
        return withContext(Dispatchers.IO) {
            try {
                // Assuming your apiService.clearCart expects only the token
                // And returns a Response<GenericSuccessApiResponse> or similar
                val response = apiService.clearCart("Bearer $token")
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

    // The placeOrder and verifyAndPlaceOrder logic remains outside this repository for now,
    // as it's currently in your CartActivity.
    // If you decide to move them here, you would define OrderRequest and OrderConfirmation/Response models
    // and use the getAuthToken() method similarly.
}