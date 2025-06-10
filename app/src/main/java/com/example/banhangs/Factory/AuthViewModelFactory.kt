package com.example.banhangs.Factory

import com.example.banhangs.Repository.UserPreferencesRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.banhangs.Repository.AuthRepository
import com.example.banhangs.ViewModel.AuthViewModel

// Factory for creating AuthViewModel with AuthRepository and UserPreferencesRepository
class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository // Add this parameter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Pass both repositories to AuthViewModel constructor
            return AuthViewModel(authRepository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}