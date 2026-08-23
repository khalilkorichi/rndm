package com.rndm.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProfileType {
    PLAYERS,
    CLUBS,
    NATIONAL_TEAMS
}
