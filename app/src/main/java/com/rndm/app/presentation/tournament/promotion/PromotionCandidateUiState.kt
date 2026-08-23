package com.rndm.app.presentation.tournament.promotion

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant

@Immutable
data class PromotionCandidateUiState(
    val tournament: Tournament? = null,
    val directQualifiers: List<TournamentParticipant> = emptyList(),
    val promotedCandidates: List<TournamentParticipant> = emptyList(),
    val isTieBreakNeeded: Boolean = false,
    val tiedCandidates: List<TournamentParticipant> = emptyList(),
    val selectedTieBreakWinner: TournamentParticipant? = null,
    val isSpinning: Boolean = false,
    val isLoading: Boolean = true,
    val isBracketGenerated: Boolean = false
)
