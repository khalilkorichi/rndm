package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class PromoteUserByEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.promoteUserByEmail(email)
    }
}
