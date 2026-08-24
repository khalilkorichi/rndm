package com.rndm.app.domain.repository

import com.rndm.app.domain.model.AdminRequest
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    suspend fun submitRequest(request: AdminRequest): Result<Unit>
    fun observeAllRequests(): Flow<List<AdminRequest>>
    fun observeRequestsByRequester(requesterUid: String): Flow<List<AdminRequest>>
    suspend fun approveRequest(requestId: String): Result<Unit>
    suspend fun rejectRequest(requestId: String, reason: String?): Result<Unit>
}
