package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TournamentGroup(
    val groupIndex: Int,
    val groupName: String,
    val standings: List<GroupStanding> = emptyList(),
    val matches: List<Match> = emptyList()
)
