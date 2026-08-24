package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreAuditLogDto(
    val id: String = "",
    val actorUid: String = "",
    val actorRole: String = "admin",
    val action: String = "",
    val matchId: Long? = null,
    val details: String = "",
    val timestamp: Long = 0L
)
