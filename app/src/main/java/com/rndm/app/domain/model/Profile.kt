package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Profile(
    val id: Long = 0,
    val name: String,
    val type: ProfileType,
    val groupId: Long? = null,
    val items: List<ProfileItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
) {
    val activeItems: List<ProfileItem>
        get() = items.filter { it.isActive }

    val excludedItems: List<ProfileItem>
        get() = items.filter { !it.isActive }

    val activeCount: Int
        get() = activeItems.size

    val excludedCount: Int
        get() = excludedItems.size
}

