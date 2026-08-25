package com.rndm.app.presentation.tournament.create

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.TournamentType

@Immutable
data class CreateTournamentUiState(
    val name: String = "",
    val type: TournamentType = TournamentType.GROUPS_KNOCKOUT,
    val playersProfiles: List<Profile> = emptyList(),
    val clubsProfiles: List<Profile> = emptyList(),
    val selectedPlayersProfileId: Long? = null,
    val selectedClubsProfileId: Long? = null,
    val isClubsLotteryEnabled: Boolean = false,
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
    val isLoading: Boolean = false,
    val isCreated: Long? = null,
    val errorMessage: String? = null
) {
    val selectedPlayerProfile: Profile?
        get() = playersProfiles.firstOrNull { it.id == selectedPlayersProfileId }

    val selectedClubProfile: Profile?
        get() = if (isClubsLotteryEnabled) {
            clubsProfiles.firstOrNull { it.id == selectedClubsProfileId }
        } else null

    val totalPlayers: Int
        get() = selectedPlayerProfile?.items?.size ?: 0

    val isPlayerCountValid: Boolean
        get() = totalPlayers >= 3

    val isEnoughPlayersForGroups: Boolean
        get() = totalPlayers >= groupsCount

    val playersPerGroupBase: Int
        get() = if (groupsCount > 0 && totalPlayers > 0) totalPlayers / groupsCount else 0

    val extraPlayersCount: Int
        get() = if (groupsCount > 0 && totalPlayers > 0) totalPlayers % groupsCount else 0

    val totalQualifiers: Int
        get() = groupsCount * qualifiersPerGroup

    val knockoutStageName: String
        get() = when {
            totalQualifiers <= 2 -> "المباراة النهائية"
            totalQualifiers <= 4 -> "نصف النهائي"
            totalQualifiers <= 8 -> "ربع النهائي"
            totalQualifiers <= 16 -> "دور الـ 16"
            else -> "الأدوار الإقصائية ($totalQualifiers متأهل)"
        }

    val estimatedGroupMatchesCount: Int
        get() {
            if (groupsCount <= 0 || totalPlayers <= 0) return 0
            var totalMatches = 0
            for (g in 0 until groupsCount) {
                val groupSize = playersPerGroupBase + if (g < extraPlayersCount) 1 else 0
                if (groupSize >= 2) {
                    totalMatches += (groupSize * (groupSize - 1)) / 2
                }
            }
            return totalMatches
        }

    val canCreate: Boolean
        get() = !isLoading && isPlayerCountValid && isEnoughPlayersForGroups
}
