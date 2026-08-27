package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.model.LiveTournamentPreview
import com.rndm.app.domain.repository.SyncRepository
import javax.inject.Inject

class GetLiveTournamentPreviewUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(remoteTournamentId: String): Result<LiveTournamentPreview> {
        return syncRepository.getLiveTournamentPreview(remoteTournamentId)
    }
}
