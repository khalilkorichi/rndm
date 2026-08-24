package com.rndm.app.presentation.profile.player

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.PlayerCareerStats
import com.rndm.app.domain.model.PlayerHeadToHead
import com.rndm.app.domain.model.PlayerMatchRecord
import com.rndm.app.domain.model.PlayerTournamentParticipation

enum class PlayerProfileTab(val title: String) {
    OVERVIEW("الإحصائيات"),
    TOURNAMENTS("سجل البطولات"),
    MATCHES("المباريات"),
    HEAD_TO_HEAD("المواجهات المباشرة")
}

@Immutable
data class PlayerProfileUiState(
    val playerName: String = "",
    val stats: PlayerCareerStats = PlayerCareerStats(playerName = ""),
    val tournamentHistory: List<PlayerTournamentParticipation> = emptyList(),
    val matchHistory: List<PlayerMatchRecord> = emptyList(),
    val headToHead: List<PlayerHeadToHead> = emptyList(),
    val selectedTab: PlayerProfileTab = PlayerProfileTab.OVERVIEW,
    val isLoading: Boolean = true,
    val isEditSheetOpen: Boolean = false,
    val error: String? = null
)
