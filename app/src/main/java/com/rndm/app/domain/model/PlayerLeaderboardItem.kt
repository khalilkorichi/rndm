package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PlayerLeaderboardItem(
    val rank: Int = 0,
    val playerName: String,
    val nickname: String? = null,
    val avatarIcon: String? = null,
    val titlesCount: Int = 0,
    val runnerUpCount: Int = 0,
    val totalTournaments: Int = 0,
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalDraws: Int = 0,
    val totalLosses: Int = 0,
    val winRate: Float = 0f,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val goalDifference: Int = 0,
    val cleanSheets: Int = 0,
    val points: Int = 0
)

@Immutable
@Serializable
data class PlayerQuickStats(
    val playerName: String,
    val nickname: String? = null,
    val avatarIcon: String? = null,
    val titlesCount: Int = 0,
    val goalsScored: Int = 0,
    val totalMatches: Int = 0,
    val winRate: Float = 0f
)
