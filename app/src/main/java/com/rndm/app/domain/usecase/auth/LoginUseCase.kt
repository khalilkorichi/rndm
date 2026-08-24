package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    suspend operator fun invoke(email: String, password: String): Result<String> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال البريد الإلكتروني"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صالح (name@domain.com)"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كلمة المرور"))
        }
        return authRepository.login(trimmedEmail, password)
    }
}

