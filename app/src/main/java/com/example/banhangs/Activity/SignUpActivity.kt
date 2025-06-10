package com.example.banhangs.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar // For loading indicator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity // Assuming BaseActivity isn't strictly needed here
import androidx.lifecycle.Observer
import com.example.banhangs.Model.RegisterRequest // Your RegisterRequest model
import com.example.banhangs.Network.RetrofitClient // For ApiService if needed directly (shouldn't be)
import com.example.banhangs.R
import com.example.banhangs.ViewModel.AuthViewModel
import com.example.banhangs.ViewModel.AuthUiState // Your UI State
import com.example.banhangs.Factory.AuthViewModelFactory // Your Factory
import com.example.banhangs.Repository.AuthRepository
import com.example.banhangs.Repository.UserPreferencesRepository

class SignUpActivity : AppCompatActivity() { // Changed from BaseActivity if not strictly needed for this screen

    // private lateinit var mAuth: FirebaseAuth // No longer using Firebase Auth directly here

    private lateinit var edtFirstName: EditText // Added
    private lateinit var edtLastName: EditText  // Added
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText // Added for password confirmation
    private lateinit var btnSignUp: Button
    private lateinit var btnChangeToLogin: ImageButton // Renamed for clarity
    private lateinit var progressBar: ProgressBar // Added

    private val authViewModel: AuthViewModel by viewModels {
        val userPrefsRepo = UserPreferencesRepository(applicationContext)
        val apiService = RetrofitClient.instance
        val authRepo = AuthRepository(apiService, userPrefsRepo)
        AuthViewModelFactory(authRepo, userPrefsRepo)
    }

    private val TAG = "SignUpActivity"

    @SuppressLint("MissingInflatedId") // Keep if relevant for your layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Keep if you use it
        setContentView(R.layout.activity_sign_up) // Ensure this layout has the new fields

        // Initialize Views - MAKE SURE IDs MATCH YOUR XML
        edtFirstName = findViewById(R.id.edtsignup_first_name) // Example ID
        edtLastName = findViewById(R.id.edtsignup_last_name)   // Example ID
        edtEmail = findViewById(R.id.edtsignup_email)
        edtPassword = findViewById(R.id.edtpassword_signup) // Changed ID to avoid conflict with login
        edtConfirmPassword = findViewById(R.id.edt_confirm_password) // Example ID
        btnSignUp = findViewById(R.id.btn_SignUp)
        btnChangeToLogin = findViewById(R.id.btn_ChangeToLogin) // Renamed for clarity, map to your "Back" or "Login" button
        progressBar = findViewById(R.id.progressBar_signup) // Example ID for a ProgressBar

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            performRegistration()
        }

        btnChangeToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupObservers() {
        authViewModel.authUiState.observe(this, Observer { state ->
            when (state) {
                is AuthUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSignUp.isEnabled = false
                }
                is AuthUiState.Success -> { // Assuming Success state is used for successful registration
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                    authViewModel.registrationAttemptCompleted() // Reset state
                    // Navigate to LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                is AuthUiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Registration Failed: ${state.message}", Toast.LENGTH_LONG).show()
                    authViewModel.registrationAttemptCompleted() // Reset state
                }
                is AuthUiState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                } // Handle other states if you add them (e.g., LoggedOut)
            }
        })
    }

    private fun performRegistration() {
        val firstName = edtFirstName.text.toString().trim()
        val lastName = edtLastName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val confirmPassword = edtConfirmPassword.text.toString().trim()

        if (firstName.isEmpty()) {
            Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show()
            edtFirstName.requestFocus()
            return
        }
        if (lastName.isEmpty()) {
            Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show()
            edtLastName.requestFocus()
            return
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            edtEmail.requestFocus()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
            edtEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            edtPassword.requestFocus()
            return
        }
        if (password.length < 6) { // Example: Minimum password length
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            edtPassword.requestFocus()
            return
        }
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
            edtConfirmPassword.requestFocus()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            edtConfirmPassword.requestFocus()
            return
        }

        val registerRequest = RegisterRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
            // Add any other fields your RegisterRequest needs
        )
        Log.d(TAG, "Attempting registration with request: $registerRequest")
        authViewModel.registerUser(registerRequest)
    }
}