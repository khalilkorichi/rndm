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
        penaltyScoreTwo: Int? = null,
        isExtraTime: Boolean = false
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
            isExtraTime = isExtraTime,
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

        // Fetch fresh matches state after bracket advancement
        val allFinalMatches = tournamentRepository.getMatches(tournamentId).first()

        // Sync to remote Firestore and log audit trail if remote tournament
        syncRepository.syncMatchScore(
            tournamentId = tournamentId,
            match = updatedMatch,
            oldScoreOne = oldScoreOne,
            oldScoreTwo = oldScoreTwo
        )

        // Sync all updated bracket/group matches to remote Firestore in batch
        syncRepository.syncTournamentMatches(tournamentId, allFinalMatches)
    }

    suspend fun directQualifyMatch(
        tournamentId: Long,
        match: Match
    ) {
        val winnerName = when {
            match.isPlayerTwoLuckyLoser || match.playerTwoName == "أحسن خاسر" || match.playerTwoName.isNullOrBlank() || match.playerTwoName == "BYE" -> match.playerOneName
            match.isPlayerOneLuckyLoser || match.playerOneName == "أحسن خاسر" || match.playerOneName.isBlank() || match.playerOneName == "BYE" -> match.playerTwoName ?: "TBD"
            else -> match.playerOneName
        }
        val loserName = "BYE"

        val updatedMatch = match.copy(
            winnerName = winnerName,
            status = MatchStatus.FINISHED,
            updatedAt = System.currentTimeMillis()
        )
        tournamentRepository.updateMatch(updatedMatch)

        // Advance winner in knockout bracket
        if (match.stage != MatchStage.GROUP_STAGE && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL) {
            val allMatchesFromRepo = tournamentRepository.getMatches(tournamentId).first()
            val allMatches = allMatchesFromRepo.map { if (it.id == updatedMatch.id) updatedMatch else it }
            advanceKnockoutWinners(tournamentId, updatedMatch, winnerName, loserName, allMatches)
        }

        // Fetch fresh matches state after bracket advancement
        val allFinalMatches = tournamentRepository.getMatches(tournamentId).first()

        // Sync to remote Firestore
        syncRepository.syncMatchScore(
            tournamentId = tournamentId,
            match = updatedMatch,
            oldScoreOne = match.scoreOne,
            oldScoreTwo = match.scoreTwo
        )
        syncRepository.syncTournamentMatches(tournamentId, allFinalMatches)
    }

    suspend fun undoDirectQualifyMatch(
        tournamentId: Long,
        match: Match
    ) {
        val updatedMatch = match.copy(
            winnerName = null,
            scoreOne = null,
            scoreTwo = null,
            penaltyScoreOne = null,
            penaltyScoreTwo = null,
            isExtraTime = false,
            status = MatchStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        tournamentRepository.updateMatch(updatedMatch)

        // Revert winner advancement in knockout bracket
        if (match.stage != MatchStage.GROUP_STAGE && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL) {
            val allMatchesFromRepo = tournamentRepository.getMatches(tournamentId).first()
            val allMatches = allMatchesFromRepo.map { if (it.id == updatedMatch.id) updatedMatch else it }
            revertKnockoutWinners(tournamentId, updatedMatch, allMatches)
        }

        // Fetch fresh matches and update lucky loser slots if applicable
        val allMatchesAfterRevert = tournamentRepository.getMatches(tournamentId).first()
        updateLuckyLoserSlots(allMatchesAfterRevert, updatedMatch, "")

        val allFinalMatches = tournamentRepository.getMatches(tournamentId).first()

        // Sync to remote Firestore
        syncRepository.syncMatchScore(
            tournamentId = tournamentId,
            match = updatedMatch,
            oldScoreOne = match.scoreOne,
            oldScoreTwo = match.scoreTwo
        )
        syncRepository.syncTournamentMatches(tournamentId, allFinalMatches)
    }

    private suspend fun revertKnockoutWinners(
        tournamentId: Long,
        match: Match,
        allMatches: List<Match>
    ) {
        when (match.stage) {
            MatchStage.ROUND_OF_32 -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val targetR16Index = (matchIdx - 1) / 2 + 1
                val isFirstInPair = (matchIdx % 2) != 0
                val targetMatch = allMatches.firstOrNull { it.stage == MatchStage.ROUND_OF_16 && it.bracketMatchIndex == targetR16Index }
                targetMatch?.let {
                    val placeholder = "فائز دور الـ 32 ($matchIdx)"
                    val updated = if (isFirstInPair) it.copy(playerOneName = placeholder, playerOneClub = null) else it.copy(playerTwoName = placeholder, playerTwoClub = null)
                    tournamentRepository.updateMatch(updated)
                }
            }

            MatchStage.ROUND_OF_16 -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val targetQfIndex = (matchIdx - 1) / 2 + 1
                val isFirstInPair = (matchIdx % 2) != 0
                val targetMatch = allMatches.firstOrNull { it.stage == MatchStage.QUARTER_FINALS && it.bracketMatchIndex == targetQfIndex }
                targetMatch?.let {
                    val placeholder = "فائز دور الـ 16 ($matchIdx)"
                    val updated = if (isFirstInPair) it.copy(playerOneName = placeholder, playerOneClub = null) else it.copy(playerTwoName = placeholder, playerTwoClub = null)
                    tournamentRepository.updateMatch(updated)
                }
            }

            MatchStage.QUARTER_FINALS -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val semi1 = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 1 }
                val semi2 = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 2 }
                val placeholder = "فائز ربع النهائي $matchIdx"

                when (matchIdx) {
                    1 -> semi1?.let { tournamentRepository.updateMatch(it.copy(playerOneName = placeholder, playerOneClub = null)) }
                    2 -> semi1?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = placeholder, playerTwoClub = null)) }
                    3 -> semi2?.let { tournamentRepository.updateMatch(it.copy(playerOneName = placeholder, playerOneClub = null)) }
                    4 -> semi2?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = placeholder, playerTwoClub = null)) }
                }
            }

            MatchStage.SEMI_FINALS -> {
                val matchIdx = match.bracketMatchIndex ?: 1
                val finalMatch = allMatches.firstOrNull { it.stage == MatchStage.FINAL }
                val thirdMatch = allMatches.firstOrNull { it.stage == MatchStage.THIRD_PLACE }
                val winnerPlaceholder = "فائز نصف النهائي $matchIdx"
                val loserPlaceholder = "خاسر نصف النهائي $matchIdx"

                if (matchIdx == 1) {
                    finalMatch?.let { tournamentRepository.updateMatch(it.copy(playerOneName = winnerPlaceholder, playerOneClub = null)) }
                    thirdMatch?.let { tournamentRepository.updateMatch(it.copy(playerOneName = loserPlaceholder, playerOneClub = null)) }
                } else {
                    finalMatch?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = winnerPlaceholder, playerTwoClub = null)) }
                    thirdMatch?.let { tournamentRepository.updateMatch(it.copy(playerTwoName = loserPlaceholder, playerTwoClub = null)) }
                }
            }

            else -> Unit
        }
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
                    3 -> semi2?.let { tournamentRepository.updateMatch(it.copy(playerOneName = winner)) }
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
        // 1. Check Lucky Loser slots in Round of 16 (for 9..15 players)
        val r16LuckySlots = allMatches.filter {
            it.stage == MatchStage.ROUND_OF_16 && 
            (it.isPlayerTwoLuckyLoser || it.isPlayerOneLuckyLoser || it.playerTwoName == "أحسن خاسر" || it.playerOneName == "أحسن خاسر") && 
            it.status != MatchStatus.FINISHED
        }
        if (r16LuckySlots.isNotEmpty()) {
            val regularR16Matches = allMatches.filter {
                it.stage == MatchStage.ROUND_OF_16 && !it.isPlayerTwoLuckyLoser && !it.isPlayerOneLuckyLoser && it.status == MatchStatus.FINISHED
            }
            val bestLosers = evaluateBestLosersUseCase(regularR16Matches)
            r16LuckySlots.forEachIndexed { index, slotMatch ->
                val loserCandidate = bestLosers.getOrNull(index)
                if (loserCandidate != null) {
                    val isP1Lucky = slotMatch.isPlayerOneLuckyLoser || slotMatch.playerOneName == "أحسن خاسر"
                    val updated = if (isP1Lucky) {
                        slotMatch.copy(
                            playerOneName = loserCandidate.playerName,
                            playerOneClub = loserCandidate.clubName,
                            isPlayerOneLuckyLoser = true
                        )
                    } else {
                        slotMatch.copy(
                            playerTwoName = loserCandidate.playerName,
                            playerTwoClub = loserCandidate.clubName,
                            isPlayerTwoLuckyLoser = true
                        )
                    }
                    tournamentRepository.updateMatch(updated)
                }
            }
        }

        // 2. Check Lucky Loser slots in Quarter Finals (for 5..7 players)
        val qfLuckySlots = allMatches.filter {
            it.stage == MatchStage.QUARTER_FINALS && 
            (it.isPlayerTwoLuckyLoser || it.isPlayerOneLuckyLoser || it.playerTwoName == "أحسن خاسر" || it.playerOneName == "أحسن خاسر") && 
            it.status != MatchStatus.FINISHED
        }
        if (qfLuckySlots.isNotEmpty()) {
            val regularQfMatches = allMatches.filter {
                it.stage == MatchStage.QUARTER_FINALS && !it.isPlayerTwoLuckyLoser && !it.isPlayerOneLuckyLoser && it.status == MatchStatus.FINISHED
            }
            val bestLosers = evaluateBestLosersUseCase(regularQfMatches)
            qfLuckySlots.forEachIndexed { index, slotMatch ->
                val loserCandidate = bestLosers.getOrNull(index)
                if (loserCandidate != null) {
                    val isP1Lucky = slotMatch.isPlayerOneLuckyLoser || slotMatch.playerOneName == "أحسن خاسر"
                    val updated = if (isP1Lucky) {
                        slotMatch.copy(
                            playerOneName = loserCandidate.playerName,
                            playerOneClub = loserCandidate.clubName,
                            isPlayerOneLuckyLoser = true
                        )
                    } else {
                        slotMatch.copy(
                            playerTwoName = loserCandidate.playerName,
                            playerTwoClub = loserCandidate.clubName,
                            isPlayerTwoLuckyLoser = true
                        )
                    }
                    tournamentRepository.updateMatch(updated)
                }
            }
        }

        // 3. Check Lucky Loser slots in Semi Finals (for 3, 5, 6, 11, 12 players)
        val sfLuckySlots = allMatches.filter {
            it.stage == MatchStage.SEMI_FINALS && 
            (it.isPlayerTwoLuckyLoser || it.isPlayerOneLuckyLoser || it.playerTwoName == "أحسن خاسر" || it.playerOneName == "أحسن خاسر") && 
            it.status != MatchStatus.FINISHED
        }
        if (sfLuckySlots.isNotEmpty()) {
            val qfMatches = allMatches.filter {
                it.stage == MatchStage.QUARTER_FINALS && !it.isPlayerTwoLuckyLoser && !it.isPlayerOneLuckyLoser && it.status == MatchStatus.FINISHED
            }
            if (qfMatches.isNotEmpty()) {
                val bestLosers = evaluateBestLosersUseCase(qfMatches)
                sfLuckySlots.forEachIndexed { index, slotMatch ->
                    val loserCandidate = bestLosers.getOrNull(index)
                    if (loserCandidate != null) {
                        val isP1Lucky = slotMatch.isPlayerOneLuckyLoser || slotMatch.playerOneName == "أحسن خاسر"
                        val updated = if (isP1Lucky) {
                            slotMatch.copy(
                                playerOneName = loserCandidate.playerName,
                                playerOneClub = loserCandidate.clubName,
                                isPlayerOneLuckyLoser = true
                            )
                        } else {
                            slotMatch.copy(
                                playerTwoName = loserCandidate.playerName,
                                playerTwoClub = loserCandidate.clubName,
                                isPlayerTwoLuckyLoser = true
                            )
                        }
                        tournamentRepository.updateMatch(updated)
                    }
                }
            } else {
                val sf1Match = allMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && it.bracketMatchIndex == 1 && it.status == MatchStatus.FINISHED }
                if (sf1Match != null) {
                    val loserCandidate = evaluateBestLosersUseCase(listOf(sf1Match)).firstOrNull()
                    if (loserCandidate != null) {
                        sfLuckySlots.forEach { slotMatch ->
                            val isP1Lucky = slotMatch.isPlayerOneLuckyLoser || slotMatch.playerOneName == "أحسن خاسر"
                            val updated = if (isP1Lucky) {
                                slotMatch.copy(
                                    playerOneName = loserCandidate.playerName,
                                    playerOneClub = loserCandidate.clubName,
                                    isPlayerOneLuckyLoser = true
                                )
                            } else {
                                slotMatch.copy(
                                    playerTwoName = loserCandidate.playerName,
                                    playerTwoClub = loserCandidate.clubName,
                                    isPlayerTwoLuckyLoser = true
                                )
                            }
                            tournamentRepository.updateMatch(updated)
                        }
                    }
                }
            }
        }
    }
}
