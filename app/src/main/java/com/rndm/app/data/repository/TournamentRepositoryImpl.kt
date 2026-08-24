package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.mapper.toDomain
import com.rndm.app.data.mapper.toEntity
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.repository.TournamentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TournamentRepositoryImpl @Inject constructor(
    private val tournamentDao: TournamentDao,
    private val matchDao: MatchDao,
    private val ioDispatcher: CoroutineDispatcher
) : TournamentRepository {

    override fun getAllTournaments(): Flow<List<Tournament>> {
        return tournamentDao.getAllTournaments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTournaments(): Flow<List<Tournament>> {
        return tournamentDao.getActiveTournaments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getArchivedTournaments(): Flow<List<Tournament>> {
        return tournamentDao.getArchivedTournaments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTournamentById(id: Long): Flow<Tournament?> {
        return tournamentDao.getTournamentByIdFlow(id).map { it?.toDomain() }
    }

    override fun getParticipants(tournamentId: Long): Flow<List<TournamentParticipant>> {
        return tournamentDao.getParticipantsByTournamentId(tournamentId).map { list ->
            list.map { it.toDomain() }.distinctBy { it.playerName }
        }
    }

    override fun getMatches(tournamentId: Long): Flow<List<Match>> {
        return matchDao.getMatchesByTournamentId(tournamentId).map { list ->
            val domainList = list.map { it.toDomain() }
            val groupMatches = domainList.filter { it.stage == MatchStage.GROUP_STAGE }
            val knockoutMatches = domainList.filter { it.stage != MatchStage.GROUP_STAGE }
                .sortedWith(
                    compareByDescending<Match> { it.status == com.rndm.app.domain.model.MatchStatus.FINISHED }
                        .thenByDescending { it.id }
                )
                .distinctBy { Pair(it.stage, it.bracketMatchIndex ?: 1) }
                .sortedWith(
                    compareBy<Match> { it.stage.ordinal }
                        .thenBy { it.roundIndex }
                        .thenBy { it.bracketMatchIndex ?: 1 }
                )
            groupMatches + knockoutMatches
        }
    }

    override fun getMatchesByStage(tournamentId: Long, stage: MatchStage): Flow<List<Match>> {
        return matchDao.getMatchesByTournamentAndStage(tournamentId, stage).map { list ->
            list.map { it.toDomain() }
                .sortedWith(
                    compareByDescending<Match> { it.status == com.rndm.app.domain.model.MatchStatus.FINISHED }
                        .thenByDescending { it.id }
                )
                .distinctBy { it.bracketMatchIndex ?: 1 }
        }
    }

    override suspend fun saveTournament(
        tournament: Tournament,
        participants: List<TournamentParticipant>,
        matches: List<Match>
    ): Long = withContext(ioDispatcher) {
        val tournamentId = tournamentDao.insertTournament(tournament.toEntity())
        tournamentDao.insertParticipants(participants.map { it.toEntity(tournamentId) })
        matchDao.insertMatches(matches.map { it.toEntity(tournamentId) })
        tournamentId
    }

    override suspend fun updateTournamentStage(id: Long, stage: TournamentStage) = withContext(ioDispatcher) {
        val existing = tournamentDao.getTournamentById(id) ?: return@withContext
        tournamentDao.updateTournament(existing.copy(stage = stage, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateMatch(match: Match) = withContext(ioDispatcher) {
        matchDao.updateMatch(match.toEntity())
    }

    override suspend fun saveKnockoutMatches(tournamentId: Long, matches: List<Match>) = withContext(ioDispatcher) {
        matchDao.deleteKnockoutMatches(tournamentId)
        matchDao.insertMatches(matches.map { it.toEntity(tournamentId) })
    }

    override suspend fun deleteTournament(id: Long) = withContext(ioDispatcher) {
        tournamentDao.deleteTournamentById(id)
        matchDao.deleteMatchesByTournamentId(id)
        tournamentDao.deleteParticipantsByTournamentId(id)
    }

    override suspend fun archiveTournament(id: Long) = withContext(ioDispatcher) {
        tournamentDao.archiveTournament(id)
    }

    override suspend fun unarchiveTournament(id: Long) = withContext(ioDispatcher) {
        tournamentDao.unarchiveTournament(id)
    }

    override suspend fun replaceParticipant(
        tournamentId: Long,
        oldPlayerName: String,
        newPlayerName: String,
        newClubName: String?
    ) = withContext(ioDispatcher) {
        tournamentDao.replaceParticipant(tournamentId, oldPlayerName, newPlayerName, newClubName)
        matchDao.replacePlayerInMatches(tournamentId, oldPlayerName, newPlayerName, newClubName)
    }

    override suspend fun swapMatchOrder(tournamentId: Long, matchId1: Long, matchId2: Long) = withContext(ioDispatcher) {
        matchDao.swapMatchOrder(matchId1, matchId2)
    }

    override suspend fun swapPlayersInMatches(
        tournamentId: Long,
        matchId1: Long,
        isSlot1A: Boolean,
        matchId2: Long,
        isSlot1B: Boolean
    ) = withContext(ioDispatcher) {
        val m1 = matchDao.getMatchById(matchId1)
        val m2 = matchDao.getMatchById(matchId2)
        matchDao.swapPlayersInMatches(matchId1, isSlot1A, matchId2, isSlot1B)

        if (m1 != null && m2 != null && m1.groupIndex != null && m2.groupIndex != null && m1.groupIndex != m2.groupIndex) {
            val nameA = if (isSlot1A) m1.playerOneName else (m1.playerTwoName ?: "")
            val nameB = if (isSlot1B) m2.playerOneName else (m2.playerTwoName ?: "")
            if (nameA.isNotBlank() && nameB.isNotBlank()) {
                tournamentDao.updateParticipantGroup(tournamentId, nameA, m2.groupIndex)
                tournamentDao.updateParticipantGroup(tournamentId, nameB, m1.groupIndex)
            }
        }
    }
}

