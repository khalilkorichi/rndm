package com.rndm.app.domain.usecase

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import com.rndm.app.domain.usecase.draw.GenerateRoundRobinPairingsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRandomProvider : RandomProvider {
    var fixedInt: Int = 0
    var shouldReverseShuffle: Boolean = false

    override fun nextInt(from: Int, until: Int): Int = fixedInt.coerceIn(from, until - 1)
    override fun nextInt(until: Int): Int = fixedInt % until
    override fun <T> shuffle(list: List<T>): List<T> = if (shouldReverseShuffle) list.reversed() else list
    override fun <T> pickRandom(list: List<T>): T? = list.getOrNull(fixedInt % list.size)
}

class GenerateRoundRobinPairingsUseCaseTest {

    private lateinit var fakeRandomProvider: FakeRandomProvider
    private val drawRepository: DrawRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private lateinit var useCase: GenerateRoundRobinPairingsUseCase

    @Before
    fun setUp() {
        fakeRandomProvider = FakeRandomProvider()
        useCase = GenerateRoundRobinPairingsUseCase(
            randomProvider = fakeRandomProvider,
            drawRepository = drawRepository,
            profileRepository = profileRepository
        )
    }

    @Test
    fun `even number of players generates complete pairings with no byes`() = runTest {
        val players = listOf(
            ProfileItem(id = 1, profileId = 10, label = "لاعب 1", order = 0),
            ProfileItem(id = 2, profileId = 10, label = "لاعب 2", order = 1),
            ProfileItem(id = 3, profileId = 10, label = "لاعب 3", order = 2),
            ProfileItem(id = 4, profileId = 10, label = "لاعب 4", order = 3)
        )

        val result = useCase(profileId = 10L, items = players)

        assertEquals(DrawType.ROUND_ROBIN, result.drawType)
        assertEquals(2, result.pairings.size)
        assertEquals("لاعب 1", result.pairings[0].playerOne.label)
        assertEquals("لاعب 2", result.pairings[0].playerTwo?.label)
        assertEquals("لاعب 3", result.pairings[1].playerOne.label)
        assertEquals("لاعب 4", result.pairings[1].playerTwo?.label)
    }

    @Test
    fun `odd number of players generates pairings with bye for last player`() = runTest {
        val players = listOf(
            ProfileItem(id = 1, profileId = 10, label = "لاعب 1", order = 0),
            ProfileItem(id = 2, profileId = 10, label = "لاعب 2", order = 1),
            ProfileItem(id = 3, profileId = 10, label = "لاعب 3", order = 2)
        )

        val result = useCase(profileId = 10L, items = players)

        assertEquals(2, result.pairings.size)
        assertNotNull(result.pairings[0].playerTwo)
        assertNull(result.pairings[1].playerTwo)
        assertEquals("لاعب 3", result.pairings[1].playerOne.label)
    }

    @Test
    fun `7 players round robin generates 3 matches and exactly 1 Bye with no duplicates`() = runTest {
        val realRandom = com.rndm.app.core.util.DefaultRandomProvider()
        val realUseCase = GenerateRoundRobinPairingsUseCase(realRandom, drawRepository, profileRepository)

        val sevenPlayers = (1..7).map {
            ProfileItem(id = it.toLong(), profileId = 1L, label = "لاعب $it", order = it - 1)
        }

        val result = realUseCase(profileId = 1L, items = sevenPlayers)

        assertEquals(DrawType.ROUND_ROBIN, result.drawType)
        // 7 players = 3 pairings of 2 players + 1 Bye = 4 total pairings entries
        assertEquals(4, result.pairings.size)

        val byes = result.pairings.filter { it.playerTwo == null }
        val matches = result.pairings.filter { it.playerTwo != null }

        assertEquals(1, byes.size)
        assertEquals(3, matches.size)

        // Check that all 7 players appear exactly once
        val allPairedPlayers = matches.flatMap { listOf(it.playerOne.label, it.playerTwo!!.label) } + byes.map { it.playerOne.label }
        assertEquals(7, allPairedPlayers.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception when less than 2 players provided`() = runTest {
        val players = listOf(
            ProfileItem(id = 1, profileId = 10, label = "لاعب 1", order = 0)
        )
        useCase(profileId = 10L, items = players)
    }
}
