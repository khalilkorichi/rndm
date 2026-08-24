package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Match(
    val id: Long = 0L,
    val tournamentId: Long = 0L,
    val stage: MatchStage = MatchStage.GROUP_STAGE,
    val groupIndex: Int? = null,
    val roundIndex: Int = 1,
    val bracketMatchIndex: Int? = null,
    val playerOneName: String,
    val playerOneClub: String? = null,
    val playerTwoName: String? = null,
    val playerTwoClub: String? = null,
    val scoreOne: Int? = null,
    val scoreTwo: Int? = null,
    val penaltyScoreOne: Int? = null,
    val penaltyScoreTwo: Int? = null,
    val winnerName: String? = null,
    val status: MatchStatus = MatchStatus.PENDING,
    val scheduledTimestamp: Long? = null,
    val isPlayerOneLuckyLoser: Boolean = false,
    val isPlayerTwoLuckyLoser: Boolean = false,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val updatedAt: Long = System.currentTimeMillis()
)
