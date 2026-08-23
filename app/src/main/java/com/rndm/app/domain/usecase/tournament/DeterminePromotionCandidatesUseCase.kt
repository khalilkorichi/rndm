package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.GroupStanding
import com.rndm.app.domain.model.TournamentGroup
import com.rndm.app.domain.model.TournamentParticipant
import javax.inject.Inject

data class PromotionDecision(
    val directQualifiers: List<TournamentParticipant>,
    val promotedCandidates: List<TournamentParticipant>,
    val isTieBreakNeeded: Boolean,
    val tiedCandidates: List<TournamentParticipant>,
    val targetBracketSize: Int
)

class DeterminePromotionCandidatesUseCase @Inject constructor() {

    operator fun invoke(groups: List<TournamentGroup>, qualifiersPerGroup: Int = 2): PromotionDecision {
        val directQualifiers = groups.flatMap { group ->
            group.standings.filter { it.isQualified }.map { it.participant }
        }

        val directCount = directQualifiers.size
        val targetBracketSize = when {
            directCount <= 2 -> 2
            directCount <= 4 -> 4
            directCount <= 8 -> 8
            else -> 16
        }

        val shortfall = targetBracketSize - directCount
        if (shortfall <= 0) {
            return PromotionDecision(
                directQualifiers = directQualifiers,
                promotedCandidates = emptyList(),
                isTieBreakNeeded = false,
                tiedCandidates = emptyList(),
                targetBracketSize = targetBracketSize
            )
        }

        val thirdPlaceStandings = groups.mapNotNull { group ->
            group.standings.firstOrNull { it.rank == qualifiersPerGroup + 1 }
        }.sortedWith(
            compareByDescending<GroupStanding> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
        )

        if (thirdPlaceStandings.size < shortfall) {
            return PromotionDecision(
                directQualifiers = directQualifiers,
                promotedCandidates = thirdPlaceStandings.map { it.participant },
                isTieBreakNeeded = false,
                tiedCandidates = emptyList(),
                targetBracketSize = targetBracketSize
            )
        }

        val boundaryIndex = shortfall - 1
        val boundaryCandidate = thirdPlaceStandings[boundaryIndex]
        val nextCandidate = thirdPlaceStandings.getOrNull(boundaryIndex + 1)

        val isTiedWithNext = nextCandidate != null &&
                boundaryCandidate.points == nextCandidate.points &&
                boundaryCandidate.goalDifference == nextCandidate.goalDifference &&
                boundaryCandidate.goalsFor == nextCandidate.goalsFor

        if (isTiedWithNext) {
            val tiedStandings = thirdPlaceStandings.filter {
                it.points == boundaryCandidate.points &&
                        it.goalDifference == boundaryCandidate.goalDifference &&
                        it.goalsFor == boundaryCandidate.goalsFor
            }
            return PromotionDecision(
                directQualifiers = directQualifiers,
                promotedCandidates = thirdPlaceStandings.take(boundaryIndex).map { it.participant },
                isTieBreakNeeded = true,
                tiedCandidates = tiedStandings.map { it.participant },
                targetBracketSize = targetBracketSize
            )
        }

        return PromotionDecision(
            directQualifiers = directQualifiers,
            promotedCandidates = thirdPlaceStandings.take(shortfall).map { it.participant },
            isTieBreakNeeded = false,
            tiedCandidates = emptyList(),
            targetBracketSize = targetBracketSize
        )
    }
}
