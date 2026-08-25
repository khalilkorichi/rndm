package com.rndm.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.UserPreferencesRepository
import com.rndm.app.domain.usecase.auth.GetAllUsersUseCase
import com.rndm.app.domain.usecase.auth.GetCurrentUserProfileUseCase
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.auth.LogoutAdminUseCase
import com.rndm.app.domain.usecase.auth.PromoteUserByEmailUseCase
import com.rndm.app.domain.usecase.auth.UpdateUserRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val logoutAdminUseCase: LogoutAdminUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase,
    private val promoteUserByEmailUseCase: PromoteUserByEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var usersJob: Job? = null

    init {
        observePreferencesAndRole()
    }

    private fun observePreferencesAndRole() {
        viewModelScope.launch {
            val prefsFlow = combine(
                userPreferencesRepository.themeMode,
                userPreferencesRepository.isSoundEnabled,
                userPreferencesRepository.isMatchReminderEnabled,
                userPreferencesRepository.isDrawAlertsEnabled
            ) { themeMode, isSound, isMatch, isDraw ->
                arrayOf(themeMode, isSound, isMatch, isDraw)
            }

            combine(
                prefsFlow,
                getCurrentUserRoleUseCase(),
                getCurrentUserProfileUseCase()
            ) { prefs, role, profile ->
                _uiState.value.copy(
                    themeMode = prefs[0] as ThemeMode,
                    isSoundEnabled = prefs[1] as Boolean,
                    isMatchReminderEnabled = prefs[2] as Boolean,
                    isDrawAlertsEnabled = prefs[3] as Boolean,
                    userRole = role,
                    currentUserProfile = profile
                )
            }.collect { newState ->
                _uiState.value = newState
                if (newState.userRole == UserRole.ADMIN && usersJob == null) {
                    observeUsersList()
                } else if (newState.userRole != UserRole.ADMIN) {
                    usersJob?.cancel()
                    usersJob = null
                }
            }
        }
    }

    private fun observeUsersList() {
        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            getAllUsersUseCase().collect { list ->
                _uiState.update { it.copy(usersList = list) }
            }
        }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun onSoundToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(enabled)
        }
    }

    fun onMatchReminderToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMatchReminderEnabled(enabled)
        }
    }

    fun onDrawAlertsToggle(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDrawAlertsEnabled(enabled)
        }
    }

    fun onOpenAdminLoginDialog() {
        _uiState.update { it.copy(isAdminLoginDialogOpen = true) }
    }

    fun onDismissAdminLoginDialog() {
        _uiState.update { it.copy(isAdminLoginDialogOpen = false) }
    }

    fun onOpenRoleInfoDialog() {
        _uiState.update { it.copy(isRoleInfoDialogOpen = true) }
    }

    fun onDismissRoleInfoDialog() {
        _uiState.update { it.copy(isRoleInfoDialogOpen = false) }
    }

    fun onOpenUserManagementDialog() {
        _uiState.update { it.copy(isUserManagementDialogOpen = true, userActionMessage = null) }
        observeUsersList()
    }

    fun onDismissUserManagementDialog() {
        _uiState.update { it.copy(isUserManagementDialogOpen = false, userActionMessage = null) }
    }

    fun onPromoteUser(targetUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserActionLoading = true, userActionMessage = null) }
            val result = updateUserRoleUseCase(targetUid, UserRole.ADMIN)
            if (result.isSuccess) {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "تمت ترقية المستخدم إلى مدير بنجاح") }
            } else {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "فشل تحديث الصلاحية: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun onDemoteUser(targetUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserActionLoading = true, userActionMessage = null) }
            val result = updateUserRoleUseCase(targetUid, UserRole.USER)
            if (result.isSuccess) {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "تم تغيير الصلاحية إلى مستخدم عادي بنجاح") }
            } else {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "فشل تحديث الصلاحية: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun onPromoteByEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserActionLoading = true, userActionMessage = null) }
            val result = promoteUserByEmailUseCase(email)
            if (result.isSuccess) {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "تمت ترقية الحساب ($email) كمدير بنجاح") }
            } else {
                _uiState.update { it.copy(isUserActionLoading = false, userActionMessage = "فشلت الترقية: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun onLogoutAdmin() {
        viewModelScope.launch {
            logoutAdminUseCase()
        }
    }
}
