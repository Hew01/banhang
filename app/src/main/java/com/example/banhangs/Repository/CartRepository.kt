package com.example.banhangs.Repository // Or your chosen repository package

import com.example.banhangs.Model.* // Import your models including ProductDetailsModel
import com.example.banhangs.Network.ApiService // Your Retrofit ApiService interface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Result class for handling success/failure, common in Kotlin
// You might already have a similar sealed class or can use a simple one like this.
// sealed class Result<out T> {
//     data class Success<out T>(val data: T) : Result<T>()
//     data class Error(val exception: Exception) : Result<Nothing>()
// }
// For simplicity, I'll use Kotlin's built-in Result type (kotlin.Result)

class CartRepository(private val apiService: ApiService) {

    /**
     * Fetches all items currently in the user's cart from the API.
     */
    suspend fun getCartItems(): Result<List<CartItemData>> {
        return withContext(Dispatchers.IO) { // Perform network call on IO dispatcher
            try {
                val response = apiService.getCartItems() // Assumes getCartItems() is defined in ApiService
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        Result.success(apiResponse.data)
                    } else {
                        val errorMessage = "Failed to get cart items: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to get cart items: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to get cart items: Exception - ${e.message}", e))
            }
        }
    }

    /**
     * Adds a specified quantity of a product to the user's cart.
     * @param product The ProductDetailsModel of the item to add.
     * @param quantity The number of items to add.
     */
    suspend fun addToCart(product: ProductDetailsModel, quantity: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddToCartRequest(
                    productId = product.productId,
                    quantity = quantity
                )
                val response = apiService.addToCart(request) // Assumes addToCart() is defined in ApiService
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(true)
                    } else {
                        val errorMessage = "Failed to add to cart: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to add to cart: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to add to cart: Exception - ${e.message}", e))
            }
        }
    }

    /**
     * Updates the quantity of a specific product in the cart.
     * @param productId The ID of the product to update.
     * @param newQuantity The new quantity for the product.
     */
    suspend fun updateCartItemQuantity(productId: String, newQuantity: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateCartItemQuantityRequest(productId, newQuantity)
                val response = apiService.updateCartItemQuantity(request) // Assumes updateCartItemQuantity() is defined
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(true)
                    } else {
                        val errorMessage = "Failed to update cart quantity: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to update cart quantity: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to update cart quantity: Exception - ${e.message}", e))
            }
        }
    }

    /**
     * Removes a specified quantity of a product from the cart.
     * Based on your API spec for /api/Carts/remove.
     * @param productId The ID of the product to remove.
     * @param quantityToRemove The quantity of the product to remove.
     */
    suspend fun removeProductFromCart(productId: String, quantityToRemove: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RemoveProductFromCartRequest(productId, quantityToRemove)
                val response = apiService.removeProductFromCart(request) // Assumes removeProductFromCart() is defined
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(true)
                    } else {
                        val errorMessage = "Failed to remove product from cart: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to remove product from cart: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to remove product from cart: Exception - ${e.message}", e))
            }
        }
    }

    /**
     * Clears all items from the user's cart.
     */
    suspend fun clearCart(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.clearCart() // Assumes clearCart() is defined in ApiService
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data == true) {
                        Result.success(true)
                    } else {
                        val errorMessage = "Failed to clear cart: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to clear cart: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to clear cart: Exception - ${e.message}", e))
            }
        }
    }
}