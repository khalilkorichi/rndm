package com.rndm.app.core.navigation

import com.rndm.app.domain.model.DrawType
import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object ProfileList : Destination

    @Serializable
    data class ProfileDetail(val profileId: Long) : Destination

    @Serializable
    data class CreateEditProfile(val profileId: Long = 0L, val typeName: String = "PLAYERS") : Destination

    @Serializable
    data class DrawSetup(val profileId: Long = 0L) : Destination

    @Serializable
    data class Draw(val profileId: Long, val drawType: DrawType) : Destination

    @Serializable
    data class ClubDuelDraw(val profileId: Long = 0L, val targetClub: String = "") : Destination

    @Serializable
    data object DrawResult : Destination

    @Serializable
    data object MatchFixtures : Destination

    @Serializable
    data object TournamentList : Destination

    @Serializable
    data object CreateTournament : Destination

    @Serializable
    data object JoinTournament : Destination

    @Serializable
    data class TournamentDetail(val tournamentId: Long) : Destination

    @Serializable
    data class TournamentBracket(val tournamentId: Long) : Destination

    @Serializable
    data class PromotionCandidate(val tournamentId: Long) : Destination

    @Serializable
    data object TournamentArchive : Destination

    @Serializable
    data class PlayerProfile(val playerName: String) : Destination

    @Serializable
    data object PlayersLeaderboard : Destination

    @Serializable
    data object Settings : Destination
}
