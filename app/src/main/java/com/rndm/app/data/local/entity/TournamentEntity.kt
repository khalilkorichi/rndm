package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rndm.app.domain.model.SyncStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentStatus
import com.rndm.app.domain.model.TournamentType

@Entity(
    tableName = "tournaments",
    indices = [
        Index(value = ["isArchived", "updatedAt"]),
        Index(value = ["remoteId"]),
        Index(value = ["shareCode"])
    ]
)
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: TournamentType = TournamentType.GROUPS_KNOCKOUT,
    val stage: TournamentStage = TournamentStage.GROUPS,
    val status: TournamentStatus = TournamentStatus.ACTIVE,
    val playersProfileId: Long,
    val clubsProfileId: Long? = null,
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
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
