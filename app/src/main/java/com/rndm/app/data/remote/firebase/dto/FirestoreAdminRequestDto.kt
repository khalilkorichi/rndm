package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreAdminRequestDto(
    val id: String = "",
    val type: String = "CHANGE_SCORE",
    val tournamentId: String = "",
    val tournamentName: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val requesterEmail: String = "",
    val status: String = "PENDING",
    val createdAt: Long = 0L,
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val adminNote: String? = null,
    // Score change payload
    val matchId: Long? = null,
    val remoteMatchId: String? = null,
    val scoreOne: Int? = null,
    val scoreTwo: Int? = null,
    val penaltyScoreOne: Int? = null,
    val penaltyScoreTwo: Int? = null,
    val isExtraTime: Boolean? = null,
    val playerOneName: String? = null,
    val playerOneClub: String? = null,
    val playerTwoName: String? = null,
    val playerTwoClub: String? = null,
    // Match reorder payload
    val matchId1: Long? = null,
    val matchId2: Long? = null,
    val matchOneDesc: String? = null,
    val matchTwoDesc: String? = null,
    // Player swap payload
    val isSlot1A: Boolean? = null,
    val isSlot1B: Boolean? = null,
    // Description/Summary
    val description: String = ""
)
