package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_groups")
data class ProfileGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "ic_folder",
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
