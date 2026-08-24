package com.rndm.app.domain.usecase.request

import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.repository.RequestRepository
import javax.inject.Inject

class SubmitTournamentRequestUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    suspend operator fun invoke(request: AdminRequest): Result<Unit> {
        return requestRepository.submitRequest(request)
    }
}
