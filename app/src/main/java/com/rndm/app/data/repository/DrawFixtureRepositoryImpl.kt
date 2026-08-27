package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentExclusionEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import com.rndm.app.data.mapper.toDomain
import com.rndm.app.data.mapper.toEntity
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentType
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.usecase.tournament.EvaluateBestLosersUseCase
import com.rndm.app.domain.usecase.tournament.GenerateKnockoutBracketUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawFixtureRepositoryImpl @Inject constructor(
    private val tournamentDao: TournamentDao,
    private val matchDao: MatchDao,
    private val evaluateBestLosersUseCase: EvaluateBestLosersUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : DrawFixtureRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var _currentTournamentId: Long = 0L
    private var _customTournamentName: String? = null
    override val currentTournamentId: Long
        get() = _currentTournamentId

    private val _fixtures = MutableStateFlow<List<DrawFixture>>(emptyList())
    override val fixtures: StateFlow<List<DrawFixture>> = _fixtures.asStateFlow()

    private val _pendingNewPlayers = MutableStateFlow<List<String>>(emptyList())
    override val pendingNewPlayers: StateFlow<List<String>> = _pendingNewPlayers.asStateFlow()

    override fun queueNewPlayersForDraw(names: List<String>) {
        _pendingNewPlayers.update { current -> (current + names).distinct() }
    }

    override fun consumePendingNewPlayers(): List<String> {
        val list = _pendingNewPlayers.value
        _pendingNewPlayers.value = emptyList()
        return list
    }

    override fun loadTournamentFixtures(tournamentId: Long) {
        scope.launch {
            _currentTournamentId = tournamentId
            val participants = tournamentDao.getParticipantsList(tournamentId)
            val participantClubs = participants.associate { it.playerName to it.clubName }
            val matches = matchDao.getMatchesList(tournamentId)
                .filter { it.roundIndex == 1 }
                .sortedBy { it.bracketMatchIndex ?: it.id.toInt() }

            val loaded = matches.mapIndexed { idx, m ->
                DrawFixture(
                    id = "fixture_${idx + 1}",
                    matchNumber = idx + 1,
                    playerOneName = m.playerOneName,
                    playerOneTeam = participantClubs[m.playerOneName] ?: m.playerOneClub,
                    playerTwoName = if (m.playerTwoName == "BYE" || m.playerTwoName == "TBD") null else m.playerTwoName,
                    playerTwoTeam = participantClubs[m.playerTwoName] ?: m.playerTwoClub,
                    scoreOne = m.scoreOne,
                    scoreTwo = m.scoreTwo,
                    isFinished = m.status == MatchStatus.FINISHED
                )
            }
            if (loaded.isNotEmpty()) {
                _fixtures.value = loaded
            }
        }
    }

    override fun setFixtures(fixtures: List<DrawFixture>) {
        _fixtures.value = fixtures
        syncToTournament(fixtures)
    }

    override fun addOrUpdateFixture(fixture: DrawFixture) {
        _fixtures.update { current ->
            val index = current.indexOfFirst { it.id == fixture.id }
            val updated = if (index >= 0) {
                current.toMutableList().apply { set(index, fixture) }
            } else {
                current + fixture
            }
            syncToTournament(updated)
            updated
        }
    }

    override fun updateFixtureScore(fixtureId: String, scoreOne: Int?, scoreTwo: Int?) {
        _fixtures.update { current ->
            val updated = current.map {
                if (it.id == fixtureId) {
                    it.copy(
                        scoreOne = scoreOne,
                        scoreTwo = scoreTwo,
                        isFinished = scoreOne != null && scoreTwo != null
                    )
                } else {
                    it
                }
            }
            syncToTournament(updated)
            updated
        }
    }

    private fun syncToTournament(fixtures: List<DrawFixture>) {
        if (fixtures.isEmpty()) return
        scope.launch {
            val totalPlayers = fixtures.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) }.distinct().size
            val isSevenPlayers = fixtures.size == 4 && fixtures.lastOrNull()?.playerTwoName == null

            val tournamentName = _customTournamentName ?: "بطولة قرعة ($totalPlayers لاعبين)"

            if (currentTournamentId == 0L) {
                val entity = TournamentEntity(
                    name = tournamentName,
                    type = TournamentType.DRAW_KNOCKOUT,
                    stage = TournamentStage.KNOCKOUT_ROUNDS,
                    playersProfileId = 0L,
                    groupsCount = 0,
                    qualifiersPerGroup = 0
                )
                _currentTournamentId = tournamentDao.insertTournament(entity)
            } else {
                val existing = tournamentDao.getTournamentById(currentTournamentId)
                if (existing != null) {
                    tournamentDao.updateTournament(
                        existing.copy(
                            name = tournamentName,
                            stage = existing.stage,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Sync Participants
            val participants = mutableListOf<TournamentParticipantEntity>()
            fixtures.forEach { f ->
                participants.add(
                    TournamentParticipantEntity(
                        tournamentId = currentTournamentId,
                        playerItemId = 0L,
                        playerName = f.playerOneName,
                        clubName = f.playerOneTeam,
                        groupIndex = 0
                    )
                )
                f.playerTwoName?.let { p2 ->
                    participants.add(
                        TournamentParticipantEntity(
                            tournamentId = currentTournamentId,
                            playerItemId = 0L,
                            playerName = p2,
                            clubName = f.playerTwoTeam,
                            groupIndex = 0
                        )
                    )
                }
            }
            tournamentDao.deleteParticipantsByTournamentId(currentTournamentId)
            tournamentDao.insertParticipants(participants.distinctBy { it.playerName })

            val distinctParticipants = participants.distinctBy { it.playerName }.map { it.toDomain() }
            val domainMatches = GenerateKnockoutBracketUseCase.generateBracketMatches(currentTournamentId, distinctParticipants)

            val matches = domainMatches.mapIndexed { idx, match ->
                val matchingFixture = fixtures.getOrNull(match.bracketMatchIndex?.minus(1) ?: idx)
                if (matchingFixture != null && matchingFixture.isFinished && match.roundIndex == 1) {
                    val winner = calculateWinner(matchingFixture.scoreOne, matchingFixture.scoreTwo, match.playerOneName, match.playerTwoName)
                    match.copy(
                        scoreOne = matchingFixture.scoreOne,
                        scoreTwo = matchingFixture.scoreTwo,
                        winnerName = winner,
                        status = MatchStatus.FINISHED
                    ).toEntity(currentTournamentId)
                } else {
                    match.toEntity(currentTournamentId)
                }
            }

            matchDao.deleteMatchesByTournamentId(currentTournamentId)
            matchDao.insertMatches(matches)
        }
    }

    private fun calculateWinner(s1: Int?, s2: Int?, p1: String, p2: String?): String? {
        if (s1 == null || s2 == null) return null
        return when {
            s1 > s2 -> p1
            s2 > s1 -> p2 ?: "TBD"
            else -> null
        }
    }

    override fun swapFixtures(index1: Int, index2: Int) {
        _fixtures.update { list ->
            if (index1 !in list.indices || index2 !in list.indices || index1 == index2) return@update list
            val mutable = list.toMutableList()
            val temp = mutable[index1]
            mutable[index1] = mutable[index2]
            mutable[index2] = temp
            val renumbered = mutable.mapIndexed { idx, item ->
                item.copy(id = "fixture_${idx + 1}", matchNumber = idx + 1)
            }
            syncToTournament(renumbered)
            renumbered
        }
    }

    override fun moveFixture(fromIndex: Int, toIndex: Int) {
        _fixtures.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return@update list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            val renumbered = mutable.mapIndexed { idx, f ->
                f.copy(id = "fixture_${idx + 1}", matchNumber = idx + 1)
            }
            syncToTournament(renumbered)
            renumbered
        }
    }

    override fun swapPlayers(fixtureId1: String, isSlot1A: Boolean, fixtureId2: String, isSlot1B: Boolean) {
        _fixtures.update { list ->
            val idx1 = list.indexOfFirst { it.id == fixtureId1 }
            val idx2 = list.indexOfFirst { it.id == fixtureId2 }
            if (idx1 < 0 || idx2 < 0) return@update list

            val mutable = list.toMutableList()
            val f1 = mutable[idx1]
            val f2 = mutable[idx2]

            if (idx1 == idx2) {
                val updated = f1.copy(
                    playerOneName = f1.playerTwoName ?: f1.playerOneName,
                    playerOneTeam = f1.playerTwoTeam,
                    playerTwoName = f1.playerOneName,
                    playerTwoTeam = f1.playerOneTeam
                )
                mutable[idx1] = updated
            } else {
                val p1Name = if (isSlot1A) f1.playerOneName else (f1.playerTwoName ?: "BYE")
                val p1Team = if (isSlot1A) f1.playerOneTeam else f1.playerTwoTeam

                val p2Name = if (isSlot1B) f2.playerOneName else (f2.playerTwoName ?: "BYE")
                val p2Team = if (isSlot1B) f2.playerOneTeam else f2.playerTwoTeam

                val updatedF1 = if (isSlot1A) {
                    f1.copy(playerOneName = p2Name, playerOneTeam = p2Team)
                } else {
                    f1.copy(playerTwoName = if (p2Name == "BYE") null else p2Name, playerTwoTeam = p2Team)
                }

                val updatedF2 = if (isSlot1B) {
                    f2.copy(playerOneName = p1Name, playerOneTeam = p1Team)
                } else {
                    f2.copy(playerTwoName = if (p1Name == "BYE") null else p1Name, playerTwoTeam = p1Team)
                }

                mutable[idx1] = updatedF1
                mutable[idx2] = updatedF2
            }

            syncToTournament(mutable)
            mutable
        }
    }

    override fun replacePlayer(oldPlayerName: String, newPlayerName: String, newClubName: String?) {
        _fixtures.update { list ->
            list.map { f ->
                var updated = f
                if (f.playerOneName == oldPlayerName) {
                    updated = updated.copy(
                        playerOneName = newPlayerName,
                        playerOneTeam = newClubName ?: updated.playerOneTeam
                    )
                }
                if (f.playerTwoName == oldPlayerName) {
                    updated = updated.copy(
                        playerTwoName = newPlayerName,
                        playerTwoTeam = newClubName ?: updated.playerTwoTeam
                    )
                }
                updated
            }
        }

        if (currentTournamentId > 0) {
            scope.launch {
                tournamentDao.replaceParticipant(currentTournamentId, oldPlayerName, newPlayerName, newClubName)
                matchDao.replacePlayerInMatches(currentTournamentId, oldPlayerName, newPlayerName, newClubName)
            }
        }
    }

    override suspend fun finalizeTournament(
        name: String,
        excludedPlayers: List<String>,
        excludedClubs: List<String>,
        excludedTeams: List<String>
    ): Long {
        _customTournamentName = name
        val list = _fixtures.value
        val totalPlayers = list.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) }.distinct().size

        val tournamentId = if (_currentTournamentId == 0L) {
            val entity = TournamentEntity(
                name = name,
                type = TournamentType.DRAW_KNOCKOUT,
                stage = TournamentStage.KNOCKOUT_ROUNDS,
                playersProfileId = 0L,
                groupsCount = 0,
                qualifiersPerGroup = 0
            )
            val newId = tournamentDao.insertTournament(entity)
            _currentTournamentId = newId
            newId
        } else {
            val existing = tournamentDao.getTournamentById(_currentTournamentId)
            if (existing != null) {
                tournamentDao.updateTournament(
                    existing.copy(
                        name = name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _currentTournamentId
        }

        // Save exclusions specifically for this tournament
        tournamentDao.deleteExclusionsByTournamentId(tournamentId)
        val exclusions = mutableListOf<TournamentExclusionEntity>()
        excludedPlayers.forEach { exclusions.add(TournamentExclusionEntity(tournamentId = tournamentId, category = "PLAYERS", itemLabel = it)) }
        excludedClubs.forEach { exclusions.add(TournamentExclusionEntity(tournamentId = tournamentId, category = "CLUBS", itemLabel = it)) }
        excludedTeams.forEach { exclusions.add(TournamentExclusionEntity(tournamentId = tournamentId, category = "NATIONAL_TEAMS", itemLabel = it)) }
        if (exclusions.isNotEmpty()) {
            tournamentDao.insertExclusions(exclusions)
        }

        return tournamentId
    }

    override fun clearFixtures() {
        _fixtures.value = emptyList()
        _currentTournamentId = 0L
        _customTournamentName = null
    }
}
