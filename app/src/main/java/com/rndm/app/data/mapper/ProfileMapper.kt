package com.rndm.app.data.mapper

import com.rndm.app.data.local.dao.ProfileWithItems
import com.rndm.app.data.local.entity.ProfileEntity
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileType

fun ProfileWithItems.toDomain(): Profile {
    val profileType = try {
        ProfileType.valueOf(profile.type)
    } catch (e: IllegalArgumentException) {
        ProfileType.PLAYERS
    }
    return Profile(
        id = profile.id,
        name = profile.name,
        type = profileType,
        items = items.sortedBy { it.order }.map { it.toDomain() },
        createdAt = profile.createdAt,
        lastUsedAt = profile.lastUsedAt
    )
}

fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        type = type.name,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
