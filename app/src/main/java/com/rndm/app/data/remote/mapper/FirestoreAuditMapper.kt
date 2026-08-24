package com.rndm.app.data.remote.mapper

import com.rndm.app.data.remote.firebase.dto.FirestoreAuditLogDto
import com.rndm.app.domain.model.AuditLog

fun FirestoreAuditLogDto.toDomain(tournamentId: Long = 0L): AuditLog {
    return AuditLog(
        id = id,
        tournamentId = tournamentId,
        actorUid = actorUid,
        actorRole = actorRole,
        action = action,
        matchId = matchId,
        details = details,
        timestamp = timestamp
    )
}

fun AuditLog.toFirestoreDto(): FirestoreAuditLogDto {
    return FirestoreAuditLogDto(
        id = id,
        actorUid = actorUid,
        actorRole = actorRole,
        action = action,
        matchId = matchId,
        details = details,
        timestamp = timestamp
    )
}
