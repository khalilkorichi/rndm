package com.rndm.app.presentation.tournament.bracket

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.UserRole

@Immutable
data class TournamentBracketUiState(
    val tournament: Tournament? = null,
    val knockoutMatches: List<Match> = emptyList(),
    val selectedMatchForScore: Match? = null,
    val isLoading: Boolean = true,
    val userRole: UserRole = UserRole.GUEST,
    val requestFeedbackMessage: String? = null
) {
    val isRequestMode: Boolean
        get() {
            val t = tournament ?: return false
            return t.isRemote && !t.isHost && userRole != UserRole.ADMIN
        }

    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
}
