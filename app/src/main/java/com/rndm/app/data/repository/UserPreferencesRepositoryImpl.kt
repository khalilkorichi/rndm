package com.rndm.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rndm.app.domain.repository.UserPreferencesRepository
import com.rndm.app.presentation.settings.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("rndm_theme_prefs", Context.MODE_PRIVATE)
    }

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MATCH_REMINDER_ENABLED = booleanPreferencesKey("match_reminder_enabled")
        val DRAW_ALERTS_ENABLED = booleanPreferencesKey("draw_alerts_enabled")
    }

    override fun getInitialThemeMode(): ThemeMode {
        val modeName = sharedPreferences.getString("theme_mode", null) ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(modeName)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val modeName = preferences[PreferencesKeys.THEME_MODE]
                ?: sharedPreferences.getString("theme_mode", null)
                ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (_: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }
        .onEach { mode ->
            sharedPreferences.edit().putString("theme_mode", mode.name).apply()
        }

    override val isSoundEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] ?: true
        }

    override val isMatchReminderEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.MATCH_REMINDER_ENABLED] ?: true
        }

    override val isDrawAlertsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DRAW_ALERTS_ENABLED] ?: true
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit().putString("theme_mode", mode.name).apply()
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    override suspend fun setMatchReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MATCH_REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setDrawAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRAW_ALERTS_ENABLED] = enabled
        }
    }
}
