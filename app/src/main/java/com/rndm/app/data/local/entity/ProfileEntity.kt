package com.rndm.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    indices = [
        Index(value = ["lastUsedAt", "createdAt"]),
        Index(value = ["groupId"])
    ]
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val groupId: Long? = null,
    val createdAt: Long,
    val lastUsedAt: Long?
)

