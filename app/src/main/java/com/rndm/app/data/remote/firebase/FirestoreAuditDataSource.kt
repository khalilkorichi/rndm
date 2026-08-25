package com.rndm.app.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rndm.app.data.remote.firebase.dto.FirestoreAuditLogDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAuditDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun logAction(tournamentRemoteId: String, logDto: FirestoreAuditLogDto): Result<Unit> {
        return try {
            val logId = if (logDto.id.isNotBlank()) logDto.id else "log_${UUID.randomUUID()}"
            val finalDto = logDto.copy(id = logId, timestamp = System.currentTimeMillis())
            firestore.collection("tournaments").document(tournamentRemoteId)
                .collection("auditLogs").document(logId).set(finalDto).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    fun observeAuditLogs(tournamentRemoteId: String): Flow<List<FirestoreAuditLogDto>> = callbackFlow {
        val auditRef = firestore.collection("tournaments").document(tournamentRemoteId)
            .collection("auditLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        val listener = auditRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val logs = snapshot.toObjects(FirestoreAuditLogDto::class.java)
                trySend(logs)
            }
        }
        awaitClose { listener.remove() }
    }
}
