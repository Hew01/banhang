package com.example.banhangs.Helper // Or your chosen repository package

import com.example.banhangs.Model.AddToCartRequest
import com.example.banhangs.Model.CartItemData
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.RemoveProductFromCartRequest
import com.example.banhangs.Model.UpdateCartItemQuantityRequest
import com.example.banhangs.Network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ManagmentCart(private val apiService: ApiService) {

    // Function to convert ProductDetailsModel to AddToCartRequest
    // You might want a more generic Item model if adding different types of items
    private fun ProductDetailsModel.toAddToCartRequest(quantity: Int): AddToCartRequest {
        return AddToCartRequest(
            productId = this.productId,
            quantity = quantity
        )
    }

    suspend fun getCartItems(): Result<List<CartItemData>> {
        return try {
            val response = apiService.getCartItems()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to get cart items: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToCart(product: ProductDetailsModel, quantity: Int): Result<Boolean> {
        return try {
            val request = product.toAddToCartRequest(quantity) // Use the conversion
            val response = apiService.addToCart(request)
            if (response.isSuccessful && response.body()?.data == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to add to cart: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCartItemQuantity(productId: String, newQuantity: Int): Result<Boolean> {
        return try {
            val request = UpdateCartItemQuantityRequest(productId, newQuantity)
            val response = apiService.updateCartItemQuantity(request)
            if (response.isSuccessful && response.body()?.data == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update cart quantity: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeProductFromCart(productId: String, quantityToRemove: Int): Result<Boolean> {
        // This 'quantityToRemove' is based on your API spec for /api/Carts/remove
        // If the API means "remove this product entirely if quantity is 0 or not specified",
        // you might need to adjust the request or have a separate method.
        // Based on your spec, it seems like it removes a specific quantity.
        return try {
            val request = RemoveProductFromCartRequest(productId, quantityToRemove)
            val response = apiService.removeProductFromCart(request)
            if (response.isSuccessful && response.body()?.data == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to remove product from cart: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(): Result<Boolean> {
        return try {
            val response = apiService.clearCart()
            if (response.isSuccessful && response.body()?.data == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to clear cart: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

