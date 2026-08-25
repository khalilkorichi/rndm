package com.rndm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rndm.app.data.local.entity.ProfileItemEntity

@Dao
interface ProfileItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ProfileItemEntity>)

    @Query("DELETE FROM profile_items WHERE profileId = :profileId")
    suspend fun deleteItemsByProfileId(profileId: Long)

    @Query("SELECT * FROM profile_items WHERE profileId = :profileId ORDER BY `order` ASC LIMIT :limit OFFSET :offset")
    suspend fun getItemsByProfileId(profileId: Long, limit: Int = 100, offset: Int = 0): List<ProfileItemEntity>

    @Query("UPDATE profile_items SET isActive = :isActive WHERE id = :itemId")
    suspend fun updateItemActiveState(itemId: Long, isActive: Boolean)

    @Query("UPDATE profile_items SET isActive = :isActive WHERE profileId = :profileId AND label = :label")
    suspend fun updateItemActiveStateByLabel(profileId: Long, label: String, isActive: Boolean)

    @Query("UPDATE profile_items SET isActive = 1 WHERE profileId = :profileId")
    suspend fun resetAllItemsToActive(profileId: Long)
}

