package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tournament_participants",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["remoteId"])
    ]
)
data class TournamentParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tournamentId: Long,
    val playerItemId: Long,
    val playerName: String,
    val clubName: String? = null,
    val groupIndex: Int = 0,
    val remoteId: String? = null
)
