package com.rndm.app.domain.usecase.request

import com.rndm.app.domain.repository.RequestRepository
import javax.inject.Inject

class RejectAdminRequestUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    suspend operator fun invoke(requestId: String, reason: String?): Result<Unit> {
        return requestRepository.rejectRequest(requestId, reason)
    }
}
