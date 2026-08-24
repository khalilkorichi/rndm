package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<UserProfile>> {
        return authRepository.getAllUsers()
    }
}
