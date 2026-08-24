package com.rndm.app.domain.model

data class UserProfile(
    val uid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)
