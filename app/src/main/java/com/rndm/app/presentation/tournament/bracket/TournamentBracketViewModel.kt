package com.rndm.app.presentation.tournament.bracket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.RequestType
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.request.SubmitTournamentRequestUseCase
import com.rndm.app.domain.usecase.tournament.GetTournamentDetailUseCase
import com.rndm.app.domain.usecase.tournament.UpdateMatchScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentBracketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTournamentDetailUseCase: GetTournamentDetailUseCase,
    private val updateMatchScoreUseCase: UpdateMatchScoreUseCase,
    private val getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase,
    private val submitTournamentRequestUseCase: SubmitTournamentRequestUseCase
) : ViewModel() {

    private val tournamentId: Long = checkNotNull(savedStateHandle["tournamentId"])
    private val _uiState = MutableStateFlow(TournamentBracketUiState())
    val uiState: StateFlow<TournamentBracketUiState> = _uiState.asStateFlow()

    init {
        observeUserRole()
        loadBracket()
    }

    private fun observeUserRole() {
        viewModelScope.launch {
            getCurrentUserRoleUseCase().collect { role ->
                _uiState.update { it.copy(userRole = role) }
            }
        }
    }

    private fun loadBracket() {
        viewModelScope.launch {
            getTournamentDetailUseCase(tournamentId).collect { tournament ->
                if (tournament != null) {
                    _uiState.update {
                        it.copy(
                            tournament = tournament,
                            knockoutMatches = tournament.knockoutMatches,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onSelectMatchForScore(match: Match) {
        _uiState.update { it.copy(selectedMatchForScore = match) }
    }

    fun onDismissScoreDialog() {
        _uiState.update { it.copy(selectedMatchForScore = null) }
    }

    fun onSaveScore(
        scoreOne: Int,
        scoreTwo: Int,
        penaltyOne: Int? = null,
        penaltyTwo: Int? = null,
        note: String = ""
    ) {
        val match = _uiState.value.selectedMatchForScore ?: return
        val tournament = _uiState.value.tournament ?: return
        val role = _uiState.value.userRole

        viewModelScope.launch {
            if (tournament.isRemote && role != UserRole.ADMIN && !tournament.isHost) {
                // Submit Request to Admin / Host
                val noteSnippet = if (note.isNotBlank()) " | ملاحظة: $note" else ""
                val request = AdminRequest(
                    id = "",
                    type = RequestType.CHANGE_SCORE,
                    tournamentId = tournament.remoteId ?: tournamentId.toString(),
                    tournamentName = tournament.name,
                    requesterUid = "",
                    requesterName = "",
                    requesterEmail = "",
                    matchId = match.id,
                    remoteMatchId = match.remoteId,
                    scoreOne = scoreOne,
                    scoreTwo = scoreTwo,
                    penaltyScoreOne = penaltyOne,
                    penaltyScoreTwo = penaltyTwo,
                    playerOneName = match.playerOneName,
                    playerTwoName = match.playerTwoName,
                    description = "طلب تعديل نتيجة مباراة (${match.playerOneName} ضد ${match.playerTwoName}) إلى ($scoreOne - $scoreTwo)$noteSnippet"
                )
                val result = submitTournamentRequestUseCase(request)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            selectedMatchForScore = null,
                            requestFeedbackMessage = "تم إرسال طلب تعديل النتيجة للأدمن بنجاح 📨 بانتظار المراجعة والاعتماد"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            selectedMatchForScore = null,
                            requestFeedbackMessage = "تعذر إرسال الطلب، يرجى التأكد من اتصال الإنترنت"
                        )
                    }
                }
            } else {
                // Direct update (Local or Admin or Host)
                updateMatchScoreUseCase(tournamentId, match, scoreOne, scoreTwo, penaltyOne, penaltyTwo)
                _uiState.update { it.copy(selectedMatchForScore = null) }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(requestFeedbackMessage = null) }
    }
}
