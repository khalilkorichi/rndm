package com.rndm.app.presentation.tournament.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.usecase.sync.JoinTournamentByCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinTournamentViewModel @Inject constructor(
    private val joinTournamentByCodeUseCase: JoinTournamentByCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinTournamentUiState())
    val uiState: StateFlow<JoinTournamentUiState> = _uiState.asStateFlow()

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(codeInput = code, errorMessage = null) }
    }

    fun joinTournament(onSuccess: (Long) -> Unit) {
        val code = _uiState.value.codeInput.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال كود البطولة") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = joinTournamentByCodeUseCase(code)
            if (result.isSuccess) {
                val tournamentId = result.getOrThrow()
                _uiState.update { it.copy(isLoading = false, joinedTournamentId = tournamentId) }
                onSuccess(tournamentId)
            } else {
                val error = result.exceptionOrNull()
                val rawMsg = error?.message.orEmpty()
                val friendlyMessage = when {
                    rawMsg.contains("offline", ignoreCase = true) ||
                    rawMsg.contains("UNAVAILABLE", ignoreCase = true) ||
                    rawMsg.contains("network", ignoreCase = true) ->
                        "تعذر الاتصال بالخادم السحابي. يرجى التأكد من اتصال الإنترنت وإعادة المحاولة."

                    rawMsg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                        "ليس لديك صلاحية للوصول إلى هذه البطولة."

                    error is IllegalArgumentException ->
                        error.message ?: "كود البطولة غير صحيح أو غير موجود"

                    else ->
                        if (rawMsg.isNotBlank() && !rawMsg.startsWith("Failed to get document")) {
                            rawMsg
                        } else {
                            "كود البطولة غير صحيح أو لم يتم العثور عليها سحابياً"
                        }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage
                    )
                }
            }
        }
    }
}
