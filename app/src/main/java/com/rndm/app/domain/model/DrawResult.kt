package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DrawResult(
    val drawType: DrawType,
    val selectedItem: ProfileItem? = null,
    val pairings: List<MatchPairing> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
