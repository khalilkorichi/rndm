package com.rndm.app.presentation.admin

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.usecase.auth.LoginUseCase
import com.rndm.app.domain.usecase.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminLoginUiState())
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()

    fun toggleMode(isSignUp: Boolean) {
        _uiState.update { it.copy(isSignUpMode = isSignUp, errorMessage = null) }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val cleanEmail = state.email.trim().lowercase()

        if (cleanEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال البريد الإلكتروني") }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال صيغة بريد إلكتروني صالحة (example@domain.com)") }
            return
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال كلمة المرور") }
            return
        }

        if (state.isSignUpMode) {
            if (state.password.length < 6) {
                _uiState.update { it.copy(errorMessage = "كلمة المرور يجب أن لا تقل عن 6 خانات") }
                return
            }
            if (state.password != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "كلمة المرور وتأكيدها غير متطابقين") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = signUpUseCase(
                    email = cleanEmail,
                    password = state.password,
                    displayName = state.displayName
                )
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.localizedMessage ?: "فشل إنشاء الحساب، يرجى التحقق من البيانات"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = loginUseCase(cleanEmail, state.password)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "بيانات الدخول غير صحيحة، يرجى التأكد من البريد وكلمة المرور"
                        )
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AdminLoginUiState()
    }
}
