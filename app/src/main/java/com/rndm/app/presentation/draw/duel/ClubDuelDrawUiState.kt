package com.rndm.app.presentation.draw.duel

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant

@Immutable
data class ClubDuelDrawUiState(
    val isLoading: Boolean = true,
    val contestedClub: String = "ريال مدريد",
    val availableClubs: List<ProfileItem> = emptyList(),
    val availablePlayers: List<ProfileItem> = emptyList(),
    val clubsProfiles: List<Profile> = emptyList(),
    val playersProfiles: List<Profile> = emptyList(),
    val selectedClubsProfile: Profile? = null,
    val selectedPlayersProfile: Profile? = null,
    val contestants: List<String> = listOf("خليل", "عبدو"),
    val isSpinning: Boolean = false,
    val selectedIndex: Int = -1,
    val targetRotation: Float = 0f,
    val spinTrigger: Long = 0L,
    val winnerName: String? = null,
    val activeTournaments: List<Tournament> = emptyList(),
    val selectedTournament: Tournament? = null,
    val tournamentParticipants: List<TournamentParticipant> = emptyList(),
    val isAssignToTournamentOpen: Boolean = false,
    val isAddContestantDialogOpen: Boolean = false,
    val isSelectClubDialogOpen: Boolean = false,
    val isResultDialogOpen: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
) {
    val canSpin: Boolean
        get() = !isSpinning && contestants.size >= 2 && contestedClub.isNotBlank()
}
