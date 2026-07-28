package com.nopal.tunascreen.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appearanceDataStore by preferencesDataStore(name = "tuna_screen_appearance")

@Singleton
class AppearanceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme")
    private val displayModeKey = stringPreferencesKey("display_mode")

    val themeMode: Flow<TunaThemeMode> = context.appearanceDataStore.data.map { preferences ->
        TunaThemeMode.fromStorage(preferences[themeKey])
    }

    val displayMode: Flow<DisplayModeOverride> = context.appearanceDataStore.data.map { preferences ->
        DisplayModeOverride.fromStorage(preferences[displayModeKey])
    }

    suspend fun setThemeMode(themeMode: TunaThemeMode) {
        context.appearanceDataStore.edit { preferences ->
            preferences[themeKey] = themeMode.storageValue
        }
    }

    suspend fun setDisplayMode(displayMode: DisplayModeOverride) {
        context.appearanceDataStore.edit { preferences ->
            preferences[displayModeKey] = displayMode.storageValue
        }
    }
}
