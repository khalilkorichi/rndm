package com.rndm.app.presentation.tournament.promotion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.usecase.tournament.DeterminePromotionCandidatesUseCase
import com.rndm.app.domain.usecase.tournament.GenerateKnockoutBracketUseCase
import com.rndm.app.domain.usecase.tournament.GetTournamentDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromotionCandidateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTournamentDetailUseCase: GetTournamentDetailUseCase,
    private val determinePromotionCandidatesUseCase: DeterminePromotionCandidatesUseCase,
    private val generateKnockoutBracketUseCase: GenerateKnockoutBracketUseCase,
    private val randomProvider: RandomProvider
) : ViewModel() {

    private val tournamentId: Long = checkNotNull(savedStateHandle["tournamentId"])
    private val _uiState = MutableStateFlow(PromotionCandidateUiState())
    val uiState: StateFlow<PromotionCandidateUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getTournamentDetailUseCase(tournamentId).collect { tournament ->
                if (tournament != null) {
                    val decision = determinePromotionCandidatesUseCase(
                        groups = tournament.groups,
                        qualifiersPerGroup = tournament.qualifiersPerGroup
                    )
                    _uiState.update {
                        it.copy(
                            tournament = tournament,
                            directQualifiers = decision.directQualifiers,
                            promotedCandidates = decision.promotedCandidates,
                            isTieBreakNeeded = decision.isTieBreakNeeded,
                            tiedCandidates = decision.tiedCandidates,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun performTieBreakDraw() {
        val candidates = _uiState.value.tiedCandidates
        if (candidates.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSpinning = true) }
            delay(1200)
            val selected = randomProvider.pickRandom(candidates)
            _uiState.update { it.copy(isSpinning = false, selectedTieBreakWinner = selected) }
        }
    }

    fun confirmAndGenerateBracket() {
        val state = _uiState.value
        val allQualifiers = mutableListOf<TournamentParticipant>()
        allQualifiers.addAll(state.directQualifiers)
        allQualifiers.addAll(state.promotedCandidates)
        state.selectedTieBreakWinner?.let { allQualifiers.add(it) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            generateKnockoutBracketUseCase(tournamentId, allQualifiers)
            _uiState.update { it.copy(isLoading = false, isBracketGenerated = true) }
        }
    }
}
