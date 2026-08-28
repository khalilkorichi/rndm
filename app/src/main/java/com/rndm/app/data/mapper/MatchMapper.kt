package com.rndm.app.data.mapper

import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.domain.model.Match

fun MatchEntity.toDomain(): Match {
    return Match(
        id = id,
        tournamentId = tournamentId,
        stage = stage,
        groupIndex = groupIndex,
        roundIndex = roundIndex,
        bracketMatchIndex = bracketMatchIndex,
        playerOneName = playerOneName,
        playerOneClub = playerOneClub,
        playerTwoName = playerTwoName,
        playerTwoClub = playerTwoClub,
        scoreOne = scoreOne,
        scoreTwo = scoreTwo,
        penaltyScoreOne = penaltyScoreOne,
        penaltyScoreTwo = penaltyScoreTwo,
        isExtraTime = isExtraTime,
        winnerName = winnerName,
        status = status,
        scheduledTimestamp = scheduledTimestamp,
        isPlayerOneLuckyLoser = isPlayerOneLuckyLoser,
        isPlayerTwoLuckyLoser = isPlayerTwoLuckyLoser,
        remoteId = remoteId,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )
}

fun Match.toEntity(tournamentId: Long = this.tournamentId): MatchEntity {
    return MatchEntity(
        id = id,
        tournamentId = tournamentId,
        stage = stage,
        groupIndex = groupIndex,
        roundIndex = roundIndex,
        bracketMatchIndex = bracketMatchIndex,
        playerOneName = playerOneName,
        playerOneClub = playerOneClub,
        playerTwoName = playerTwoName,
        playerTwoClub = playerTwoClub,
        scoreOne = scoreOne,
        scoreTwo = scoreTwo,
        penaltyScoreOne = penaltyScoreOne,
        penaltyScoreTwo = penaltyScoreTwo,
        isExtraTime = isExtraTime,
        winnerName = winnerName,
        status = status,
        scheduledTimestamp = scheduledTimestamp,
        isPlayerOneLuckyLoser = isPlayerOneLuckyLoser,
        isPlayerTwoLuckyLoser = isPlayerTwoLuckyLoser,
        remoteId = remoteId,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )
}
