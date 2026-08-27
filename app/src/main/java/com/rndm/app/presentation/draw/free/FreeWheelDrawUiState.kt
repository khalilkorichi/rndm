package com.rndm.app.presentation.draw.free

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Immutable
data class FreeWheelWinner(
    val item: ProfileItem,
    val category: DrawCategory,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class FreeWheelDrawUiState(
    val selectedCategory: DrawCategory = DrawCategory.PLAYERS,
    val playersProfiles: List<Profile> = emptyList(),
    val clubsProfiles: List<Profile> = emptyList(),
    val nationalTeamsProfiles: List<Profile> = emptyList(),
    val selectedPlayersProfile: Profile? = null,
    val selectedClubsProfile: Profile? = null,
    val selectedNationalTeamsProfile: Profile? = null,
    val remainingPlayers: List<ProfileItem> = emptyList(),
    val excludedPlayers: List<ProfileItem> = emptyList(),
    val remainingClubs: List<ProfileItem> = emptyList(),
    val excludedClubs: List<ProfileItem> = emptyList(),
    val remainingNationalTeams: List<ProfileItem> = emptyList(),
    val excludedNationalTeams: List<ProfileItem> = emptyList(),
    val isSpinning: Boolean = false,
    val selectedIndex: Int = -1,
    val targetRotation: Float = 0f,
    val spinTrigger: Long = 0L,
    val isLoading: Boolean = true,
    val isSetupDialogOpen: Boolean = true,
    val winnerItem: ProfileItem? = null,
    val isWinnerDialogOpen: Boolean = false,
    val recentWinners: List<FreeWheelWinner> = emptyList(),
    val isAdmin: Boolean = false,
    val errorMessage: String? = null
) {
    val currentWheelItems: List<ProfileItem>
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

    val currentSelectedProfile: Profile?
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> selectedPlayersProfile
            DrawCategory.CLUBS -> selectedClubsProfile
            DrawCategory.NATIONAL_TEAMS -> selectedNationalTeamsProfile
        }

    val currentCategoryProfiles: List<Profile>
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> playersProfiles
            DrawCategory.CLUBS -> clubsProfiles
            DrawCategory.NATIONAL_TEAMS -> nationalTeamsProfiles
        }

    val canSpin: Boolean
        get() = !isSpinning && currentWheelItems.isNotEmpty()
}
