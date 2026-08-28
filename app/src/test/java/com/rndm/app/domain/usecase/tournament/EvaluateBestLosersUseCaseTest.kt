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

    @Test
    fun `evaluateBestLosers ranks Penalties top, then Extra Time, then Regular Time regardless of score difference`() {
        // Match 1: Player A vs Player B -> Ended in Extra Time 1 - 2 (GD -1)
        val matchExtraTime = Match(
            id = 10L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player A",
            playerTwoName = "Player B",
            scoreOne = 1,
            scoreTwo = 2,
            isExtraTime = true,
            winnerName = "Player B",
            status = MatchStatus.FINISHED
        )

        // Match 2: Player C vs Player D -> Ended in Regular Time 0 - 1 (GD -1)
        val matchRegularTime = Match(
            id = 20L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player C",
            playerTwoName = "Player D",
            scoreOne = 0,
            scoreTwo = 1,
            isExtraTime = false,
            winnerName = "Player D",
            status = MatchStatus.FINISHED
        )

        // Match 3: Player E vs Player F -> Ended in Penalties 2 - 2 (PK 4 - 5)
        val matchPenalties = Match(
            id = 30L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player E",
            playerTwoName = "Player F",
            scoreOne = 2,
            scoreTwo = 2,
            penaltyScoreOne = 4,
            penaltyScoreTwo = 5,
            isExtraTime = true,
            winnerName = "Player F",
            status = MatchStatus.FINISHED
        )

        // Match 4: Player G vs Player H -> Ended in Extra Time 2 - 3 (GD -1, Goals 2)
        val matchExtraTimeHighGoals = Match(
            id = 40L,
            stage = MatchStage.ROUND_OF_16,
            playerOneName = "Player G",
            playerTwoName = "Player H",
            scoreOne = 2,
            scoreTwo = 3,
            isExtraTime = true,
            winnerName = "Player H",
            status = MatchStatus.FINISHED
        )

        val rankedLosers = useCase(listOf(matchExtraTime, matchRegularTime, matchPenalties, matchExtraTimeHighGoals))

        assertEquals(4, rankedLosers.size)

        // Rank 1: Penalties loser (Player E)
        assertEquals("Player E", rankedLosers[0].playerName)
        assertEquals(true, rankedLosers[0].lostByPenalties)

        // Rank 2: Extra Time loser with 2 goals scored (Player G: 2-3)
        assertEquals("Player G", rankedLosers[1].playerName)
        assertEquals(true, rankedLosers[1].lostInExtraTime)
        assertEquals(false, rankedLosers[1].lostByPenalties)
        assertEquals(2, rankedLosers[1].goalsScored)

        // Rank 3: Extra Time loser with 1 goal scored (Player A: 1-2)
        assertEquals("Player A", rankedLosers[2].playerName)
        assertEquals(true, rankedLosers[2].lostInExtraTime)
        assertEquals(false, rankedLosers[2].lostByPenalties)
        assertEquals(1, rankedLosers[2].goalsScored)

        // Rank 4: Regular Time loser (Player C: 0-1)
        assertEquals("Player C", rankedLosers[3].playerName)
        assertEquals(false, rankedLosers[3].lostInExtraTime)
        assertEquals(false, rankedLosers[3].lostByPenalties)
    }
}
