package com.rndm.app.domain.repository

import com.rndm.app.domain.model.AuditLog
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentStatus
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun publishTournament(tournamentId: Long): Result<Tournament>
    suspend fun joinTournamentByCode(shareCode: String): Result<Long>
    fun observeRemoteMatches(tournamentId: Long): Flow<Unit>
    suspend fun syncMatchScore(tournamentId: Long, match: Match, oldScoreOne: Int?, oldScoreTwo: Int?): Result<Unit>
    suspend fun updateTournamentStatus(tournamentId: Long, status: TournamentStatus): Result<Unit>
    fun observeAuditLogs(tournamentId: Long): Flow<List<AuditLog>>
}
