package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rndm.app.data.local.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {

    @Query("SELECT * FROM player_profiles WHERE name = :name LIMIT 1")
    fun getPlayerProfileByName(name: String): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profiles WHERE name = :name LIMIT 1")
    suspend fun getPlayerProfileByNameSync(name: String): PlayerProfileEntity?

    @Query("SELECT * FROM player_profiles ORDER BY updatedAt DESC")
    fun getAllPlayerProfiles(): Flow<List<PlayerProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfileEntity): Long

    @Query("DELETE FROM player_profiles WHERE name = :name")
    suspend fun deleteProfileByName(name: String)
}
