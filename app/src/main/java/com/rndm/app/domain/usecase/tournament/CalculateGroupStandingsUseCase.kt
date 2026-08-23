package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.GroupStanding
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentParticipant
import javax.inject.Inject

class CalculateGroupStandingsUseCase @Inject constructor() {

    operator fun invoke(
        participants: List<TournamentParticipant>,
        matches: List<Match>,
        qualifiersCount: Int = 2
    ): List<GroupStanding> {
        val standingsMap = participants.associateWith { participant ->
            StandingAccumulator(participant = participant)
        }.toMutableMap()

        matches.filter { it.status == MatchStatus.FINISHED && it.scoreOne != null && it.scoreTwo != null }
            .forEach { match ->
                val p1 = participants.firstOrNull { it.playerName == match.playerOneName }
                val p2 = participants.firstOrNull { it.playerName == match.playerTwoName }

                if (p1 != null && p2 != null) {
                    val s1 = standingsMap[p1]
                    val s2 = standingsMap[p2]
                    val score1 = match.scoreOne ?: 0
                    val score2 = match.scoreTwo ?: 0

                    if (s1 != null && s2 != null) {
                        s1.played++
                        s2.played++
                        s1.goalsFor += score1
                        s1.goalsAgainst += score2
                        s2.goalsFor += score2
                        s2.goalsAgainst += score1

                        when {
                            score1 > score2 -> {
                                s1.won++
                                s1.points += 3
                                s2.lost++
                            }
                            score2 > score1 -> {
                                s2.won++
                                s2.points += 3
                                s1.lost++
                            }
                            else -> {
                                s1.drawn++
                                s1.points += 1
                                s2.drawn++
                                s2.points += 1
                            }
                        }
                    }
                }
            }

        val sorted = standingsMap.values.sortedWith(
            compareByDescending<StandingAccumulator> { it.points }
                .thenByDescending { it.goalsFor - it.goalsAgainst }
                .thenByDescending { it.goalsFor }
                .thenBy { it.participant.playerName }
        )

        return sorted.mapIndexed { index, item ->
            val rank = index + 1
            GroupStanding(
                participant = item.participant,
                played = item.played,
                won = item.won,
                drawn = item.drawn,
                lost = item.lost,
                goalsFor = item.goalsFor,
                goalsAgainst = item.goalsAgainst,
                goalDifference = item.goalsFor - item.goalsAgainst,
                points = item.points,
                rank = rank,
                isQualified = rank <= qualifiersCount,
                isPromotionCandidate = rank == qualifiersCount + 1
            )
        }
    }

    private data class StandingAccumulator(
        val participant: TournamentParticipant,
        var played: Int = 0,
        var won: Int = 0,
        var drawn: Int = 0,
        var lost: Int = 0,
        var goalsFor: Int = 0,
        var goalsAgainst: Int = 0,
        var points: Int = 0
    )
}
