package com.example.banhangs.ViewModel // Or your ViewModel package

import androidx.lifecycle.*
import com.example.banhangs.Repository.CartRepository
import com.example.banhangs.Repository.ProductRepository
import com.example.banhangs.Model.ApiCommentModel
import com.example.banhangs.Model.ProductDetailsModel // Your existing model for product details
import kotlinx.coroutines.launch

class DetailViewModel(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val productId: String // Passed from Activity
) : ViewModel() {

    private val _productDetails = MutableLiveData<ProductDetailsModel?>()
    val productDetails: LiveData<ProductDetailsModel?> = _productDetails

    private val _comments = MutableLiveData<List<ApiCommentModel>>()
    val comments: LiveData<List<ApiCommentModel>> = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // To observe cart operation results
    private val _addToCartSuccess = MutableLiveData<Boolean>()
    val addToCartSuccess: LiveData<Boolean> = _addToCartSuccess


    init {
        // Fetch product details if your 'item' in DetailActivity is just a summary
        // If 'item' is already the full ProductDetailsModel, you can set _productDetails directly
        // For this example, let's assume 'productId' is the key and we might need to fetch full details
        // loadProductDetails(productId) // You'd implement this if needed
        loadComments()
    }

    // Call this if you pass the full ProductDetailsModel to the activity
    fun setInitialProductDetails(product: ProductDetailsModel) {
        _productDetails.value = product
        // If productId is part of ProductDetailsModel, you can extract it here
        // and then call loadComments() if productId wasn't available at init.
    }


    fun loadComments() {
        if (productId.isEmpty()) {
            _error.value = "Product ID is missing, cannot load comments."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = productRepository.getProductComments(productId)
            result.fold(
                onSuccess = { commentList ->
                    _comments.value = commentList
                    if (commentList.isEmpty()) {
                        _toastMessage.value = "No comments yet for this product."
                    }
                },
                onFailure = { e ->
                    _error.value = "Failed to load comments: ${e.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun postComment(commentText: String, rating: Float? = null) {
        if (productId.isEmpty()) {
            _error.value = "Product ID is missing, cannot post comment."
            return
        }
        if (commentText.isBlank()) {
            _toastMessage.value = "Comment cannot be empty."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = productRepository.postProductComment(productId, commentText, rating)
            result.fold(
                onSuccess = { newComment ->
                    _toastMessage.value = "Comment posted successfully!"
                    // Add to current list or reload comments
                    val currentComments = _comments.value?.toMutableList() ?: mutableListOf()
                    currentComments.add(0, newComment) // Add to top
                    _comments.value = currentComments
                },
                onFailure = { e ->
                    _error.value = "Failed to post comment: ${e.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun addToCart(product: ProductDetailsModel, quantity: Int) {
        if (quantity <= 0) {
            _toastMessage.value = "Quantity must be greater than zero."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val result = cartRepository.addToCart(product, quantity)
            result.fold(
                onSuccess = {
                    _toastMessage.value = "${product.name} added to cart!"
                    _addToCartSuccess.value = true // Signal success
                },
                onFailure = { e ->
                    _error.value = "Failed to add to cart: ${e.message}"
                    _addToCartSuccess.value = false // Signal failure
                }
            )
            _isLoading.value = false
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    fun onErrorShown() {
        _error.value = null
    }
}

// You'll need a ViewModelFactory for DetailViewModel if it has constructor dependencies
// (like productId, CartRepository, ProductRepository)
// Example using ViewModelProvider.Factory:
class DetailViewModelFactory(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val productId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(cartRepository, productRepository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}