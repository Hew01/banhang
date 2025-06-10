package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.banhangs.databinding.ActivityLoginBinding // Assuming ViewBinding
import com.example.banhangs.Model.LoginRequest
import com.example.banhangs.Network.RetrofitClient // For ApiService instance if needed by Factory
import com.example.banhangs.Repository.AuthRepository
import com.example.banhangs.ViewModel.AuthUiState
import com.example.banhangs.ViewModel.AuthViewModel
import com.example.banhangs.Factory.AuthViewModelFactory
import com.example.banhangs.Repository.UserPreferencesRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var navigatedFromLoginSuccess = false // Flag to manage navigation state

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            AuthRepository(RetrofitClient.instance),
            UserPreferencesRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The isLoggedIn observer will handle initial redirection if already logged in.
        // This is generally cleaner than a manual check here.

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val loginIdentifier = binding.edtemailLogin.text.toString().trim()
            val pass = binding.edtpasswordLogin.text.toString().trim()

            if (loginIdentifier.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.loginUser(LoginRequest(loginIdentifier, pass))
        }

        binding.btnChangeSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            // Consider if you need to finish LoginActivity here or not based on your flow
        }
    }

    private fun setupObservers() {
        // Observe the UI state from AuthViewModel
        authViewModel.authUiState.observe(this, Observer { state ->
            // Reset common UI elements
            binding.btnLogin.isEnabled = true

            when (state) {
                is AuthUiState.Idle -> {
                    // UI is in its default state
                }
                is AuthUiState.Loading -> {
                    binding.btnLogin.isEnabled = false
                }
                is AuthUiState.Success -> {
                    // Important: The actual navigation is now handled by the isLoggedIn observer
                    // to ensure it happens consistently, even if the user was already logged in.
                    // This state primarily confirms the login *action* was successful.
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    Log.i("LoginActivity", "Login action successful.")
                    navigatedFromLoginSuccess = true // Set flag that success state was reached
                    // The isLoggedIn observer will see true and navigate.
                }
                is AuthUiState.Error -> {
                    Log.e("LoginActivity", "Login failed: ${state.message}")
                }
            }
        })

        // Observe login status for navigation and initial checks
        // Using repeatOnLifecycle to ensure collection stops when the view is destroyed
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Or RESUMED
                authViewModel.isLoggedIn.observe(this@LoginActivity) { isLoggedIn ->
                    // This observer will fire when:
                    // 1. The Activity starts and UserPreferencesRepository emits initial state.
                    // 2. After a successful login sets IS_LOGGED_IN to true.
                    // 3. After a logout sets IS_LOGGED_IN to false.

                    if (isLoggedIn) {
                        // Only navigate if this activity is still active and visible
                        // and we haven't *just* navigated due to login success in this instance.
                        if (!isFinishing && !isChangingConfigurations) {
                            Log.i("LoginActivity", "User is logged in (observed). Navigating to MainActivity.")
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Ensure LoginActivity is removed from the back stack
                        }
                    } else {
                        // User is not logged in.
                        // If `navigatedFromLoginSuccess` was true, it means login was successful
                        // but then `isLoggedIn` became false (e.g. logout immediately after, unlikely, or error saving session).
                        // Reset the flag if the user is now logged out.
                        if (navigatedFromLoginSuccess) {
                            Log.w("LoginActivity", "User was logged in successfully but now isLoggedIn is false.")
                            navigatedFromLoginSuccess = false
                        }
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset the flag if the user navigates back to LoginActivity
        // after a successful login (e.g., presses back from MainActivity immediately).
        // This ensures that if isLoggedIn is still true, it can re-evaluate navigation.
        navigatedFromLoginSuccess = false
    }
}