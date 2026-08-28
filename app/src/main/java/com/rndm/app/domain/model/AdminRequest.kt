package com.rndm.app.domain.model

enum class RequestType {
    CHANGE_SCORE,
    SWAP_MATCH_ORDER,
    SWAP_PLAYERS,
    PLAYER_REPLACE,
    PUBLISH_TOURNAMENT,
    GENERAL
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class AdminRequest(
    val id: String,
    val type: RequestType,
    val tournamentId: String,
    val tournamentName: String,
    val requesterUid: String,
    val requesterName: String,
    val requesterEmail: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
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
