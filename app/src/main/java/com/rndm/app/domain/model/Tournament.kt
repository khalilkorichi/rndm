package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Tournament(
    val id: Long = 0L,
    val name: String,
    val type: TournamentType = TournamentType.GROUPS_KNOCKOUT,
    val stage: TournamentStage = TournamentStage.GROUPS,
    val playersProfileId: Long,
    val clubsProfileId: Long? = null,
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
    val participants: List<TournamentParticipant> = emptyList(),
    val groups: List<TournamentGroup> = emptyList(),
    val knockoutMatches: List<Match> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
