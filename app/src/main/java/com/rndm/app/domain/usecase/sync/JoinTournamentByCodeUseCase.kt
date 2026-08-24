package com.rndm.app.domain.usecase.sync

import com.rndm.app.domain.repository.SyncRepository
import javax.inject.Inject

class JoinTournamentByCodeUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(shareCode: String): Result<Long> {
        val trimmed = shareCode.trim().uppercase()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كود بطولة صحيح"))
        }
        return syncRepository.joinTournamentByCode(trimmed)
    }
}
