package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.repository.TournamentRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateKnockoutBracketUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val useCase = GenerateKnockoutBracketUseCase(tournamentRepository)

    @Test
    fun `generating knockout bracket for 8 players creates 4 QF, 2 SF, 1 3rd place, and 1 Final`() = runTest {
        val participants = (1..8).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val qfMatches = matches.filter { it.stage == MatchStage.QUARTER_FINALS }
        val sfMatches = matches.filter { it.stage == MatchStage.SEMI_FINALS }
        val thirdPlace = matches.filter { it.stage == MatchStage.THIRD_PLACE }
        val finalMatches = matches.filter { it.stage == MatchStage.FINAL }

        assertEquals(4, qfMatches.size)
        assertEquals(2, sfMatches.size)
        assertEquals(1, thirdPlace.size)
        assertEquals(1, finalMatches.size)

        assertEquals("Player 1", qfMatches[0].playerOneName)
        assertEquals("Player 2", qfMatches[0].playerTwoName)
        assertEquals(1, qfMatches[0].bracketMatchIndex)

        assertEquals("Player 7", qfMatches[3].playerOneName)
        assertEquals("Player 8", qfMatches[3].playerTwoName)
    }

    @Test
    fun `generating knockout bracket for 7 players creates Match 4 with Player 7 vs Lucky Loser`() = runTest {
        val participants = (1..7).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val qfMatches = matches.filter { it.stage == MatchStage.QUARTER_FINALS }
        assertEquals(4, qfMatches.size)

        // Matches 1..3 have players 1..6
        assertEquals("Player 1", qfMatches[0].playerOneName)
        assertEquals("Player 2", qfMatches[0].playerTwoName)

        assertEquals("Player 3", qfMatches[1].playerOneName)
        assertEquals("Player 4", qfMatches[1].playerTwoName)

        assertEquals("Player 5", qfMatches[2].playerOneName)
        assertEquals("Player 6", qfMatches[2].playerTwoName)

        // Match 4 has Player 7 vs Lucky Loser
        assertEquals("Player 7", qfMatches[3].playerOneName)
        assertEquals("أحسن خاسر", qfMatches[3].playerTwoName)
        assertTrue(qfMatches[3].isPlayerTwoLuckyLoser)
    }

    @Test
    fun `generating knockout bracket for 4 players creates 2 SF, 1 3rd place, and 1 Final`() = runTest {
        val participants = (1..4).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val sfMatches = matches.filter { it.stage == MatchStage.SEMI_FINALS }
        val thirdPlace = matches.filter { it.stage == MatchStage.THIRD_PLACE }
        val finalMatches = matches.filter { it.stage == MatchStage.FINAL }

        assertEquals(2, sfMatches.size)
        assertEquals(1, thirdPlace.size)
        assertEquals(1, finalMatches.size)
    }

    @Test
    fun `generating knockout bracket for 3 players creates 2 SF, 1 3rd place, and 1 Final with Lucky Loser`() = runTest {
        val participants = (1..3).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val sfMatches = matches.filter { it.stage == MatchStage.SEMI_FINALS }
        val thirdPlace = matches.filter { it.stage == MatchStage.THIRD_PLACE }
        val finalMatches = matches.filter { it.stage == MatchStage.FINAL }

        assertEquals(2, sfMatches.size)
        assertEquals(1, thirdPlace.size)
        assertEquals(1, finalMatches.size)

        assertEquals("Player 1", sfMatches[0].playerOneName)
        assertEquals("Player 2", sfMatches[0].playerTwoName)

        assertEquals("Player 3", sfMatches[1].playerOneName)
        assertEquals("أحسن خاسر", sfMatches[1].playerTwoName)
        assertTrue(sfMatches[1].isPlayerTwoLuckyLoser)
    }

    @Test
    fun `generating knockout bracket for 5 players creates 2 QF and 2 SF with Lucky Loser slot in SF2`() = runTest {
        val participants = (1..5).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val qfMatches = matches.filter { it.stage == MatchStage.QUARTER_FINALS }
        val sfMatches = matches.filter { it.stage == MatchStage.SEMI_FINALS }
        val finalMatches = matches.filter { it.stage == MatchStage.FINAL }

        assertEquals(2, qfMatches.size)
        assertEquals(2, sfMatches.size)
        assertEquals(1, finalMatches.size)

        assertEquals("Player 1", qfMatches[0].playerOneName)
        assertEquals("Player 2", qfMatches[0].playerTwoName)

        assertEquals("Player 3", qfMatches[1].playerOneName)
        assertEquals("Player 4", qfMatches[1].playerTwoName)

        assertEquals("Player 5", sfMatches[1].playerOneName)
        assertEquals("أحسن خاسر", sfMatches[1].playerTwoName)
        assertTrue(sfMatches[1].isPlayerTwoLuckyLoser)
    }

    @Test
    fun `generating knockout bracket for 6 players creates 3 QF and 2 SF with Lucky Loser slot in SF2`() = runTest {
        val participants = (1..6).map {
            TournamentParticipant(
                playerItemId = it.toLong(),
                playerName = "Player $it",
                groupIndex = 0
            )
        }

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        val qfMatches = matches.filter { it.stage == MatchStage.QUARTER_FINALS }
        val sfMatches = matches.filter { it.stage == MatchStage.SEMI_FINALS }

        assertEquals(3, qfMatches.size)
        assertEquals(2, sfMatches.size)

        assertEquals("فائز ربع النهائي 3", sfMatches[1].playerOneName)
        assertEquals("أحسن خاسر", sfMatches[1].playerTwoName)
        assertTrue(sfMatches[1].isPlayerTwoLuckyLoser)
    }

    @Test
    fun `generating knockout bracket for 2 players creates 1 Final match`() = runTest {
        val participants = listOf(
            TournamentParticipant(playerItemId = 1L, playerName = "Player 1", groupIndex = 0),
            TournamentParticipant(playerItemId = 2L, playerName = "Player 2", groupIndex = 0)
        )

        val matches = useCase(tournamentId = 1L, qualifiers = participants)

        assertEquals(1, matches.size)
        assertEquals(MatchStage.FINAL, matches[0].stage)
        assertEquals("Player 1", matches[0].playerOneName)
        assertEquals("Player 2", matches[0].playerTwoName)
    }
}
