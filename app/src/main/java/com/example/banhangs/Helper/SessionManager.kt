package com.example.banhangs.Helper // Or your preferred package

import android.content.Context
import android.content.SharedPreferences
import com.example.banhangs.Model.UserData

class SessionManager(context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences("AppSession", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_ID = "user_id"
        const val USER_FIRST_NAME = "user_first_name" // For individual fields if needed
        const val USER_LAST_NAME = "user_last_name"
        const val USER_FULL_NAME = "user_full_name" // To store the combined name
        const val USER_EMAIL = "user_email"
        const val USER_ADDRESS = "user_address"
        const val USER_PHONE_NUMBER = "user_phone_number" // Match UserData field
        const val USER_ROLE_NAME = "user_role_name"
        const val USER_PICTURE_URL = "user_picture_url"
        // Add other user details you want to save
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    fun saveUserDetails(userData: UserData) {
        val editor = prefs.edit()
        editor.putString(USER_ID, userData.userId)
        editor.putString(USER_FIRST_NAME, userData.firstName)
        editor.putString(USER_LAST_NAME, userData.lastName)

        // Construct and save a full name for easier retrieval
        val fullName = ("${userData.firstName ?: ""} ${userData.lastName ?: ""}").trim()
        if (fullName.isNotEmpty()) {
            editor.putString(USER_FULL_NAME, fullName)
        } else {
            // Fallback to email or userId if name is not available, or leave it null
            editor.putString(USER_FULL_NAME, userData.email ?: userData.userId)
        }

        editor.putString(USER_EMAIL, userData.email)
        editor.putString(USER_ADDRESS, userData.address)
        editor.putString(USER_PHONE_NUMBER, userData.phoneNumber) // Use phoneNumber
        editor.putString(USER_ROLE_NAME, userData.roleName)
        editor.putString(USER_PICTURE_URL, userData.pictureUrl)
        // Save other relevant fields from UserData as needed
        // e.g., editor.putInt(USER_GENDER, userData.gender ?: -1)
        editor.apply()
    }
    fun fetchUserId(): String? = prefs.getString(USER_ID, null)
    fun fetchUserFullName(): String? = prefs.getString(USER_FULL_NAME, null) // For display
    fun fetchUserEmail(): String? = prefs.getString(USER_EMAIL, null)
    fun fetchUserAddress(): String? = prefs.getString(USER_ADDRESS, null)
    fun fetchUserPhoneNumber(): String? = prefs.getString(USER_PHONE_NUMBER, null)
    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}