package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stash_preferences")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val AUTO_SCAN_COMPLETED = booleanPreferencesKey("auto_scan_completed")
        val CLOUD_SYNC_URL = stringPreferencesKey("cloud_sync_url")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    val viewMode: Flow<LibraryViewMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.VIEW_MODE] ?: LibraryViewMode.GRID.name
        try {
            LibraryViewMode.valueOf(raw)
        } catch (_: Exception) {
            LibraryViewMode.GRID
        }
    }

    suspend fun setViewMode(mode: LibraryViewMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIEW_MODE] = mode.name
        }
    }

    val sortOrder: Flow<LibrarySortOrder> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.SORT_ORDER] ?: LibrarySortOrder.DATE_ADDED.name
        try {
            LibrarySortOrder.valueOf(raw)
        } catch (_: Exception) {
            LibrarySortOrder.DATE_ADDED
        }
    }

    suspend fun setSortOrder(order: LibrarySortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = order.name
        }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    val autoScanCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SCAN_COMPLETED] ?: false
    }

    suspend fun setAutoScanCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_COMPLETED] = completed
        }
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    suspend fun setUserSession(email: String?) {
        context.dataStore.edit { preferences ->
            if (email != null) {
                preferences[PreferencesKeys.USER_EMAIL] = email
                preferences[PreferencesKeys.IS_LOGGED_IN] = true
            } else {
                preferences.remove(PreferencesKeys.USER_EMAIL)
                preferences[PreferencesKeys.IS_LOGGED_IN] = false
            }
        }
    }

    val cloudSyncUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CLOUD_SYNC_URL] ?: "https://stash-api.vercel.app"
    }

    suspend fun setCloudSyncUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_SYNC_URL] = url
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
