package com.example.banhangs.ViewModel // Or your ViewModel package

import androidx.lifecycle.*
import com.example.banhangs.Repository.CartRepository
import com.example.banhangs.Model.CartItemData
import com.example.banhangs.Model.CartItemUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {

    private val _cartItems = MutableLiveData<List<CartItemData>>()
    val cartItems: LiveData<List<CartItemData>> = _cartItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _totalAmount = MutableLiveData<Double>()
    val totalAmount: LiveData<Double> = _totalAmount

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    init {
        loadCartItems()
    }

    fun loadCartItems() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = cartRepository.getCartItems()
            result.fold(
                onSuccess = { items ->
                    _cartItems.value = items
                    calculateTotal(items)
                    _isEmpty.value = items.isEmpty()
                    if (items.isEmpty()) {
                        _toastMessage.value = "Your cart is empty."
                    }
                },
                onFailure = { e ->
                    _error.value = "Failed to load cart: ${e.message}"
                    _cartItems.value = emptyList() // Ensure list is empty on error
                    calculateTotal(emptyList())
                    _isEmpty.value = true
                }
            )
            _isLoading.value = false
        }
    }

    fun updateItemQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            // If quantity is 0 or less, treat as removal or show error
            // For now, let's assume the API handles newQuantity=0 as removal or this is validated before calling
            removeItem(productId, 0) // Or call a specific remove if API needs it
            _toastMessage.value = "Item quantity must be positive. To remove, use the remove button."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = cartRepository.updateItemQuantity(productId, newQuantity)
            result.fold(
                onSuccess = {
                    _toastMessage.value = "Cart updated."
                    loadCartItems() // Reload to get updated totals and item states from server
                },
                onFailure = { e ->
                    _error.value = "Failed to update quantity: ${e.message}"
                }
            )
            // No need to set isLoading to false here if loadCartItems() does it
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
    fun removeItem(productId: String, currentQuantityInCart: Int) {
        _isLoading.value = true
        viewModelScope.launch { // Use viewModelScope for UI-related coroutines
            // Delegate the actual removal to the CartRepository
            // The repository handles auth, network, and forming the specific request.
            val result = cartRepository.removeItemFromCart(productId, currentQuantityInCart)

            result.fold(
                onSuccess = {
                    _toastMessage.value = "Item removed successfully."
                    loadCartItems() // Reload cart items to reflect the removal and update UI/totals
                },
                onFailure = { exception ->
                    _error.value = "Failed to remove item: ${exception.message}"
                    // Optionally, you might still call loadCartItems() to ensure UI
                    // consistency if the backend state might have partially changed or to clear optimistic updates.
                }
            )
            // _isLoading.value = false; // loadCartItems() should handle setting isLoading to false
        }
    }


    fun clearCart() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = cartRepository.clearCart()
            result.fold(
                onSuccess = {
                    _toastMessage.value = "Cart cleared."
                    _cartItems.value = emptyList()
                    calculateTotal(emptyList())
                    _isEmpty.value = true
                },
                onFailure = { e ->
                    _error.value = "Failed to clear cart: ${e.message}"
                }
            )
            _isLoading.value = false // Set here as we are not calling loadCartItems()
        }
    }


    private fun calculateTotal(items: List<CartItemData>) {
        // The 'totalPrice' in CartItemData from your API might already be (item.priceAtOrderTime * item.quantity) - item.discount
        // If so, sum those up.
        // If 'totalPrice' is just the unit price, then you need to multiply by quantity here.
        // Based on your CartItemData: "@SerializedName("totalPrice") val totalPrice: Double?"
        // This suggests totalPrice is the final price for that line item.
        _totalAmount.value = items.sumOf { it.totalPrice ?: (it.priceAtOrderTime * it.quantity) - (it.discount ?: 0.0) }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    fun onErrorShown() {
        _error.value = null
    }
}
