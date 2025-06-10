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

// Extension property to create the DataStore instance (typically at the top level of your Kotlin file)
// The name "user_preferences" will be the filename of the DataStore file.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("user_token")
    val USER_ID = stringPreferencesKey("user_id")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val USER_FULL_NAME = stringPreferencesKey("user_full_name") // <-- ADDED KEY
}

class UserPreferencesRepository(private val context: Context) {

    // --- User Token ---
    val userTokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences()) // Emit empty preferences on error
            } else {
                throw exception // Rethrow other exceptions
            }
        }
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_TOKEN]
        }

    suspend fun saveUserToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_TOKEN] = token
            preferences[UserPreferencesKeys.IS_LOGGED_IN] = true // Also mark as logged in
        }
    }

    // A suspend function to get the token once (useful for interceptors if handled carefully)
    // Be mindful of using this if the token can change and you need reactive updates.
    // For reactive updates, observe userTokenFlow.
    suspend fun getUserToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[UserPreferencesKeys.USER_TOKEN]
        }.firstOrNull() // Takes the first emitted value or null if the flow completes without emitting
    }

    // --- User ID ---
    val userIdFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_ID]
        }

    suspend fun getUserId(): String? {
        return context.dataStore.data
            .map { preferences ->
                preferences[UserPreferencesKeys.USER_ID]
            }
            .catch { exception -> // Also good to handle potential exceptions here
                if (exception is IOException) {
                    emit(null) // Emit null or handle error as appropriate
                } else {
                    throw exception
                }
            }
            .firstOrNull() // Takes the first emitted value (the current ID or null)
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_ID] = userId
        }
    }

    // --- User Full Name --- <--- NEW SECTION ---
    val userFullNameFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_FULL_NAME] // Use the key here
        }

    suspend fun saveUserFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_FULL_NAME] = fullName
        }
    }

    suspend fun getUserFullName(): String? { // Optional: for one-time reads
        return userFullNameFlow.firstOrNull()
    }

    // --- Login Status ---
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[UserPreferencesKeys.IS_LOGGED_IN] ?: false // Default to false if not set
        }

    // --- Clear Preferences (Logout) ---
    suspend fun clearUserPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear() // Clears all preferences in this DataStore
            // Or selectively clear:
            // preferences.remove(UserPreferencesKeys.USER_TOKEN)
            // preferences.remove(UserPreferencesKeys.USER_ID)
            // preferences[UserPreferencesKeys.IS_LOGGED_IN] = false
        }
    }

    suspend fun clearUserSession() {
        clearUserPreferences()
    }
}