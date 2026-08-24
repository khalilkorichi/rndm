package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.model.SyncStatus
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentType
import com.rndm.app.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncUseCasesTest {

    private val syncRepository: SyncRepository = mockk(relaxed = true)

    private lateinit var publishTournamentUseCase: PublishTournamentUseCase
    private lateinit var joinTournamentByCodeUseCase: JoinTournamentByCodeUseCase
    private lateinit var observeRemoteTournamentUseCase: ObserveRemoteTournamentUseCase

    private val testTournament = Tournament(
        id = 1L,
        name = "بطولة رمضان الكبرى",
        type = TournamentType.GROUPS_KNOCKOUT,
        stage = TournamentStage.GROUPS,
        playersProfileId = 1L,
        remoteId = "tourn_xyz",
        shareCode = "KHL-7A3",
        isRemote = true,
        isHost = true,
        syncStatus = SyncStatus.SYNCED
    )

    @Before
    fun setUp() {
        publishTournamentUseCase = PublishTournamentUseCase(syncRepository)
        joinTournamentByCodeUseCase = JoinTournamentByCodeUseCase(syncRepository)
        observeRemoteTournamentUseCase = ObserveRemoteTournamentUseCase(syncRepository)
    }

    @Test
    fun `publishTournamentUseCase calls repository publishTournament`() = runTest {
        coEvery { syncRepository.publishTournament(1L) } returns Result.success(testTournament)

        val result = publishTournamentUseCase(1L)

        assertTrue(result.isSuccess)
        assertEquals("KHL-7A3", result.getOrNull()?.shareCode)
        coVerify { syncRepository.publishTournament(1L) }
    }

    @Test
    fun `joinTournamentByCodeUseCase with valid code calls repository joinTournamentByCode`() = runTest {
        coEvery { syncRepository.joinTournamentByCode("KHL-7A3") } returns Result.success(10L)

        val result = joinTournamentByCodeUseCase("khl-7a3")

        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())
        coVerify { syncRepository.joinTournamentByCode("KHL-7A3") }
    }

    @Test
    fun `joinTournamentByCodeUseCase with unhyphenated code calls repository joinTournamentByCode`() = runTest {
        coEvery { syncRepository.joinTournamentByCode("W33Z77") } returns Result.success(20L)

        val result = joinTournamentByCodeUseCase("w33z77")

        assertTrue(result.isSuccess)
        assertEquals(20L, result.getOrNull())
        coVerify { syncRepository.joinTournamentByCode("W33Z77") }
    }

    @Test
    fun `joinTournamentByCodeUseCase with too short code returns failure`() = runTest {
        val result = joinTournamentByCodeUseCase("AB")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { syncRepository.joinTournamentByCode(any()) }
    }
}
