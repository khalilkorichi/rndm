package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.GroupStanding
import com.rndm.app.domain.model.TournamentGroup
import com.rndm.app.domain.model.TournamentParticipant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterminePromotionCandidatesUseCaseTest {

    private val useCase = DeterminePromotionCandidatesUseCase()

    @Test
    fun `when 3 groups have 2 qualifiers each (6 total), needs 2 promoted 3rd places to reach 8 bracket`() {
        val pA1 = TournamentParticipant(playerItemId = 1, playerName = "A1", groupIndex = 0)
        val pA2 = TournamentParticipant(playerItemId = 2, playerName = "A2", groupIndex = 0)
        val pA3 = TournamentParticipant(playerItemId = 3, playerName = "A3", groupIndex = 0) // 3 pts, GD +1

        val pB1 = TournamentParticipant(playerItemId = 4, playerName = "B1", groupIndex = 1)
        val pB2 = TournamentParticipant(playerItemId = 5, playerName = "B2", groupIndex = 1)
        val pB3 = TournamentParticipant(playerItemId = 6, playerName = "B3", groupIndex = 1) // 4 pts, GD +2

        val pC1 = TournamentParticipant(playerItemId = 7, playerName = "C1", groupIndex = 2)
        val pC2 = TournamentParticipant(playerItemId = 8, playerName = "C2", groupIndex = 2)
        val pC3 = TournamentParticipant(playerItemId = 9, playerName = "C3", groupIndex = 2) // 1 pt, GD -2

        val groupA = TournamentGroup(
            groupIndex = 0,
            groupName = "A",
            standings = listOf(
                GroupStanding(participant = pA1, points = 6, rank = 1, isQualified = true),
                GroupStanding(participant = pA2, points = 4, rank = 2, isQualified = true),
                GroupStanding(participant = pA3, points = 3, goalDifference = 1, rank = 3)
            )
        )

        val groupB = TournamentGroup(
            groupIndex = 1,
            groupName = "B",
            standings = listOf(
                GroupStanding(participant = pB1, points = 6, rank = 1, isQualified = true),
                GroupStanding(participant = pB2, points = 4, rank = 2, isQualified = true),
                GroupStanding(participant = pB3, points = 4, goalDifference = 2, rank = 3)
            )
        )

        val groupC = TournamentGroup(
            groupIndex = 2,
            groupName = "C",
            standings = listOf(
                GroupStanding(participant = pC1, points = 6, rank = 1, isQualified = true),
                GroupStanding(participant = pC2, points = 4, rank = 2, isQualified = true),
                GroupStanding(participant = pC3, points = 1, goalDifference = -2, rank = 3)
            )
        )

        val decision = useCase(listOf(groupA, groupB, groupC), qualifiersPerGroup = 2)

        assertEquals(6, decision.directQualifiers.size)
        assertEquals(8, decision.targetBracketSize)
        assertEquals(2, decision.promotedCandidates.size)
        assertFalse(decision.isTieBreakNeeded)
        assertEquals("B3", decision.promotedCandidates[0].playerName)
        assertEquals("A3", decision.promotedCandidates[1].playerName)
    }

    @Test
    fun `when two 3rd place candidates have identical points and goal difference, flags tie break`() {
        val pA3 = TournamentParticipant(playerItemId = 3, playerName = "A3", groupIndex = 0) // 3 pts, GD 0
        val pB3 = TournamentParticipant(playerItemId = 6, playerName = "B3", groupIndex = 1) // 3 pts, GD 0

        val groupA = TournamentGroup(
            groupIndex = 0,
            groupName = "A",
            standings = listOf(
                GroupStanding(participant = TournamentParticipant(playerItemId = 1, playerName = "A1"), rank = 1, isQualified = true),
                GroupStanding(participant = TournamentParticipant(playerItemId = 2, playerName = "A2"), rank = 2, isQualified = true),
                GroupStanding(participant = pA3, points = 3, goalDifference = 0, goalsFor = 2, rank = 3)
            )
        )

        val groupB = TournamentGroup(
            groupIndex = 1,
            groupName = "B",
            standings = listOf(
                GroupStanding(participant = TournamentParticipant(playerItemId = 4, playerName = "B1"), rank = 1, isQualified = true),
                GroupStanding(participant = TournamentParticipant(playerItemId = 5, playerName = "B2"), rank = 2, isQualified = true),
                GroupStanding(participant = pB3, points = 3, goalDifference = 0, goalsFor = 2, rank = 3)
            )
        )

        // Only 1 spot available for promotion (direct = 4 -> target = 4, but if target was 8 and needed 1)
        // Let's test shortfall = 1 with a 3rd group
        val pC1 = TournamentParticipant(playerItemId = 7, playerName = "C1", groupIndex = 2)
        val pC2 = TournamentParticipant(playerItemId = 8, playerName = "C2", groupIndex = 2)
        val pC3 = TournamentParticipant(playerItemId = 9, playerName = "C3", groupIndex = 2)

        val groupC = TournamentGroup(
            groupIndex = 2,
            groupName = "C",
            standings = listOf(
                GroupStanding(participant = pC1, rank = 1, isQualified = true),
                GroupStanding(participant = pC2, rank = 2, isQualified = true),
                GroupStanding(participant = pC3, points = 6, goalDifference = 5, rank = 3) // Guaranteed #1 3rd place
            )
        )

        // Direct count = 6. Target = 8. Shortfall = 2.
        // C3 takes spot 1 (6 pts).
        // Spot 2 is tied between A3 (3 pts, 0 GD, 2 GF) and B3 (3 pts, 0 GD, 2 GF)!
        val decision = useCase(listOf(groupA, groupB, groupC), qualifiersPerGroup = 2)

        assertTrue(decision.isTieBreakNeeded)
        assertEquals(2, decision.tiedCandidates.size)
        assertTrue(decision.tiedCandidates.any { it.playerName == "A3" })
        assertTrue(decision.tiedCandidates.any { it.playerName == "B3" })
    }
}
