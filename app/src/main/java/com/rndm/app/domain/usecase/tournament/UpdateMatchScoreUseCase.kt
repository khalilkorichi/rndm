package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.repository.SyncRepository
import com.rndm.app.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateMatchScoreUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val evaluateBestLosersUseCase: EvaluateBestLosersUseCase,
    private val syncRepository: SyncRepository
) {

    suspend operator fun invoke(
        tournamentId: Long,
        match: Match,
        scoreOne: Int,
        scoreTwo: Int,
        penaltyScoreOne: Int? = null,
        penaltyScoreTwo: Int? = null
    ) {
        val oldScoreOne = match.scoreOne
        val oldScoreTwo = match.scoreTwo

        val winnerName = when {
            scoreOne > scoreTwo -> match.playerOneName
            scoreTwo > scoreOne -> match.playerTwoName ?: "TBD"
            penaltyScoreOne != null && penaltyScoreTwo != null -> {
                if (penaltyScoreOne > penaltyScoreTwo) match.playerOneName else match.playerTwoName ?: "TBD"
            }
            else -> match.playerOneName
        }
        val loserName = if (winnerName == match.playerOneName) match.playerTwoName ?: "TBD" else match.playerOneName

        val updatedMatch = match.copy(
            scoreOne = scoreOne,
            scoreTwo = scoreTwo,
            penaltyScoreOne = penaltyScoreOne,
            penaltyScoreTwo = penaltyScoreTwo,
            winnerName = winnerName,
            status = MatchStatus.FINISHED,
            updatedAt = System.currentTimeMillis()
        )
        tournamentRepository.updateMatch(updatedMatch)

        // Advance in knockout bracket if applicable
        if (match.stage != MatchStage.GROUP_STAGE) {
            val allMatchesFromRepo = tournamentRepository.getMatches(tournamentId).first()
            val allMatches = allMatchesFromRepo.map { if (it.id == updatedMatch.id) updatedMatch else it }
            advanceKnockoutWinners(tournamentId, updatedMatch, winnerName, loserName, allMatches)
        }

        // Sync to remote Firestore and log audit trail if remote tournament
        syncRepository.syncMatchScore(
            tournamentId = tournamentId,
            match = updatedMatch,
            oldScoreOne = oldScoreOne,
            oldScoreTwo = oldScoreTwo
        )
    }

    private suspend fun advanceKnockoutWinners(
        tournamentId: Long,
        match: Match,
        winner: String,
        loser: String,
        allMatches: List<Match>
    ) {
        when (match.stage) {
            MatchStage.ROUND_OF_32 -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val targetR16Index = (matchIdx - 1) / 2 + 1
                val isFirstInPair = (matchIdx % 2) != 0
                val targetMatch = allMatches.firstOrNull { it.stage == MatchStage.ROUND_OF_16 && it.bracketMatchIndex == targetR16Index }
                targetMatch?.let {
                    val updated = if (isFirstInPair) it.copy(playerOneName = winner) else it.copy(playerTwoName = winner)
                    tournamentRepository.updateMatch(updated)
                }
            }

            MatchStage.ROUND_OF_16 -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val targetQfIndex = (matchIdx - 1) / 2 + 1
                val isFirstInPair = (matchIdx % 2) != 0
                val targetMatch = allMatches.firstOrNull { it.stage == MatchStage.QUARTER_FINALS && it.bracketMatchIndex == targetQfIndex }
                targetMatch?.let {
                    val updated = if (isFirstInPair) it.copy(playerOneName = winner) else it.copy(playerTwoName = winner)
                    tournamentRepository.updateMatch(updated)
                }
            }

            MatchStage.QUARTER_FINALS -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val semi1 = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 1 }
                val semi2 = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 2 }

                when (matchIdx) {
                    1 -> semi1?.let { tournamentRepository.updateMatch(it.copy(playerOneName = winner)) }
                    2 -> semi1?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = winner)) }
                    3 -> {
                        // In 6-player tournaments, QF3 winner goes to semi2
                        semi2?.let { tournamentRepository.updateMatch(it.copy(playerOneName = winner)) }
                    }
                    4 -> semi2?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = winner)) }
                }
            }

            MatchStage.SEMI_FINALS -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val finalMatch = allMatches.firstOrNull { it.stage == MatchStage.FINAL }
                val thirdMatch = allMatches.firstOrNull { it.stage == MatchStage.THIRD_PLACE }

                if (matchIdx == 1) {
                    finalMatch?.let { tournamentRepository.updateMatch(it.copy(playerOneName = winner)) }
                    thirdMatch?.let { tournamentRepository.updateMatch(it.copy(playerOneName = loser)) }
                } else {
                    finalMatch?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = winner)) }
                    thirdMatch?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = loser)) }
                }
            }

            MatchStage.FINAL -> {
                tournamentRepository.updateTournamentStage(tournamentId, TournamentStage.COMPLETED)
            }

            else -> Unit
        }

        // Dynamically resolve and update any pending Lucky Loser slots across all stages
        updateLuckyLoserSlots(allMatches, match, loser)
    }

    private suspend fun updateLuckyLoserSlots(allMatches: List<Match>, currentFinishedMatch: Match, currentLoserName: String) {
        // 1. Check Lucky Loser slots in Round of 16 (for 13..15 players)
        val r16LuckySlots = allMatches.filter {
            it.stage == MatchStage.ROUND_OF_16 && (it.isPlayerTwoLuckyLoser || it.playerTwoName == "أحسن خاسر") && it.status != MatchStatus.FINISHED
        }
        if (r16LuckySlots.isNotEmpty()) {
            val regularR16Matches = allMatches.filter {
                it.stage == MatchStage.ROUND_OF_16 && !it.isPlayerTwoLuckyLoser && it.status == MatchStatus.FINISHED
            }
            val bestLosers = evaluateBestLosersUseCase(regularR16Matches)
            r16LuckySlots.forEachIndexed { index, slotMatch ->
                val loserCandidate = bestLosers.getOrNull(index)
                if (loserCandidate != null) {
                    tournamentRepository.updateMatch(
                        slotMatch.copy(
                            playerTwoName = loserCandidate.playerName,
                            playerTwoClub = loserCandidate.clubName,
                            isPlayerTwoLuckyLoser = true
                        )
                    )
                }
            }
        }

        // 2. Check Lucky Loser slots in Quarter Finals (for 7 players)
        val qfLuckySlots = allMatches.filter {
            it.stage == MatchStage.QUARTER_FINALS && (it.isPlayerTwoLuckyLoser || it.playerTwoName == "أحسن خاسر") && it.status != MatchStatus.FINISHED
        }
        if (qfLuckySlots.isNotEmpty()) {
            val regularQfMatches = allMatches.filter {
                it.stage == MatchStage.QUARTER_FINALS && !it.isPlayerTwoLuckyLoser && it.status == MatchStatus.FINISHED
            }
            val bestLosers = evaluateBestLosersUseCase(regularQfMatches)
            qfLuckySlots.forEachIndexed { index, slotMatch ->
                val loserCandidate = bestLosers.getOrNull(index)
                if (loserCandidate != null) {
                    tournamentRepository.updateMatch(
                        slotMatch.copy(
                            playerTwoName = loserCandidate.playerName,
                            playerTwoClub = loserCandidate.clubName,
                            isPlayerTwoLuckyLoser = true
                        )
                    )
                }
            }
        }

        // 3. Check Lucky Loser slots in Semi Finals (for 3, 5, 6 players)
        val sfLuckySlots = allMatches.filter {
            it.stage == MatchStage.SEMI_FINALS && (it.isPlayerTwoLuckyLoser || it.playerTwoName == "أحسن خاسر") && it.status != MatchStatus.FINISHED
        }
        if (sfLuckySlots.isNotEmpty()) {
            val qfMatches = allMatches.filter { it.stage == MatchStage.QUARTER_FINALS && it.status == MatchStatus.FINISHED }
            if (qfMatches.isNotEmpty()) {
                // For 5 or 6 players: Lucky loser evaluated from finished Quarter-Finals
                val bestLosers = evaluateBestLosersUseCase(qfMatches)
                sfLuckySlots.forEachIndexed { index, slotMatch ->
                    val loserCandidate = bestLosers.getOrNull(index)
                    if (loserCandidate != null) {
                        tournamentRepository.updateMatch(
                            slotMatch.copy(
                                playerTwoName = loserCandidate.playerName,
                                playerTwoClub = loserCandidate.clubName,
                                isPlayerTwoLuckyLoser = true
                            )
                        )
                    }
                }
            } else {
                // For 3 players: Lucky loser comes directly from Semi-Final Match 1
                val sf1Match = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 1 && it.status == MatchStatus.FINISHED }
                if (sf1Match != null) {
                    val bestLosers = evaluateBestLosersUseCase(listOf(sf1Match))
                    val topLoser = bestLosers.firstOrNull()
                    if (topLoser != null) {
                        sfLuckySlots.firstOrNull()?.let { slotMatch ->
                            tournamentRepository.updateMatch(
                                slotMatch.copy(
                                    playerTwoName = topLoser.playerName,
                                    playerTwoClub = topLoser.clubName,
                                    isPlayerTwoLuckyLoser = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
