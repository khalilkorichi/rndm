package com.rndm.app.presentation.tournament.detail

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.usecase.tournament.LoserCandidate

@Immutable
data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val selectedTab: TournamentDetailTab = TournamentDetailTab.OVERVIEW,
    val selectedGroupIndex: Int = 0,
    val selectedMatchForScore: Match? = null,
    val allMatches: List<Match> = emptyList(),
    val bestLosers: List<LoserCandidate> = emptyList(),
    val isLoading: Boolean = true,
    val isPromotionReady: Boolean = false,
    val isKnockoutReady: Boolean = false,
    val playerToReplace: String? = null,
    val playerToReplaceClub: String? = null
)
