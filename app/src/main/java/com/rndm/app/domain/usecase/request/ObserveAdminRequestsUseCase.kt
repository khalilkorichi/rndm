package com.rndm.app.domain.usecase.request

import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.repository.RequestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAdminRequestsUseCase @Inject constructor(
    private val requestRepository: RequestRepository
) {
    operator fun invoke(): Flow<List<AdminRequest>> {
        return requestRepository.observeAllRequests()
    }

    fun forRequester(requesterUid: String): Flow<List<AdminRequest>> {
        return requestRepository.observeRequestsByRequester(requesterUid)
    }
}
