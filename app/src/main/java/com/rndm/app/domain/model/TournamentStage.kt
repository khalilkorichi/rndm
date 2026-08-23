package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class TournamentStage {
    GROUPS,
    PROMOTION_PLAYOFF,
    KNOCKOUT_ROUNDS,
    COMPLETED
}
