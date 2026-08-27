package com.rndm.app.presentation.draw.flipcards

import androidx.lifecycle.SavedStateHandle
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.data.repository.DrawFixtureRepositoryImpl
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateItemActiveStateUseCase
import com.rndm.app.presentation.draw.wheel.DrawCategory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlipCardDrawViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAllProfilesUseCase = mockk<GetAllProfilesUseCase>()
    private val getProfileByIdUseCase = mockk<GetProfileByIdUseCase>()
    private val updateItemActiveStateUseCase = mockk<UpdateItemActiveStateUseCase>(relaxed = true)
    private lateinit var fixtureRepository: DrawFixtureRepositoryImpl
    private val randomProvider = mockk<RandomProvider>()

    private fun createViewModel(profileId: Long = 1L): FlipCardDrawViewModel {
        return FlipCardDrawViewModel(
            getAllProfilesUseCase = getAllProfilesUseCase,
            getProfileByIdUseCase = getProfileByIdUseCase,
            updateItemActiveStateUseCase = updateItemActiveStateUseCase,
            drawFixtureRepository = fixtureRepository,
            randomProvider = randomProvider,
            savedStateHandle = SavedStateHandle(mapOf("profileId" to profileId))
        )
    }

    private val playersProfile = Profile(
        id = 1L,
        name = "الأصدقاء",
        type = ProfileType.PLAYERS,
        items = listOf(
            ProfileItem(id = 1, profileId = 1, label = "خليل", order = 0),
            ProfileItem(id = 2, profileId = 1, label = "عبدو", order = 1),
            ProfileItem(id = 3, profileId = 1, label = "ديدو", order = 2),
            ProfileItem(id = 4, profileId = 1, label = "أحمد", order = 3)
        )
    )

    private val clubsProfile = Profile(
        id = 2L,
        name = "الأندية الأوروبية",
        type = ProfileType.CLUBS,
        items = listOf(
            ProfileItem(id = 10, profileId = 2, label = "ريال مدريد", order = 0),
            ProfileItem(id = 11, profileId = 2, label = "برشلونة", order = 1),
            ProfileItem(id = 12, profileId = 2, label = "ليفربول", order = 2),
            ProfileItem(id = 13, profileId = 2, label = "بايرن ميونخ", order = 3)
        )
    )

    private val teamsProfile = Profile(
        id = 3L,
        name = "المنتخبات العالمية",
        type = ProfileType.NATIONAL_TEAMS,
        items = listOf(
            ProfileItem(id = 20, profileId = 3, label = "البرازيل", order = 0),
            ProfileItem(id = 21, profileId = 3, label = "الأرجنتين", order = 1)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fixtureRepository = DrawFixtureRepositoryImpl(
            tournamentDao = mockk(relaxed = true),
            matchDao = mockk(relaxed = true),
            evaluateBestLosersUseCase = com.rndm.app.domain.usecase.tournament.EvaluateBestLosersUseCase(),
            ioDispatcher = testDispatcher
        )
        every { getAllProfilesUseCase() } returns flowOf(listOf(playersProfile, clubsProfile, teamsProfile))
        coEvery { getProfileByIdUseCase(1L) } returns playersProfile
        coEvery { getProfileByIdUseCase(2L) } returns clubsProfile
        coEvery { getProfileByIdUseCase(3L) } returns teamsProfile
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `card click reveals item and assigns player to match after celebration delay`() = runTest {
        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.fixtures.size)

        // Click card at index 0 ("خليل")
        viewModel.onCardClick(0)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isRevealing)
        assertEquals(0, viewModel.uiState.value.flippedCardIndex)
        assertEquals("خليل", viewModel.uiState.value.drawResult?.selectedItem?.label)

        // Advance celebration delay (1200ms)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        // Card consumed and assigned to match 1
        assertFalse(viewModel.uiState.value.isRevealing)
        assertEquals(-1, viewModel.uiState.value.flippedCardIndex)
        assertEquals(3, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(1, viewModel.uiState.value.fixtures.size)
        assertEquals("خليل", viewModel.uiState.value.fixtures[0].playerOneName)
        assertEquals(null, viewModel.uiState.value.fixtures[0].playerTwoName)

        // Click card at index 0 ("عبدو") to form opponent
        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(1, viewModel.uiState.value.fixtures.size)
        assertEquals("خليل", viewModel.uiState.value.fixtures[0].playerOneName)
        assertEquals("عبدو", viewModel.uiState.value.fixtures[0].playerTwoName)
    }

    @Test
    fun `switching to clubs category and flipping cards assigns clubs sequentially to matches`() = runTest {
        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Draw Match 1 (Khalil vs Abdou)
        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Switch to CLUBS
        viewModel.onCategorySelect(DrawCategory.CLUBS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DrawCategory.CLUBS, viewModel.uiState.value.selectedCategory)
        assertEquals(4, viewModel.uiState.value.remainingClubs.size)

        // 3. Flip Club 1 (assigned to Khalil)
        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ريال مدريد", viewModel.uiState.value.fixtures[0].playerOneTeam)
        assertEquals(null, viewModel.uiState.value.fixtures[0].playerTwoTeam)
        assertEquals(3, viewModel.uiState.value.remainingClubs.size)

        // 4. Flip Club 2 (assigned to Abdou)
        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ريال مدريد", viewModel.uiState.value.fixtures[0].playerOneTeam)
        assertEquals("برشلونة", viewModel.uiState.value.fixtures[0].playerTwoTeam)
        assertEquals(2, viewModel.uiState.value.remainingClubs.size)
    }

    @Test
    fun `shuffle cards triggers random provider shuffle on active deck`() = runTest {
        every { randomProvider.shuffle<ProfileItem>(any()) } answers {
            firstArg<List<ProfileItem>>().reversed()
        }

        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("خليل", viewModel.uiState.value.remainingPlayers[0].label)

        viewModel.onShuffleCards()
        assertTrue(viewModel.uiState.value.isShuffling)
        assertEquals(1L, viewModel.uiState.value.shuffleTrigger)

        testDispatcher.scheduler.advanceTimeBy(700)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShuffling)
        assertEquals("أحمد", viewModel.uiState.value.remainingPlayers[0].label)
        assertEquals("خليل", viewModel.uiState.value.remainingPlayers.last().label)
    }

    @Test
    fun `excluding candidate removes card from deck and adds to excluded list`() = runTest {
        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.excludedPlayers.size)

        val playerToExclude = viewModel.uiState.value.remainingPlayers.first { it.label == "ديدو" }
        viewModel.excludeItem(DrawCategory.PLAYERS, playerToExclude)

        assertEquals(3, viewModel.uiState.value.remainingPlayers.size)
        assertTrue(viewModel.uiState.value.remainingPlayers.none { it.label == "ديدو" })
        assertEquals(1, viewModel.uiState.value.excludedPlayers.size)
        assertEquals("ديدو", viewModel.uiState.value.excludedPlayers[0].label)

        // Restore
        viewModel.restoreExcludedItem(DrawCategory.PLAYERS, playerToExclude)
        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.excludedPlayers.size)
    }

    @Test
    fun `adding new players updates remaining cards deck and filters unassigned clubs`() = runTest {
        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddNewPlayers(listOf("كريم", "سامي"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.remainingPlayers.size)
        assertTrue(viewModel.uiState.value.remainingPlayers.any { it.label == "كريم" })
        assertTrue(viewModel.uiState.value.remainingPlayers.any { it.label == "سامي" })
    }

    @Test
    fun `resetDraw clears all fixtures and restores all cards from profile`() = runTest {
        val viewModel = createViewModel(profileId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCardClick(0)
        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(1, viewModel.uiState.value.fixtures.size)

        viewModel.resetDraw()

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.fixtures.size)
        assertEquals(-1, viewModel.uiState.value.flippedCardIndex)
    }
}
