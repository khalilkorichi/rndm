package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreMatchDto(
    val id: String = "",
    val stage: String = "GROUP_STAGE",
    val groupIndex: Int? = null,
    val roundIndex: Int = 1,
    val bracketMatchIndex: Int? = null,
    val playerOneName: String = "",
    val playerOneClub: String? = null,
    val playerTwoName: String? = null,
    val playerTwoClub: String? = null,
    val scoreOne: Int? = null,
    val scoreTwo: Int? = null,
    val penaltyScoreOne: Int? = null,
    val penaltyScoreTwo: Int? = null,
    val winnerName: String? = null,
    val status: String = "PENDING",
    val scheduledTimestamp: Long? = null,
    val isPlayerOneLuckyLoser: Boolean = false,
    val isPlayerTwoLuckyLoser: Boolean = false,
    val updatedByUid: String? = null,
    val updatedAt: Long = 0L
)
