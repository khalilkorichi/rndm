package com.rndm.app.domain.repository

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentStage
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun getAllTournaments(): Flow<List<Tournament>>
    fun getActiveTournaments(): Flow<List<Tournament>>
    fun getArchivedTournaments(): Flow<List<Tournament>>
    fun getTournamentById(id: Long): Flow<Tournament?>
    fun getParticipants(tournamentId: Long): Flow<List<TournamentParticipant>>
    fun getMatches(tournamentId: Long): Flow<List<Match>>
    fun getMatchesByStage(tournamentId: Long, stage: MatchStage): Flow<List<Match>>
    suspend fun saveTournament(
        tournament: Tournament,
        participants: List<TournamentParticipant>,
        matches: List<Match>
    ): Long
    suspend fun updateTournamentStage(id: Long, stage: TournamentStage)
    suspend fun updateMatch(match: Match)
    suspend fun saveKnockoutMatches(tournamentId: Long, matches: List<Match>)
    suspend fun deleteTournament(id: Long)
    suspend fun archiveTournament(id: Long)
    suspend fun unarchiveTournament(id: Long)
    suspend fun replaceParticipant(
        tournamentId: Long,
        oldPlayerName: String,
        newPlayerName: String,
        newClubName: String? = null
    )
    suspend fun swapMatchOrder(tournamentId: Long, matchId1: Long, matchId2: Long)
    suspend fun swapPlayersInMatches(
        tournamentId: Long,
        matchId1: Long,
        isSlot1A: Boolean,
        matchId2: Long,
        isSlot1B: Boolean
    )
}
