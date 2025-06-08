package com.example.banhangs.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.CategoryModel
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.ProductsByCategoryResponse
import com.example.banhangs.Model.SliderModel
// Import your RetrofitClient and ApiService
import com.example.banhangs.Network.RetrofitClient // Make sure this path is correct
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel() {

    private val apiService = RetrofitClient.instance


    private val _Recommended = MutableLiveData<MutableList<ProductDetailsModel>>()
    val recommended: LiveData<MutableList<ProductDetailsModel>> = _Recommended

    private val _searchResults = MutableLiveData<List<ProductDetailsModel>>(emptyList())
    val searchResults: LiveData<List<ProductDetailsModel>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _searchedItems =
        MutableLiveData<List<ProductDetailsModel>?>(emptyList()) // Initialize with emptyList
    val searchedItems: MutableLiveData<List<ProductDetailsModel>?> = _searchedItems

    private val _errorMessage = MutableLiveData<String?>() // For error messages
    val errorMessage: LiveData<String?> = _errorMessage

    private val _itemsByCategoryId = MutableLiveData<List<ProductDetailsModel>>()
    val itemsByCategoryId: LiveData<List<ProductDetailsModel>> = _itemsByCategoryId

    private val _recommendedItems =
        MutableLiveData<List<ProductDetailsModel>?>() // If you don't have this already
    val recommendedItems: MutableLiveData<List<ProductDetailsModel>?> =
        _recommendedItems // If you don't have this already

    private val _categories =
        MutableLiveData<List<CategoryModel>>() // Assuming you want a List, not MutableList here
    val categories: LiveData<List<CategoryModel>> = _categories

    // LiveData for Banners
    private val _banners = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banners

    private val TAG = "MainViewModel"

    fun loadItemsByCategoryId(categoryId: String) {
        if (categoryId.isBlank()) {
            _errorMessage.value = "Category ID cannot be blank."
            _itemsByCategoryId.value = emptyList()
            Log.w("MainViewModel", "loadItemsByCategoryId called with blank categoryId.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        // _itemsByCategoryId.value = emptyList() // Optionally clear immediately

        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Loading items for category ID: $categoryId from API")
                val response = apiService.getProductsByCategoryId(categoryId = categoryId)

                if (response.isSuccessful) {
                    val productsResponse: ProductsByCategoryResponse? = response.body()
                    if (productsResponse?.data != null && (productsResponse.retCode == 0 || productsResponse.statusCode == 200 || productsResponse.statusCode == 0)) {
                        val mappedProducts = productsResponse.data.map { productFromApi ->
                            // Map ProductFromCategory to your ProductDetailsModel
                            ProductDetailsModel(
                                productId = productFromApi.productId,
                                name = productFromApi.name,
                                mainImageUrl = productFromApi.mainImageUrl,
                                price = productFromApi.price ?: 0.0, // Default if API sends null
                                stock = productFromApi.stock,
                                categoryName = productFromApi.categoryName,
                                averageRating = productFromApi.averageRating,
                                soldCount = productFromApi.soldCount,
                                ratedCount = productFromApi.ratedCount,

                                // Fields in ProductDetailsModel but NOT in ProductFromCategory
                                // Provide defaults or nulls as appropriate for your UI.
                                shortDescription = null, // Or "View details..."
                                description = null,
                                galleryImageUrls = emptyList(), // Default to empty list
                                salePrice = null,
                                saleStart = null,
                                saleEnd = null,
                                categoryId = categoryId, // Use the categoryId passed to the function
                                brandId = null,
                                brandName = null,
                                isOnSale = false, // Assume false unless detail API says otherwise
                                isFeatured = false
                            )
                        }
                        _itemsByCategoryId.value = mappedProducts
                        Log.i(TAG, "Successfully loaded ${mappedProducts.size} products for category: $categoryId")
                    } else {
                        val errorMsg = "API error (categoryId: $categoryId): retCode=${productsResponse?.retCode}, statusCode=${productsResponse?.statusCode}, message=${productsResponse?.systemMessage ?: "Unknown API logic error"}"
                        _errorMessage.value = errorMsg
                        _itemsByCategoryId.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg = "Failed to fetch products (categoryId: $categoryId): HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                    _errorMessage.value = errorMsg
                    _itemsByCategoryId.value = emptyList()
                    Log.e(TAG, errorMsg)
                }
            } catch (e: IOException) { // For network connectivity issues
                val errorMsg =
                    "Network error loading items for category $categoryId: ${e.message}"
                _errorMessage.value = errorMsg
                _itemsByCategoryId.value = emptyList()
                Log.e("MainViewModel", errorMsg, e)
            } catch (e: Exception) { // For other issues like JSON parsing or unexpected errors
                val errorMsg = "Error loading items for category $categoryId: ${e.message}"
                _errorMessage.value = errorMsg
                _itemsByCategoryId.value = emptyList()
                Log.e("MainViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchProductsByName(query: String) {
        if (query.isBlank()) {
            _searchedItems.value = emptyList() // Clear results for blank query
            _errorMessage.value = null
            Log.d(TAG, "Search query is blank, clearing search results.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Searching products with query: '$query'")
                // Replace with your actual search API call and response model
                // For demonstration, assuming it returns a similar structure to ProductsByCategoryResponse
                val response = apiService.searchProductsByName(searchTerm = query) // Ensure this method exists and is correctly defined

                if (response.isSuccessful) {
                    val mappedProducts: List<ProductDetailsModel>? = response.body()

                    if (mappedProducts != null) { // Check if the list itself is not null
                        _searchedItems.value = mappedProducts
                        Log.i(TAG, "Search successful for '$query', found ${mappedProducts.size} products.")
                    } else {
                        // This case means the API call was successful (2xx) but the body was unexpectedly null.
                        val errorMsg = "Search API success but body was null for query '$query'"
                        _errorMessage.value = errorMsg
                        _searchedItems.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg = "Search request failed for query '$query': HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                    _errorMessage.value = errorMsg
                    _searchedItems.value = emptyList()
                    Log.e(TAG, errorMsg)
                }
            } catch (e: IOException) {
                val errorMsg = "Network error during search for '$query': ${e.message}"
                _errorMessage.value = errorMsg
                _searchedItems.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Unexpected error during search for '$query': ${e.message}"
                _errorMessage.value = errorMsg
                _searchedItems.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

        fun loadRecommendedItems() { // Renamed from loadRecommended
            _isLoading.value = true
            _errorMessage.value = null
            // _recommendedItems.value = emptyList() // Optionally clear immediately

            viewModelScope.launch {
                try {
                    Log.d(TAG, "Fetching recommended products.")
                    val response = apiService.getRecommendedItems() // This returns Response<List<ProductDetailsModel>>

                    if (response.isSuccessful) {
                        // DIRECTLY GET THE LIST OF ProductDetailsModel
                        val mappedProducts: List<ProductDetailsModel>? = response.body()

                        if (mappedProducts != null) { // Check if the list itself is not null
                            _recommendedItems.value = mappedProducts
                            Log.i(TAG, "Successfully loaded ${mappedProducts.size} recommended products.")
                        } else {
                            // API call was successful (2xx) but the body was unexpectedly null.
                            val errorMsg = "Recommended items API success but body was null."
                            _errorMessage.value = errorMsg
                            _recommendedItems.value = emptyList()
                            Log.e(TAG, errorMsg)
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "No error body"
                        val errorMsg = "Failed to fetch recommended items: HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                        _errorMessage.value = errorMsg
                        _recommendedItems.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } catch (e: IOException) {
                    val errorMsg = "Network error fetching recommended items: ${e.message}"
                    _errorMessage.value = errorMsg
                    _recommendedItems.value = emptyList()
                    Log.e(TAG, errorMsg, e)
                } catch (e: Exception) {
                    val errorMsg = "Unexpected error fetching recommended items: ${e.message}"
                    _errorMessage.value = errorMsg
                    _recommendedItems.value = emptyList()
                    Log.e(TAG, errorMsg, e)
                } finally {
                    _isLoading.value = false
                }
            }
        }


    fun loadCategories() { // Renamed from loadCategory for consistency
            _isLoading.value = true
            _errorMessage.value = null // Clear previous error
            viewModelScope.launch {
                try {
                    Log.d("MainViewModel", "Loading categories.")
                    val response = apiService.getCategories() // Calls the method in ApiService

                    if (response.isSuccessful) {
                        val categoryList = response.body()
                        _categories.value = categoryList ?: emptyList() // Handle possible null body
                        Log.d("MainViewModel", "Loaded ${categoryList?.size ?: 0} categories.")
                    } else {
                        val errorMsg =
                            "Failed to load categories: ${response.code()} ${response.message()}"
                        _errorMessage.value = errorMsg
                        _categories.value = emptyList() // Clear on error
                        Log.e(
                            "MainViewModel",
                            "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                        )
                    }
                } catch (e: IOException) {
                    val errorMsg = "Network error loading categories: ${e.message}"
                    _errorMessage.value = errorMsg
                    _categories.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } catch (e: Exception) {
                    val errorMsg = "Error loading categories: ${e.message}"
                    _errorMessage.value = errorMsg
                    _categories.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun loadBanners() {
            _isLoading.value = true
            _errorMessage.value = null // Clear previous error
            viewModelScope.launch {
                try {
                    Log.d("MainViewModel", "Loading banners.")
                    val response = apiService.getBanners() // Calls the method in ApiService

                    if (response.isSuccessful) {
                        val bannerList = response.body()
                        _banners.value = bannerList ?: emptyList() // Handle possible null body
                        Log.d("MainViewModel", "Loaded ${bannerList?.size ?: 0} banners.")
                    } else {
                        val errorMsg =
                            "Failed to load banners: ${response.code()} ${response.message()}"
                        _errorMessage.value = errorMsg
                        _banners.value = emptyList() // Clear on error
                        Log.e(
                            "MainViewModel",
                            "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                        )
                    }
                } catch (e: IOException) {
                    val errorMsg = "Network error loading banners: ${e.message}"
                    _errorMessage.value = errorMsg
                    _banners.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } catch (e: Exception) {
                    val errorMsg = "Error loading banners: ${e.message}"
                    _errorMessage.value = errorMsg
                    _banners.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

