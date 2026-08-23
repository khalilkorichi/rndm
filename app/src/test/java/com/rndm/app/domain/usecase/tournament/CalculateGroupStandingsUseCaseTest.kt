package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentParticipant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateGroupStandingsUseCaseTest {

    private val useCase = CalculateGroupStandingsUseCase()

    @Test
    fun `calculate standings sorts by points, then goal difference, then goals for`() {
        val p1 = TournamentParticipant(id = 1, playerItemId = 1, playerName = "Ali")
        val p2 = TournamentParticipant(id = 2, playerItemId = 2, playerName = "Omar")
        val p3 = TournamentParticipant(id = 3, playerItemId = 3, playerName = "Zaid")
        val participants = listOf(p1, p2, p3)

        // Ali 3 - 0 Omar (Ali 3 pts, Omar 0 pts)
        // Omar 2 - 1 Zaid (Omar 3 pts, Zaid 0 pts)
        // Ali 1 - 1 Zaid (Ali 4 pts, Zaid 1 pt)
        val matches = listOf(
            Match(
                stage = MatchStage.GROUP_STAGE,
                playerOneName = "Ali",
                playerTwoName = "Omar",
                scoreOne = 3,
                scoreTwo = 0,
                status = MatchStatus.FINISHED
            ),
            Match(
                stage = MatchStage.GROUP_STAGE,
                playerOneName = "Omar",
                playerTwoName = "Zaid",
                scoreOne = 2,
                scoreTwo = 1,
                status = MatchStatus.FINISHED
            ),
            Match(
                stage = MatchStage.GROUP_STAGE,
                playerOneName = "Ali",
                playerTwoName = "Zaid",
                scoreOne = 1,
                scoreTwo = 1,
                status = MatchStatus.FINISHED
            )
        )

        val standings = useCase(participants, matches, qualifiersCount = 2)

        assertEquals("Ali", standings[0].participant.playerName)
        assertEquals(4, standings[0].points)
        assertEquals(3, standings[0].goalDifference)
        assertTrue(standings[0].isQualified)

        assertEquals("Omar", standings[1].participant.playerName)
        assertEquals(3, standings[1].points)
        assertEquals(-2, standings[1].goalDifference)
        assertTrue(standings[1].isQualified)

        assertEquals("Zaid", standings[2].participant.playerName)
        assertEquals(1, standings[2].points)
        assertEquals(-1, standings[2].goalDifference)
        assertTrue(standings[2].isPromotionCandidate)
    }
}
