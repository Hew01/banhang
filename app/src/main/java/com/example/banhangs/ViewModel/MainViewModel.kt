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
        MutableLiveData<List<ProductDetailsModel>>(emptyList()) // Initialize with emptyList
    val searchedItems: LiveData<List<ProductDetailsModel>> = _searchedItems

    private val _errorMessage = MutableLiveData<String?>() // For error messages
    val errorMessage: LiveData<String?> = _errorMessage

    private val _itemsByCategoryId = MutableLiveData<List<ProductDetailsModel>>()
    val itemsByCategoryId: LiveData<List<ProductDetailsModel>> = _itemsByCategoryId

    private val _recommendedItems =
        MutableLiveData<List<ProductDetailsModel>>() // If you don't have this already
    val recommendedItems: LiveData<List<ProductDetailsModel>> =
        _recommendedItems // If you don't have this already

    private val _categories =
        MutableLiveData<List<CategoryModel>>() // Assuming you want a List, not MutableList here
    val categories: LiveData<List<CategoryModel>> = _categories

    // LiveData for Banners
    private val _banners = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banners

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
                    // Check your API's success indicator, e.g., retCode == 0 or a specific statusCode
                    if (productsResponse?.data != null && (productsResponse.retCode == 0 || productsResponse.statusCode == 200 || productsResponse.statusCode == 0) ) {
                        // Map ProductFromCategory to ProductDetailsModel
                        // You need to define ProductDetailsModel or ensure ProductFromCategory is compatible
                        // with what your ListItemsAdapter expects.
                        // For this example, I'll assume ProductDetailsModel needs specific fields from ProductFromCategory.
                        val productDetailsList = productsResponse.data.map { productFromApi ->
                            ProductDetailsModel(
                                    productId = productFromApi.productId,
                                    name = productFromApi.name,
                                    mainImageUrl = productFromApi.mainImageUrl, // Keep as nullable if ProductDetailData allows
                                    price = productFromApi.price ?: 0.0, // Provide a default if null, or ensure ProductDetailData's price is nullable

                                    // Fields from ProductFromCategory that are also in ProductDetailData
                                    stock = productFromApi.stock,
                                    categoryName = productFromApi.categoryName, // This is available
                                    averageRating = productFromApi.averageRating,
                                    soldCount = productFromApi.soldCount,
                                    ratedCount = productFromApi.ratedCount,

                                    // Fields in ProductDetailData but NOT in ProductFromCategory:
                                    // Provide defaults or nulls.
                                    // These would typically be fetched when viewing a single product's details.
                                    shortDescription = null, // Or a default like "View details for more."
                                    description = null,      // Or a default
                                    galleryImageUrls = null, // Or emptyList()
                                    salePrice = null,        // No sale info in ProductFromCategory
                                    saleStart = null,
                                    saleEnd = null,
                                    categoryId = null,       // ProductFromCategory has categoryName, not ID directly.
                                    // If you need categoryId here, you might need to fetch it separately
                                    // or pass it down if it's the same ID used to filter.
                                    // For now, let's assume it's the one used in the API call:
                                    // categoryId = categoryId, // If 'categoryId' is the parameter to loadItemsByCategoryId

                                    brandId = null,          // No brand info in ProductFromCategory
                                    brandName = null,
                                    isOnSale = false,        // Assume not on sale unless detail endpoint says so
                                    isFeatured = false       // Assume not featured
                                )
                            }
                        _itemsByCategoryId.value = productDetailsList
                        Log.d(
                            "MainViewModel",
                            "Loaded ${productDetailsList.size} items for category $categoryId"
                        )
                    } else {
                        val errorMsg = "API error for category $categoryId: retCode=${productsResponse?.retCode}, statusCode=${productsResponse?.statusCode}, message=${productsResponse?.systemMessage ?: "Unknown API error"}"
                        _errorMessage.value = errorMsg
                        _itemsByCategoryId.value = emptyList() // Clear on API logical error
                        Log.e("MainViewModel", errorMsg)
                    }
                } else {
                    val errorMsg =
                        "Failed to load items for category $categoryId: ${response.code()} ${response.message()}"
                    _errorMessage.value = errorMsg
                    _itemsByCategoryId.value = emptyList() // Clear on HTTP error
                    Log.e(
                        "MainViewModel",
                        "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                    )
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

    fun searchProductsByName(query: String) { // Changed from searchItemsByName for consistency with your request
        if (query.isBlank()) {
            _searchedItems.value = emptyList() // Use the LiveData for search results
            _errorMessage.value = null // Clear previous search error
            Log.d("MainViewModel", "Search query is blank, clearing results.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear previous error
        _searchedItems.value = emptyList() // Clear previous search results immediately

        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Searching for products with query: $query")
                // Call the new method in your ApiService
                val response = apiService.searchProductsByName(searchTerm = query)

                if (response.isSuccessful) {
                    val items: List<ProductDetailsModel>? = response.body()
                    if (items != null) {
                        _searchedItems.value = items ?: emptyList()
                        Log.d("MainViewModel", "Found ${items.size} products for query: $query")
                    } else {
                        _searchedItems.value = emptyList() // Handle null body case
                        Log.d(
                            "MainViewModel",
                            "Search successful but response body was null for query: $query"
                        )
                    }
                } else {
                    val errorMsg = "Search failed: ${response.code()} ${response.message()}"
                    _errorMessage.value = errorMsg
                    // _searchedItems.value is already emptyList() from above
                    Log.e(
                        "MainViewModel",
                        "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: IOException) { // For network connectivity issues
                val errorMsg = "Network error during search: ${e.message}"
                _errorMessage.value = errorMsg
                // _searchedItems.value is already emptyList()
                Log.e("MainViewModel", errorMsg, e)
            } catch (e: Exception) { // For other issues like JSON parsing or unexpected errors
                val errorMsg = "Error during search: ${e.message}"
                _errorMessage.value = errorMsg
                // _searchedItems.value is already emptyList()
                Log.e("MainViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }

        fun loadItemsByCategoryId(categoryId: String) { // Renamed from loadFiltered
            if (categoryId.isBlank()) {
                _errorMessage.value = "Category ID cannot be blank."
                _itemsByCategoryId.value = emptyList() // Clear previous results
                Log.w("MainViewModel", "loadItemsByCategoryId called with blank categoryId.")
                return
            }

            _isLoading.value = true
            _errorMessage.value = null
            // _itemsByCategoryId.value = emptyList() // Optionally clear immediately

            viewModelScope.launch {
                try {
                    Log.d("MainViewModel", "Loading items for category ID: $categoryId")
                    // Using the @Path version from ApiService example:
                    val response = apiService.getProductsByCategoryId(categoryId = categoryId)
                    // If using the @Query version, it would be:
                    // val response = apiService.getItemsByCategoryIdQuery(categoryId = categoryId)

                    if (response.isSuccessful) {
                        val items = response.body()
                        _itemsByCategoryId.value =
                            (items ?: emptyList()) as List<ProductDetailsModel>? // Post empty list if body is null
                        Log.d(
                            "MainViewModel",
                            "Loaded ${items?.size ?: 0} items for category $categoryId"
                        )
                    } else {
                        val errorMsg =
                            "Failed to load items for category $categoryId: ${response.code()} ${response.message()}"
                        _errorMessage.value = errorMsg
                        _itemsByCategoryId.value = emptyList() // Clear on error
                        Log.e(
                            "MainViewModel",
                            "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                        )
                    }
                } catch (e: IOException) {
                    val errorMsg =
                        "Network error loading items for category $categoryId: ${e.message}"
                    _errorMessage.value = errorMsg
                    _itemsByCategoryId.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } catch (e: Exception) {
                    val errorMsg = "Error loading items for category $categoryId: ${e.message}"
                    _errorMessage.value = errorMsg
                    _itemsByCategoryId.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
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
                    Log.d("MainViewModel", "Loading recommended items.")
                    // Using the direct endpoint version from ApiService example:
                    val response = apiService.getRecommendedItems()
                    // If using the query parameter version (e.g., getItemsFiltered(isRecommended = true)):
                    // val response = apiService.getItemsFiltered(isRecommended = true)

                    if (response.isSuccessful) {
                        val items = response.body()
                        _recommendedItems.value =
                            items ?: emptyList() // Post empty list if body is null
                        Log.d("MainViewModel", "Loaded ${items?.size ?: 0} recommended items.")
                    } else {
                        val errorMsg =
                            "Failed to load recommended items: ${response.code()} ${response.message()}"
                        _errorMessage.value = errorMsg
                        _recommendedItems.value = emptyList() // Clear on error
                        Log.e(
                            "MainViewModel",
                            "$errorMsg - Error Body: ${response.errorBody()?.string()}"
                        )
                    }
                } catch (e: IOException) {
                    val errorMsg = "Network error loading recommended items: ${e.message}"
                    _errorMessage.value = errorMsg
                    _recommendedItems.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
                } catch (e: Exception) {
                    val errorMsg = "Error loading recommended items: ${e.message}"
                    _errorMessage.value = errorMsg
                    _recommendedItems.value = emptyList()
                    Log.e("MainViewModel", errorMsg, e)
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
}