package com.example.banhangs.ViewModel // Or com.example.banhangs.Factory if you moved it

import android.app.Application // <<----- IMPORT THIS
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.banhangs.Repository.CartRepository
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.Repository.UserPreferencesRepository

class CartViewModelFactory(
    private val application: Application // <<----- CORRECTED TYPE HERE
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            // 'application' is now an Application instance, which IS a Context
            val userPrefsRepository = UserPreferencesRepository(application) // This is now correct

            val cartRepository = CartRepository(
                RetrofitClient.instance, // Make sure RetrofitClient.instance provides your ApiService
                userPrefsRepository
            )
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(cartRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}