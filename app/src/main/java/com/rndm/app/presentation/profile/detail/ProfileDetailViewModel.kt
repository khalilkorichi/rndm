package com.rndm.app.presentation.profile.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState.asStateFlow()

    init {
        val profileId = savedStateHandle.get<Long>("profileId") ?: 0L
        if (profileId > 0L) {
            loadProfile(profileId)
        }
    }

    fun initializeWithId(profileId: Long) {
        if (profileId > 0L && _uiState.value.profile?.id != profileId) {
            loadProfile(profileId)
        }
    }

    private fun loadProfile(profileId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = getProfileByIdUseCase(profileId)
            if (profile != null) {
                _uiState.update { it.copy(isLoading = false, profile = profile, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "البروفايل غير موجود") }
            }
        }
    }
}
