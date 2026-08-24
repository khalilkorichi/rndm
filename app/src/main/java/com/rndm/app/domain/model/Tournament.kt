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
    val status: TournamentStatus = TournamentStatus.ACTIVE,
    val playersProfileId: Long,
    val clubsProfileId: Long? = null,
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
    val participants: List<TournamentParticipant> = emptyList(),
    val groups: List<TournamentGroup> = emptyList(),
    val knockoutMatches: List<Match> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val remoteId: String? = null,
    val shareCode: String? = null,
    val isRemote: Boolean = false,
    val isHost: Boolean = true,
    val hostUid: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val lastSyncedAt: Long? = null,
    val remoteVersion: Long = 0L
)
