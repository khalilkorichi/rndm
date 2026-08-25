package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.rndm.app.data.local.entity.ProfileEntity
import com.rndm.app.data.local.entity.ProfileItemEntity
import kotlinx.coroutines.flow.Flow

data class ProfileWithItems(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val items: List<ProfileItemEntity>
)

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getAllProfilesWithItems(): Flow<List<ProfileWithItems>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileWithItems(id: Long): ProfileWithItems?

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY lastUsedAt DESC LIMIT 1")
    suspend fun getRecentProfile(): ProfileWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE profiles SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Transaction
    @Query("SELECT * FROM profiles WHERE groupId = :groupId ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getProfilesByGroupId(groupId: Long): Flow<List<ProfileWithItems>>

    @Query("UPDATE profiles SET groupId = :groupId WHERE id = :profileId")
    suspend fun updateProfileGroup(profileId: Long, groupId: Long?)
}

