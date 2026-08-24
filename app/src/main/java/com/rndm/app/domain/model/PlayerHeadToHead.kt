package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PlayerHeadToHead(
    val opponentName: String,
    val opponentNickname: String? = null,
    val opponentAvatar: String? = null,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val winRate: Float = 0f
)
