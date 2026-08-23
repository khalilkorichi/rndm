package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.TournamentRepository
import javax.inject.Inject

class AssignDuelWinnerToTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val drawFixtureRepository: DrawFixtureRepository
) {
    suspend operator fun invoke(
        tournamentId: Long,
        targetPlayerName: String,
        winnerPlayerName: String,
        contestedClub: String
    ) {
        require(tournamentId > 0) { "معرف البطولة غير صالح" }
        require(winnerPlayerName.isNotBlank()) { "اسم الفائز لا يمكن أن يكون فارغاً" }
        require(contestedClub.isNotBlank()) { "اسم النادي لا يمكن أن يكون فارغاً" }

        // Update tournament participant and matches
        tournamentRepository.replaceParticipant(
            tournamentId = tournamentId,
            oldPlayerName = targetPlayerName,
            newPlayerName = winnerPlayerName,
            newClubName = contestedClub
        )

        // Also update any live draw fixtures feed if matching
        drawFixtureRepository.replacePlayer(
            oldPlayerName = targetPlayerName,
            newPlayerName = winnerPlayerName,
            newClubName = contestedClub
        )
    }
}
