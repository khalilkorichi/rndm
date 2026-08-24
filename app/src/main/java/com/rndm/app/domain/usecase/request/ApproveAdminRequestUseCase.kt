package com.rndm.app.domain.usecase.request

import com.rndm.app.domain.repository.RequestRepository
import javax.inject.Inject

class ApproveAdminRequestUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> {
        return requestRepository.approveRequest(requestId)
    }
}
