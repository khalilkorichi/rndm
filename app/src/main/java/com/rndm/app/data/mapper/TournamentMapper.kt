package com.rndm.app.data.mapper

import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant

fun TournamentEntity.toDomain(
    participants: List<TournamentParticipantEntity> = emptyList()
): Tournament {
    return Tournament(
        id = id,
        name = name,
        type = type,
        stage = stage,
        status = status,
        playersProfileId = playersProfileId,
        clubsProfileId = clubsProfileId,
        groupsCount = groupsCount,
        qualifiersPerGroup = qualifiersPerGroup,
        participants = participants.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        remoteId = remoteId,
        shareCode = shareCode,
        isRemote = isRemote,
        isHost = isHost,
        isPublic = isPublic,
        hostUid = hostUid,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        remoteVersion = remoteVersion
    )
}

fun Tournament.toEntity(): TournamentEntity {
    return TournamentEntity(
        id = id,
        name = name,
        type = type,
        stage = stage,
        status = status,
        playersProfileId = playersProfileId,
        clubsProfileId = clubsProfileId,
        groupsCount = groupsCount,
        qualifiersPerGroup = qualifiersPerGroup,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        remoteId = remoteId,
        shareCode = shareCode,
        isRemote = isRemote,
        isHost = isHost,
        isPublic = isPublic,
        hostUid = hostUid,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        remoteVersion = remoteVersion
    )
}

fun TournamentParticipantEntity.toDomain(): TournamentParticipant {
    return TournamentParticipant(
        id = id,
        tournamentId = tournamentId,
        playerItemId = playerItemId,
        playerName = playerName,
        clubName = clubName,
        groupIndex = groupIndex,
        remoteId = remoteId
    )
}

fun TournamentParticipant.toEntity(tournamentId: Long): TournamentParticipantEntity {
    return TournamentParticipantEntity(
        id = id,
        tournamentId = tournamentId,
        playerItemId = playerItemId,
        playerName = playerName,
        clubName = clubName,
        groupIndex = groupIndex,
        remoteId = remoteId
    )
}
