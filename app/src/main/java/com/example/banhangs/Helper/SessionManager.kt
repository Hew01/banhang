package com.example.banhangs.Helper // Or your preferred package

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences("AppSession", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
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

    fun saveUserDetails(userId: String, userName: String) {
        val editor = prefs.edit()
        editor.putString(USER_ID, userId)
        editor.putString(USER_NAME, userName)
        editor.apply()
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, "Anonymous") // Default if not found
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}