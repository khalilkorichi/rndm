package com.rndm.app.data.remote.mapper

import com.rndm.app.data.remote.firebase.dto.FirestoreAdminRequestDto
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.RequestStatus
import com.rndm.app.domain.model.RequestType

fun FirestoreAdminRequestDto.toDomain(): AdminRequest {
    return AdminRequest(
        id = id,
        type = try { RequestType.valueOf(type) } catch (e: Exception) { RequestType.GENERAL },
        tournamentId = tournamentId,
        tournamentName = tournamentName,
        requesterUid = requesterUid,
        requesterName = requesterName,
        requesterEmail = requesterEmail,
        status = try { RequestStatus.valueOf(status) } catch (e: Exception) { RequestStatus.PENDING },
        createdAt = createdAt,
        reviewedAt = reviewedAt,
        reviewedBy = reviewedBy,
        adminNote = adminNote,
        matchId = matchId,
        remoteMatchId = remoteMatchId,
        scoreOne = scoreOne,
        scoreTwo = scoreTwo,
        penaltyScoreOne = penaltyScoreOne,
        penaltyScoreTwo = penaltyScoreTwo,
        playerOneName = playerOneName,
        playerOneClub = playerOneClub,
        playerTwoName = playerTwoName,
        playerTwoClub = playerTwoClub,
        matchId1 = matchId1,
        matchId2 = matchId2,
        matchOneDesc = matchOneDesc,
        matchTwoDesc = matchTwoDesc,
        isSlot1A = isSlot1A,
        isSlot1B = isSlot1B,
        description = description
    )
}

fun AdminRequest.toFirestoreDto(): FirestoreAdminRequestDto {
    return FirestoreAdminRequestDto(
        id = id,
        type = type.name,
        tournamentId = tournamentId,
        tournamentName = tournamentName,
        requesterUid = requesterUid,
        requesterName = requesterName,
        requesterEmail = requesterEmail,
        status = status.name,
        createdAt = createdAt,
        reviewedAt = reviewedAt,
        reviewedBy = reviewedBy,
        adminNote = adminNote,
        matchId = matchId,
        remoteMatchId = remoteMatchId,
        scoreOne = scoreOne,
        scoreTwo = scoreTwo,
        penaltyScoreOne = penaltyScoreOne,
        penaltyScoreTwo = penaltyScoreTwo,
        playerOneName = playerOneName,
        playerOneClub = playerOneClub,
        playerTwoName = playerTwoName,
        playerTwoClub = playerTwoClub,
        matchId1 = matchId1,
        matchId2 = matchId2,
        matchOneDesc = matchOneDesc,
        matchTwoDesc = matchTwoDesc,
        isSlot1A = isSlot1A,
        isSlot1B = isSlot1B,
        description = description
    )
}
