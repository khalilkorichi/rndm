package com.rndm.app.presentation.admin

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole

@Immutable
data class AdminLoginUiState(
    val isSignUpMode: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val loggedInRole: UserRole? = null,
    val loggedInProfile: UserProfile? = null
)

