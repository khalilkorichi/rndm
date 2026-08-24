package com.rndm.app.domain.repository

import com.rndm.app.domain.model.PlayerCareerStats
import com.rndm.app.domain.model.PlayerHeadToHead
import com.rndm.app.domain.model.PlayerLeaderboardItem
import com.rndm.app.domain.model.PlayerMatchRecord
import com.rndm.app.domain.model.PlayerQuickStats
import com.rndm.app.domain.model.PlayerTournamentParticipation
import kotlinx.coroutines.flow.Flow

interface PlayerProfileRepository {

    fun getPlayerCareerStats(playerName: String): Flow<PlayerCareerStats>

    fun getPlayerTournamentHistory(playerName: String): Flow<List<PlayerTournamentParticipation>>

    fun getPlayerMatchHistory(playerName: String): Flow<List<PlayerMatchRecord>>

    fun getPlayerHeadToHead(playerName: String): Flow<List<PlayerHeadToHead>>

    fun getAllPlayersLeaderboard(): Flow<List<PlayerLeaderboardItem>>

    fun getPlayersQuickStats(playerNames: List<String>): Flow<Map<String, PlayerQuickStats>>

    suspend fun savePlayerCustomProfile(
        name: String,
        nickname: String?,
        avatarIcon: String?,
        favoriteClub: String?,
        notes: String?
    )
}
