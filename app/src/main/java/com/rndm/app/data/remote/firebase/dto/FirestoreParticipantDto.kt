package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreParticipantDto(
    val id: String = "",
    val playerItemId: Long = 0L,
    val playerName: String = "",
    val clubName: String? = null,
    val groupIndex: Int = 0,
    val joinedAt: Long = 0L
)
