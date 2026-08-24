package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.repository.SyncRepository
import com.rndm.app.domain.repository.TournamentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateMatchScoreUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val evaluateBestLosersUseCase = EvaluateBestLosersUseCase()
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val useCase = UpdateMatchScoreUseCase(tournamentRepository, evaluateBestLosersUseCase, syncRepository)

    @Test
    fun `finishing QF match advances winner to corresponding Semi-Final match`() = runTest {
        val qf1 = Match(
            id = 1L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 1,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            status = MatchStatus.PENDING
        )

        val semi1 = Match(
            id = 5L,
            tournamentId = 10L,
            stage = MatchStage.SEMI_FINALS,
            bracketMatchIndex = 1,
            playerOneName = "فائز ربع النهائي 1",
            playerTwoName = "فائز ربع النهائي 2",
            status = MatchStatus.PENDING
        )

        coEvery { tournamentRepository.getMatches(10L) } returns flowOf(listOf(qf1, semi1))

        useCase(
            tournamentId = 10L,
            match = qf1,
            scoreOne = 3,
            scoreTwo = 1
        )

        // 1. Verify QF match was updated as finished with score
        coVerify {
            tournamentRepository.updateMatch(match {
                it.id == 1L && it.scoreOne == 3 && it.scoreTwo == 1 && it.winnerName == "Player 1" && it.status == MatchStatus.FINISHED
            })
        }

        // 2. Verify semi1 has updated playerOneName to "Player 1"
        coVerify {
            tournamentRepository.updateMatch(match {
                it.id == 5L && it.stage == MatchStage.SEMI_FINALS && it.playerOneName == "Player 1"
            })
        }
    }

    @Test
    fun `in 7-player tournament, when QF matches 1, 2, 3 finish, top best loser is placed into Match 4`() = runTest {
        val qf1 = Match(
            id = 1L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 1,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            scoreOne = 2,
            scoreTwo = 0,
            winnerName = "Player 1",
            status = MatchStatus.FINISHED
        )

        val qf2 = Match(
            id = 2L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 2,
            playerOneName = "Player 3",
            playerTwoName = "Player 4",
            scoreOne = 2,
            scoreTwo = 2,
            penaltyScoreOne = 4,
            penaltyScoreTwo = 5,
            winnerName = "Player 4",
            status = MatchStatus.FINISHED
        )

        val qf3 = Match(
            id = 3L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 3,
            playerOneName = "Player 5",
            playerTwoName = "Player 6",
            status = MatchStatus.PENDING
        )

        val qf4 = Match(
            id = 4L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 4,
            playerOneName = "Player 7",
            playerTwoName = "أحسن خاسر",
            isPlayerTwoLuckyLoser = true,
            status = MatchStatus.PENDING
        )

        val semi1 = Match(id = 5L, tournamentId = 10L, stage = MatchStage.SEMI_FINALS, bracketMatchIndex = 1, playerOneName = "P1", playerTwoName = "P2")
        val semi2 = Match(id = 6L, tournamentId = 10L, stage = MatchStage.SEMI_FINALS, bracketMatchIndex = 2, playerOneName = "P3", playerTwoName = "P4")

        coEvery { tournamentRepository.getMatches(10L) } returns flowOf(listOf(qf1, qf2, qf3, qf4, semi1, semi2))

        // Update QF3 where Player 5 wins 3 - 2 against Player 6
        useCase(
            tournamentId = 10L,
            match = qf3,
            scoreOne = 3,
            scoreTwo = 2
        )

        // Player 3 lost on penalties in QF2 (2-2, 4-5) -> Highest priority best loser!
        coVerify {
            tournamentRepository.updateMatch(match {
                it.id == 4L && it.playerTwoName == "Player 3" && it.isPlayerTwoLuckyLoser
            })
        }
    }

    @Test
    fun `in 3-player tournament, finishing SF1 places the loser into SF2 to play Player 3`() = runTest {
        val sf1 = Match(
            id = 1L,
            tournamentId = 10L,
            stage = MatchStage.SEMI_FINALS,
            bracketMatchIndex = 1,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            status = MatchStatus.PENDING
        )

        val sf2 = Match(
            id = 2L,
            tournamentId = 10L,
            stage = MatchStage.SEMI_FINALS,
            bracketMatchIndex = 2,
            playerOneName = "Player 3",
            playerTwoName = "أحسن خاسر",
            isPlayerTwoLuckyLoser = true,
            status = MatchStatus.PENDING
        )

        val finalMatch = Match(id = 3L, tournamentId = 10L, stage = MatchStage.FINAL, bracketMatchIndex = 1, playerOneName = "TBD", playerTwoName = "TBD")

        coEvery { tournamentRepository.getMatches(10L) } returns flowOf(listOf(sf1, sf2, finalMatch))

        // SF1: Player 1 wins 3 - 1 against Player 2 -> Player 2 is the loser
        useCase(
            tournamentId = 10L,
            match = sf1,
            scoreOne = 3,
            scoreTwo = 1
        )

        // Verify Player 2 is placed into SF2 against Player 3
        coVerify {
            tournamentRepository.updateMatch(match {
                it.id == 2L && it.playerTwoName == "Player 2" && it.isPlayerTwoLuckyLoser
            })
        }
    }

    @Test
    fun `in 5-player tournament, finishing QF matches places top best loser into SF2 to play Player 5`() = runTest {
        val qf1 = Match(
            id = 1L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 1,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            scoreOne = 2,
            scoreTwo = 0,
            winnerName = "Player 1",
            status = MatchStatus.FINISHED
        )

        val qf2 = Match(
            id = 2L,
            tournamentId = 10L,
            stage = MatchStage.QUARTER_FINALS,
            bracketMatchIndex = 2,
            playerOneName = "Player 3",
            playerTwoName = "Player 4",
            status = MatchStatus.PENDING
        )

        val sf1 = Match(id = 3L, tournamentId = 10L, stage = MatchStage.SEMI_FINALS, bracketMatchIndex = 1, playerOneName = "Player 1", playerTwoName = "TBD")
        val sf2 = Match(
            id = 4L,
            tournamentId = 10L,
            stage = MatchStage.SEMI_FINALS,
            bracketMatchIndex = 2,
            playerOneName = "Player 5",
            playerTwoName = "أحسن خاسر",
            isPlayerTwoLuckyLoser = true,
            status = MatchStatus.PENDING
        )

        coEvery { tournamentRepository.getMatches(10L) } returns flowOf(listOf(qf1, qf2, sf1, sf2))

        // QF2: Player 3 vs Player 4 (2-1) -> Player 4 loses with GD -1, Player 2 lost with GD -2
        useCase(
            tournamentId = 10L,
            match = qf2,
            scoreOne = 2,
            scoreTwo = 1
        )

        // Player 4 is the top best loser (GD -1 vs GD -2)
        coVerify {
            tournamentRepository.updateMatch(match {
                it.id == 4L && it.playerTwoName == "Player 4" && it.isPlayerTwoLuckyLoser
            })
        }
    }

    @Test
    fun `finishing Final match marks tournament as COMPLETED`() = runTest {
        val finalMatch = Match(
            id = 10L,
            tournamentId = 10L,
            stage = MatchStage.FINAL,
            bracketMatchIndex = 1,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            status = MatchStatus.PENDING
        )

        coEvery { tournamentRepository.getMatches(10L) } returns flowOf(listOf(finalMatch))

        useCase(
            tournamentId = 10L,
            match = finalMatch,
            scoreOne = 2,
            scoreTwo = 1
        )

        coVerify {
            tournamentRepository.updateTournamentStage(10L, TournamentStage.COMPLETED)
        }
    }
}
