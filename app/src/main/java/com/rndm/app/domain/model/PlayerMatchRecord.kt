package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PlayerMatchRecord(
    val matchId: Long,
    val tournamentId: Long,
    val tournamentName: String,
    val stage: MatchStage,
    val date: Long,
    val playerClub: String? = null,
    val opponentName: String,
    val opponentClub: String? = null,
    val playerScore: Int,
    val opponentScore: Int,
    val playerPenalty: Int? = null,
    val opponentPenalty: Int? = null,
    val outcome: MatchOutcome
)
