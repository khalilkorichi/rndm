package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginAdminUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("بيانات الدخول لا يمكن أن تكون فارغة"))
        }
        return authRepository.loginAdmin(cleanEmail, password)
    }
}

