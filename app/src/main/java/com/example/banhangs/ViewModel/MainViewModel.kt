package com.example.banhangs.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.CategoriesApiResponse
import com.example.banhangs.Model.CategoryModel
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.ProductsByCategoryResponse
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.Network.ApiResponse // Ensure this matches your project
import com.example.banhangs.Model.ProductDetailData // Ensure this matches your project
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class MainViewModel : ViewModel() {

    private val apiService = RetrofitClient.instance

    // isLoading and errorMessage are fine
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // For items by category (assuming this one is correct based on no errors for it)
    private val _itemsByCategoryId = MutableLiveData<List<ProductDetailsModel>>()
    val itemsByCategoryId: LiveData<List<ProductDetailsModel>> = _itemsByCategoryId

    // --- Corrected LiveData exposure for searchedItems ---
    private val _searchedItems = MutableLiveData<List<ProductDetailsModel>?>(emptyList())
    val searchedItems: LiveData<List<ProductDetailsModel>?> = _searchedItems // Expose LiveData

    // --- Corrected LiveData exposure for recommendedItems ---
    // This is likely related to the line 155 error context
    private val _recommendedItems = MutableLiveData<List<ProductDetailsModel>?>(emptyList())
    val recommendedItems: LiveData<List<ProductDetailsModel>?> =
        _recommendedItems // Expose LiveData


    private val _categories = MutableLiveData<List<CategoryModel>?>()
    val categories: MutableLiveData<List<CategoryModel>?> = _categories

    private val _banners = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banners

    private val TAG = "MainViewModel"

    // loadItemsByCategoryId seems okay based on your feedback, so keeping it as is
    fun loadItemsByCategoryId(categoryId: String) {
        if (categoryId.isBlank()) {
            _errorMessage.value = "Category ID cannot be blank."
            _itemsByCategoryId.value = emptyList()
            Log.w(TAG, "loadItemsByCategoryId called with blank categoryId.")
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading items for category ID: $categoryId from API")
                val response = apiService.getProductsByCategoryId(categoryId = categoryId)
                if (response.isSuccessful) {
                    val productsResponse: ProductsByCategoryResponse? = response.body()
                    if (productsResponse?.data != null && (productsResponse.retCode == 0 || productsResponse.statusCode == 200 || productsResponse.statusCode == 0)) {
                        val mappedProducts = productsResponse.data.map { productFromApi ->
                            ProductDetailsModel(
                                productId = productFromApi.productId,
                                name = productFromApi.name,
                                mainImageUrl = productFromApi.mainImageUrl,
                                price = productFromApi.price ?: 0.0,
                                stock = productFromApi.stock,
                                categoryName = productFromApi.categoryName,
                                averageRating = productFromApi.averageRating,
                                soldCount = productFromApi.soldCount,
                                ratedCount = productFromApi.ratedCount,
                                shortDescription = null,
                                description = null,
                                galleryImageUrls = emptyList(),
                                salePrice = null,
                                saleStart = null,
                                saleEnd = null,
                                categoryId = categoryId,
                                brandId = null,
                                brandName = null,
                                isOnSale = false,
                                isFeatured = false
                            )
                        }
                        _itemsByCategoryId.value = mappedProducts
                        Log.i(
                            TAG,
                            "Successfully loaded ${mappedProducts.size} products for category: $categoryId"
                        )
                    } else {
                        val errorMsg =
                            "API error (categoryId: $categoryId): retCode=${productsResponse?.retCode}, statusCode=${productsResponse?.statusCode}, message=${productsResponse?.systemMessage ?: "Unknown API logic error"}"
                        _errorMessage.value = errorMsg
                        _itemsByCategoryId.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg =
                        "Failed to fetch products (categoryId: $categoryId): HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                    _errorMessage.value = errorMsg
                    _itemsByCategoryId.value = emptyList()
                    Log.e(TAG, errorMsg)
                }
            } catch (e: IOException) {
                val errorMsg = "Network error loading items for category $categoryId: ${e.message}"
                _errorMessage.value = errorMsg
                _itemsByCategoryId.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Error loading items for category $categoryId: ${e.message}"
                _errorMessage.value = errorMsg
                _itemsByCategoryId.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun searchProductsByName(query: String) {
        if (query.isBlank()) {
            _searchedItems.value = emptyList()
            _errorMessage.value = null
            Log.d(TAG, "Search query is blank, clearing search results.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Searching products with query: '$query'")
                val response: retrofit2.Response<List<ApiResponse<ProductDetailData>>> =
                    apiService.searchProductsByName(searchTerm = query)

                if (response.isSuccessful) {
                    val apiResponseList: List<ApiResponse<ProductDetailData>>? = response.body()

                    if (apiResponseList != null) {
                        val products = apiResponseList.mapNotNull { apiResponseItem ->
                            if (apiResponseItem.data != null && (apiResponseItem.retCode == 0)) { // Assuming 0 is success
                                val productData = apiResponseItem.data!!
                                ProductDetailsModel(
                                    productId = productData.productId,
                                    name = productData.name,
                                    mainImageUrl = productData.mainImageUrl,
                                    price = productData.price ?: 0.0,
                                    stock = productData.stock,
                                    categoryName = productData.categoryName,
                                    averageRating = productData.averageRating,
                                    soldCount = productData.soldCount,
                                    ratedCount = productData.ratedCount,
                                    shortDescription = productData.shortDescription,
                                    description = productData.description,
                                    galleryImageUrls = productData.galleryImageUrls ?: emptyList(),
                                    salePrice = productData.salePrice,
                                    saleStart = productData.saleStart,
                                    saleEnd = productData.saleEnd,
                                    categoryId = productData.categoryId,
                                    brandId = productData.brandId,
                                    brandName = productData.brandName,
                                    isOnSale = productData.isOnSale ?: false,
                                    isFeatured = productData.isFeatured ?: false
                                )
                            } else {
                                Log.w(
                                    TAG,
                                    "Skipping item in search results: retCode=${apiResponseItem.retCode}, message=${apiResponseItem.systemMessage}"
                                )
                                null
                            }
                        }
                        _searchedItems.value = products
                        Log.i(
                            TAG,
                            "Search successful for '$query', found ${products.size} products."
                        )
                    } else {
                        val errorMsg = "Search API success but body was null for query '$query'"
                        _errorMessage.value = errorMsg
                        _searchedItems.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg =
                        "Search request failed for query '$query': HTTP ${response.code()} ${response.message()}. Error: $errorBody"
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

    // This is the function likely related to the line 155 error
    fun loadRecommendedItems() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching recommended products.")
                // ADJUSTING THE EXPECTED TYPE HERE TO MATCH THE ERROR MESSAGE'S "ACTUAL" TYPE
                val response: retrofit2.Response<List<ProductDetailsModel>> = // <--- Adjusted type
                    apiService.getRecommendedItems(userId = "68484aa57b44b2d92ca2018a")

                if (response.isSuccessful) {
                    val productList: List<ProductDetailsModel>? = response.body()

                    if (productList != null) {
                        _recommendedItems.value =
                            productList // Directly assign, no complex mapping needed
                        Log.i(TAG, "Successfully loaded ${productList.size} recommended products.")
                    } else {
                        val errorMsg = "Recommended items API success but body was null."
                        _errorMessage.value = errorMsg
                        _recommendedItems.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg =
                        "Failed to fetch recommended items: HTTP ${response.code()} ${response.message()}. Error: $errorBody"
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


    fun loadCategories() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading categories.")
                val response: Response<CategoriesApiResponse> =
                    apiService.getCategories() // Use new response type

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        _categories.value = apiResponse.data // Access the list via apiResponse.data
                        Log.d(TAG, "Loaded ${apiResponse.data.size} categories.")
                    } else {
                        val errorMsg =
                            "Failed to load categories: API reported error (retCode=${apiResponse?.retCode}) or data was null. Message: ${apiResponse?.systemMessage}"
                        _errorMessage.value = errorMsg
                        _categories.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorMsg =
                        "Failed to load categories: HTTP ${response.code()} ${response.message()}"
                    _errorMessage.value = errorMsg
                    _categories.value = emptyList()
                    Log.e(TAG, "$errorMsg - Error Body: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                val errorMsg = "Network error loading categories: ${e.message}"
                _errorMessage.value = errorMsg
                _categories.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) { // Catch specific JsonSyntaxException or IllegalStateException if needed
                val errorMsg = "Error parsing categories response: ${e.message}"
                _errorMessage.value = errorMsg
                _categories.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun loadBanners() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading banners.")
                val response = apiService.getBanners()

                if (response.isSuccessful) {
                    val bannerList = response.body()
                    _banners.value = bannerList ?: emptyList()
                    Log.d(TAG, "Loaded ${bannerList?.size ?: 0} banners.")
                } else {
                    val errorMsg = "Failed to load banners: ${response.code()} ${response.message()}"
                    _errorMessage.value = errorMsg
                    _banners.value = emptyList()
                    Log.e(TAG, "$errorMsg - Error Body: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                val errorMsg = "Network error loading banners: ${e.message}"
                _errorMessage.value = errorMsg
                _banners.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Error loading banners: ${e.message}"
                _errorMessage.value = errorMsg
                _banners.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Removed the duplicate _Recommended and _searchResults if they are not used
    // and if _recommendedItems and _searchedItems are their replacements.
    // If they ARE used for different purposes, you can keep them.
}