package com.rndm.app.presentation.draw

import androidx.lifecycle.SavedStateHandle
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.MatchPairing
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.draw.GenerateRoundRobinPairingsUseCase
import com.rndm.app.domain.usecase.draw.PerformSpinListDrawUseCase
import com.rndm.app.domain.usecase.profile.CreateProfileGroupUseCase
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.GetProfileGroupsUseCase
import com.rndm.app.domain.usecase.profile.UpdateItemActiveStateUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import com.rndm.app.presentation.draw.flipcards.FlipCardDrawViewModel
import com.rndm.app.presentation.draw.result.DrawResultViewModel
import com.rndm.app.presentation.draw.setup.DrawSetupViewModel
import com.rndm.app.presentation.draw.spinlist.SpinListDrawViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawSetupAndModesTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAllProfilesUseCase = mockk<GetAllProfilesUseCase>()
    private val getProfileGroupsUseCase = mockk<GetProfileGroupsUseCase>()
    private val createProfileGroupUseCase = mockk<CreateProfileGroupUseCase>()
    private val createProfileUseCase = mockk<CreateProfileUseCase>()
    private val updateProfileUseCase = mockk<UpdateProfileUseCase>()
    private val updateItemActiveStateUseCase = mockk<UpdateItemActiveStateUseCase>(relaxed = true)
    private val getCurrentUserRoleUseCase = mockk<GetCurrentUserRoleUseCase>()
    private val drawFixtureRepository = mockk<DrawFixtureRepository>(relaxed = true)
    private val getProfileByIdUseCase = mockk<GetProfileByIdUseCase>()
    private val drawRepository = mockk<DrawRepository>(relaxed = true)
    private val generateRoundRobinPairingsUseCase = mockk<GenerateRoundRobinPairingsUseCase>()
    private val performSpinListDrawUseCase = mockk<PerformSpinListDrawUseCase>()
    private val randomProvider = mockk<RandomProvider>(relaxed = true)

    private val sampleProfile = Profile(
        id = 10L,
        name = "دوري الأبطال",
        type = ProfileType.CLUBS,
        items = listOf(
            ProfileItem(id = 1, profileId = 10, label = "ريال مدريد", order = 0, isActive = true),
            ProfileItem(id = 2, profileId = 10, label = "مانشستر سيتي", order = 1, isActive = true),
            ProfileItem(id = 3, profileId = 10, label = "بايرن ميونخ", order = 2, isActive = true),
            ProfileItem(id = 4, profileId = 10, label = "باريس سان جيرمان", order = 3, isActive = true)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getCurrentUserRoleUseCase() } returns flowOf(UserRole.ADMIN)
        every { getAllProfilesUseCase() } returns flowOf(listOf(sampleProfile))
        every { getProfileGroupsUseCase() } returns flowOf(emptyList())
        every { drawRepository.getLatestDrawResult() } returns flowOf(null)
        coEvery { getProfileByIdUseCase(10L) } returns sampleProfile
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DrawSetupViewModel allows selecting all draw types`() = runTest {
        val viewModel = DrawSetupViewModel(
            getAllProfilesUseCase = getAllProfilesUseCase,
            getProfileGroupsUseCase = getProfileGroupsUseCase,
            createProfileGroupUseCase = createProfileGroupUseCase,
            createProfileUseCase = createProfileUseCase,
            updateProfileUseCase = updateProfileUseCase,
            getCurrentUserRoleUseCase = getCurrentUserRoleUseCase,
            drawFixtureRepository = drawFixtureRepository,
            savedStateHandle = SavedStateHandle(mapOf("profileId" to 10L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DrawType.WHEEL, viewModel.uiState.value.selectedDrawType)

        // Select Flip Cards
        viewModel.onDrawTypeSelected(DrawType.FLIP_CARDS)
        assertEquals(DrawType.FLIP_CARDS, viewModel.uiState.value.selectedDrawType)

        // Select Spin List
        viewModel.onDrawTypeSelected(DrawType.SPIN_LIST)
        assertEquals(DrawType.SPIN_LIST, viewModel.uiState.value.selectedDrawType)

        // Select Round Robin
        viewModel.onDrawTypeSelected(DrawType.ROUND_ROBIN)
        assertEquals(DrawType.ROUND_ROBIN, viewModel.uiState.value.selectedDrawType)

        // Back to Wheel
        viewModel.onDrawTypeSelected(DrawType.WHEEL)
        assertEquals(DrawType.WHEEL, viewModel.uiState.value.selectedDrawType)
    }

    @Test
    fun `DrawResultViewModel generates round robin pairings when initialized`() = runTest {
        val expectedPairings = listOf(
            MatchPairing(playerOne = sampleProfile.items[0], playerTwo = sampleProfile.items[1]),
            MatchPairing(playerOne = sampleProfile.items[2], playerTwo = sampleProfile.items[3])
        )
        val expectedResult = DrawResult(
            drawType = DrawType.ROUND_ROBIN,
            pairings = expectedPairings
        )
        coEvery { generateRoundRobinPairingsUseCase(10L, any()) } returns expectedResult

        val viewModel = DrawResultViewModel(
            drawRepository = drawRepository,
            getProfileByIdUseCase = getProfileByIdUseCase,
            generateRoundRobinPairingsUseCase = generateRoundRobinPairingsUseCase
        )

        viewModel.initialize(profileId = 10L, drawType = DrawType.ROUND_ROBIN)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value)
        assertEquals(DrawType.ROUND_ROBIN, viewModel.uiState.value?.drawType)
        assertEquals(2, viewModel.uiState.value?.pairings?.size)
    }

    @Test
    fun `FlipCardDrawViewModel performs draw on card click using active items`() = runTest {
        val viewModel = FlipCardDrawViewModel(
            getAllProfilesUseCase = getAllProfilesUseCase,
            getProfileByIdUseCase = getProfileByIdUseCase,
            updateItemActiveStateUseCase = updateItemActiveStateUseCase,
            drawFixtureRepository = drawFixtureRepository,
            randomProvider = randomProvider,
            savedStateHandle = SavedStateHandle(mapOf("profileId" to 10L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCardClick(1)
        assertTrue(viewModel.uiState.value.isRevealing)
        assertEquals(1, viewModel.uiState.value.flippedCardIndex)
    }

    @Test
    fun `SpinListDrawViewModel starts spin and sets target scroll index`() = runTest {
        val expectedResult = DrawResult(
            drawType = DrawType.SPIN_LIST,
            selectedItem = sampleProfile.items[2]
        )
        coEvery { performSpinListDrawUseCase(10L, any()) } returns expectedResult

        val viewModel = SpinListDrawViewModel(
            getProfileByIdUseCase = getProfileByIdUseCase,
            performSpinListDrawUseCase = performSpinListDrawUseCase,
            savedStateHandle = SavedStateHandle(mapOf("profileId" to 10L))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startSpin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.selectedIndex)
        assertEquals(expectedResult, viewModel.uiState.value.drawResult)
        assertTrue(viewModel.uiState.value.targetScrollIndex > 0)
    }
}
