package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.repository.SyncRepository
import javax.inject.Inject

class PublishTournamentUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(tournamentId: Long): Result<Tournament> {
        return syncRepository.publishTournament(tournamentId)
    }
}
