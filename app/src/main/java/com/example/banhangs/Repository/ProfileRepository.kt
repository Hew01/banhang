package com.example.banhangs.Repository // Or your repository package

import android.util.Log
// Ensure your Model imports are correct based on the previous steps
import com.example.banhangs.Model.ChangePasswordRequest
import com.example.banhangs.Model.GenericSuccessApiResponse
import com.example.banhangs.Model.UserData
import com.example.banhangs.Model.UserInformationRequest
import com.example.banhangs.Network.ApiService
import kotlin.text.contains
// No need for kotlinx.coroutines.flow.firstOrNull here if directly using suspend get functions
// import kotlinx.coroutines.flow.firstOrNull // Keep if other parts of the class use it on actual Flows

// Make sure these are the correct imports for isNullOrBlank
import kotlin.text.isNullOrBlank

class ProfileRepository(
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository // For token and userId
) {
    private val TAG = "ProfileRepository"

    // Helper to check your API's specific success conditions for BooleanApiResponse
    private fun isBooleanApiResponseSuccessful(response: GenericSuccessApiResponse?): Boolean {
        return response?.retCode == 0 && response.data == true &&
                response.systemMessage?.contains("error", ignoreCase = true) == false // Adjust as needed
    }

    suspend fun getUserDetails(): Result<UserData> {
        // Corrected: getUserToken() and getUserId() are suspend functions returning String?
        val token = userPreferencesRepository.getUserToken()
        val userId = userPreferencesRepository.getUserId()

        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            return Result.failure(Exception("User not authenticated or userId missing."))
        }

        return try {
            val response = apiService.getUserDetails("Bearer $token", userId)
            if (response.isSuccessful) {
                val apiResponse = response.body() // This should be UserDataApiResponse
                if (apiResponse?.retCode == 0 && apiResponse.data != null) {
                    Log.d(TAG, "Successfully fetched user details: ${apiResponse.data}")
                    Result.success(apiResponse.data)
                } else {
                    val errorMsg = apiResponse?.systemMessage ?: "Failed to get user details (API error)"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Error fetching user details: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching user details", e)
            Result.failure(e)
        }
    }

    // Assuming UserInformationRequest was intended to be UserUpdateRequest from previous context
    suspend fun updateUserDetails(updateRequest: UserInformationRequest): Result<Boolean> {
        val token = userPreferencesRepository.getUserToken()
        val userId = userPreferencesRepository.getUserId()

        if (userId.isNullOrBlank()) {
            return Result.failure(Exception("UserId missing for update."))
        }
        // Token might be optional for this endpoint based on your API spec, but good to have if needed
        val authToken = if (token.isNullOrBlank()) "" else "Bearer $token"


        return try {
            // Ensure apiService.updateUserDetails expects UserUpdateRequest
            val response = apiService.updateUserDetails(authToken, userId, updateRequest)
            if (response.isSuccessful) {
                val apiResponse = response.body() // This should be BooleanApiResponse
                if (isBooleanApiResponseSuccessful(apiResponse)) {
                    Log.d(TAG, "Successfully updated user details.")
                    Result.success(true)
                } else {
                    val errorMsg = apiResponse?.systemMessage ?: "Failed to update user details (API error)"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Error updating user details: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating user details", e)
            Result.failure(e)
        }
    }

    suspend fun changePassword(changeRequest: ChangePasswordRequest): Result<Boolean> {
        val token = userPreferencesRepository.getUserToken()

        if (token.isNullOrBlank()) {
            return Result.failure(Exception("User not authenticated for password change."))
        }

        return try {
            val response = apiService.changePassword("Bearer $token", changeRequest)
            if (response.isSuccessful) {
                val apiResponse = response.body() // This should be BooleanApiResponse
                if (isBooleanApiResponseSuccessful(apiResponse)) {
                    Log.d(TAG, "Successfully changed password.")
                    Result.success(true)
                } else {
                    val errorMsg = apiResponse?.systemMessage ?: "Failed to change password (API error)"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Error changing password: ${response.code()} - ${response.message()} - $errorBody"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception changing password", e)
            Result.failure(e)
        }
    }

}