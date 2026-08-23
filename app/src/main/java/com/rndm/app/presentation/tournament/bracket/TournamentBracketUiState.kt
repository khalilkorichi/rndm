package com.rndm.app.presentation.tournament.bracket

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Tournament

@Immutable
data class TournamentBracketUiState(
    val tournament: Tournament? = null,
    val knockoutMatches: List<Match> = emptyList(),
    val selectedMatchForScore: Match? = null,
    val isLoading: Boolean = true
)
