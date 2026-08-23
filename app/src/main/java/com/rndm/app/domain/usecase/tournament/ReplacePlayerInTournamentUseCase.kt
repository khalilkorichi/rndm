package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.TournamentRepository
import javax.inject.Inject

class ReplacePlayerInTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val drawFixtureRepository: DrawFixtureRepository
) {
    suspend operator fun invoke(
        tournamentId: Long,
        oldPlayerName: String,
        newPlayerName: String,
        newClubName: String? = null
    ) {
        if (tournamentId > 0) {
            tournamentRepository.replaceParticipant(tournamentId, oldPlayerName, newPlayerName, newClubName)
        }
        drawFixtureRepository.replacePlayer(oldPlayerName, newPlayerName, newClubName)
    }
}
