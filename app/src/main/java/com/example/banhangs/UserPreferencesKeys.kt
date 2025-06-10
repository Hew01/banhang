import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

object UserPreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("user_token")
    val USER_ID = stringPreferencesKey("user_id")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    // Add other keys as needed, e.g., for user email, name, theme preference etc.
}