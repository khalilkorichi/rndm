package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MatchPairing(
    val playerOne: ProfileItem,
    val playerTwo: ProfileItem? = null // null indicates a 'Bye' for odd player counts
)
