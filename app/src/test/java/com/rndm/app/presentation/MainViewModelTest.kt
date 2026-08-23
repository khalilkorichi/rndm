package com.rndm.app.presentation

import app.cash.turbine.test
import com.rndm.app.domain.repository.UserPreferencesRepository
import com.rndm.app.presentation.settings.ThemeMode
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
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<UserPreferencesRepository>()
    private val themeModeFlow = MutableStateFlow(ThemeMode.DARK)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.themeMode } returns themeModeFlow
        every { repository.getInitialThemeMode() } returns ThemeMode.DARK
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `themeMode initializes with initial cached value immediately`() = runTest {
        val viewModel = MainViewModel(repository)

        // Verifies the initial value is directly DARK before coroutine collections
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        viewModel.themeMode.test {
            val current = awaitItem()
            assertEquals(ThemeMode.DARK, current)

            themeModeFlow.value = ThemeMode.LIGHT
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(ThemeMode.LIGHT, updated)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
