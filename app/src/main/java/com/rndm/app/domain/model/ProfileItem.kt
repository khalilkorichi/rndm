package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ProfileItem(
    val id: Long = 0,
    val profileId: Long = 0,
    val label: String,
    val order: Int = 0
)
