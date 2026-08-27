package com.rndm.app.presentation.draw.flipcards

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Immutable
data class FlipCardDrawUiState(
    val isLoading: Boolean = true,
    val selectedCategory: DrawCategory = DrawCategory.PLAYERS,
    val playersProfiles: List<Profile> = emptyList(),
    val clubsProfiles: List<Profile> = emptyList(),
    val nationalTeamsProfiles: List<Profile> = emptyList(),
    val selectedPlayersProfile: Profile? = null,
    val selectedClubsProfile: Profile? = null,
    val selectedNationalTeamsProfile: Profile? = null,
    val remainingPlayers: List<ProfileItem> = emptyList(),
    val remainingClubs: List<ProfileItem> = emptyList(),
    val remainingNationalTeams: List<ProfileItem> = emptyList(),
    val excludedPlayers: List<ProfileItem> = emptyList(),
    val excludedClubs: List<ProfileItem> = emptyList(),
    val excludedNationalTeams: List<ProfileItem> = emptyList(),
    val fixtures: List<DrawFixture> = emptyList(),
    val flippedCardIndex: Int = -1,
    val isRevealing: Boolean = false,
    val drawResult: DrawResult? = null,
    val currentDrawingPrompt: String = "",
    val isAddPlayersDialogOpen: Boolean = false,
    val isExcludeDialogOpen: Boolean = false,
    val reorderingFixture: DrawFixture? = null,
    val swappingPlayerSlot: Pair<DrawFixture, Boolean>? = null,
    val error: String? = null
) {
    val existingPlayerNames: List<String>
        get() = (fixtures.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) } + remainingPlayers.map { it.label } + excludedPlayers.map { it.label }).distinct()

    val currentCardsItems: List<ProfileItem>
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> remainingPlayers
            DrawCategory.CLUBS -> remainingClubs
            DrawCategory.NATIONAL_TEAMS -> remainingNationalTeams
        }

    val currentExcludedItems: List<ProfileItem>
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> excludedPlayers
            DrawCategory.CLUBS -> excludedClubs
            DrawCategory.NATIONAL_TEAMS -> excludedNationalTeams
        }

    val currentProfileName: String
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> selectedPlayersProfile?.name ?: "بروفايل اللاعبين"
            DrawCategory.CLUBS -> selectedClubsProfile?.name ?: "بروفايل الأندية"
            DrawCategory.NATIONAL_TEAMS -> selectedNationalTeamsProfile?.name ?: "بروفايل المنتخبات"
        }
}
