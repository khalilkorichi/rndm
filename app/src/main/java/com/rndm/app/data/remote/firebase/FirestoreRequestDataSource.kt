package com.rndm.app.data.remote.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rndm.app.data.remote.firebase.dto.FirestoreAdminRequestDto
import com.rndm.app.data.remote.firebase.dto.FirestoreAuditLogDto
import com.rndm.app.data.remote.firebase.dto.FirestoreMatchDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRequestDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auditDataSource: FirestoreAuditDataSource
) {

    suspend fun submitRequest(request: FirestoreAdminRequestDto): Result<Unit> {
        return try {
            val requestId = if (request.id.isNotBlank()) request.id else "req_${UUID.randomUUID()}"
            val finalDto = request.copy(
                id = requestId,
                status = "PENDING",
                createdAt = if (request.createdAt > 0L) request.createdAt else System.currentTimeMillis()
            )
            firestore.collection("admin_requests").document(requestId).set(finalDto).await()
            Log.d("REQUEST_RNDM", "Submitted request: $requestId (${request.type})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("REQUEST_RNDM", "Failed to submit request", e)
            Result.failure(e)
        }
    }

    fun observeAllRequests(): Flow<List<FirestoreAdminRequestDto>> = callbackFlow {
        val requestsRef = firestore.collection("admin_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        val listener = requestsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreAdminRequestDto::class.java)
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeRequestsByRequester(requesterUid: String): Flow<List<FirestoreAdminRequestDto>> = callbackFlow {
        val requestsRef = firestore.collection("admin_requests")
            .whereEqualTo("requesterUid", requesterUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = requestsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreAdminRequestDto::class.java)
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun approveRequest(requestId: String, adminUid: String): Result<Unit> {
        return try {
            val reqDoc = firestore.collection("admin_requests").document(requestId).get().await()
            if (!reqDoc.exists()) {
                return Result.failure(IllegalArgumentException("الطلب غير موجود"))
            }

            val request = reqDoc.toObject(FirestoreAdminRequestDto::class.java)
                ?: return Result.failure(IllegalStateException("تعذر قراءة بيانات الطلب"))

            val batch = firestore.batch()
            val now = System.currentTimeMillis()

            // 1. Handle CHANGE_SCORE
            if (request.type == "CHANGE_SCORE" && request.tournamentId.isNotBlank()) {
                val matchRemoteId = request.remoteMatchId
                val tournamentMatchesRef = firestore.collection("tournaments")
                    .document(request.tournamentId)
                    .collection("matches")

                val targetDocRef = if (!matchRemoteId.isNullOrBlank()) {
                    tournamentMatchesRef.document(matchRemoteId)
                } else {
                    // Look up match by player names or id
                    val snapshot = tournamentMatchesRef.get().await()
                    val matchDoc = snapshot.documents.firstOrNull { doc ->
                        val p1 = doc.getString("playerOneName")
                        val p2 = doc.getString("playerTwoName")
                        (p1 == request.playerOneName && p2 == request.playerTwoName) || doc.id == request.matchId?.toString()
                    }
                    matchDoc?.reference
                }

                if (targetDocRef != null) {
                    val s1 = request.scoreOne ?: 0
                    val s2 = request.scoreTwo ?: 0
                    val p1 = request.penaltyScoreOne ?: 0
                    val p2 = request.penaltyScoreTwo ?: 0
                    val winnerName = when {
                        s1 > s2 -> request.playerOneName
                        s2 > s1 -> request.playerTwoName
                        p1 > p2 -> request.playerOneName
                        p2 > p1 -> request.playerTwoName
                        else -> null
                    }

                    val matchUpdates = mutableMapOf<String, Any?>(
                        "scoreOne" to request.scoreOne,
                        "scoreTwo" to request.scoreTwo,
                        "penaltyScoreOne" to request.penaltyScoreOne,
                        "penaltyScoreTwo" to request.penaltyScoreTwo,
                        "status" to "FINISHED",
                        "updatedByUid" to adminUid,
                        "updatedAt" to now
                    )
                    if (winnerName != null) {
                        matchUpdates["winnerName"] = winnerName
                    }
                    batch.update(targetDocRef, matchUpdates)
                }
            }

            // 2. Handle SWAP_MATCH_ORDER
            if (request.type == "SWAP_MATCH_ORDER" && request.tournamentId.isNotBlank()) {
                val matchesRef = firestore.collection("tournaments")
                    .document(request.tournamentId)
                    .collection("matches")
                val snapshot = matchesRef.get().await()
                val match1Doc = snapshot.documents.firstOrNull { doc ->
                    doc.id == request.matchId1?.toString() ||
                    (doc.getString("playerOneName") != null && request.matchOneDesc?.contains(doc.getString("playerOneName")!!) == true)
                }
                val match2Doc = snapshot.documents.firstOrNull { doc ->
                    doc.id == request.matchId2?.toString() ||
                    (doc.getString("playerOneName") != null && request.matchTwoDesc?.contains(doc.getString("playerOneName")!!) == true)
                }

                if (match1Doc != null && match2Doc != null) {
                    val round1 = match1Doc.getLong("roundIndex") ?: 1L
                    val round2 = match2Doc.getLong("roundIndex") ?: 1L
                    batch.update(match1Doc.reference, mapOf("roundIndex" to round2, "updatedAt" to now))
                    batch.update(match2Doc.reference, mapOf("roundIndex" to round1, "updatedAt" to now))
                }
            }

            // 3. Handle SWAP_PLAYERS
            if (request.type == "SWAP_PLAYERS" && request.tournamentId.isNotBlank()) {
                val matchesRef = firestore.collection("tournaments")
                    .document(request.tournamentId)
                    .collection("matches")
                val snapshot = matchesRef.get().await()
                val match1Doc = snapshot.documents.firstOrNull { doc ->
                    doc.id == request.matchId1?.toString() ||
                    doc.getString("playerOneName") == request.playerOneName ||
                    doc.getString("playerTwoName") == request.playerOneName
                }
                val match2Doc = snapshot.documents.firstOrNull { doc ->
                    doc.id == request.matchId2?.toString() ||
                    doc.getString("playerOneName") == request.playerTwoName ||
                    doc.getString("playerTwoName") == request.playerTwoName
                }

                if (match1Doc != null && match2Doc != null) {
                    val isSlot1A = request.isSlot1A ?: true
                    val isSlot1B = request.isSlot1B ?: true

                    val p1Name = if (isSlot1A) match1Doc.getString("playerOneName") else match1Doc.getString("playerTwoName")
                    val p1Club = if (isSlot1A) match1Doc.getString("playerOneClub") else match1Doc.getString("playerTwoClub")

                    val p2Name = if (isSlot1B) match2Doc.getString("playerOneName") else match2Doc.getString("playerTwoName")
                    val p2Club = if (isSlot1B) match2Doc.getString("playerOneClub") else match2Doc.getString("playerTwoClub")

                    val updates1 = if (isSlot1A) {
                        mapOf("playerOneName" to p2Name, "playerOneClub" to p2Club, "updatedAt" to now)
                    } else {
                        mapOf("playerTwoName" to p2Name, "playerTwoClub" to p2Club, "updatedAt" to now)
                    }

                    val updates2 = if (isSlot1B) {
                        mapOf("playerOneName" to p1Name, "playerOneClub" to p1Club, "updatedAt" to now)
                    } else {
                        mapOf("playerTwoName" to p1Name, "playerTwoClub" to p1Club, "updatedAt" to now)
                    }

                    batch.update(match1Doc.reference, updates1)
                    batch.update(match2Doc.reference, updates2)
                }
            }

            // 4. Handle PLAYER_REPLACE
            if (request.type == "PLAYER_REPLACE" && request.tournamentId.isNotBlank()) {
                val oldName = request.playerOneName.orEmpty()
                val newName = request.playerTwoName.orEmpty()
                val newClub = request.playerTwoClub

                if (oldName.isNotBlank() && newName.isNotBlank()) {
                    // Update participants
                    val partsRef = firestore.collection("tournaments").document(request.tournamentId).collection("participants")
                    val partsSnap = partsRef.whereEqualTo("playerName", oldName).get().await()
                    partsSnap.documents.forEach { doc ->
                        val uMap = mutableMapOf<String, Any?>("playerName" to newName)
                        if (newClub != null) uMap["clubName"] = newClub
                        batch.update(doc.reference, uMap)
                    }

                    // Update matches
                    val matchesRef = firestore.collection("tournaments").document(request.tournamentId).collection("matches")
                    val mSnap1 = matchesRef.whereEqualTo("playerOneName", oldName).get().await()
                    mSnap1.documents.forEach { doc ->
                        val uMap = mutableMapOf<String, Any?>("playerOneName" to newName, "updatedAt" to now)
                        if (newClub != null) uMap["playerOneClub"] = newClub
                        batch.update(doc.reference, uMap)
                    }
                    val mSnap2 = matchesRef.whereEqualTo("playerTwoName", oldName).get().await()
                    mSnap2.documents.forEach { doc ->
                        val uMap = mutableMapOf<String, Any?>("playerTwoName" to newName, "updatedAt" to now)
                        if (newClub != null) uMap["playerTwoClub"] = newClub
                        batch.update(doc.reference, uMap)
                    }
                }
            }

            // 5. Handle PUBLISH_TOURNAMENT
            if (request.type == "PUBLISH_TOURNAMENT" && request.tournamentId.isNotBlank()) {
                val tournRef = firestore.collection("tournaments").document(request.tournamentId)
                batch.update(tournRef, mapOf("status" to "ACTIVE", "updatedAt" to now))
            }

            // 6. Mark request document as APPROVED
            val reqRef = firestore.collection("admin_requests").document(requestId)
            batch.update(
                reqRef,
                mapOf(
                    "status" to "APPROVED",
                    "reviewedAt" to now,
                    "reviewedBy" to adminUid
                )
            )

            batch.commit().await()

            // Log Audit
            if (request.tournamentId.isNotBlank()) {
                auditDataSource.logAction(
                    request.tournamentId,
                    FirestoreAuditLogDto(
                        actorUid = adminUid,
                        actorRole = "admin",
                        action = "REQUEST_APPROVED",
                        matchId = request.matchId,
                        details = "تمت الموافقة على طلب ${request.type} المقدم من ${request.requesterName}: ${request.description}",
                        timestamp = now
                    )
                )
            }

            Log.d("REQUEST_RNDM", "Approved request: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("REQUEST_RNDM", "Failed to approve request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(requestId: String, adminUid: String, reason: String?): Result<Unit> {
        return try {
            val reqDoc = firestore.collection("admin_requests").document(requestId).get().await()
            if (!reqDoc.exists()) {
                return Result.failure(IllegalArgumentException("الطلب غير موجود"))
            }

            val request = reqDoc.toObject(FirestoreAdminRequestDto::class.java)
            val now = System.currentTimeMillis()

            firestore.collection("admin_requests").document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "reviewedAt" to now,
                    "reviewedBy" to adminUid,
                    "adminNote" to reason
                )
            ).await()

            if (request != null && request.tournamentId.isNotBlank()) {
                auditDataSource.logAction(
                    request.tournamentId,
                    FirestoreAuditLogDto(
                        actorUid = adminUid,
                        actorRole = "admin",
                        action = "REQUEST_REJECTED",
                        matchId = request.matchId,
                        details = "تم رفض طلب ${request.type} المقدم من ${request.requesterName}. السبب: ${reason ?: "غير محدد"}",
                        timestamp = now
                    )
                )
            }

            Log.d("REQUEST_RNDM", "Rejected request: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("REQUEST_RNDM", "Failed to reject request", e)
            Result.failure(e)
        }
    }
}
