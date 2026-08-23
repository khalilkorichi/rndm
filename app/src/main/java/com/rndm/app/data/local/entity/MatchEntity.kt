package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus

@Entity(
    tableName = "matches",
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
        Index(value = ["tournamentId", "stage", "roundIndex"])
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tournamentId: Long,
    val stage: MatchStage = MatchStage.GROUP_STAGE,
    val groupIndex: Int? = null,
    val roundIndex: Int = 1,
    val bracketMatchIndex: Int? = null,
    val playerOneName: String,
    val playerOneClub: String? = null,
    val playerTwoName: String? = null,
    val playerTwoClub: String? = null,
    val scoreOne: Int? = null,
    val scoreTwo: Int? = null,
    val penaltyScoreOne: Int? = null,
    val penaltyScoreTwo: Int? = null,
    val winnerName: String? = null,
    val status: MatchStatus = MatchStatus.PENDING,
    val scheduledTimestamp: Long? = null,
    val isPlayerOneLuckyLoser: Boolean = false,
    val isPlayerTwoLuckyLoser: Boolean = false
)
