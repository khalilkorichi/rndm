package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTournamentsUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository
) {
    operator fun invoke(): Flow<List<Tournament>> {
        return tournamentRepository.getAllTournaments()
    }
}
