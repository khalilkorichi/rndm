package com.rndm.app.presentation.draw.wheel

import androidx.lifecycle.SavedStateHandle
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.data.repository.DrawFixtureRepositoryImpl
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WheelDrawViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAllProfilesUseCase = mockk<GetAllProfilesUseCase>()
    private val getProfileByIdUseCase = mockk<GetProfileByIdUseCase>()
    private lateinit var fixtureRepository: DrawFixtureRepositoryImpl
    private val randomProvider = mockk<RandomProvider>()

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
    fun `drawing players generates match fixtures and switching to clubs assigns clubs sequentially`() = runTest {
        every { randomProvider.nextInt(any()) } returns 0

        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Spin Player 1
        viewModel.startSpin()
        assertTrue(viewModel.uiState.value.isSpinning)
        assertTrue(viewModel.uiState.value.targetRotation > 0f)

        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.fixtures.size)
        assertEquals("خليل", viewModel.uiState.value.fixtures[0].playerOneName)
        assertEquals(null, viewModel.uiState.value.fixtures[0].playerTwoName)
        assertEquals(0f, viewModel.uiState.value.targetRotation)

        // 2. Spin Player 2 (Match 1 opponent)
        viewModel.startSpin()
        assertTrue(viewModel.uiState.value.isSpinning)
        assertTrue(viewModel.uiState.value.targetRotation > 0f)

        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.fixtures.size)
        assertEquals("خليل", viewModel.uiState.value.fixtures[0].playerOneName)
        assertEquals("عبدو", viewModel.uiState.value.fixtures[0].playerTwoName)
        assertEquals(0f, viewModel.uiState.value.targetRotation)

        // 3. Switch to Clubs category
        viewModel.onCategorySelect(DrawCategory.CLUBS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DrawCategory.CLUBS, viewModel.uiState.value.selectedCategory)
        assertEquals(4, viewModel.uiState.value.currentWheelItems.size)

        // 4. Spin Club 1 (assigned to Khalil)
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ريال مدريد", viewModel.uiState.value.fixtures[0].playerOneTeam)
        assertEquals(null, viewModel.uiState.value.fixtures[0].playerTwoTeam)

        // 5. Spin Club 2 (assigned to Abdou)
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ريال مدريد", viewModel.uiState.value.fixtures[0].playerOneTeam)
        assertEquals("برشلونة", viewModel.uiState.value.fixtures[0].playerTwoTeam)
    }

    @Test
    fun `consecutive spins across items spin when multiple remain and directly assign when only one remains`() = runTest {
        var returnIdx = 0
        every { randomProvider.nextInt(any()) } answers {
            val max = firstArg<Int>()
            returnIdx % max
        }

        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Spins 1..3 (4, 3, 2 items left): Wheel spins
        for (spin in 1..3) {
            viewModel.startSpin()
            val stateDuringSpin = viewModel.uiState.value
            assertTrue("Spin $spin should be spinning", stateDuringSpin.isSpinning)
            assertTrue("Spin $spin targetRotation must be > 1800f", stateDuringSpin.targetRotation >= 1800f)

            viewModel.onSpinComplete()
            testDispatcher.scheduler.advanceUntilIdle()

            val stateAfterSpin = viewModel.uiState.value
            assertEquals("Spin $spin targetRotation should reset to 0f", 0f, stateAfterSpin.targetRotation)
            assertEquals("Remaining players should decrease", 4 - spin, stateAfterSpin.remainingPlayers.size)
        }

        // Spin 4 (1 item left): Directly assigns without spinning
        assertEquals(1, viewModel.uiState.value.remainingPlayers.size)
        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterFinalSpin = viewModel.uiState.value
        assertEquals("Spin 4 should NOT spin", false, stateAfterFinalSpin.isSpinning)
        assertEquals("Spin 4 targetRotation should be 0", 0f, stateAfterFinalSpin.targetRotation)
        assertEquals("Remaining players should be 0", 0, stateAfterFinalSpin.remainingPlayers.size)
        assertEquals(2, stateAfterFinalSpin.fixtures.size)
        assertEquals("أحمد", stateAfterFinalSpin.fixtures[1].playerTwoName)
    }

    @Test
    fun `single remaining player is directly assigned as winner without spinning`() = runTest {
        val singlePlayerProfile = Profile(
            id = 99L,
            name = "لاعب وحيد",
            type = ProfileType.PLAYERS,
            items = listOf(
                ProfileItem(id = 991, profileId = 99, label = "البطل الوحيد", order = 0)
            )
        )
        coEvery { getProfileByIdUseCase(99L) } returns singlePlayerProfile

        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 99L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initializeWithProfileId(99L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.remainingPlayers.size)

        // Call startSpin
        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSpinning)
        assertEquals(0f, state.targetRotation)
        assertEquals(0, state.remainingPlayers.size)
        assertEquals(1, state.fixtures.size)
        assertEquals("البطل الوحيد", state.fixtures[0].playerOneName)
        assertEquals("البطل الوحيد", state.drawResult?.selectedItem?.label)
    }

    @Test
    fun `adding new players to draw refreshes remaining players and filters remaining clubs to unassigned only`() = runTest {
        every { randomProvider.nextInt(any()) } returns 0

        // Provide 6 clubs so 4 get assigned and 2 remain for new players
        val extendedClubsProfile = clubsProfile.copy(
            items = clubsProfile.items + listOf(
                ProfileItem(id = 14, profileId = 2, label = "مانشستر سيتي", order = 4),
                ProfileItem(id = 15, profileId = 2, label = "تشيلسي", order = 5)
            )
        )
        coEvery { getProfileByIdUseCase(2L) } returns extendedClubsProfile
        every { getAllProfilesUseCase() } returns flowOf(listOf(playersProfile, extendedClubsProfile, teamsProfile))

        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Draw all 4 original players (2 matches)
        // Player 1
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Player 2
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Player 3
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Player 4 (last player directly assigns)
        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.fixtures.size)
        assertEquals(0, viewModel.uiState.value.remainingPlayers.size)

        // 2. Switch to CLUBS and draw 4 clubs
        viewModel.onCategorySelect(DrawCategory.CLUBS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Club 1
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Club 2
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Club 3
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Club 4
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ريال مدريد", viewModel.uiState.value.fixtures[0].playerOneTeam)
        assertEquals("برشلونة", viewModel.uiState.value.fixtures[0].playerTwoTeam)
        assertEquals("ليفربول", viewModel.uiState.value.fixtures[1].playerOneTeam)
        assertEquals("بايرن ميونخ", viewModel.uiState.value.fixtures[1].playerTwoTeam)

        // 3. Add 2 new players ("سامي", "كريم")
        viewModel.onAddNewPlayers(listOf("سامي", "كريم"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify remaining players are the 2 new ones
        assertEquals(DrawCategory.PLAYERS, viewModel.uiState.value.selectedCategory)
        assertEquals(2, viewModel.uiState.value.remainingPlayers.size)
        val remainingPlayerNames = viewModel.uiState.value.remainingPlayers.map { it.label }
        assertTrue(remainingPlayerNames.contains("سامي"))
        assertTrue(remainingPlayerNames.contains("كريم"))

        // Verify remaining clubs only contains the 2 unassigned clubs ("مانشستر سيتي" and "تشيلسي")
        assertEquals(2, viewModel.uiState.value.remainingClubs.size)
        val remainingClubLabels = viewModel.uiState.value.remainingClubs.map { it.label }
        assertTrue(remainingClubLabels.contains("مانشستر سيتي"))
        assertTrue(remainingClubLabels.contains("تشيلسي"))
        assertTrue(!remainingClubLabels.contains("ريال مدريد"))
        assertTrue(!remainingClubLabels.contains("برشلونة"))

        // 4. Spin the 2 new players
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startSpin() // last remaining new player directly assigns
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.fixtures.size)
        assertEquals("سامي", viewModel.uiState.value.fixtures[2].playerOneName)
        assertEquals("كريم", viewModel.uiState.value.fixtures[2].playerTwoName)

        // 5. Switch to clubs and draw club for new player
        viewModel.onCategorySelect(DrawCategory.CLUBS)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("مانشستر سيتي", viewModel.uiState.value.fixtures[2].playerOneTeam)
    }

    @Test
    fun `adding single new player pairs with odd player waiting for opponent`() = runTest {
        every { randomProvider.nextInt(any()) } returns 0

        // Profile with 3 players so 1 player ends up waiting for opponent
        val oddPlayersProfile = Profile(
            id = 5L,
            name = "ثلاثة لاعبين",
            type = ProfileType.PLAYERS,
            items = listOf(
                ProfileItem(id = 51, profileId = 5, label = "خليل", order = 0),
                ProfileItem(id = 52, profileId = 5, label = "عبدو", order = 1),
                ProfileItem(id = 53, profileId = 5, label = "ديدو", order = 2)
            )
        )
        coEvery { getProfileByIdUseCase(5L) } returns oddPlayersProfile
        every { getAllProfilesUseCase() } returns flowOf(listOf(oddPlayersProfile, clubsProfile, teamsProfile))

        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 5L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Draw Player 1
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Draw Player 2 (completes match 1)
        viewModel.startSpin()
        viewModel.onSpinComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Draw Player 3 (last one directly assigns to match 2 playerOne, playerTwo is null)
        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.fixtures.size)
        assertEquals("خليل", viewModel.uiState.value.fixtures[0].playerOneName)
        assertEquals("عبدو", viewModel.uiState.value.fixtures[0].playerTwoName)
        assertEquals("ديدو", viewModel.uiState.value.fixtures[1].playerOneName)
        assertEquals(null, viewModel.uiState.value.fixtures[1].playerTwoName)
        assertEquals(0, viewModel.uiState.value.remainingPlayers.size)

        // 4. Add 1 new player ("طارق") who just joined
        viewModel.onAddNewPlayers(listOf("طارق"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.remainingPlayers.size)

        // 5. Spin the new player (1 remaining, directly assigns to opponent of Match 2)
        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.fixtures.size)
        assertEquals("ديدو", viewModel.uiState.value.fixtures[1].playerOneName)
        assertEquals("طارق", viewModel.uiState.value.fixtures[1].playerTwoName)
    }

    @Test
    fun `excluding candidate removes from wheel and adds to excluded list without deleting from profile`() = runTest {
        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.excludedPlayers.size)

        val playerToExclude = viewModel.uiState.value.remainingPlayers.first { it.label == "ديدو" }
        viewModel.excludeItem(DrawCategory.PLAYERS, playerToExclude)

        assertEquals(3, viewModel.uiState.value.remainingPlayers.size)
        assertTrue(viewModel.uiState.value.remainingPlayers.none { it.label == "ديدو" })
        assertEquals(1, viewModel.uiState.value.excludedPlayers.size)
        assertEquals("ديدو", viewModel.uiState.value.excludedPlayers[0].label)
        // Original profile items remain 4
        assertEquals(4, viewModel.uiState.value.selectedPlayersProfile?.items?.size)
    }

    @Test
    fun `restoring excluded candidate puts them back in wheel remaining items`() = runTest {
        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val playerToExclude = viewModel.uiState.value.remainingPlayers.first { it.label == "ديدو" }
        viewModel.excludeItem(DrawCategory.PLAYERS, playerToExclude)

        assertEquals(3, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(1, viewModel.uiState.value.excludedPlayers.size)

        // Restore
        viewModel.restoreExcludedItem(DrawCategory.PLAYERS, playerToExclude)

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.excludedPlayers.size)
        assertTrue(viewModel.uiState.value.remainingPlayers.any { it.label == "ديدو" })
    }

    @Test
    fun `resetDraw clears excluded lists and restores all initial profile items`() = runTest {
        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val item1 = viewModel.uiState.value.remainingPlayers[0]
        val item2 = viewModel.uiState.value.remainingPlayers[1]
        viewModel.excludeItem(DrawCategory.PLAYERS, item1)
        viewModel.excludeItem(DrawCategory.PLAYERS, item2)

        assertEquals(2, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(2, viewModel.uiState.value.excludedPlayers.size)

        viewModel.resetDraw()

        assertEquals(4, viewModel.uiState.value.remainingPlayers.size)
        assertEquals(0, viewModel.uiState.value.excludedPlayers.size)
        assertEquals(0, viewModel.uiState.value.fixtures.size)
    }

    @Test
    fun `onOpenExcludeDialog and onDismissExcludeDialog manage dialog state`() = runTest {
        val viewModel = WheelDrawViewModel(
            getAllProfilesUseCase,
            getProfileByIdUseCase,
            fixtureRepository,
            randomProvider,
            SavedStateHandle(mapOf("profileId" to 1L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isExcludeDialogOpen)

        viewModel.onOpenExcludeDialog()
        assertEquals(true, viewModel.uiState.value.isExcludeDialogOpen)

        viewModel.onDismissExcludeDialog()
        assertEquals(false, viewModel.uiState.value.isExcludeDialogOpen)
    }
}
