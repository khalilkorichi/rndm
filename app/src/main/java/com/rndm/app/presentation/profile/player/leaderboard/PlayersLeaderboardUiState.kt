package com.rndm.app.presentation.profile.player.leaderboard

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.PlayerLeaderboardItem

enum class LeaderboardSortBy(val title: String) {
    TITLES("الألقاب 🏆"),
    GOALS("الأهداف ⚽"),
    WIN_RATE("نسبة الفوز 📈"),
    MATCHES("المباريات 🎮")
}

@Immutable
data class PlayersLeaderboardUiState(
    val players: List<PlayerLeaderboardItem> = emptyList(),
    val sortBy: LeaderboardSortBy = LeaderboardSortBy.TITLES,
    val isLoading: Boolean = true,
    val error: String? = null
)
