package com.rndm.app.presentation.profile.edit

import androidx.lifecycle.SavedStateHandle
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CreateEditProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getProfileByIdUseCase = mockk<GetProfileByIdUseCase>()
    private val createProfileUseCase = mockk<CreateProfileUseCase>()
    private val updateProfileUseCase = mockk<UpdateProfileUseCase>()

    private fun createViewModel(
        profileId: Long = 0L,
        typeName: String? = null
    ): CreateEditProfileViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            set("profileId", profileId)
            if (typeName != null) set("typeName", typeName)
        }
        return CreateEditProfileViewModel(
            getProfileByIdUseCase = getProfileByIdUseCase,
            createProfileUseCase = createProfileUseCase,
            updateProfileUseCase = updateProfileUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onEditItem updates the item name at valid index in-place`() = runTest {
        val viewModel = createViewModel()

        viewModel.onNameChange("دوري الأصدقاء")
        viewModel.onItemInputChange("خليل")
        viewModel.onAddItem()
        viewModel.onItemInputChange("عبدو")
        viewModel.onAddItem()
        viewModel.onItemInputChange("أحمد")
        viewModel.onAddItem()

        assertEquals(listOf("خليل", "عبدو", "أحمد"), viewModel.uiState.value.items)

        // Edit index 1 from "عبدو" to "عبد الرحمن"
        viewModel.onEditItem(index = 1, newLabel = "عبد الرحمن")

        assertEquals(listOf("خليل", "عبد الرحمن", "أحمد"), viewModel.uiState.value.items)
    }

    @Test
    fun `onEditItem ignores blank string or whitespace-only update`() = runTest {
        val viewModel = createViewModel()

        viewModel.onItemInputChange("ريال مدريد")
        viewModel.onAddItem()
        viewModel.onItemInputChange("برشلونة")
        viewModel.onAddItem()

        // Try blank update
        viewModel.onEditItem(index = 0, newLabel = "   ")

        // Should remain untouched
        assertEquals(listOf("ريال مدريد", "برشلونة"), viewModel.uiState.value.items)
    }

    @Test
    fun `onEditItem ignores out-of-bounds index`() = runTest {
        val viewModel = createViewModel()

        viewModel.onItemInputChange("المغرب")
        viewModel.onAddItem()
        viewModel.onItemInputChange("الجزائر")
        viewModel.onAddItem()

        // Out of bounds index
        viewModel.onEditItem(index = 5, newLabel = "تونس")
        viewModel.onEditItem(index = -1, newLabel = "مصر")

        assertEquals(listOf("المغرب", "الجزائر"), viewModel.uiState.value.items)
    }

    @Test
    fun `load existing profile and edit item preserves profile data and order`() = runTest {
        val existingProfile = Profile(
            id = 10L,
            name = "أندية أوروبا",
            type = ProfileType.CLUBS,
            items = listOf(
                ProfileItem(id = 1, profileId = 10, label = "مانشستر سيتي", order = 0),
                ProfileItem(id = 2, profileId = 10, label = "ليفربول", order = 1),
                ProfileItem(id = 3, profileId = 10, label = "أرسنال", order = 2)
            )
        )
        coEvery { getProfileByIdUseCase(10L) } returns existingProfile

        val viewModel = createViewModel(profileId = 10L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditMode)
        assertEquals("أندية أوروبا", viewModel.uiState.value.name)
        assertEquals(listOf("مانشستر سيتي", "ليفربول", "أرسنال"), viewModel.uiState.value.items)

        // Edit Liverpool to Bayern Munich
        viewModel.onEditItem(index = 1, newLabel = "بايرن ميونخ")

        assertEquals(listOf("مانشستر سيتي", "بايرن ميونخ", "أرسنال"), viewModel.uiState.value.items)
    }
}
