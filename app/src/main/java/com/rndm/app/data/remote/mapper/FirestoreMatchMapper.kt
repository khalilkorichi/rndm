package com.rndm.app.data.remote.mapper

import com.rndm.app.data.remote.firebase.dto.FirestoreMatchDto
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.SyncStatus

fun FirestoreMatchDto.toDomain(tournamentId: Long = 0L, localMatchId: Long = 0L): Match {
    return Match(
        id = localMatchId,
        tournamentId = tournamentId,
        stage = try { MatchStage.valueOf(stage) } catch (e: Exception) { MatchStage.GROUP_STAGE },
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
        winnerName = winnerName,
        status = try { MatchStatus.valueOf(status) } catch (e: Exception) { MatchStatus.PENDING },
        scheduledTimestamp = scheduledTimestamp,
        isPlayerOneLuckyLoser = isPlayerOneLuckyLoser,
        isPlayerTwoLuckyLoser = isPlayerTwoLuckyLoser,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = updatedAt
    )
}

fun Match.toFirestoreDto(remoteId: String? = null, actorUid: String? = null): FirestoreMatchDto {
    return FirestoreMatchDto(
        id = this.remoteId ?: remoteId ?: "",
        stage = stage.name,
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
        winnerName = winnerName,
        status = status.name,
        scheduledTimestamp = scheduledTimestamp,
        isPlayerOneLuckyLoser = isPlayerOneLuckyLoser,
        isPlayerTwoLuckyLoser = isPlayerTwoLuckyLoser,
        updatedByUid = actorUid,
        updatedAt = updatedAt
    )
}
