package com.rndm.app.domain.repository

import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserRole: Flow<UserRole>
    val currentUid: Flow<String?>
    val currentUserProfile: Flow<UserProfile?>
    suspend fun initializeGuestSession(): Result<String>
    suspend fun signUp(email: String, password: String, displayName: String): Result<String>
    suspend fun login(email: String, password: String): Result<String>
    suspend fun loginAdmin(email: String, password: String): Result<String>
    suspend fun logout(): Result<Unit>
    suspend fun logoutAdmin(): Result<Unit>
    suspend fun getCurrentRole(): UserRole
    suspend fun getCurrentUserProfile(): UserProfile?
    fun getAllUsers(): Flow<List<UserProfile>>
    suspend fun updateUserRole(targetUid: String, newRole: UserRole): Result<Unit>
    suspend fun promoteUserByEmail(email: String): Result<Unit>
}
