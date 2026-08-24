package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Result<String> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال البريد الإلكتروني"))
        }
        if (!emailRegex.matches(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صالح بالصيغة المعروفة (مثل name@domain.com)"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("كلمة المرور يجب أن تتكون من 6 خانات على الأقل"))
        }
        return authRepository.signUp(trimmedEmail, password, displayName.trim())
    }
}

