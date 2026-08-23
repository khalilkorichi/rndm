package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.domain.model.MatchStage
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY stage ASC, roundIndex ASC, id ASC")
    fun getMatchesByTournamentId(tournamentId: Long): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId AND stage = :stage ORDER BY roundIndex ASC, id ASC")
    fun getMatchesByTournamentAndStage(tournamentId: Long, stage: MatchStage): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId LIMIT :limit OFFSET :offset")
    suspend fun getMatchesList(tournamentId: Long, limit: Int = 100, offset: Int = 0): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId AND stage != 'GROUP_STAGE'")
    suspend fun deleteKnockoutMatches(tournamentId: Long)

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesByTournamentId(tournamentId: Long)

    @Query("UPDATE matches SET playerOneName = :newPlayerName, playerOneClub = COALESCE(:newClubName, playerOneClub) WHERE tournamentId = :tournamentId AND playerOneName = :oldPlayerName")
    suspend fun replacePlayerOne(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?)

    @Query("UPDATE matches SET playerTwoName = :newPlayerName, playerTwoClub = COALESCE(:newClubName, playerTwoClub) WHERE tournamentId = :tournamentId AND playerTwoName = :oldPlayerName")
    suspend fun replacePlayerTwo(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?)

    @Query("UPDATE matches SET winnerName = :newPlayerName WHERE tournamentId = :tournamentId AND winnerName = :oldPlayerName")
    suspend fun replaceWinner(tournamentId: Long, oldPlayerName: String, newPlayerName: String)

    @Transaction
    suspend fun replacePlayerInMatches(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?) {
        replacePlayerOne(tournamentId, oldPlayerName, newPlayerName, newClubName)
        replacePlayerTwo(tournamentId, oldPlayerName, newPlayerName, newClubName)
        replaceWinner(tournamentId, oldPlayerName, newPlayerName)
    }
}
