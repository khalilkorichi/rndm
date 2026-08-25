package com.rndm.app.data.mapper

import com.rndm.app.data.local.entity.ProfileItemEntity
import com.rndm.app.domain.model.ProfileItem

fun ProfileItemEntity.toDomain(): ProfileItem {
    return ProfileItem(
        id = id,
        profileId = profileId,
        label = label,
        order = order,
        isActive = isActive
    )
}

fun ProfileItem.toEntity(profileId: Long = this.profileId): ProfileItemEntity {
    return ProfileItemEntity(
        id = id,
        profileId = profileId,
        label = label,
        order = order,
        isActive = isActive
    )
}

