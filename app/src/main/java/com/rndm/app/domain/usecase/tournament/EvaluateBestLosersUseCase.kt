package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStatus
import javax.inject.Inject

data class LoserCandidate(
    val matchId: Long,
    val playerName: String,
    val clubName: String?,
    val lostByPenalties: Boolean,
    val goalDifference: Int, // (loserScore - winnerScore), e.g. 1 - 2 = -1
    val goalsScored: Int,
    val penaltyGoalsScored: Int = 0
) : Comparable<LoserCandidate> {
    override fun compareTo(other: LoserCandidate): Int {
        // 1. Lost by penalty shootout is top priority (considered 0 goal difference in regular time)
        if (this.lostByPenalties != other.lostByPenalties) {
            return if (this.lostByPenalties) 1 else -1
        }
        if (this.lostByPenalties && other.lostByPenalties) {
            val penaltyDiff = this.penaltyGoalsScored.compareTo(other.penaltyGoalsScored)
            if (penaltyDiff != 0) return penaltyDiff
        }

        // 2. Goal difference (e.g. -1 > -3)
        val gdDiff = this.goalDifference.compareTo(other.goalDifference)
        if (gdDiff != 0) return gdDiff

        // 3. Goals scored in match (e.g. 2 > 1)
        val goalsDiff = this.goalsScored.compareTo(other.goalsScored)
        if (goalsDiff != 0) return goalsDiff

        // 4. Stable tie-break
        return other.playerName.compareTo(this.playerName)
    }
}

class EvaluateBestLosersUseCase @Inject constructor() {

    /**
     * Extracts and ranks defeated players from finished matches in a round.
     * Highest ranked loser is at index 0.
     */
    operator fun invoke(matches: List<Match>): List<LoserCandidate> {
        val finishedMatches = matches.filter {
            it.status == MatchStatus.FINISHED &&
                    it.scoreOne != null &&
                    it.scoreTwo != null &&
                    !it.winnerName.isNullOrBlank()
        }

        val candidates = finishedMatches.mapNotNull { match ->
            val p1 = match.playerOneName
            val p2 = match.playerTwoName ?: return@mapNotNull null
            val s1 = match.scoreOne ?: 0
            val s2 = match.scoreTwo ?: 0
            val winner = match.winnerName

            val isP1Winner = winner == p1
            val loserName = if (isP1Winner) p2 else p1
            val loserClub = if (isP1Winner) match.playerTwoClub else match.playerOneClub

            val loserScore = if (isP1Winner) s2 else s1
            val winnerScore = if (isP1Winner) s1 else s2

            val lostByPenalties = match.penaltyScoreOne != null &&
                    match.penaltyScoreTwo != null &&
                    s1 == s2

            val loserPenaltyScore = if (lostByPenalties) {
                if (isP1Winner) match.penaltyScoreTwo ?: 0 else match.penaltyScoreOne ?: 0
            } else 0

            LoserCandidate(
                matchId = match.id,
                playerName = loserName,
                clubName = loserClub,
                lostByPenalties = lostByPenalties,
                goalDifference = loserScore - winnerScore,
                goalsScored = loserScore,
                penaltyGoalsScored = loserPenaltyScore
            )
        }

        // Sort descending (best loser first)
        return candidates.sortedDescending()
    }
}
