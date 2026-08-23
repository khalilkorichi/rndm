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
    fun loadTournamentFixtures(tournamentId: Long)
    fun queueNewPlayersForDraw(names: List<String>)
    fun consumePendingNewPlayers(): List<String>
    fun clearFixtures()
}


