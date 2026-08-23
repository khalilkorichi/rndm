package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.repository.TournamentRepository
import javax.inject.Inject

class UnarchiveTournamentUseCase @Inject constructor(
    private val repository: TournamentRepository
) {
    suspend operator fun invoke(tournamentId: Long) {
        repository.unarchiveTournament(tournamentId)
    }
}
