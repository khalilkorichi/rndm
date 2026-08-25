package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rndm.app.data.local.entity.ProfileGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileGroupDao {
    @Query("SELECT * FROM profile_groups ORDER BY createdAt ASC")
    fun getAllGroups(): Flow<List<ProfileGroupEntity>>

    @Query("SELECT * FROM profile_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ProfileGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ProfileGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ProfileGroupEntity)

    @Delete
    suspend fun deleteGroup(group: ProfileGroupEntity)

    @Query("DELETE FROM profile_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}
