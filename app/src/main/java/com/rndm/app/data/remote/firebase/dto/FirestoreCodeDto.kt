package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreCodeDto(
    val shareCode: String = "",
    val tournamentId: String = "",
    val hostUid: String = "",
    val createdAt: Long = 0L,
    val isActive: Boolean = true
)
