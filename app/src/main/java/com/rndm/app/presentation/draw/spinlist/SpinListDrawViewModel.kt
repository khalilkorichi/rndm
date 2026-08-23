package com.rndm.app.presentation.draw.spinlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.usecase.draw.PerformSpinListDrawUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SpinListDrawUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val isSpinning: Boolean = false,
    val selectedIndex: Int = -1,
    val drawResult: DrawResult? = null,
    val targetScrollIndex: Int = 0,
    val error: String? = null
)

@HiltViewModel
class SpinListDrawViewModel @Inject constructor(
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val performSpinListDrawUseCase: PerformSpinListDrawUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpinListDrawUiState())
    val uiState: StateFlow<SpinListDrawUiState> = _uiState.asStateFlow()

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

    fun startSpin() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isSpinning || profile.items.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSpinning = true) }
            val result = performSpinListDrawUseCase(profile.id, profile.items)
            val selectedIndex = profile.items.indexOfFirst { it.id == result.selectedItem?.id }.coerceAtLeast(0)

            // Calculate repeated index for infinite-feel scroll
            val targetIndex = (profile.items.size * 5) + selectedIndex
            _uiState.update {
                it.copy(
                    selectedIndex = selectedIndex,
                    drawResult = result,
                    targetScrollIndex = targetIndex
                )
            }
        }
    }
}
