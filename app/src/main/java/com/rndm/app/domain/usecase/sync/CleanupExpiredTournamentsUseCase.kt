package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.repository.SyncRepository
import javax.inject.Inject

class CleanupExpiredTournamentsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.cleanupExpiredTournaments()
    }
}
