package com.rndm.app.data.remote.mapper

import com.rndm.app.data.remote.firebase.dto.FirestoreTournamentDto
import com.rndm.app.domain.model.SyncStatus
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentStatus
import com.rndm.app.domain.model.TournamentType

fun FirestoreTournamentDto.toDomain(): Tournament {
    return Tournament(
        id = 0L, // Local Room auto-increment ID to be assigned upon insert
        name = name,
        type = try { TournamentType.valueOf(type) } catch (e: Exception) { TournamentType.GROUPS_KNOCKOUT },
        stage = try { TournamentStage.valueOf(stage) } catch (e: Exception) { TournamentStage.GROUPS },
        status = try { TournamentStatus.valueOf(status) } catch (e: Exception) { TournamentStatus.ACTIVE },
        playersProfileId = 0L,
        clubsProfileId = null,
        groupsCount = groupsCount,
        qualifiersPerGroup = qualifiersPerGroup,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteId = id,
        shareCode = shareCode,
        isRemote = true,
        isHost = false, // When fetched from remote, defaults to viewer unless hostUid matches current user
        hostUid = hostUid,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis(),
        remoteVersion = version
    )
}

fun Tournament.toFirestoreDto(hostUid: String, memberIds: List<String> = listOf(hostUid)): FirestoreTournamentDto {
    return FirestoreTournamentDto(
        id = remoteId ?: "",
        name = name,
        type = type.name,
        stage = stage.name,
        status = status.name,
        hostUid = this.hostUid ?: hostUid,
        memberIds = memberIds,
        editorIds = listOf(this.hostUid ?: hostUid),
        shareCode = shareCode ?: "",
        groupsCount = groupsCount,
        qualifiersPerGroup = qualifiersPerGroup,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = remoteVersion
    )
}
