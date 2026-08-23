package com.rndm.app.domain.usecase

import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.repository.ProfileRepository
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.DeleteProfileUseCase
import com.rndm.app.domain.usecase.profile.DuplicateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ProfileUseCasesTest {

    private val profileRepository: ProfileRepository = mockk(relaxed = true)

    private lateinit var createProfileUseCase: CreateProfileUseCase
    private lateinit var updateProfileUseCase: UpdateProfileUseCase
    private lateinit var deleteProfileUseCase: DeleteProfileUseCase
    private lateinit var duplicateProfileUseCase: DuplicateProfileUseCase
    private lateinit var getAllProfilesUseCase: GetAllProfilesUseCase
    private lateinit var getProfileByIdUseCase: GetProfileByIdUseCase

    private val validProfile = Profile(
        id = 1L,
        name = "دوري الأصدقاء",
        type = ProfileType.PLAYERS,
        items = listOf(
            ProfileItem(id = 1, profileId = 1, label = "خليل", order = 0),
            ProfileItem(id = 2, profileId = 1, label = "أحمد", order = 1)
        )
    )

    @Before
    fun setUp() {
        createProfileUseCase = CreateProfileUseCase(profileRepository)
        updateProfileUseCase = UpdateProfileUseCase(profileRepository)
        deleteProfileUseCase = DeleteProfileUseCase(profileRepository)
        duplicateProfileUseCase = DuplicateProfileUseCase(profileRepository)
        getAllProfilesUseCase = GetAllProfilesUseCase(profileRepository)
        getProfileByIdUseCase = GetProfileByIdUseCase(profileRepository)
    }

    @Test
    fun `createProfileUseCase calls repository with valid profile`() = runTest {
        coEvery { profileRepository.createProfile(validProfile) } returns 1L

        val id = createProfileUseCase(validProfile)

        assertEquals(1L, id)
        coVerify { profileRepository.createProfile(validProfile) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createProfileUseCase throws exception when name is blank`() = runTest {
        val blankProfile = validProfile.copy(name = "   ")
        createProfileUseCase(blankProfile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createProfileUseCase throws exception when less than 2 items`() = runTest {
        val singleItemProfile = validProfile.copy(items = listOf(validProfile.items[0]))
        createProfileUseCase(singleItemProfile)
    }

    @Test
    fun `deleteProfileUseCase calls repository deleteProfile`() = runTest {
        deleteProfileUseCase(1L)
        coVerify { profileRepository.deleteProfile(1L) }
    }

    @Test
    fun `duplicateProfileUseCase calls repository duplicateProfile`() = runTest {
        coEvery { profileRepository.duplicateProfile(1L, "دوري الأصدقاء (نسخة)") } returns 2L

        val newId = duplicateProfileUseCase(1L, "دوري الأصدقاء (نسخة)")

        assertEquals(2L, newId)
        coVerify { profileRepository.duplicateProfile(1L, "دوري الأصدقاء (نسخة)") }
    }

    @Test
    fun `getAllProfilesUseCase returns flow from repository`() = runTest {
        every { profileRepository.observeAllProfiles() } returns flowOf(listOf(validProfile))

        val result = getAllProfilesUseCase().first()

        assertEquals(1, result.size)
        assertEquals("دوري الأصدقاء", result[0].name)
    }
}
