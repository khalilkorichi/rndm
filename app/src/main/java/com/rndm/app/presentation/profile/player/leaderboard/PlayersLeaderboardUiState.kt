package com.rndm.app.presentation.profile.player.leaderboard

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.rndm.app.R
import com.rndm.app.domain.model.PlayerLeaderboardItem

enum class LeaderboardSortBy(
    val title: String,
    @DrawableRes val iconRes: Int
) {
    TITLES("الألقاب", R.drawable.ic_trophy),
    GOALS("الأهداف", R.drawable.ic_football),
    WIN_RATE("نسبة الفوز", R.drawable.ic_chart),
    MATCHES("المباريات", R.drawable.ic_swords)
}

@Immutable
data class PlayersLeaderboardUiState(
    val players: List<PlayerLeaderboardItem> = emptyList(),
    val sortBy: LeaderboardSortBy = LeaderboardSortBy.TITLES,
    val isLoading: Boolean = true,
    val error: String? = null
)
