package com.rndm.app.data.remote.mapper

import com.rndm.app.data.remote.firebase.dto.FirestoreParticipantDto
import com.rndm.app.domain.model.TournamentParticipant

fun FirestoreParticipantDto.toDomain(tournamentId: Long = 0L): TournamentParticipant {
    return TournamentParticipant(
        id = 0L,
        tournamentId = tournamentId,
        playerItemId = playerItemId,
        playerName = playerName,
        clubName = clubName,
        groupIndex = groupIndex,
        remoteId = id
    )
}

fun TournamentParticipant.toFirestoreDto(remoteId: String? = null): FirestoreParticipantDto {
    return FirestoreParticipantDto(
        id = this.remoteId ?: remoteId ?: "",
        playerItemId = playerItemId,
        playerName = playerName,
        clubName = clubName,
        groupIndex = groupIndex,
        joinedAt = System.currentTimeMillis()
    )
}
