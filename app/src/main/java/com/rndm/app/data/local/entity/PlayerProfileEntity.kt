package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "player_profiles",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class PlayerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val nickname: String? = null,
    val avatarIcon: String? = null,
    val favoriteClub: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
