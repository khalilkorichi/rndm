package com.rndm.app.presentation.settings

import app.cash.turbine.test
import com.rndm.app.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val isSoundFlow = MutableStateFlow(true)
    private val isMatchReminderFlow = MutableStateFlow(true)
    private val isDrawAlertsFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.themeMode } returns themeModeFlow
        every { repository.isSoundEnabled } returns isSoundFlow
        every { repository.isMatchReminderEnabled } returns isMatchReminderFlow
        every { repository.isDrawAlertsEnabled } returns isDrawAlertsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `changing theme mode updates repository and emits new UiState`() = runTest {
        coEvery { repository.setThemeMode(ThemeMode.DARK) } coAnswers {
            themeModeFlow.value = ThemeMode.DARK
        }

        val viewModel = SettingsViewModel(repository)

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
