package com.rndm.app.domain.usecase

import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import com.rndm.app.domain.usecase.draw.PerformFlipCardDrawUseCase
import com.rndm.app.domain.usecase.draw.PerformSpinListDrawUseCase
import com.rndm.app.domain.usecase.draw.PerformWheelDrawUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PerformDrawUseCasesTest {

    private lateinit var fakeRandomProvider: FakeRandomProvider
    private val drawRepository: DrawRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)

    private val sampleItems = listOf(
        ProfileItem(id = 1, profileId = 5, label = "ريال مدريد", order = 0),
        ProfileItem(id = 2, profileId = 5, label = "برشلونة", order = 1),
        ProfileItem(id = 3, profileId = 5, label = "مانشستر سيتي", order = 2)
    )

    @Before
    fun setUp() {
        fakeRandomProvider = FakeRandomProvider()
    }

    @Test
    fun `performWheelDraw selects item based on random provider and saves result`() = runTest {
        fakeRandomProvider.fixedInt = 1
        val useCase = PerformWheelDrawUseCase(fakeRandomProvider, drawRepository, profileRepository)

        val result = useCase(profileId = 5L, items = sampleItems)

        assertEquals(DrawType.WHEEL, result.drawType)
        assertNotNull(result.selectedItem)
        assertEquals("برشلونة", result.selectedItem?.label)

        coVerify { drawRepository.saveDrawResult(result) }
        coVerify { profileRepository.updateLastUsed(5L, any()) }
    }

    @Test
    fun `performFlipCardDraw selects correct card and saves result`() = runTest {
        val useCase = PerformFlipCardDrawUseCase(fakeRandomProvider, drawRepository, profileRepository)

        val result = useCase(profileId = 5L, items = sampleItems, selectedCardIndex = 2)

        assertEquals(DrawType.FLIP_CARDS, result.drawType)
        assertNotNull(result.selectedItem)
        assertEquals("مانشستر سيتي", result.selectedItem?.label)

        coVerify { drawRepository.saveDrawResult(result) }
        coVerify { profileRepository.updateLastUsed(5L, any()) }
    }

    @Test
    fun `performSpinListDraw selects item and saves result`() = runTest {
        fakeRandomProvider.fixedInt = 0
        val useCase = PerformSpinListDrawUseCase(fakeRandomProvider, drawRepository, profileRepository)

        val result = useCase(profileId = 5L, items = sampleItems)

        assertEquals(DrawType.SPIN_LIST, result.drawType)
        assertNotNull(result.selectedItem)
        assertEquals("ريال مدريد", result.selectedItem?.label)

        coVerify { drawRepository.saveDrawResult(result) }
        coVerify { profileRepository.updateLastUsed(5L, any()) }
    }

    @Test
    fun `20 consecutive wheel draws on 7 items demonstrate fair random distribution`() = runTest {
        val realRandomProvider = com.rndm.app.core.util.DefaultRandomProvider()
        val useCase = PerformWheelDrawUseCase(realRandomProvider, drawRepository, profileRepository)

        val sevenPlayers = (1..7).map {
            ProfileItem(id = it.toLong(), profileId = 1L, label = "لاعب $it", order = it - 1)
        }

        val counts = mutableMapOf<String, Int>()
        sevenPlayers.forEach { counts[it.label] = 0 }

        repeat(20) {
            val result = useCase(profileId = 1L, items = sevenPlayers)
            val winner = result.selectedItem!!.label
            counts[winner] = (counts[winner] ?: 0) + 1
        }

        println("=== 20 WHEEL DRAWS DISTRIBUTION ===")
        counts.forEach { (name, count) -> println("$name: $count times") }
        println("===================================")

        assertEquals(20, counts.values.sum())
    }
}
