package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class TournamentType {
    GROUPS_KNOCKOUT,
    DRAW_KNOCKOUT,
    LEAGUE,
    TRIANGLE_SOLO,
    KNOCKOUT_ONLY
}
