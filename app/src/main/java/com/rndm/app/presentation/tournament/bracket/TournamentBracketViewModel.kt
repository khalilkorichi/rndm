package com.rndm.app.presentation.tournament.bracket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Match
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
    private val updateMatchScoreUseCase: UpdateMatchScoreUseCase
) : ViewModel() {

    private val tournamentId: Long = checkNotNull(savedStateHandle["tournamentId"])
    private val _uiState = MutableStateFlow(TournamentBracketUiState())
    val uiState: StateFlow<TournamentBracketUiState> = _uiState.asStateFlow()

    init {
        loadBracket()
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

    fun onSaveScore(scoreOne: Int, scoreTwo: Int, penaltyOne: Int? = null, penaltyTwo: Int? = null) {
        val match = _uiState.value.selectedMatchForScore ?: return
        viewModelScope.launch {
            updateMatchScoreUseCase(tournamentId, match, scoreOne, scoreTwo, penaltyOne, penaltyTwo)
            _uiState.update { it.copy(selectedMatchForScore = null) }
        }
    }
}
