package com.example.banhangs.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.banhangs.Model.LoginRequest // Assuming you have this model
import com.example.banhangs.Repository.AuthRepository
import com.example.banhangs.Repository.UserPreferencesRepository
import kotlinx.coroutines.launch
import kotlin.text.fold
import kotlin.text.isNullOrBlank

// Define a sealed class for UI state management (optional but good practice)
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState() // Could also hold user data if needed by UI immediately
    data class Error(val message: String) : AuthUiState()
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
            val result = authRepository.loginUser(loginRequest)
            result.fold(
                onSuccess = { loginResponseData ->
                    // Assuming loginResponseData contains the token and user details
                    val token = loginResponseData.token
                    val userId = loginResponseData.user.userId // Make sure path is correct

                    userPreferencesRepository.saveUserToken(token)
                    userPreferencesRepository.saveUserId(userId)
                    // IS_LOGGED_IN is typically set within saveUserToken in UserPreferencesRepository

                    _authUiState.value = AuthUiState.Success
                    // _navigateToHome.value = Event(Unit) // For navigation trigger
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Unknown login error")
                }
            )
        }
    }

    /**
     * Logs out the current user by clearing their preferences.
     * Updates _authUiState (e.g., to redirect or update UI).
     */
    fun logoutUser() {
        viewModelScope.launch {
            userPreferencesRepository.clearUserPreferences()
            // After clearing, isLoggedIn Flow will emit false, triggering UI updates
            // _navigateToLogin.value = Event(Unit) // For navigation trigger
            // Optionally, set a specific state for logout if needed
            // _authUiState.value = AuthUiState.Idle // Or a new LoggedOut state
        }
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