package com.rndm.app.data.repository

import com.rndm.app.data.remote.firebase.FirebaseAuthDataSource
import com.rndm.app.data.remote.firebase.FirestoreRequestDataSource
import com.rndm.app.data.remote.mapper.toDomain
import com.rndm.app.data.remote.mapper.toFirestoreDto
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.repository.RequestRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestRepositoryImpl @Inject constructor(
    private val requestDataSource: FirestoreRequestDataSource,
    private val authDataSource: FirebaseAuthDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : RequestRepository {

    override suspend fun submitRequest(request: AdminRequest): Result<Unit> = withContext(ioDispatcher) {
        var currentUid = authDataSource.currentUid
        if (currentUid.isNullOrBlank()) {
            currentUid = authDataSource.signInAnonymously().getOrNull() ?: "guest_${java.util.UUID.randomUUID().toString().take(8)}"
        }
        val profile = authDataSource.getCurrentUserProfile()
        val finalRequest = request.copy(
            requesterUid = if (request.requesterUid.isNotBlank()) request.requesterUid else currentUid,
            requesterName = if (request.requesterName.isNotBlank()) request.requesterName else (profile?.displayName ?: "ضيف (مجهول)"),
            requesterEmail = if (request.requesterEmail.isNotBlank()) request.requesterEmail else (profile?.email ?: "guest@rndm.app")
        )
        requestDataSource.submitRequest(finalRequest.toFirestoreDto())
    }

    override fun observeAllRequests(): Flow<List<AdminRequest>> {
        return requestDataSource.observeAllRequests()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeRequestsByRequester(requesterUid: String): Flow<List<AdminRequest>> {
        return requestDataSource.observeRequestsByRequester(requesterUid)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun approveRequest(requestId: String): Result<Unit> = withContext(ioDispatcher) {
        val adminUid = authDataSource.currentUid ?: "admin"
        requestDataSource.approveRequest(requestId, adminUid)
    }

    override suspend fun rejectRequest(requestId: String, reason: String?): Result<Unit> = withContext(ioDispatcher) {
        val adminUid = authDataSource.currentUid ?: "admin"
        requestDataSource.rejectRequest(requestId, adminUid, reason)
    }
}
