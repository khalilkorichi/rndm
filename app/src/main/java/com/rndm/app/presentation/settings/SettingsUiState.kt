package com.rndm.app.presentation.settings

import androidx.compose.runtime.Immutable

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSoundEnabled: Boolean = true,
    val isMatchReminderEnabled: Boolean = true,
    val isDrawAlertsEnabled: Boolean = true,
    val isTournamentUpdatesEnabled: Boolean = true,
    val appVersion: String = "1.0.0"
)
