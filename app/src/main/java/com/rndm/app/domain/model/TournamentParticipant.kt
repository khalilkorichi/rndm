package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TournamentParticipant(
    val id: Long = 0L,
    val tournamentId: Long = 0L,
    val playerItemId: Long,
    val playerName: String,
    val clubName: String? = null,
    val groupIndex: Int = 0,
    val remoteId: String? = null
)
