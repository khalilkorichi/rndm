package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AuditLog(
    val id: String = "",
    val tournamentId: Long = 0L,
    val actorUid: String = "",
    val actorRole: String = "admin",
    val action: String = "",
    val matchId: Long? = null,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
