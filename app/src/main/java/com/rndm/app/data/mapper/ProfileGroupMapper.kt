package com.rndm.app.data.mapper

import com.rndm.app.data.local.entity.ProfileGroupEntity
import com.rndm.app.domain.model.ProfileGroup

fun ProfileGroupEntity.toDomain(): ProfileGroup {
    return ProfileGroup(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        createdAt = createdAt
    )
}

fun ProfileGroup.toEntity(): ProfileGroupEntity {
    return ProfileGroupEntity(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        createdAt = createdAt
    )
}
