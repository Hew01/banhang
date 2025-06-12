package com.example.banhangs.Repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property to create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("user_token")
    val USER_ID = stringPreferencesKey("user_id")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val USER_FULL_NAME = stringPreferencesKey("user_full_name")
    val USER_EMAIL = stringPreferencesKey("user_email") // <-- ADDED KEY
    val USER_PHONE_NUMBER = stringPreferencesKey("user_phone_number") // <-- ADDED KEY
}

class UserPreferencesRepository(private val context: Context) {

    // --- User Token ---
    val userTokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.USER_TOKEN] }

    suspend fun saveUserToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_TOKEN] = token
            preferences[UserPreferencesKeys.IS_LOGGED_IN] = true
        }
    }

    suspend fun getUserToken(): String? = userTokenFlow.firstOrNull()

    // --- User ID ---
    val userIdFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.USER_ID] }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_ID] = userId
        }
    }

    suspend fun getUserId(): String? = userIdFlow.firstOrNull()

    // --- User Full Name (can be used for customerName) ---
    val userFullNameFlow: Flow<String?> = context.dataStore.data // This is your "userNameFlow" effectively
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.USER_FULL_NAME] }

    suspend fun saveUserFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_FULL_NAME] = fullName
        }
    }

    suspend fun getUserFullName(): String? = userFullNameFlow.firstOrNull()

    // --- User Email --- <--- NEW SECTION ---
    val userEmailFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.USER_EMAIL] }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun getUserEmail(): String? = userEmailFlow.firstOrNull()

    // --- User Phone Number --- <--- NEW SECTION ---
    val userPhoneNumberFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.USER_PHONE_NUMBER] }

    suspend fun saveUserPhoneNumber(phoneNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_PHONE_NUMBER] = phoneNumber
        }
    }

    suspend fun getUserPhoneNumber(): String? = userPhoneNumberFlow.firstOrNull()


    // --- Login Status ---
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[UserPreferencesKeys.IS_LOGGED_IN] ?: false }

    // --- Clear Preferences (Logout) ---
    suspend fun clearUserPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Consider renaming to just clear() or clearAllData() if it's the only clear operation
    suspend fun clearUserSession() {
        clearUserPreferences()
    }
}