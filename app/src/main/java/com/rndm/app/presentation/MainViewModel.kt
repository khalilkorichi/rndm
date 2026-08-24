package com.rndm.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.repository.UserPreferencesRepository
import com.rndm.app.domain.usecase.auth.InitializeGuestSessionUseCase
import com.rndm.app.presentation.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    private val initializeGuestSessionUseCase: InitializeGuestSessionUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = userPreferencesRepository.getInitialThemeMode()
        )

    init {
        viewModelScope.launch {
            initializeGuestSessionUseCase()
        }
    }
}
