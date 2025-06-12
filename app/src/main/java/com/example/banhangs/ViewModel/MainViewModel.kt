package com.example.banhangs.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.CategoriesApiResponse
import com.example.banhangs.Model.CategoryModel
import com.example.banhangs.Model.HomeData
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.ProductsByCategoryResponse
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.Network.ApiResponse // Ensure this matches your project
import com.example.banhangs.Model.ProductDetailData // Ensure this matches your project
import com.example.banhangs.Model.ProductSummaryModel
import com.example.banhangs.Network.ApiService
import com.example.banhangs.Repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class MainViewModel(
    application: android.app.Application // If UserPreferencesRepository needs context
) : ViewModel() { // Or AndroidViewModel(application) if context is needed

    private val apiService = RetrofitClient.instance
    private val userPreferencesRepository = UserPreferencesRepository(application) // Initialize if needed

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for Home Screen Content
    private val _homeData = MutableLiveData<HomeData?>()
    val homeData: LiveData<HomeData?> = _homeData

    // Separate LiveData for banners and product lists for easier observation in UI
    private val _bannerImageUrls = MutableLiveData<List<String>>(emptyList())
    val bannerImageUrls: LiveData<List<String>> = _bannerImageUrls

    private val _hotProducts = MutableLiveData<List<ProductSummaryModel>>(emptyList())
    val hotProducts: LiveData<List<ProductSummaryModel>> = _hotProducts

    private val _newProducts = MutableLiveData<List<ProductSummaryModel>>(emptyList())
    val newProducts: LiveData<List<ProductSummaryModel>> = _newProducts

    private val _featuredProducts = MutableLiveData<List<ProductSummaryModel>>(emptyList())
    val featuredProducts: LiveData<List<ProductSummaryModel>> = _featuredProducts


    // If you still have a separate "recommended items" list that uses ProductDetailsModel
    private val _recommendedItems = MutableLiveData<List<ProductDetailsModel>?>(emptyList())
    val recommendedItems: MutableLiveData<List<ProductDetailsModel>?> = _recommendedItems


    // Function to load all home screen content
    fun loadHomeContent() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // val token = userPreferencesRepository.userTokenFlow.firstOrNull() // Example: get token
                // val userId = userPreferencesRepository.userIdFlow.firstOrNull() // Example: get userId

                // Pass token/userId if your API endpoint requires them
                // For example: apiService.getHomeContent(token = "Bearer $token", userId = userId)
                // If not needed, call: apiService.getHomeContent()

                Log.d(TAG, "Fetching home content...")
                val response = apiService.getHomeContent() // Adjust parameters as needed

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        _homeData.value = apiResponse.data // Store the whole HomeData object
                        // Update individual LiveData for easier UI binding
                        _bannerImageUrls.value = apiResponse.data.bannerImageUrls ?: emptyList()
                        _hotProducts.value = apiResponse.data.hotProducts ?: emptyList()
                        _newProducts.value = apiResponse.data.newProducts ?: emptyList()
                        _featuredProducts.value = apiResponse.data.featureProducts ?: emptyList() // Corrected from featureProducts
                        Log.i(TAG, "Successfully loaded home content. Banners: ${_bannerImageUrls.value?.size}, Hot: ${_hotProducts.value?.size}, New: ${_newProducts.value?.size}, Featured: ${_featuredProducts.value?.size}")
                    } else {
                        val errorMsg = "Home content API Error: retCode=${apiResponse?.retCode}, message=${apiResponse?.systemMessage ?: "Unknown API logic error"}"
                        _errorMessage.value = errorMsg
                        clearHomeDataOnError()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg = "Failed to fetch home content: HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                    _errorMessage.value = errorMsg
                    clearHomeDataOnError()
                    Log.e(TAG, errorMsg)
                }
            } catch (e: IOException) {
                val errorMsg = "Network error fetching home content: ${e.message}"
                _errorMessage.value = errorMsg
                clearHomeDataOnError()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Unexpected error fetching home content: ${e.message}"
                _errorMessage.value = errorMsg
                clearHomeDataOnError()
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearHomeDataOnError() {
        _homeData.value = null
        _bannerImageUrls.value = emptyList()
        _hotProducts.value = emptyList()
        _newProducts.value = emptyList()
        _featuredProducts.value = emptyList()
    }


    // Your existing loadRecommendedItems function:
    // Decide if this is still needed. If "recommended" items are part of the
    // new HomeData (e.g., within hotProducts or featureProducts), you might remove this
    // or adapt it. If it's a separate list with a different model (ProductDetailsModel), keep it.
    fun loadRecommendedItems() {
        // If this list is different from what's in HomeData and uses ProductDetailsModel
        _isLoading.value = true // Consider separate isLoading flags if calls can be independent
        _errorMessage.value = null // Or separate error messages

        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching recommended products (separate call).")
                val token = userPreferencesRepository.userTokenFlow.firstOrNull()
                val userId = userPreferencesRepository.userIdFlow.firstOrNull()

                if (token == null || userId == null) {
                    _errorMessage.value = "User not authenticated for recommended items."
                    _recommendedItems.value = emptyList()
                    _isLoading.value = false // Ensure loading state is reset
                    return@launch
                }

                // Assuming getRecommendedItems expects a List<ProductDetailsModel>
                val response: retrofit2.Response<ApiResponse<List<ProductDetailsModel>>> =
                    apiService.getRecommendedItems(userId = userId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        _recommendedItems.value = apiResponse.data
                        Log.i(TAG, "Successfully loaded ${apiResponse.data.size} recommended products (separate).")
                    } else {
                        val errorMsg = "Recommended items API Error (separate): retCode=${apiResponse?.retCode}, message=${apiResponse?.systemMessage ?: "Unknown API logic error"}"
                        _errorMessage.value = errorMsg
                        _recommendedItems.value = emptyList()
                        Log.e(TAG, errorMsg)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val errorMsg = "Failed to fetch recommended items (separate): HTTP ${response.code()} ${response.message()}. Error: $errorBody"
                    _errorMessage.value = errorMsg
                    _recommendedItems.value = emptyList()
                    Log.e(TAG, errorMsg)
                }
            } catch (e: IOException) {
                val errorMsg = "Network error fetching recommended items (separate): ${e.message}"
                _errorMessage.value = errorMsg
                _recommendedItems.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Unexpected error fetching recommended items (separate): ${e.message}"
                _errorMessage.value = errorMsg
                _recommendedItems.value = emptyList()
                Log.e(TAG, errorMsg, e)
            } finally {
                // If you have separate isLoading flags, manage them accordingly.
                // For simplicity, using the global one here.
                _isLoading.value = false
            }
        }
    }

    // Call this from your Fragment/Activity's onCreate or onResume
    fun initialDataLoad() {
        loadHomeContent()
        // Optionally, if recommended items are separate and always needed:
        // loadRecommendedItems()
    }
}