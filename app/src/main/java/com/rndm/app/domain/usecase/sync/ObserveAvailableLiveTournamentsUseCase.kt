package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAvailableLiveTournamentsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<List<Tournament>> {
        return syncRepository.observeAvailableLiveTournaments()
    }
}
