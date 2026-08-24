package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments ORDER BY updatedAt DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedTournaments(): Flow<List<TournamentEntity>>

    @Query("UPDATE tournaments SET isArchived = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun archiveTournament(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tournaments SET isArchived = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun unarchiveTournament(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Long): TournamentEntity?

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournamentByIdFlow(id: Long): Flow<TournamentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<TournamentParticipantEntity>)

    @Query("SELECT * FROM tournament_participants")
    fun getAllParticipants(): Flow<List<TournamentParticipantEntity>>

    @Query("SELECT * FROM tournament_participants WHERE playerName = :playerName")
    fun getParticipantsByPlayerName(playerName: String): Flow<List<TournamentParticipantEntity>>

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId ORDER BY groupIndex ASC, id ASC")
    fun getParticipantsByTournamentId(tournamentId: Long): Flow<List<TournamentParticipantEntity>>

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId LIMIT :limit OFFSET :offset")
    suspend fun getParticipantsList(tournamentId: Long, limit: Int = 100, offset: Int = 0): List<TournamentParticipantEntity>

    @Query("DELETE FROM tournament_participants WHERE tournamentId = :tournamentId")
    suspend fun deleteParticipantsByTournamentId(tournamentId: Long)

    @Query("UPDATE tournament_participants SET playerName = :newPlayerName, clubName = CASE WHEN :newClubName IS NOT NULL THEN :newClubName ELSE clubName END WHERE tournamentId = :tournamentId AND playerName = :oldPlayerName")
    suspend fun replaceParticipant(tournamentId: Long, oldPlayerName: String, newPlayerName: String, newClubName: String?)

    @Query("UPDATE tournament_participants SET groupIndex = :newGroupIndex WHERE tournamentId = :tournamentId AND playerName = :playerName")
    suspend fun updateParticipantGroup(tournamentId: Long, playerName: String, newGroupIndex: Int)
}
