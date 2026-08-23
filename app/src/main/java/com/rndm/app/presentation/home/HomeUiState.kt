package com.rndm.app.presentation.home

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.Tournament

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val recentProfile: Profile? = null,
    val topProfiles: List<Profile> = emptyList(),
    val totalProfilesCount: Int = 0,
    val activeTournament: Tournament? = null,
    val activeTournamentMatches: List<Match> = emptyList(),
    val currentMatchIndex: Int = 0,
    val totalTournamentsCount: Int = 0,
    val activeTournamentsCount: Int = 0,
    val completedTournamentsCount: Int = 0,
    val recentChampionTournament: Tournament? = null,
    val selectedMatchForScore: Match? = null,
    val error: String? = null
)
