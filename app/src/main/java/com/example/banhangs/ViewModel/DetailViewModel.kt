package com.example.banhangs.ViewModel // Or your ViewModel package

import androidx.annotation.OptIn
import androidx.lifecycle.*
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.banhangs.Repository.CartRepository
import com.example.banhangs.Repository.ProductRepository
import com.example.banhangs.Model.ApiCommentModel
import com.example.banhangs.Model.ProductDetailsModel // Your existing model for product details
import kotlinx.coroutines.launch
import kotlin.text.fold
import kotlin.text.isBlank
import kotlin.text.isNotBlank

class DetailViewModel @OptIn(UnstableApi::class) constructor
    (
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val viewModelProductId: String // Passed from Activity
) : ViewModel() {

    private val _productDetails = MutableLiveData<ProductDetailsModel?>()
    val productDetails: LiveData<ProductDetailsModel?> = _productDetails

    private val _comments = MutableLiveData<List<ApiCommentModel>>()
    val comments: LiveData<List<ApiCommentModel>> = _comments

    private val _isLoadingProduct = MutableLiveData<Boolean>() // Separate loading for product
    val isLoadingProduct: LiveData<Boolean> = _isLoadingProduct

    private val _isLoadingComments = MutableLiveData<Boolean>() // Separate loading for comments
    val isLoadingComments: LiveData<Boolean> = _isLoadingComments

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // To observe cart operation results
    private val _addToCartSuccess = MutableLiveData<Boolean>()
    val addToCartSuccess: LiveData<Boolean> = _addToCartSuccess


    init {
        Log.d("DetailViewModel", "Initializing with productId: $viewModelProductId")
        if (viewModelProductId.isNotBlank() && viewModelProductId != "INVALID_ID_FALLBACK") {
            loadFullProductDetails() // Fetch full details first
            loadComments()           // Then load comments (or after product details success)
        } else {
            _error.value = "Product ID is invalid for ViewModel initialization."
        }
    }

    // Call this if you pass the full ProductDetailsModel to the activity
    fun setInitialProductData(product: ProductDetailsModel) {
        if (_productDetails.value == null) { // Only set if full details haven't loaded yet or to provide initial UI
            _productDetails.value = product
        }
    }

    @OptIn(UnstableApi::class)
    private fun loadFullProductDetails() {
        if (viewModelProductId.isBlank() || viewModelProductId == "INVALID_ID_FALLBACK") {
            _error.value = "Cannot load product details: Product ID is missing or invalid."
            return
        }
        _isLoadingProduct.value = true
        viewModelScope.launch {
            Log.d("DetailViewModel", "Fetching full product details for ID: $viewModelProductId")
            // Assuming productRepository has a method like getProductDetailsById
            val result = productRepository.getProductDetails(viewModelProductId)
            result.fold(
                onSuccess = { fullProduct ->
                    _productDetails.value = fullProduct
                    Log.d("DetailViewModel", "Successfully fetched full product details: ${fullProduct.name}")
                    // Optionally, trigger comment loading here if you want it strictly after product details
                    // loadComments()
                },
                onFailure = { e ->
                    _error.value = "Failed to load product details: ${e.message}"
                    Log.e("DetailViewModel", "Error fetching product details: ${e.message}", e)
                }
            )
            _isLoadingProduct.value = false
        }
    }

    @OptIn(UnstableApi::class)
    fun loadComments() {
        if (viewModelProductId.isBlank() || viewModelProductId == "INVALID_ID_FALLBACK") {
            _error.value = "Product ID is missing, cannot load comments."
            return
        }
        _isLoadingComments.value = true
        viewModelScope.launch {
            Log.d("DetailViewModel", "Fetching comments for ID: $viewModelProductId")
            val result = productRepository.getProductComments(viewModelProductId) // Ensure this method exists
            result.fold(
                onSuccess = { commentList ->
                    _comments.value = commentList
                    if (commentList.isEmpty()) {
                        _toastMessage.value = "No comments yet for this product."
                    }
                },
                onFailure = { e ->
                    // The error "Failed to load comments: Failed to get comments: Network Error - Code: 404"
                    // will be set here. This confirms the 404 is from the getProductComments call.
                    _error.value = "Failed to load comments: ${e.message}"
                    Log.e("DetailViewModel", "Error fetching comments: ${e.message}", e)
                }
            )
            _isLoadingComments.value = false
        }
    }


    fun postComment(commentText: String, rating: Float? = null) {
        if (viewModelProductId.isEmpty() || viewModelProductId == "INVALID_ID_FALLBACK") {
            _error.value = "Product ID is missing, cannot post comment."
            return
        }
        if (commentText.isBlank()) {
            _toastMessage.value = "Comment cannot be empty."
            return
        }
        _isLoadingComments.value = true // Or a general isLoading
        viewModelScope.launch {
            val result = productRepository.postProductComment(viewModelProductId, commentText, rating)
            result.fold(
                onSuccess = { newComment ->
                    _toastMessage.value = "Comment posted successfully!"
                    val currentComments = _comments.value?.toMutableList() ?: mutableListOf()
                    currentComments.add(0, newComment)
                    _comments.value = currentComments
                },
                onFailure = { e ->
                    _error.value = "Failed to post comment: ${e.message}"
                }
            )
            _isLoadingComments.value = false
        }
    }

    fun addToCart(product: ProductDetailsModel, quantity: Int) {
        if (quantity <= 0) {
            _toastMessage.value = "Quantity must be greater than zero."
            return
        }
        // Use the product details from the ViewModel's state if available and up-to-date
        val currentProduct = _productDetails.value ?: product // Fallback to passed product if VM's is null

        _isLoadingProduct.value = true // Or a general isLoading
        viewModelScope.launch {
            // Pass currentProduct.productId or viewModelProductId
            val result = cartRepository.addToCart(currentProduct, quantity)
            result.fold(
                onSuccess = {
                    _toastMessage.value = "${currentProduct.name} added to cart!"
                    _addToCartSuccess.value = true
                },
                onFailure = { e ->
                    _error.value = "Failed to add to cart: ${e.message}"
                    _addToCartSuccess.value = false
                }
            )
            _isLoadingProduct.value = false
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