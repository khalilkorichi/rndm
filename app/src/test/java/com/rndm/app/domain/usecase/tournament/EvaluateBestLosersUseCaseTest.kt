package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateBestLosersUseCaseTest {

    private val useCase = EvaluateBestLosersUseCase()

    @Test
    fun `evaluateBestLosers ranks penalty losers highest, then smallest goal difference, then most goals scored`() {
        // Player 1 lost 0 - 3 to Player 2 (GD: -3, Goals: 0)
        val match1 = Match(
            id = 1L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player 1",
            playerTwoName = "Player 2",
            scoreOne = 0,
            scoreTwo = 3,
            winnerName = "Player 2",
            status = MatchStatus.FINISHED
        )

        // Player 4 lost 1 - 2 to Player 3 (GD: -1, Goals: 1)
        val match2 = Match(
            id = 2L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player 3",
            playerTwoName = "Player 4",
            scoreOne = 2,
            scoreTwo = 1,
            winnerName = "Player 3",
            status = MatchStatus.FINISHED
        )

        // Player 5 lost on penalties to Player 6 (Regular: 2 - 2, Penalties: 3 - 4)
        val match3 = Match(
            id = 3L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player 5",
            playerTwoName = "Player 6",
            scoreOne = 2,
            scoreTwo = 2,
            penaltyScoreOne = 3,
            penaltyScoreTwo = 4,
            winnerName = "Player 6",
            status = MatchStatus.FINISHED
        )

        // Player 7 lost 2 - 3 to Player 8 (GD: -1, Goals: 2) -> Better than Player 4 because goals scored (2 > 1)
        val match4 = Match(
            id = 4L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player 7",
            playerTwoName = "Player 8",
            scoreOne = 2,
            scoreTwo = 3,
            winnerName = "Player 8",
            status = MatchStatus.FINISHED
        )

        val rankedLosers = useCase(listOf(match1, match2, match3, match4))

        assertEquals(4, rankedLosers.size)
        // 1st: Player 5 (lost on penalties)
        assertEquals("Player 5", rankedLosers[0].playerName)
        assertEquals(true, rankedLosers[0].lostByPenalties)

        // 2nd: Player 7 (lost by 1 goal with 2 goals scored: 2-3)
        assertEquals("Player 7", rankedLosers[1].playerName)
        assertEquals(-1, rankedLosers[1].goalDifference)
        assertEquals(2, rankedLosers[1].goalsScored)

        // 3rd: Player 4 (lost by 1 goal with 1 goal scored: 1-2)
        assertEquals("Player 4", rankedLosers[2].playerName)
        assertEquals(-1, rankedLosers[2].goalDifference)
        assertEquals(1, rankedLosers[2].goalsScored)

        // 4th: Player 1 (lost by 3 goals: 0-3)
        assertEquals("Player 1", rankedLosers[3].playerName)
        assertEquals(-3, rankedLosers[3].goalDifference)
    }
}
