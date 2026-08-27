package com.rndm.app.presentation.tournament.detail

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.UserRole
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
    val reorderingMatch: Match? = null,
    val swappingPlayerSlot: Pair<Match, Boolean>? = null,
    val editingParticipant: TournamentParticipant? = null,
    val isMyRequestsSheetOpen: Boolean = false,
    val myRequests: List<AdminRequest> = emptyList(),
    val userRole: UserRole = UserRole.GUEST,
    val isPublishing: Boolean = false,
    val publishErrorMessage: String? = null,
    val isBroadcasting: Boolean = false,
    val isBroadcasted: Boolean = false,
    val isShareDialogOpen: Boolean = false,
    val activeShareCode: String? = null,
    val requestFeedbackMessage: String? = null
) {
    val isRequestMode: Boolean
        get() {
            val t = tournament ?: return false
            return t.isRemote && !t.isHost && userRole != UserRole.ADMIN
        }

    val canEditScores: Boolean
        get() = true // All users can either edit directly (if Host/Admin) or submit score requests

    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
}
