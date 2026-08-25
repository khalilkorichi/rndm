package com.rndm.app.presentation.settings

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSoundEnabled: Boolean = true,
    val isMatchReminderEnabled: Boolean = true,
    val isDrawAlertsEnabled: Boolean = true,
    val isTournamentUpdatesEnabled: Boolean = true,
    val userRole: UserRole = UserRole.GUEST,
    val currentUserProfile: UserProfile? = null,
    val isAdminLoginDialogOpen: Boolean = false,
    val isUserManagementDialogOpen: Boolean = false,
    val isRoleInfoDialogOpen: Boolean = false,
    val usersList: List<UserProfile> = emptyList(),
    val isUserActionLoading: Boolean = false,
    val userActionMessage: String? = null,
    val appVersion: String = "1.0.0"
)
