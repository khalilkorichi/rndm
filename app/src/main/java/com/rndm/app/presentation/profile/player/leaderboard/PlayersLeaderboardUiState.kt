package com.rndm.app.presentation.profile.player.leaderboard

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.rndm.app.R
import com.rndm.app.domain.model.PlayerLeaderboardItem

enum class LeaderboardColumn(
    val title: String,
    val shortHeader: String,
    @DrawableRes val iconRes: Int? = null
) {
    RANK("الترتيب", "#"),
    PLAYER("اللاعب", "اللاعب"),
    TITLES("الألقاب", "🏆", R.drawable.ic_trophy),
    MATCHES("المباريات", "ل", R.drawable.ic_swords),
    WINS("الفوز", "ف"),
    DRAWS("التعادل", "ت"),
    LOSSES("الخسارة", "خ"),
    GOALS_FOR("أهداف له", "له", R.drawable.ic_football),
    GOALS_AGAINST("أهداف عليه", "عليه", R.drawable.ic_shield),
    GOAL_DIFF("فارق الأهداف", "+/-"),
    WIN_RATE("نسبة الفوز", "%", R.drawable.ic_chart),
    POINTS("النقاط", "ن"),
    CLEAN_SHEETS("شباك نظيفة", "ش.ن")
}

@Immutable
data class PlayersLeaderboardUiState(
    val players: List<PlayerLeaderboardItem> = emptyList(),
    val filteredPlayers: List<PlayerLeaderboardItem> = emptyList(),
    val searchQuery: String = "",
    val sortColumn: LeaderboardColumn = LeaderboardColumn.TITLES,
    val isAscending: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
