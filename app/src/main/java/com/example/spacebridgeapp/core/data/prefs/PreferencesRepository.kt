package com.example.spacebridgeapp.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PreferencesRepository {
    val themeMode: Flow<String?> // "light", "dark", or null (system)
    val isWifiOnlyUpload: Flow<Boolean>
    val isGridLayout: Flow<Boolean>
    val hasSeenDriveOnboarding: Flow<Boolean>
    val isDriveAuthorized: Flow<Boolean>

    suspend fun setThemeMode(mode: String?)

    suspend fun setWifiOnlyUpload(wifiOnly: Boolean)

    suspend fun setGridLayout(isGrid: Boolean)

    suspend fun setSeenDriveOnboarding(seen: Boolean)

    suspend fun setDriveAuthorized(authorized: Boolean)

    suspend fun clearUserPreferences()
}

class DefaultPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WIFI_ONLY_UPLOAD = booleanPreferencesKey("wifi_only_upload")
        val GRID_LAYOUT = booleanPreferencesKey("grid_layout")
        val SEEN_DRIVE_ONBOARDING = booleanPreferencesKey("seen_drive_onboarding")
        val IS_DRIVE_AUTHORIZED = booleanPreferencesKey("is_drive_authorized")
    }

    override val themeMode: Flow<String?> =
        dataStore.data.map { prefs ->
            prefs[Keys.THEME_MODE]
        }

    override val isWifiOnlyUpload: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.WIFI_ONLY_UPLOAD] ?: true
        }

    override val isGridLayout: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.GRID_LAYOUT] ?: true
        }

    override val hasSeenDriveOnboarding: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.SEEN_DRIVE_ONBOARDING] ?: false
        }

    override val isDriveAuthorized: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.IS_DRIVE_AUTHORIZED] ?: false
        }

    override suspend fun setThemeMode(mode: String?) {
        dataStore.edit { prefs ->
            if (mode == null) {
                prefs.remove(Keys.THEME_MODE)
            } else {
                prefs[Keys.THEME_MODE] = mode
            }
        }
    }

    override suspend fun setWifiOnlyUpload(wifiOnly: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY_UPLOAD] = wifiOnly
        }
    }

    override suspend fun setGridLayout(isGrid: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GRID_LAYOUT] = isGrid
        }
    }

    override suspend fun setSeenDriveOnboarding(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SEEN_DRIVE_ONBOARDING] = seen
        }
    }

    override suspend fun setDriveAuthorized(authorized: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_DRIVE_AUTHORIZED] = authorized
        }
    }

    override suspend fun clearUserPreferences() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SEEN_DRIVE_ONBOARDING)
            prefs.remove(Keys.IS_DRIVE_AUTHORIZED)
        }
    }
}
