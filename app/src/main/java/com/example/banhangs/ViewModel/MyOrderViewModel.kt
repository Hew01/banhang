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

    fun loadOrders() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            _errorMessage.value = "User not authenticated. Please log in."
            Log.e(TAG, "loadOrders: Auth token is null")
            _isLoading.value = false // Ensure loading stops
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        Log.d(TAG, "Loading orders with token...")

        viewModelScope.launch {
            try {
                // Ensure your ApiService.getOrders() is a suspend function
                // and takes the Authorization header.
                val response = apiService.getOrders("Bearer $token")
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0) { // Assuming ERetCode.Successfull is 0
                        _orders.value = apiResponse.data ?: emptyList()
                        if (apiResponse.data.isNullOrEmpty()) {
                            Log.d(TAG, "Successfully loaded orders, but the list is empty.")
                        } else {
                            Log.d(TAG, "Successfully loaded ${apiResponse.data.size} orders.")
                        }
                    } else {
                        _errorMessage.value = apiResponse?.systemMessage ?: "Failed to load orders: Invalid API response structure."
                        Log.e(TAG, "API Error: RetCode=${apiResponse?.retCode}, Message='${apiResponse?.systemMessage}'")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                    Log.e(TAG, "HTTP Error fetching orders: ${response.code()} - ${response.message()}. Error body: $errorBody")
                }
            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
                Log.e(TAG, "Network error: ${e.message}", e)
            } catch (e: Exception) {
                _errorMessage.value = "An unexpected error occurred."
                Log.e(TAG, "Unexpected error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}