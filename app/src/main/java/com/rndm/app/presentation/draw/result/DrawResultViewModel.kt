package com.rndm.app.presentation.draw.result

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class DrawResultUiState(
    val result: DrawResult? = null
)

@HiltViewModel
class DrawResultViewModel @Inject constructor(
    drawRepository: DrawRepository
) : ViewModel() {

    val uiState: StateFlow<DrawResult?> = drawRepository.getLatestDrawResult()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
