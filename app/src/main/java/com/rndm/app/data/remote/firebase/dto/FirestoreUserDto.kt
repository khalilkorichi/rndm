package com.rndm.app.data.remote.firebase.dto

import androidx.annotation.Keep

@Keep
data class FirestoreUserDto(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val role: String = "user",
    val createdAt: Long = 0L
)
