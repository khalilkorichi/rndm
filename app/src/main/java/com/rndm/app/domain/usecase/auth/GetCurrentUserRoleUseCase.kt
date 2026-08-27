package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserRoleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserRole> {
        return authRepository.currentUserRole
    }

    suspend fun getSyncRole(): UserRole {
        return authRepository.getCurrentRole()
    }

    fun getFastRole(): UserRole {
        return authRepository.getFastRole()
    }
}
