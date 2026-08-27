package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class FirestoreTournamentDto(
    val id: String = "",
    val name: String = "",
    val type: String = "GROUPS_KNOCKOUT",
    val stage: String = "GROUPS",
    val status: String = "ACTIVE",
    val hostUid: String = "",
    val memberIds: List<String> = emptyList(),
    val editorIds: List<String> = emptyList(),
    val shareCode: String = "",
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
    @get:PropertyName("isArchived") @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val version: Long = 1L
)
