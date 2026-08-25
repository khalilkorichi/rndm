package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ProfileGroup(
    val id: Long = 0,
    val name: String,
    val icon: String = "ic_folder",
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
