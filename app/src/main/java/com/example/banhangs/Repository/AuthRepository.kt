package com.example.banhangs.Repository

import com.example.banhangs.Model.LoginRequest
import com.example.banhangs.Model.LoginResponseData // Assuming your API returns this in LoginApiResponse.data
import com.example.banhangs.Model.RegisterRequest
import com.example.banhangs.Model.RegisterResponseData // Assuming from RegisterApiResponse.data
import com.example.banhangs.Network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val apiService: ApiService) {

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
                    val errorBody = response.errorBody()?.string() // Attempt to get more info
                    Result.failure(Exception("Login failed: Network Error - Code: ${response.code()}, Message: ${response.message()}, Details: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Login failed: Exception - ${e.message}", e))
            }
        }
    }

    suspend fun registerUser(registerRequest: RegisterRequest): Result<RegisterResponseData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(registerRequest) // Assumes ApiService has a register method
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        Result.success(apiResponse.data)
                    } else {
                        val errorMessage = "Registration failed: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Registration failed: Network Error - Code: ${response.code()}, Message: ${response.message()}, Details: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Registration failed: Exception - ${e.message}", e))
            }
        }
    }
}