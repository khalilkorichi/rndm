package com.rndm.app.presentation.draw.setup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class DrawSetupUiState(
    val isLoading: Boolean = true,
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: Long = 0L,
    val selectedDrawType: DrawType = DrawType.WHEEL,
    val error: String? = null
) {
    val selectedProfile: Profile?
        get() = profiles.find { it.id == selectedProfileId }
    val canStart: Boolean
        get() = selectedProfile != null && (selectedProfile?.items?.size ?: 0) >= 2
}

@HiltViewModel
class DrawSetupViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawSetupUiState())
    val uiState: StateFlow<DrawSetupUiState> = _uiState.asStateFlow()

    init {
        val initialProfileId = savedStateHandle.get<Long>("profileId") ?: 0L
        if (initialProfileId > 0L) {
            _uiState.update { it.copy(selectedProfileId = initialProfileId) }
        }
        loadProfiles()
    }

    fun initializeWithProfileId(profileId: Long) {
        if (profileId > 0L && _uiState.value.selectedProfileId == 0L) {
            _uiState.update { it.copy(selectedProfileId = profileId) }
        }
    }

    private fun loadProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                _uiState.update { state ->
                    val defaultProfileId = if (state.selectedProfileId != 0L && profiles.any { it.id == state.selectedProfileId }) {
                        state.selectedProfileId
                    } else {
                        profiles.firstOrNull()?.id ?: 0L
                    }
                    state.copy(
                        isLoading = false,
                        profiles = profiles,
                        selectedProfileId = defaultProfileId
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onProfileSelected(profileId: Long) {
        _uiState.update { it.copy(selectedProfileId = profileId) }
    }

    fun onDrawTypeSelected(drawType: DrawType) {
        _uiState.update { it.copy(selectedDrawType = drawType) }
    }
}
