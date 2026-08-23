package com.rndm.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DrawType {
    WHEEL,
    FLIP_CARDS,
    SPIN_LIST,
    ROUND_ROBIN
}
