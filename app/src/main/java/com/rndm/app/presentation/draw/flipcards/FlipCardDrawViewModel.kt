package com.rndm.app.presentation.draw.flipcards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.usecase.draw.PerformFlipCardDrawUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FlipCardDrawUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val flippedCardIndex: Int = -1,
    val drawResult: DrawResult? = null,
    val error: String? = null
)

@HiltViewModel
class FlipCardDrawViewModel @Inject constructor(
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val performFlipCardDrawUseCase: PerformFlipCardDrawUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlipCardDrawUiState())
    val uiState: StateFlow<FlipCardDrawUiState> = _uiState.asStateFlow()

    init {
        val profileId = savedStateHandle.get<Long>("profileId") ?: 0L
        if (profileId > 0L) {
            loadProfile(profileId)
        }
    }

    fun initializeWithProfileId(profileId: Long) {
        if (profileId > 0L && _uiState.value.profile?.id != profileId) {
            loadProfile(profileId)
        }
    }

    private fun loadProfile(profileId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = getProfileByIdUseCase(profileId)
            if (profile != null) {
                _uiState.update { it.copy(isLoading = false, profile = profile) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "البروفايل غير موجود") }
            }
        }
    }

    fun onCardClick(cardIndex: Int) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.flippedCardIndex != -1) return

        viewModelScope.launch {
            val result = performFlipCardDrawUseCase(profile.id, profile.items, cardIndex)
            _uiState.update {
                it.copy(
                    flippedCardIndex = cardIndex,
                    drawResult = result
                )
            }
        }
    }
}
