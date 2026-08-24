package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class InitializeGuestSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<String> {
        return authRepository.initializeGuestSession()
    }
}
