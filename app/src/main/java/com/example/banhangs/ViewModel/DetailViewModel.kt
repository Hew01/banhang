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

private const val TAG = "DetailViewModel_Product"

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
        Log.i(TAG, "Initializing. Received Product ID: '$viewModelProductId'")
        if (viewModelProductId.isNotBlank() && viewModelProductId != "INVALID_ID_FALLBACK") {
            Log.d(TAG, "Valid Product ID. Triggering loadFullProductDetails() and loadComments().")
            loadFullProductDetails()
            loadComments()
        } else {
            Log.e(TAG, "Initialization with INVALID Product ID ('$viewModelProductId'). Cannot load details.")
            _error.value = "Product ID is invalid for ViewModel initialization."
        }
    }

    // Call this if you pass the full ProductDetailsModel to the activity
    @OptIn(UnstableApi::class)
    fun setInitialProductData(product: ProductDetailsModel) {
        if (_productDetails.value == null) {
            Log.d(TAG, "setInitialProductData: Setting initial product data. Name: ${product.name}, Current _productDetails is null.")
            _productDetails.value = product
        } else {
            Log.d(TAG, "setInitialProductData: Attempted to set initial data, but _productDetails already has a value. Name: ${product.name}, Existing: ${_productDetails.value?.name}")
        }
    }

    @OptIn(UnstableApi::class)
    private fun loadFullProductDetails() {
        if (viewModelProductId.isBlank() || viewModelProductId == "INVALID_ID_FALLBACK") {
            Log.w(
                TAG,
                "loadFullProductDetails: Skipped. Product ID is missing or invalid ('$viewModelProductId')."
            )
            _error.value = "Cannot load product details: Product ID is missing or invalid."
            return
        }

        Log.i(
            TAG,
            "loadFullProductDetails: Starting to fetch full product details for ID: '$viewModelProductId'."
        )
        _isLoadingProduct.value = true
        viewModelScope.launch {
            Log.d(
                TAG,
                "loadFullProductDetails: Coroutine launched. Calling repository.getProductDetails for ID: '$viewModelProductId'."
            )
            val result = productRepository.getProductDetails(viewModelProductId)
            result.fold(
                onSuccess = { fullProduct ->
                    Log.i(
                        TAG,
                        "loadFullProductDetails: Successfully fetched full product details. Product Name: '${fullProduct.name}', ID: '${fullProduct.productId}'. Data: $fullProduct"
                    )
                    Log.d(
                        TAG,
                        "Full details - Desc: ${fullProduct.description?.take(30)}, Category: ${fullProduct.categoryName}, Stock: ${fullProduct.stock}"
                    )
                    _productDetails.value = fullProduct
                },
                onFailure = { e ->
                    Log.e(
                        TAG,
                        "loadFullProductDetails: Failed to fetch product details for ID: '$viewModelProductId'. Error: ${e.message}",
                        e
                    )
                    _error.value = "Failed to load product details: ${e.message}"
                }
            )
            Log.d(
                TAG,
                "loadFullProductDetails: Fetch operation complete for ID: '$viewModelProductId'. Setting isLoadingProduct to false."
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