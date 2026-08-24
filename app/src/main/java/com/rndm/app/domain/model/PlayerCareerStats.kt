package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PlayerCareerStats(
    val playerName: String,
    val nickname: String? = null,
    val avatarIcon: String? = null,
    val favoriteClub: String? = null,
    val notes: String? = null,
    val totalTournaments: Int = 0,
    val titlesCount: Int = 0,
    val runnerUpCount: Int = 0,
    val thirdPlaceCount: Int = 0,
    val bestAchievement: String = "لا توجد مشاركات سابقة",
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalDraws: Int = 0,
    val totalLosses: Int = 0,
    val winRatePercentage: Float = 0f,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val goalDifference: Int = 0,
    val averageGoalsPerMatch: Float = 0f,
    val cleanSheets: Int = 0,
    val biggestWin: String? = null,
    val mostPlayedClub: String? = null,
    val recentForm: List<MatchOutcome> = emptyList()
)

@Serializable
enum class MatchOutcome {
    WIN,
    DRAW,
    LOSS
}
