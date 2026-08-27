package com.rndm.app.presentation.draw.result

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.usecase.draw.GenerateRoundRobinPairingsUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DrawResultUiState(
    val result: DrawResult? = null
)

@HiltViewModel
class DrawResultViewModel @Inject constructor(
    private val drawRepository: DrawRepository,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val generateRoundRobinPairingsUseCase: GenerateRoundRobinPairingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrawResult?>(null)
    val uiState: StateFlow<DrawResult?> = _uiState.asStateFlow()

    init {
        drawRepository.getLatestDrawResult()
            .onEach { latest ->
                if (_uiState.value == null) {
                    _uiState.value = latest
                }
            }
            .launchIn(viewModelScope)
    }

    fun initialize(profileId: Long, drawType: DrawType) {
        if (drawType == DrawType.ROUND_ROBIN && profileId > 0L) {
            viewModelScope.launch {
                val profile = getProfileByIdUseCase(profileId)
                if (profile != null) {
                    val activeItems = profile.activeItems.ifEmpty { profile.items }
                    if (activeItems.size >= 2) {
                        val result = generateRoundRobinPairingsUseCase(profile.id, activeItems)
                        _uiState.value = result
                    }
                }
            }
        }
    }
}
