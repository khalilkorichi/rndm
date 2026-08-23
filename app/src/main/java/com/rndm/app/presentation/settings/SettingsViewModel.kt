package com.rndm.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.themeMode,
                userPreferencesRepository.isSoundEnabled,
                userPreferencesRepository.isMatchReminderEnabled,
                userPreferencesRepository.isDrawAlertsEnabled
            ) { themeMode, isSound, isMatch, isDraw ->
                SettingsUiState(
                    themeMode = themeMode,
                    isSoundEnabled = isSound,
                    isMatchReminderEnabled = isMatch,
                    isDrawAlertsEnabled = isDraw
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun onSoundToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(enabled)
        }
    }

    fun onMatchReminderToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMatchReminderEnabled(enabled)
        }
    }

    fun onDrawAlertsToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDrawAlertsEnabled(enabled)
        }
    }
}
