package com.example.banhangs.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.LoginRequest // Assuming you have this model
import com.example.banhangs.Model.RegisterRequest
import com.example.banhangs.Repository.AuthRepository
import com.example.banhangs.Repository.UserPreferencesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.text.fold
import kotlin.text.isNullOrBlank

// Define a sealed class for UI state management (optional but good practice)
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState() // Could also hold user data if needed by UI immediately
    data class Error(val message: String) : AuthUiState()
    object LoggedOut : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authUiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val authUiState: LiveData<AuthUiState> = _authUiState

    // LiveData to observe login status from anywhere, driven by DataStore
    // This allows UI to react immediately if the token is cleared or set elsewhere.
    val isLoggedIn: LiveData<Boolean> = userPreferencesRepository.isLoggedInFlow.asLiveData()
    // Alternatively, if you only want to expose a one-time check or trigger navigation:
    // private val _navigateToHome = MutableLiveData<Event<Unit>>()
    // val navigateToHome: LiveData<Event<Unit>> = _navigateToHome

    // private val _navigateToLogin = MutableLiveData<Event<Unit>>()
    // val navigateToLogin: LiveData<Event<Unit>> = _navigateToLogin


    /**
     * Attempts to log in the user with the given credentials.
     * Updates _authUiState to reflect loading, success, or error.
     */
    fun loginUser(loginRequest: LoginRequest) {
        _authUiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            viewModelScope.launch {
                val result =
                    authRepository.loginUser(loginRequest) // This calls your suspend function in AuthRepository
                result.fold(
                    // Inside AuthViewModel.loginUser() onSuccess lambda:

                    onSuccess = { loginResponseData ->
                        // Assuming loginResponseData contains the token and user details
                        val token = loginResponseData.token
                        // Ensure the path to userId, firstName, and lastName is correct based on your LoginResponseData model
                        val userId = loginResponseData.user.userId

                        // --- Construct the full name (Handling Nullable Strings) ---
                        val firstName = loginResponseData.user.firstName // This is likely String?
                        val lastName = loginResponseData.user.lastName   // This is likely String?

                        val fn = firstName?.trim() // Trim whitespace, result is String?
                        val ln = lastName?.trim()  // Trim whitespace, result is String?

                        val fullName = when {
                            !fn.isNullOrBlank() && !ln.isNullOrBlank() -> "$fn $ln"
                            !fn.isNullOrBlank() -> fn
                            !ln.isNullOrBlank() -> ln
                            else -> "User" // Fallback default if both are null or blank
                        }
                        // --- End: Construct the full name ---

                        Log.d(
                            "AuthViewModel",
                            "Login Success. Token: $token, UserId: $userId, FullName: $fullName"
                        )

                        // Save all details to DataStore
                        // These save methods in UserPreferencesRepository should expect non-null Strings
                        // if their parameters are defined as String. If they can accept String?, no change needed there.
                        // Assuming they expect non-null String for simplicity here for token and userId,
                        // and fullName is already constructed as non-null String.
                        if (token != null && userId != null) { // Add null checks if token/userId from response can be null
                            userPreferencesRepository.saveUserToken(token)
                            userPreferencesRepository.saveUserId(userId)
                            userPreferencesRepository.saveUserFullName(fullName) // fullName is already non-null String here

                            _authUiState.value = AuthUiState.Success
                        } else {
                            // Handle cases where token or userId might be unexpectedly null from a "successful" response
                            Log.e(
                                "AuthViewModel",
                                "Login technically success, but token or userId is null."
                            )
                            _authUiState.value =
                                AuthUiState.Error("Login error: Missing token or user ID.")
                        }
                    },
                    onFailure = { exception ->
                        Log.e("AuthViewModel", "Login Failed: ${exception.message}", exception)
                        _authUiState.value =
                            AuthUiState.Error(exception.message ?: "Unknown login error")
                    }
                )
            }
        }
    }

        /**
         * Logs out the current user by clearing their preferences.
         * Updates _authUiState (e.g., to redirect or update UI).
         */
        fun logoutUser() {
//            _authUiState.value = AuthUiState.Loading // Indicate logout is in progress
//            viewModelScope.launch {
//                val result = authRepository.logoutUser() // Call the repository's logout
//                result.fold(
//                    onSuccess = {
//                        // isLoggedInFlow will automatically update from clearUserPreferences
//                        // No need to call userPreferencesRepository.clearUserPreferences() here anymore
//                        _authUiState.value =
//                            AuthUiState.LoggedOut // Signal successful logout for UI
//                    },
//                    onFailure = { exception ->
//                        // Even on failure (e.g., API call failed but local was cleared, or local clear failed),
//                        // UI should probably still treat it as logged out or provide an error.
//                        // The isLoggedInFlow will reflect the local data state.
//                        _authUiState.value = AuthUiState.Error(exception.message ?: "Logout failed")
//                        // Consider if you still want to transition to LoggedOut state or a specific LogoutError state
//                        // For simplicity, error message is shown, isLoggedInFlow handles redirection.
//                    }
//                )
//            }
            _authUiState.value = AuthUiState.Loading // Indicate logout is in progress
            Log.d("AuthViewModel_Debug", "logoutUser() called. Attempting to clear preferences.")

            viewModelScope.launch {
                try {
                    // 1. Directly call clearUserPreferences()
                    userPreferencesRepository.clearUserPreferences()
                    Log.d("AuthViewModel_Debug", "clearUserPreferences() completed.")

                    // 2. Immediately try to fetch the token again
                    // IMPORTANT: For this debug scenario within a non-suspend function's coroutine
                    // and wanting to log immediately after, we might use runBlocking for simplicity
                    // to get the value synchronously from the suspend function.
                    // In real app code, you'd collect the flow or use another suspend function.
                    val tokenAfterClear: String? = runBlocking { // Use runBlocking for debug only
                        userPreferencesRepository.getUserToken()
                    }

                    // 3. Log if the token is present
                    if (tokenAfterClear.isNullOrBlank()) {
                        Log.d("AuthViewModel_Debug", "Token after clear: NULL or BLANK. Preferences likely cleared.")
                    } else {
                        Log.e("AuthViewModel_Debug", "Token after clear: '$tokenAfterClear'. Preferences NOT fully cleared or race condition.")
                    }

                    // Update UI state to reflect logout
                    _authUiState.value = AuthUiState.LoggedOut

                } catch (e: Exception) {
                    Log.e("AuthViewModel_Debug", "Error during debug logoutUser: ${e.message}", e)
                    _authUiState.value = AuthUiState.Error("Debug Logout failed: ${e.message}")
                }
            }
        }

        fun registerUser(registerRequest: RegisterRequest) {
            _authUiState.value = AuthUiState.Loading // Show loading state
            viewModelScope.launch {
                val result = authRepository.registerUser(registerRequest)
                result.fold(
                    onSuccess = {
                        // Decide what state represents successful registration
                        // Option 1: A specific state
                        // _authUiState.value = AuthUiState.RegistrationSuccessful
                        // Option 2: Back to Idle, and let UI show a success message and navigate
                        _authUiState.value =
                            AuthUiState.Success // Or a new AuthUiState.RegistrationSuccess
                        Log.i("AuthViewModel", "User registration successful via ViewModel.")
                    },
                    onFailure = { exception ->
                        _authUiState.value =
                            AuthUiState.Error(exception.message ?: "Unknown registration error")
                        Log.e(
                            "AuthViewModel",
                            "User registration failed via ViewModel: ${exception.message}"
                        )
                    }
                )
            }
        }

        // Function to reset state if needed, e.g., after showing a success/error message
        fun registrationAttemptCompleted() {
            _authUiState.value = AuthUiState.Idle
        }


        /**
         * Checks the initial login state when the ViewModel is created.
         * This might be useful to automatically navigate if already logged in.
         */
        fun checkInitialLoginState() {
            viewModelScope.launch {
                val token = userPreferencesRepository.getUserToken() // Or observe isLoggedInFlow
                if (!token.isNullOrBlank()) {
                    // User is likely logged in, potentially navigate to home
                    // Or let the isLoggedIn LiveData drive this automatically in the UI
                    // _authUiState.value = AuthUiState.Success // If you want to signal this
                }
            }
        }

        // Call this if you use the Event wrapper for navigation and want to consume the event
        // fun onNavigatedToHome() { _navigateToHome.value = null }
        // fun onNavigatedToLogin() { _navigateToLogin.value = null }
    }