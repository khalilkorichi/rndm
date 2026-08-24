package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRemoteTournamentUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(tournamentId: Long): Flow<Unit> {
        return syncRepository.observeRemoteMatches(tournamentId)
    }
}
