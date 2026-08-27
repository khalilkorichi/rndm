package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserProfile?> {
        return authRepository.currentUserProfile
    }

    suspend fun getSync(): UserProfile? {
        return authRepository.getCurrentUserProfile()
    }

    fun getFastProfile(): UserProfile? {
        return authRepository.getFastUserProfile()
    }
}
