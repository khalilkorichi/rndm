package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tournament_exclusions",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tournamentId"])
    ]
)
data class TournamentExclusionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tournamentId: Long,
    val category: String,
    val itemLabel: String
)
