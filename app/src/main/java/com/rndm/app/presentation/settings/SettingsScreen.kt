package com.rndm.app.presentation.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.BuildConfig
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.domain.model.UserRole
import com.rndm.app.presentation.admin.AdminLoginDialog
import com.rndm.app.presentation.admin.AdminRequestsTabContent
import com.rndm.app.presentation.admin.AdminRequestsViewModel
import com.rndm.app.presentation.admin.components.LoginSuccessDialog
import com.rndm.app.presentation.settings.components.NotificationSettingCard
import com.rndm.app.presentation.settings.components.RoleManagementCard
import com.rndm.app.presentation.settings.components.SoundSettingCard
import com.rndm.app.presentation.settings.components.ThemeSettingCard
import com.rndm.app.presentation.update.UpdateUiState
import com.rndm.app.presentation.update.UpdatesViewModel
import com.rndm.app.presentation.update.components.UpdatesTabContent

enum class SettingsTab(val title: String, val icon: ImageVector) {
    GENERAL("عام", Icons.Outlined.Settings),
    ADMIN_REQUESTS("الطلبات", Icons.Default.AdminPanelSettings),
    UPDATES("التحديثات", Icons.Default.SystemUpdate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    updatesViewModel: UpdatesViewModel = hiltViewModel(),
    adminRequestsViewModel: AdminRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val adminUiState by adminRequestsViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }

    val availableTabs = remember(uiState.userRole) {
        if (uiState.userRole == UserRole.ADMIN) {
            listOf(SettingsTab.GENERAL, SettingsTab.ADMIN_REQUESTS, SettingsTab.UPDATES)
        } else {
            listOf(SettingsTab.GENERAL, SettingsTab.UPDATES)
        }
    }

    LaunchedEffect(uiState.userRole) {
        if (uiState.userRole != UserRole.ADMIN && selectedTab == SettingsTab.ADMIN_REQUESTS) {
            selectedTab = SettingsTab.GENERAL
        }
    }

    val hasUpdateNotification = remember(updateUiState) {
        updateUiState is UpdateUiState.UpdateAvailable ||
        updateUiState is UpdateUiState.Downloading ||
        updateUiState is UpdateUiState.ReadyToInstall
    }

    val pendingRequestsCount = adminUiState.pendingCount

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "الإعدادات",
                titleIcon = painterResource(id = R.drawable.ic_settings_filled)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Tab Selector
            SettingsTabSelector(
                tabs = availableTabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                hasUpdateBadge = hasUpdateNotification,
                pendingRequestsCount = pendingRequestsCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content per Tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "settings_tab_animation",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    SettingsTab.GENERAL -> {
                        GeneralSettingsContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToUpdatesTab = { selectedTab = SettingsTab.UPDATES }
                        )
                    }
                    SettingsTab.ADMIN_REQUESTS -> {
                        AdminRequestsTabContent(
                            viewModel = adminRequestsViewModel
                        )
                    }
                    SettingsTab.UPDATES -> {
                        UpdatesTabContent(
                            viewModel = updatesViewModel
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAdminLoginDialogOpen) {
        AdminLoginDialog(
            onDismissRequest = viewModel::onDismissAdminLoginDialog,
            onLoginSuccess = viewModel::onDismissAdminLoginDialog
        )
    }

    if (uiState.isRoleInfoDialogOpen) {
        LoginSuccessDialog(
            userRole = uiState.userRole,
            userProfile = uiState.currentUserProfile,
            onContinue = viewModel::onDismissRoleInfoDialog
        )
    }

    if (uiState.isUserManagementDialogOpen) {
        com.rndm.app.presentation.settings.components.UserManagementDialog(
            users = uiState.usersList,
            currentUserUid = uiState.currentUserProfile?.uid,
            isLoading = uiState.isUserActionLoading,
            actionMessage = uiState.userActionMessage,
            onPromoteUser = viewModel::onPromoteUser,
            onDemoteUser = viewModel::onDemoteUser,
            onPromoteByEmail = viewModel::onPromoteByEmail,
            onDismissRequest = viewModel::onDismissUserManagementDialog
        )
    }
}

@Composable
private fun SettingsTabSelector(
    tabs: List<SettingsTab>,
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    hasUpdateBadge: Boolean,
    pendingRequestsCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (tab == SettingsTab.ADMIN_REQUESTS && pendingRequestsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) Color.White else com.rndm.app.core.theme.UpdateWarningAmber,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$pendingRequestsCount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (tab == SettingsTab.UPDATES && hasUpdateBadge) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else com.rndm.app.core.theme.UpdateSuccessGreen)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateToUpdatesTab: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RoleManagementCard(
                userRole = uiState.userRole,
                currentUserProfile = uiState.currentUserProfile,
                onOpenAdminLogin = viewModel::onOpenAdminLoginDialog,
                onOpenUserManagement = viewModel::onOpenUserManagementDialog,
                onViewRoleInfo = viewModel::onOpenRoleInfoDialog,
                onLogoutAdmin = viewModel::onLogoutAdmin
            )
        }

        item {
            ThemeSettingCard(
                themeMode = uiState.themeMode,
                onThemeModeSelected = viewModel::onThemeModeChanged
            )
        }

        item {
            SoundSettingCard(
                isSoundEnabled = uiState.isSoundEnabled,
                onSoundToggled = viewModel::onSoundToggle
            )
        }

        item {
            NotificationSettingCard(
                isMatchReminderEnabled = uiState.isMatchReminderEnabled,
                onToggleMatchReminder = viewModel::onMatchReminderToggle,
                isDrawAlertsEnabled = uiState.isDrawAlertsEnabled,
                onToggleDrawAlerts = viewModel::onDrawAlertsToggle
            )
        }

        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "حول التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RNDM — تطبيق القرعة الرياضية وإقران المباريات وإدارة البطولات",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToUpdatesTab() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الإصدار المثبت",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اضغط للتحقق من وجود تحديثات جديدة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
