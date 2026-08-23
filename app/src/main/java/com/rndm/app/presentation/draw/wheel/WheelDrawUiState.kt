package com.rndm.app.presentation.draw.wheel

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem

enum class DrawCategory {
    PLAYERS,
    CLUBS,
    NATIONAL_TEAMS
}

@Immutable
data class WheelDrawUiState(
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
    val fixtures: List<DrawFixture> = emptyList(),
    val isSpinning: Boolean = false,
    val selectedIndex: Int = -1,
    val drawResult: DrawResult? = null,
    val targetRotation: Float = 0f,
    val spinTrigger: Long = 0L,
    val currentDrawingPrompt: String = "",
    val playerToReplace: String? = null,
    val playerToReplaceClub: String? = null,
    val isAddPlayersDialogOpen: Boolean = false,
    val error: String? = null
) {
    val existingPlayerNames: List<String>
        get() = (fixtures.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) } + remainingPlayers.map { it.label }).distinct()
    val currentWheelItems: List<ProfileItem>
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> remainingPlayers
            DrawCategory.CLUBS -> remainingClubs
            DrawCategory.NATIONAL_TEAMS -> remainingNationalTeams
        }

    val currentProfileName: String
        get() = when (selectedCategory) {
            DrawCategory.PLAYERS -> selectedPlayersProfile?.name ?: "بروفايل اللاعبين"
            DrawCategory.CLUBS -> selectedClubsProfile?.name ?: "بروفايل الأندية"
            DrawCategory.NATIONAL_TEAMS -> selectedNationalTeamsProfile?.name ?: "بروفايل المنتخبات"
        }
}
