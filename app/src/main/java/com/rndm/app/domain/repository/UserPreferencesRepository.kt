package com.rndm.app.domain.repository

import com.rndm.app.presentation.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val isSoundEnabled: Flow<Boolean>
    val isMatchReminderEnabled: Flow<Boolean>
    val isDrawAlertsEnabled: Flow<Boolean>

    fun getInitialThemeMode(): ThemeMode
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMatchReminderEnabled(enabled: Boolean)
    suspend fun setDrawAlertsEnabled(enabled: Boolean)
}
