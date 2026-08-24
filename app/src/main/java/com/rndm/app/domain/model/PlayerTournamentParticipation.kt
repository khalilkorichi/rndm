package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PlayerTournamentParticipation(
    val tournamentId: Long,
    val tournamentName: String,
    val tournamentType: TournamentType,
    val tournamentDate: Long,
    val isArchived: Boolean,
    val clubName: String? = null,
    val stageReachedTitle: String,
    val stageReachedType: StageReachedType,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesDrawn: Int = 0,
    val matchesLost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val rankInTournament: Int? = null
)

@Serializable
enum class StageReachedType {
    CHAMPION,
    RUNNER_UP,
    THIRD_PLACE,
    SEMI_FINALS,
    QUARTER_FINALS,
    ROUND_OF_16,
    ROUND_OF_32,
    GROUPS_STAGE,
    PARTICIPANT
}
