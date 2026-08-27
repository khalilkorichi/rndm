package com.rndm.app.domain.repository

import com.rndm.app.domain.model.DrawFixture
import kotlinx.coroutines.flow.StateFlow

interface DrawFixtureRepository {
    val fixtures: StateFlow<List<DrawFixture>>
    val currentTournamentId: Long
    val pendingNewPlayers: StateFlow<List<String>>
    fun setFixtures(fixtures: List<DrawFixture>)
    fun addOrUpdateFixture(fixture: DrawFixture)
    fun updateFixtureScore(fixtureId: String, scoreOne: Int?, scoreTwo: Int?)
    fun replacePlayer(oldPlayerName: String, newPlayerName: String, newClubName: String? = null)
    fun swapFixtures(index1: Int, index2: Int)
    fun moveFixture(fromIndex: Int, toIndex: Int)
    fun swapPlayers(fixtureId1: String, isSlot1A: Boolean, fixtureId2: String, isSlot1B: Boolean)
    fun loadTournamentFixtures(tournamentId: Long)
    fun queueNewPlayersForDraw(names: List<String>)
    fun consumePendingNewPlayers(): List<String>
    suspend fun finalizeTournament(
        name: String,
        excludedPlayers: List<String> = emptyList(),
        excludedClubs: List<String> = emptyList(),
        excludedTeams: List<String> = emptyList()
    ): Long
    fun clearFixtures()
}


