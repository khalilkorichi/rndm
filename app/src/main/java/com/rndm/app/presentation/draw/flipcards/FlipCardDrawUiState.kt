package com.rndm.app.presentation.draw.flipcards

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Immutable
data class FlipCardState(
    val item: ProfileItem,
    val isDrawn: Boolean = false
)

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
    val playerCards: List<FlipCardState> = emptyList(),
    val clubsCards: List<FlipCardState> = emptyList(),
    val nationalTeamsCards: List<FlipCardState> = emptyList(),
    val excludedPlayers: List<ProfileItem> = emptyList(),
    val excludedClubs: List<ProfileItem> = emptyList(),
    val excludedNationalTeams: List<ProfileItem> = emptyList(),
    val fixtures: List<DrawFixture> = emptyList(),
    val flippedCardIndex: Int = -1,
    val isRevealing: Boolean = false,
    val isShuffling: Boolean = false,
    val shuffleTrigger: Long = 0L,
    val drawResult: DrawResult? = null,
    val currentDrawingPrompt: String = "",
    val isAddPlayersDialogOpen: Boolean = false,
    val isExcludeDialogOpen: Boolean = false,
    val reorderingFixture: DrawFixture? = null,
    val swappingPlayerSlot: Pair<DrawFixture, Boolean>? = null,
    val error: String? = null
) {
    val remainingPlayers: List<ProfileItem>
        get() = playerCards.filter { !it.isDrawn }.map { it.item }

    val remainingClubs: List<ProfileItem>
        get() = clubsCards.filter { !it.isDrawn }.map { it.item }

    val remainingNationalTeams: List<ProfileItem>
        get() = nationalTeamsCards.filter { !it.isDrawn }.map { it.item }

    val existingPlayerNames: List<String>
        get() = (fixtures.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) } + playerCards.map { it.item.label } + excludedPlayers.map { it.label }).distinct()

    val currentCards: List<FlipCardState>
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> playerCards
            DrawCategory.CLUBS -> clubsCards
            DrawCategory.NATIONAL_TEAMS -> nationalTeamsCards
        }

    val currentCardsItems: List<ProfileItem>
        get() = currentCards.map { it.item }

    val currentRemainingItems: List<ProfileItem>
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
