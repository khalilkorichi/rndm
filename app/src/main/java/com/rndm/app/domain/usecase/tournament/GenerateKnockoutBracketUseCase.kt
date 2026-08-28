package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.repository.TournamentRepository
import javax.inject.Inject

class GenerateKnockoutBracketUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository
) {

    suspend operator fun invoke(
        tournamentId: Long,
        qualifiers: List<TournamentParticipant>
    ): List<Match> {
        val matches = generateBracketMatches(tournamentId, qualifiers)
        tournamentRepository.saveKnockoutMatches(tournamentId, matches)
        tournamentRepository.updateTournamentStage(tournamentId, TournamentStage.KNOCKOUT_ROUNDS)
        return matches
    }

    companion object {
        fun generateBracketMatches(
            tournamentId: Long,
            qualifiers: List<TournamentParticipant>
        ): List<Match> {
            val matches = mutableListOf<Match>()
            val count = qualifiers.size

            when {
                count <= 2 -> {
                    val p1 = qualifiers.getOrNull(0)
                    val p2 = qualifiers.getOrNull(1)
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 1,
                            bracketMatchIndex = 1,
                            playerOneName = p1?.playerName ?: "TBD",
                            playerOneClub = p1?.clubName,
                            playerTwoName = p2?.playerName ?: "TBD",
                            playerTwoClub = p2?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count == 3 -> {
                    // Semi-Finals (2 matches): M1 has P1 vs P2, M2 has P3 vs Lucky Loser from M1
                    val p1 = qualifiers.getOrNull(0)
                    val p2 = qualifiers.getOrNull(1)
                    val p3 = qualifiers.getOrNull(2)

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 1,
                            playerOneName = p1?.playerName ?: "TBD",
                            playerOneClub = p1?.clubName,
                            playerTwoName = p2?.playerName ?: "TBD",
                            playerTwoClub = p2?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 2,
                            playerOneName = p3?.playerName ?: "TBD",
                            playerOneClub = p3?.clubName,
                            playerTwoName = "أحسن خاسر",
                            isPlayerTwoLuckyLoser = true,
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd Place Match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final Match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count == 4 -> {
                    // Semi-Finals (2 matches)
                    val p1 = qualifiers.getOrNull(0)
                    val p2 = qualifiers.getOrNull(1)
                    val p3 = qualifiers.getOrNull(2)
                    val p4 = qualifiers.getOrNull(3)

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 1,
                            playerOneName = p1?.playerName ?: "TBD",
                            playerOneClub = p1?.clubName,
                            playerTwoName = p2?.playerName ?: "TBD",
                            playerTwoClub = p2?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 2,
                            playerOneName = p3?.playerName ?: "TBD",
                            playerOneClub = p3?.clubName,
                            playerTwoName = p4?.playerName ?: "TBD",
                            playerTwoClub = p4?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd Place Match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final Match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count == 5 -> {
                    // Quarter-Finals (2 matches for first 4 players)
                    val p1 = qualifiers.getOrNull(0)
                    val p2 = qualifiers.getOrNull(1)
                    val p3 = qualifiers.getOrNull(2)
                    val p4 = qualifiers.getOrNull(3)
                    val p5 = qualifiers.getOrNull(4)

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.QUARTER_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 1,
                            playerOneName = p1?.playerName ?: "TBD",
                            playerOneClub = p1?.clubName,
                            playerTwoName = p2?.playerName ?: "TBD",
                            playerTwoClub = p2?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.QUARTER_FINALS,
                            roundIndex = 1,
                            bracketMatchIndex = 2,
                            playerOneName = p3?.playerName ?: "TBD",
                            playerOneClub = p3?.clubName,
                            playerTwoName = p4?.playerName ?: "TBD",
                            playerTwoClub = p4?.clubName,
                            status = MatchStatus.PENDING
                        )
                    )

                    // Semi-Finals (2 matches): SF1 is Winner QF1 vs Winner QF2, SF2 is Player 5 vs Lucky Loser
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز ربع النهائي 1",
                            playerTwoName = "فائز ربع النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 2,
                            playerOneName = p5?.playerName ?: "TBD",
                            playerOneClub = p5?.clubName,
                            playerTwoName = "أحسن خاسر",
                            isPlayerTwoLuckyLoser = true,
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd place match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count == 6 -> {
                    // Quarter-Finals (3 matches between 6 players)
                    for (i in 0 until 3) {
                        val pA = qualifiers.getOrNull(i * 2)
                        val pB = qualifiers.getOrNull(i * 2 + 1)
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.QUARTER_FINALS,
                                roundIndex = 1,
                                bracketMatchIndex = i + 1,
                                playerOneName = pA?.playerName ?: "TBD",
                                playerOneClub = pA?.clubName,
                                playerTwoName = pB?.playerName ?: "TBD",
                                playerTwoClub = pB?.clubName,
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Semi-Finals (2 matches): SF1 is Winner QF1 vs Winner QF2, SF2 is Winner QF3 vs Lucky Loser
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز ربع النهائي 1",
                            playerTwoName = "فائز ربع النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 2,
                            playerOneName = "فائز ربع النهائي 3",
                            playerTwoName = "أحسن خاسر",
                            isPlayerTwoLuckyLoser = true,
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd place match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count <= 8 -> {
                    // Quarter-Finals (4 matches)
                    val isSevenPlayers = count == 7

                    for (i in 0 until 4) {
                        val pA = qualifiers.getOrNull(i * 2)
                        val pB = if (isSevenPlayers && i == 3) null else qualifiers.getOrNull(i * 2 + 1)
                        val isLuckyLoser = isSevenPlayers && i == 3

                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.QUARTER_FINALS,
                                roundIndex = 1,
                                bracketMatchIndex = i + 1,
                                playerOneName = pA?.playerName ?: "TBD",
                                playerOneClub = pA?.clubName,
                                playerTwoName = if (isLuckyLoser) "أحسن خاسر" else (pB?.playerName ?: "TBD"),
                                playerTwoClub = pB?.clubName,
                                isPlayerTwoLuckyLoser = isLuckyLoser,
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Semi-Finals (2 matches)
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز ربع النهائي 1",
                            playerTwoName = "فائز ربع النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 2,
                            bracketMatchIndex = 2,
                            playerOneName = "فائز ربع النهائي 3",
                            playerTwoName = "فائز ربع النهائي 4",
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd place match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 3,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                count <= 16 -> {
                    // Round of 16 (Pairs of 2 + at most ONE lone player vs Lucky Loser)
                    val fullPairsCount = count / 2
                    val hasLonePlayer = (count % 2) != 0
                    val totalR16Matches = fullPairsCount + (if (hasLonePlayer) 1 else 0)

                    for (i in 0 until totalR16Matches) {
                        val isLone = hasLonePlayer && i == totalR16Matches - 1
                        val pA = qualifiers.getOrNull(if (isLone) (count - 1) else (i * 2))
                        val pB = if (isLone) null else qualifiers.getOrNull(i * 2 + 1)
                        val isLuckyLoser = isLone

                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.ROUND_OF_16,
                                roundIndex = 1,
                                bracketMatchIndex = i + 1,
                                playerOneName = pA?.playerName ?: "TBD",
                                playerOneClub = pA?.clubName,
                                playerTwoName = if (isLuckyLoser) "أحسن خاسر" else (pB?.playerName ?: "TBD"),
                                playerTwoClub = pB?.clubName,
                                isPlayerTwoLuckyLoser = isLuckyLoser,
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Quarter-Finals (Pairs of R16 winners)
                    val totalQfMatches = totalR16Matches / 2

                    for (i in 0 until totalQfMatches) {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.QUARTER_FINALS,
                                roundIndex = 2,
                                bracketMatchIndex = i + 1,
                                playerOneName = "فائز دور الـ 16 (${i * 2 + 1})",
                                playerTwoName = "فائز دور الـ 16 (${i * 2 + 2})",
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Semi-Finals (2 matches)
                    if (totalQfMatches == 2) {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 1,
                                playerOneName = "فائز ربع النهائي 1",
                                playerTwoName = "فائز ربع النهائي 2",
                                status = MatchStatus.PENDING
                            )
                        )
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 2,
                                playerOneName = "فائز دور الـ 16 5",
                                playerTwoName = "أحسن خاسر",
                                isPlayerTwoLuckyLoser = true,
                                status = MatchStatus.PENDING
                            )
                        )
                    } else if (totalQfMatches == 3) {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 1,
                                playerOneName = "فائز ربع النهائي 1",
                                playerTwoName = "فائز ربع النهائي 2",
                                status = MatchStatus.PENDING
                            )
                        )
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 2,
                                playerOneName = "فائز ربع النهائي 3",
                                playerTwoName = "أحسن خاسر",
                                isPlayerTwoLuckyLoser = true,
                                status = MatchStatus.PENDING
                            )
                        )
                    } else {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 1,
                                playerOneName = "فائز ربع النهائي 1",
                                playerTwoName = "فائز ربع النهائي 2",
                                status = MatchStatus.PENDING
                            )
                        )
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.SEMI_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = 2,
                                playerOneName = "فائز ربع النهائي 3",
                                playerTwoName = "فائز ربع النهائي 4",
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // 3rd place match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 4,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 4,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }

                else -> {
                    // Round of 32 (16 matches) + R16 (8) + QF (4) + SF (2) + 3rd Place + Final
                    for (i in 0 until 16) {
                        val pA = qualifiers.getOrNull(i * 2)
                        val pB = qualifiers.getOrNull(i * 2 + 1)
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.ROUND_OF_32,
                                roundIndex = 1,
                                bracketMatchIndex = i + 1,
                                playerOneName = pA?.playerName ?: "TBD",
                                playerOneClub = pA?.clubName,
                                playerTwoName = pB?.playerName ?: "TBD",
                                playerTwoClub = pB?.clubName,
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Round of 16 (8 matches)
                    for (i in 0 until 8) {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.ROUND_OF_16,
                                roundIndex = 2,
                                bracketMatchIndex = i + 1,
                                playerOneName = "فائز دور الـ 32 (${i * 2 + 1})",
                                playerTwoName = "فائز دور الـ 32 (${i * 2 + 2})",
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Quarter-Finals (4 matches)
                    for (i in 0 until 4) {
                        matches.add(
                            Match(
                                tournamentId = tournamentId,
                                stage = MatchStage.QUARTER_FINALS,
                                roundIndex = 3,
                                bracketMatchIndex = i + 1,
                                playerOneName = "فائز دور الـ 16 (${i * 2 + 1})",
                                playerTwoName = "فائز دور الـ 16 (${i * 2 + 2})",
                                status = MatchStatus.PENDING
                            )
                        )
                    }

                    // Semi-Finals (2 matches)
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 4,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز ربع النهائي 1",
                            playerTwoName = "فائز ربع النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.SEMI_FINALS,
                            roundIndex = 4,
                            bracketMatchIndex = 2,
                            playerOneName = "فائز ربع النهائي 3",
                            playerTwoName = "فائز ربع النهائي 4",
                            status = MatchStatus.PENDING
                        )
                    )

                    // 3rd place match
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.THIRD_PLACE,
                            roundIndex = 5,
                            bracketMatchIndex = 1,
                            playerOneName = "خاسر نصف النهائي 1",
                            playerTwoName = "خاسر نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )

                    // Final
                    matches.add(
                        Match(
                            tournamentId = tournamentId,
                            stage = MatchStage.FINAL,
                            roundIndex = 5,
                            bracketMatchIndex = 1,
                            playerOneName = "فائز نصف النهائي 1",
                            playerTwoName = "فائز نصف النهائي 2",
                            status = MatchStatus.PENDING
                        )
                    )
                }
            }

            return matches
        }
    }
}
