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

    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE playerOneName = :playerName OR playerTwoName = :playerName ORDER BY id DESC")
    fun getMatchesForPlayer(playerName: String): Flow<List<MatchEntity>>

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

    @Query("UPDATE matches SET playerOneName = :newPlayerName, playerOneClub = CASE WHEN :newClubName IS NOT NULL THEN :newClubName ELSE playerOneClub END WHERE tournamentId = :tournamentId AND playerOneName = :oldPlayerName")
    suspend fun replacePlayerOne(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?)

    @Query("UPDATE matches SET playerTwoName = :newPlayerName, playerTwoClub = CASE WHEN :newClubName IS NOT NULL THEN :newClubName ELSE playerTwoClub END WHERE tournamentId = :tournamentId AND playerTwoName = :oldPlayerName")
    suspend fun replacePlayerTwo(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?)

    @Query("UPDATE matches SET winnerName = :newPlayerName WHERE tournamentId = :tournamentId AND winnerName = :oldPlayerName")
    suspend fun replaceWinner(tournamentId: Long, oldPlayerName: String, newPlayerName: String)

    @Transaction
    suspend fun replacePlayerInMatches(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?) {
        replacePlayerOne(tournamentId, oldPlayerName, newPlayerName, newClubName)
        replacePlayerTwo(tournamentId, oldPlayerName, newPlayerName, newClubName)
        replaceWinner(tournamentId, oldPlayerName, newPlayerName)
    }

    @Transaction
    suspend fun swapMatchOrder(matchId1: Long, matchId2: Long) {
        val m1 = getMatchById(matchId1) ?: return
        val m2 = getMatchById(matchId2) ?: return
        updateMatch(m1.copy(roundIndex = m2.roundIndex, bracketMatchIndex = m2.bracketMatchIndex))
        updateMatch(m2.copy(roundIndex = m1.roundIndex, bracketMatchIndex = m1.bracketMatchIndex))
    }

    @Transaction
    suspend fun swapPlayersInMatches(matchId1: Long, isSlot1A: Boolean, matchId2: Long, isSlot1B: Boolean) {
        val m1 = getMatchById(matchId1) ?: return
        val m2 = getMatchById(matchId2) ?: return

        if (matchId1 == matchId2) {
            val updated = m1.copy(
                playerOneName = m1.playerTwoName ?: "BYE",
                playerOneClub = m1.playerTwoClub,
                playerTwoName = m1.playerOneName,
                playerTwoClub = m1.playerOneClub,
                isPlayerOneLuckyLoser = m1.isPlayerTwoLuckyLoser,
                isPlayerTwoLuckyLoser = m1.isPlayerOneLuckyLoser
            )
            updateMatch(updated)
            return
        }

        val nameA = if (isSlot1A) m1.playerOneName else (m1.playerTwoName ?: "BYE")
        val clubA = if (isSlot1A) m1.playerOneClub else m1.playerTwoClub
        val luckyA = if (isSlot1A) m1.isPlayerOneLuckyLoser else m1.isPlayerTwoLuckyLoser

        val nameB = if (isSlot1B) m2.playerOneName else (m2.playerTwoName ?: "BYE")
        val clubB = if (isSlot1B) m2.playerOneClub else m2.playerTwoClub
        val luckyB = if (isSlot1B) m2.isPlayerOneLuckyLoser else m2.isPlayerTwoLuckyLoser

        val updatedM1 = if (isSlot1A) {
            m1.copy(playerOneName = nameB, playerOneClub = clubB, isPlayerOneLuckyLoser = luckyB)
        } else {
            m1.copy(playerTwoName = nameB, playerTwoClub = clubB, isPlayerTwoLuckyLoser = luckyB)
        }

        val updatedM2 = if (isSlot1B) {
            m2.copy(playerOneName = nameA, playerOneClub = clubA, isPlayerOneLuckyLoser = luckyA)
        } else {
            m2.copy(playerTwoName = nameA, playerTwoClub = clubA, isPlayerTwoLuckyLoser = luckyA)
        }

        updateMatch(updatedM1)
        updateMatch(updatedM2)
    }
}
