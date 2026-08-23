package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.TournamentRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReplacePlayerInTournamentUseCaseTest {

    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var drawFixtureRepository: DrawFixtureRepository
    private lateinit var useCase: ReplacePlayerInTournamentUseCase

    @Before
    fun setUp() {
        tournamentRepository = mockk(relaxed = true)
        drawFixtureRepository = mockk(relaxed = true)
        useCase = ReplacePlayerInTournamentUseCase(tournamentRepository, drawFixtureRepository)
    }

    @Test
    fun `invoke should replace player in tournament repository and draw fixture repository`() = runTest {
        useCase(
            tournamentId = 42L,
            oldPlayerName = "سالم",
            newPlayerName = "ياسين",
            newClubName = "ريال مدريد"
        )

        coVerify(exactly = 1) {
            tournamentRepository.replaceParticipant(42L, "سالم", "ياسين", "ريال مدريد")
        }
        verify(exactly = 1) {
            drawFixtureRepository.replacePlayer("سالم", "ياسين", "ريال مدريد")
        }
    }

    @Test
    fun `invoke with zero tournament id should only replace in draw fixture repository`() = runTest {
        useCase(
            tournamentId = 0L,
            oldPlayerName = "خالد",
            newPlayerName = "أمين",
            newClubName = null
        )

        coVerify(exactly = 0) {
            tournamentRepository.replaceParticipant(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            drawFixtureRepository.replacePlayer("خالد", "أمين", null)
        }
    }
}
