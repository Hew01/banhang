package com.example.banhangs.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.OrderData
import com.example.banhangs.Network.ApiService
import com.example.banhangs.Helper.SessionManager
import com.example.banhangs.Network.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException

class MyOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val _orders = MutableLiveData<List<OrderData>>()
    val orders: LiveData<List<OrderData>> = _orders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val apiService: ApiService by lazy { RetrofitClient.instance }
    private val sessionManager: SessionManager = SessionManager(application)
    private val TAG = "MyOrderViewModel"
    private var userId: String? = null


    fun loadOrders() {
        val token = sessionManager.fetchAuthToken()
        val currentUserId = sessionManager.fetchUserId()

        if (token == null) {
            _errorMessage.value = "User not authenticated. Please log in."
            Log.e(TAG, "loadOrders: Auth token is null")
            _isLoading.value = false // Ensure loading stops
            return
        }
        if (currentUserId == null) { // Add this check
            _errorMessage.value = "User ID not found. Cannot load orders."
            Log.e(TAG, "loadOrders: User ID is null")
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        Log.d(TAG, "Loading orders for userId: $currentUserId with token...")

        viewModelScope.launch {
            try {
                val response = apiService.getOrdersByUserId(currentUserId)

                // Log the raw response body regardless of success if possible
                // Note: response.body() can only be consumed once.
                // For robust logging, use an OkHttp Interceptor or read errorBody separately.
                // However, for debugging, we can try to log it here.
                // A better way is to use HttpLoggingInterceptor set to Level.BODY

                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    // Log the parsed body (if successful) or a message if null
                    if (apiResponse != null) {
                        // Convert the parsed data back to JSON for logging if needed,
                        // or log specific fields. For full raw data, HttpLoggingInterceptor is best.
                        // For now, let's log if data is present and its size.
                        Log.d(TAG, "Successful response. User ID: $currentUserId Parsed apiResponse: $apiResponse") // Logs the toString() of your ApiResponse
                        // If you have Gson available and want to log the data part as JSON:
                        // val gson = Gson()
                        // Log.d(TAG, "Successful response. Data part as JSON: ${gson.toJson(apiResponse.data)}")
                    } else {
                        Log.w(TAG, "Successful response but body is null.")
                    }

                    if (apiResponse != null && apiResponse.retCode == 0) {
                        _orders.value = apiResponse.data ?: emptyList()
                        if (apiResponse.data.isNullOrEmpty()) {
                            Log.d(TAG, "Successfully loaded orders, but the list is empty. (retCode == 0)")
                        } else {
                            Log.d(TAG, "Successfully loaded ${apiResponse.data.size} orders. (retCode == 0)")
                        }
                    } else {
                        _errorMessage.value = apiResponse?.systemMessage ?: "Failed to load orders: Invalid API response structure or error retCode."
                        Log.e(TAG, "API Error: RetCode=${apiResponse?.retCode}, Message='${apiResponse?.systemMessage}'")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string() // Consume errorBody once
                    _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                    Log.e(TAG, "HTTP Error fetching orders: ${response.code()} - ${response.message()}. Error body: $errorBodyString")
                }
            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
                Log.e(TAG, "Network error: ${e.message}", e)
            } catch (e: Exception) { // Catch more specific exceptions if possible, e.g., JsonSyntaxException
                _errorMessage.value = "An unexpected error occurred."
                Log.e(TAG, "Unexpected error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}