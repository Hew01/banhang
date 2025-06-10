package com.example.banhangs.Activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.lifecycle.Observer
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope // Required for lifecycleScope
import com.example.banhangs.Model.ChangePasswordRequest
import com.example.banhangs.Model.UserData
import com.example.banhangs.Model.UserInformationRequest
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.R // Your R file
import com.example.banhangs.Repository.ProfileRepository
import com.example.banhangs.Repository.UserPreferencesRepository
import com.example.banhangs.ViewModel.AuthViewModel
import com.example.banhangs.databinding.ActivityProfileBinding
import com.example.banhangs.Factory.AuthViewModelFactory
import com.example.banhangs.Repository.AuthRepository
import kotlinx.coroutines.launch // Required for launch
import kotlin.text.ifEmpty
import kotlin.text.isNullOrBlank
import kotlin.text.trim

class ProfileActivity : BaseActivity() { // Assuming BaseActivity provides common setup
    private lateinit var binding: ActivityProfileBinding
    private val authViewModel: AuthViewModel by viewModels {
        val localUserPrefsRepo = UserPreferencesRepository(applicationContext)
        val apiService = RetrofitClient.instance // Get your ApiService instance
        val authRepo = AuthRepository(apiService, localUserPrefsRepo)

        // Now pass the created instances to the factory
        AuthViewModelFactory(authRepo, localUserPrefsRepo) // Placeholder for your factory
    }

    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var profileRepository: ProfileRepository
    private var currentUserId: String? = null
    private var currentUserData: UserData? = null // To store fetched user data

    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Repositories
        userPreferencesRepository = UserPreferencesRepository(applicationContext) // Or however you init it
        val apiService = RetrofitClient.instance // Your Retrofit instance
        profileRepository = ProfileRepository(apiService, userPreferencesRepository)

        lifecycleScope.launch {
            val token = userPreferencesRepository.getUserToken() // Correct
            currentUserId = userPreferencesRepository.getUserId() // Correct

            if (token.isNullOrBlank() || currentUserId.isNullOrBlank()) {
                redirectToLogin()
                return@launch
            }
            // Token exists, proceed to load profile from API
            loadProfileInfoFromApi()
        }
        setupClickListeners()
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, LoginActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.saveBtn.setOnClickListener {
            if (currentUserId == null) {
                Toast.makeText(this, "Cannot save, user not identified.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Use current email from fetched data or a non-editable field
            // val email = binding.emailAddress.text.toString().trim() // Email should not be editable from here normally
            val name = binding.nameEditTxt.text.toString().trim()
            val address = binding.addressEditTxt.text.toString().trim() // Assuming you renamed emailAddress to addressEditTxt for address
            val phone = binding.phoneEditTxt.text.toString().trim()

            // Construct UserUpdateRequest based on UserData fields
            // Assuming UserData has firstName, lastName, address, phoneNumber
            val parts = name.split(" ", limit = 2)
            val firstName = parts.getOrNull(0)
            val lastName = parts.getOrNull(1)

            if (firstName.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateRequest = lastName?.ifEmpty { null }?.let { it1 ->
                UserInformationRequest(
                    firstName = firstName,
                    lastName = it1,
                    address = address.ifEmpty { null },
                    phoneNumber = phone.ifEmpty { null },
                    birthday = currentUserData?.birthday, // Preserve existing if not changed
                    gender = currentUserData?.gender,     // Preserve existing
                )
            }
            if (updateRequest != null) {
                saveProfileInfoToApi(updateRequest)
            }
        }

        binding.logoutBtn.setOnClickListener { // Make sure you have a logoutBtn in your layout
            performLogout()
        }

        binding.changePasswordBtn.setOnClickListener { // Make sure you have changePasswordBtn
            showChangePasswordDialog()
        }
    }

    private fun loadProfileInfoFromApi() {
        lifecycleScope.launch {
            val result = profileRepository.getUserDetails()
            result.fold(
                onSuccess = { userData ->
                    currentUserData = userData // Store for later use (e.g. preserving fields)
                    binding.currentEmailTxt.text = userData.email ?: "No email provided"
                    // Combine firstName and lastName for the name field
                    val fullName = listOfNotNull(userData.firstName, userData.lastName).joinToString(" ")
                    binding.nameEditTxt.setText(fullName.ifEmpty { "N/A" })
                    // The binding.emailAddress was for address, let's assume it's binding.addressEditTxt now
                    binding.addressEditTxt.setText(userData.address ?: "")
                    binding.phoneEditTxt.setText(userData.phoneNumber ?: "")
                    Log.d(TAG, "API Profile loaded: $userData")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to load profile from API: ${exception.message}")
                    Toast.makeText(this@ProfileActivity, "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
                    // Optionally, load from TinyDB as fallback or show placeholders
                    // binding.nameEditTxt.setText(tinyDB.getString("profile_name") ?: "N/A")
                    binding.currentEmailTxt.text = "Error loading email"
                    // Consider if you should redirect to login if token is valid but user fetch fails repeatedly
                }
            )
        }
    }

    private fun saveProfileInfoToApi(updateRequest: UserInformationRequest) {
        lifecycleScope.launch {
            val result = profileRepository.updateUserDetails(updateRequest)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Profile saved via API.")
                    // Optionally reload profile to show confirmed changes or just finish
                    loadProfileInfoFromApi() // Reload to confirm
                    // finish()
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to save profile via API: ${exception.message}")
                    Toast.makeText(this@ProfileActivity, "Failed to update profile: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val currentPasswordEt = dialogView.findViewById<EditText>(R.id.currentPasswordEt)
        val newPasswordEt = dialogView.findViewById<EditText>(R.id.newPasswordEt)
        val confirmNewPasswordEt = dialogView.findViewById<EditText>(R.id.confirmNewPasswordEt)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null) // Set to null to override and prevent auto-dismiss
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener { dialogInterface ->
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        val currentPass = currentPasswordEt.text.toString()
                        val newPass = newPasswordEt.text.toString()
                        val confirmPass = confirmNewPasswordEt.text.toString()

                        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                            Toast.makeText(this@ProfileActivity, "All fields are required", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (newPass != confirmPass) {
                            Toast.makeText(this@ProfileActivity, "New passwords do not match", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        // Add more password validation if needed (length, complexity)

                        lifecycleScope.launch {
                            val result = profileRepository.changePassword(ChangePasswordRequest(currentPass, newPass))
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(this@ProfileActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                    dismiss() // Dismiss the dialog
                                },
                                onFailure = { exception ->
                                    Toast.makeText(this@ProfileActivity, "Failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                    // Don't dismiss, let user retry or cancel
                                }
                            )
                        }
                    }
                }
                show()
            }
    }

    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, _ ->
                lifecycleScope.launch {
                    authViewModel.logoutUser() // Clears local session
                    Toast.makeText(this@ProfileActivity, "Logged out", Toast.LENGTH_SHORT).show()
                    redirectToLogin()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // If returning to this screen and user might have been logged out elsewhere,
        // or token expired, re-check.
        lifecycleScope.launch {
            val token = userPreferencesRepository.getUserToken()
            if (token.isNullOrBlank() && currentUserId != null) { // currentUserId check to prevent loop on initial create
                Log.d(TAG, "Token cleared, redirecting to login from onResume")
                redirectToLogin()
            }
        }
    }
}