package com.rndm.app.presentation.settings

import app.cash.turbine.test
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.UserPreferencesRepository
import com.rndm.app.domain.usecase.auth.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<UserPreferencesRepository>()
    private val getCurrentUserRoleUseCase = mockk<GetCurrentUserRoleUseCase>()
    private val getCurrentUserProfileUseCase = mockk<GetCurrentUserProfileUseCase>()
    private val logoutAdminUseCase = mockk<LogoutAdminUseCase>()
    private val getAllUsersUseCase = mockk<GetAllUsersUseCase>()
    private val updateUserRoleUseCase = mockk<UpdateUserRoleUseCase>()
    private val promoteUserByEmailUseCase = mockk<PromoteUserByEmailUseCase>()

    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val isSoundFlow = MutableStateFlow(true)
    private val isMatchReminderFlow = MutableStateFlow(true)
    private val isDrawAlertsFlow = MutableStateFlow(true)
    private val roleFlow = MutableStateFlow(UserRole.GUEST)
    private val profileFlow = MutableStateFlow<UserProfile?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getInitialThemeMode() } returns ThemeMode.SYSTEM
        every { repository.themeMode } returns themeModeFlow
        every { repository.isSoundEnabled } returns isSoundFlow
        every { repository.isMatchReminderEnabled } returns isMatchReminderFlow
        every { repository.isDrawAlertsEnabled } returns isDrawAlertsFlow
        every { getCurrentUserRoleUseCase() } returns roleFlow
        every { getCurrentUserRoleUseCase.getFastRole() } returns UserRole.GUEST
        every { getCurrentUserProfileUseCase() } returns profileFlow
        every { getCurrentUserProfileUseCase.getFastProfile() } returns null
        every { getAllUsersUseCase() } returns flowOf(emptyList())
        coEvery { logoutAdminUseCase() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state immediately has fast role and profile without delay`() = runTest {
        val testProfile = UserProfile(
            uid = "user_1",
            email = "user@rndm.app",
            username = "user",
            displayName = "User One",
            role = UserRole.USER
        )
        every { getCurrentUserRoleUseCase.getFastRole() } returns UserRole.USER
        every { getCurrentUserProfileUseCase.getFastProfile() } returns testProfile

        val viewModel = SettingsViewModel(
            userPreferencesRepository = repository,
            getCurrentUserRoleUseCase = getCurrentUserRoleUseCase,
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            logoutAdminUseCase = logoutAdminUseCase,
            getAllUsersUseCase = getAllUsersUseCase,
            updateUserRoleUseCase = updateUserRoleUseCase,
            promoteUserByEmailUseCase = promoteUserByEmailUseCase
        )

        assertEquals(UserRole.USER, viewModel.uiState.value.userRole)
        assertEquals(testProfile, viewModel.uiState.value.currentUserProfile)
    }

    @Test
    fun `changing theme mode updates repository and emits new UiState`() = runTest {
        coEvery { repository.setThemeMode(ThemeMode.DARK) } coAnswers {
            themeModeFlow.value = ThemeMode.DARK
        }

        val viewModel = SettingsViewModel(
            userPreferencesRepository = repository,
            getCurrentUserRoleUseCase = getCurrentUserRoleUseCase,
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            logoutAdminUseCase = logoutAdminUseCase,
            getAllUsersUseCase = getAllUsersUseCase,
            updateUserRoleUseCase = updateUserRoleUseCase,
            promoteUserByEmailUseCase = promoteUserByEmailUseCase
        )

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val initial = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initial.themeMode)

            viewModel.onThemeModeChanged(ThemeMode.DARK)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(ThemeMode.DARK, updated.themeMode)

            coVerify(exactly = 1) { repository.setThemeMode(ThemeMode.DARK) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
