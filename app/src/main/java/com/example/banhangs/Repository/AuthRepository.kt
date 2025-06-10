package com.example.banhangs.Repository

import android.util.Log
import com.example.banhangs.Model.LoginRequest
import com.example.banhangs.Model.LoginResponseData // Assuming your API returns this in LoginApiResponse.data
import com.example.banhangs.Model.RegisterRequest
import com.example.banhangs.Model.RegisterResponseData // Assuming from RegisterApiResponse.data
import com.example.banhangs.Network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.isNullOrBlank

class AuthRepository(
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository) {
    private val TAG = "AuthRepository"
    suspend fun loginUser(loginRequest: LoginRequest): Result<LoginResponseData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(loginRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && (apiResponse.retCode == 0 || apiResponse.retCode == 3) && apiResponse.data != null) {
                        Result.success(apiResponse.data)
                    } else {
                        val errorMessage = "Login failed: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Login failed: Network Error - Code: ${response.code()}, Message: ${response.message()}, Details: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Login failed: Exception - ${e.message}", e))
            }
        }
    }

    suspend fun registerUser(registerRequest: RegisterRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting API registration for email: ${registerRequest.email}")
                val response = apiService.register(registerRequest)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    // Adjust condition based on your RegisterApiResponse structure and success criteria
                    if (apiResponse != null && apiResponse.retCode == 0 /* && apiResponse.data == true */) {
                        Log.i(
                            TAG,
                            "API Registration successful for: ${registerRequest.email}. Message: ${apiResponse.systemMessage}"
                        )
                        // No token to save on registration typically, user needs to login separately
                        // or if your register endpoint returns a token, handle it here.
                        Result.success(Unit)
                    } else {
                        val errorMessage =
                            "Registration failed: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Log.w(TAG, errorMessage)
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage =
                        "Registration failed: Network Error - Code: ${response.code()}, Message: ${response.message()}, Details: $errorBody"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val errorMessage = "Registration failed: Exception - ${e.message}"
                Log.e(TAG, errorMessage, e)
                Result.failure(Exception(errorMessage, e))
            }
        }
    }

    suspend fun logoutUser(): Result<Unit> {
        val token = userPreferencesRepository.getUserToken() // Get token for API call

        return withContext(Dispatchers.IO) {
            var apiLogoutSuccess = false
            if (!token.isNullOrBlank()) {
                try {
                    Log.d(TAG, "Attempting API logout.")
                    val response = apiService.logoutUserApi("Bearer $token")
                    if (response.isSuccessful && response.body()?.data == true && response.body()?.retCode == 0) {
                        Log.i(TAG, "API logout successful.")
                        apiLogoutSuccess = true
                    } else {
                        Log.w(TAG, "API logout failed or returned unexpected response: ${response.code()} - ${response.message()} - Body: ${response.body()?.systemMessage}")
                        // You might choose to return Result.failure here if API logout is absolutely critical
                        // and you don't want to proceed with local logout.
                        // However, it's often safer to clear local data anyway.
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "API logout network exception: ${e.message}", e)
                    // Again, decide if this is a hard failure for the whole logout operation.
                }
            } else {
                Log.w(TAG, "No token found, skipping API logout. Proceeding with local clear.")
                // If there's no token, we can't call the API, but we should still clear local data.
                apiLogoutSuccess = true // Consider this "successful" in terms of not blocking local logout
            }

            // Always clear local preferences as a fallback or primary action
            try {
                userPreferencesRepository.clearUserPreferences()
                Log.i(TAG, "Local user session cleared.")
                Result.success(Unit) // Overall logout is successful if local clear happens.
                // You could also factor in apiLogoutSuccess if it's a strict requirement.
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear local user preferences: ${e.message}", e)
                Result.failure(Exception("Failed to clear local session: ${e.message}", e))
            }
        }
    }
}