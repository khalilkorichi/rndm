package com.rndm.app.data.remote.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rndm.app.data.remote.firebase.dto.FirestoreCodeDto
import com.rndm.app.data.remote.firebase.dto.FirestoreMatchDto
import com.rndm.app.data.remote.firebase.dto.FirestoreParticipantDto
import com.rndm.app.data.remote.firebase.dto.FirestoreTournamentDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTournamentDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val secureRandom = SecureRandom()
    private val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    fun generateShareCode(): String {
        val part1 = (0..2).map { allowedChars[secureRandom.nextInt(allowedChars.length)] }.joinToString("")
        val part2 = (0..2).map { allowedChars[secureRandom.nextInt(allowedChars.length)] }.joinToString("")
        return "$part1-$part2"
    }

    suspend fun publishTournament(
        tournament: FirestoreTournamentDto,
        participants: List<FirestoreParticipantDto>,
        matches: List<FirestoreMatchDto>
    ): Result<FirestoreTournamentDto> {
        return try {
            val tournamentId = if (tournament.id.isNotBlank()) tournament.id else "tourn_${UUID.randomUUID()}"
            val shareCode = if (tournament.shareCode.isNotBlank()) tournament.shareCode else generateShareCode()

            val cleanCode = shareCode.replace("-", "").replace(" ", "").uppercase()
            val dashedCode = if (cleanCode.length == 6) "${cleanCode.take(3)}-${cleanCode.drop(3)}" else shareCode

            val finalTournament = tournament.copy(
                id = tournamentId,
                shareCode = dashedCode,
                updatedAt = System.currentTimeMillis()
            )

            val batch = firestore.batch()

            // 1. Write tournament document
            val tournamentRef = firestore.collection("tournaments").document(tournamentId)
            batch.set(tournamentRef, finalTournament)

            // 2. Write tournament_code lookup documents (both clean and dashed formats for instant indexing)
            val codeDto = FirestoreCodeDto(
                shareCode = dashedCode,
                tournamentId = tournamentId,
                hostUid = tournament.hostUid,
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
            val codeRefClean = firestore.collection("tournament_codes").document(cleanCode)
            val codeRefDashed = firestore.collection("tournament_codes").document(dashedCode)
            batch.set(codeRefClean, codeDto)
            batch.set(codeRefDashed, codeDto)

            // 3. Write participants subcollection
            participants.forEach { participant ->
                val partId = if (participant.id.isNotBlank()) participant.id else "part_${UUID.randomUUID()}"
                val partRef = tournamentRef.collection("participants").document(partId)
                batch.set(partRef, participant.copy(id = partId))
            }

            // 4. Write matches subcollection
            matches.forEach { match ->
                val matchId = if (match.id.isNotBlank()) match.id else "match_${UUID.randomUUID()}"
                val matchRef = tournamentRef.collection("matches").document(matchId)
                batch.set(matchRef, match.copy(id = matchId))
            }

            // Commit batch to Firestore
            batch.commit().await()
            Log.d("SYNC_RNDM", "Published tournament successfully: $tournamentId with code $dashedCode")
            Result.success(finalTournament)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed to publish tournament", e)
            Result.failure(e)
        }
    }

    suspend fun getTournamentIdByShareCode(code: String): Result<String> {
        return try {
            val raw = code.trim().uppercase()
            val cleanCode = raw.replace("-", "").replace(" ", "")
            val dashedCode = if (cleanCode.length == 6) "${cleanCode.take(3)}-${cleanCode.drop(3)}" else raw

            // 1. Direct document lookup in tournament_codes (clean code e.g. 66T65S)
            if (cleanCode.isNotBlank()) {
                try {
                    val cleanDoc = firestore.collection("tournament_codes").document(cleanCode).get().await()
                    if (cleanDoc.exists()) {
                        val tId = cleanDoc.getString("tournamentId")
                        if (!tId.isNullOrBlank()) return Result.success(tId)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("SYNC_RNDM", "Clean code doc check: ${e.message}")
                }
            }

            // 2. Direct document lookup in tournament_codes (dashed code e.g. 66T-65S)
            if (dashedCode.isNotBlank()) {
                try {
                    val dashedDoc = firestore.collection("tournament_codes").document(dashedCode).get().await()
                    if (dashedDoc.exists()) {
                        val tId = dashedDoc.getString("tournamentId")
                        if (!tId.isNullOrBlank()) return Result.success(tId)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("SYNC_RNDM", "Dashed code doc check: ${e.message}")
                }
            }

            // 3. Query tournament_codes collection where shareCode == dashedCode or cleanCode
            try {
                val q1 = firestore.collection("tournament_codes")
                    .whereEqualTo("shareCode", dashedCode)
                    .limit(1).get().await()
                if (!q1.isEmpty) {
                    val tId = q1.documents.first().getString("tournamentId")
                    if (!tId.isNullOrBlank()) return Result.success(tId)
                }

                val q2 = firestore.collection("tournament_codes")
                    .whereEqualTo("shareCode", cleanCode)
                    .limit(1).get().await()
                if (!q2.isEmpty) {
                    val tId = q2.documents.first().getString("tournamentId")
                    if (!tId.isNullOrBlank()) return Result.success(tId)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("SYNC_RNDM", "Query tournament_codes: ${e.message}")
            }

            // 4. Query tournaments collection directly
            try {
                val tQuery1 = firestore.collection("tournaments")
                    .whereEqualTo("shareCode", dashedCode)
                    .limit(1).get().await()
                if (!tQuery1.isEmpty) {
                    return Result.success(tQuery1.documents.first().id)
                }

                val tQuery2 = firestore.collection("tournaments")
                    .whereEqualTo("shareCode", cleanCode)
                    .limit(1).get().await()
                if (!tQuery2.isEmpty) {
                    return Result.success(tQuery2.documents.first().id)
                }

                val docById = firestore.collection("tournaments").document(raw).get().await()
                if (docById.exists()) {
                    return Result.success(docById.id)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("SYNC_RNDM", "Tournaments query check: ${e.message}")
            }

            Result.failure(IllegalArgumentException("كود البطولة غير موجود على السحابة، يرجى التأكد من قيام المنظم بنشر البطولة أولاً"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed lookup by code: $code", e)
            Result.failure(e)
        }
    }

    suspend fun getTournamentSnapshot(tournamentId: String): Result<Triple<FirestoreTournamentDto, List<FirestoreParticipantDto>, List<FirestoreMatchDto>>> {
        return try {
            val tournamentRef = firestore.collection("tournaments").document(tournamentId)
            val tournamentDoc = tournamentRef.get().await()
            if (!tournamentDoc.exists()) {
                return Result.failure(IllegalArgumentException("البطولة غير موجودة على الخادم السحابي"))
            }

            val tournamentDto = tournamentDoc.toObject(FirestoreTournamentDto::class.java)
                ?: throw IllegalArgumentException("تعذر تحميل بيانات البطولة من السحابة")

            val participants = try {
                tournamentRef.collection("participants").get().await().toObjects(FirestoreParticipantDto::class.java)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }

            val matches = try {
                tournamentRef.collection("matches").get().await().toObjects(FirestoreMatchDto::class.java)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }

            Result.success(Triple(tournamentDto, participants, matches))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed to fetch tournament snapshot", e)
            Result.failure(e)
        }
    }

    suspend fun joinTournamentAsMember(tournamentId: String, userUid: String): Result<Unit> {
        return try {
            val tournamentRef = firestore.collection("tournaments").document(tournamentId)
            tournamentRef.update("memberIds", FieldValue.arrayUnion(userUid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("SYNC_RNDM", "Failed to join member", e)
            Result.failure(e)
        }
    }

    fun observeTournamentMatches(tournamentId: String): Flow<List<FirestoreMatchDto>> = callbackFlow {
        val matchesRef = firestore.collection("tournaments").document(tournamentId).collection("matches")
        val listener = matchesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val matches = snapshot.toObjects(FirestoreMatchDto::class.java)
                trySend(matches)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateMatchScore(tournamentId: String, match: FirestoreMatchDto): Result<Unit> {
        return try {
            val matchRef = firestore.collection("tournaments").document(tournamentId)
                .collection("matches").document(match.id)
            matchRef.set(match).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed update match score", e)
            Result.failure(e)
        }
    }

    suspend fun updateTournamentStatus(tournamentId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("tournaments").document(tournamentId)
                .update("status", status, "updatedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed update tournament status", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String, shareCode: String?): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val tournamentRef = firestore.collection("tournaments").document(tournamentId)
            batch.delete(tournamentRef)
            if (!shareCode.isNullOrBlank()) {
                val cleanCode = shareCode.replace("-", "").replace(" ", "").uppercase()
                val dashedCode = if (cleanCode.length == 6) "${cleanCode.take(3)}-${cleanCode.drop(3)}" else shareCode
                val codeRefClean = firestore.collection("tournament_codes").document(cleanCode)
                val codeRefDashed = firestore.collection("tournament_codes").document(dashedCode)
                batch.delete(codeRefClean)
                batch.delete(codeRefDashed)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed delete tournament", e)
            Result.failure(e)
        }
    }

    suspend fun cleanupExpiredTournaments(): Result<Unit> {
        return try {
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
            val snapshot = firestore.collection("tournaments")
                .limit(50)
                .get()
                .await()

            val batch = firestore.batch()
            var count = 0

            for (doc in snapshot.documents) {
                val createdAt = doc.getLong("createdAt") ?: 0L
                val isArchived = doc.getBoolean("archived") ?: false
                val stage = doc.getString("stage") ?: ""
                val status = doc.getString("status") ?: ""
                val shareCode = doc.getString("shareCode")

                // Expire if older than 1 hour, or completed, or already marked archived
                if (createdAt < oneHourAgo || stage == "COMPLETED" || status == "ARCHIVED" || isArchived) {
                    batch.delete(doc.reference)
                    if (!shareCode.isNullOrBlank()) {
                        val clean = shareCode.replace("-", "").replace(" ", "").uppercase()
                        batch.delete(firestore.collection("tournament_codes").document(clean))
                        batch.delete(firestore.collection("tournament_codes").document(shareCode))
                    }
                    count++
                }
            }

            if (count > 0) {
                batch.commit().await()
                Log.d("SYNC_RNDM", "Cleaned up $count expired/completed tournaments from Cloud Firestore")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("SYNC_RNDM", "Cleanup expired tournaments error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setTournamentPublicBroadcast(tournamentId: String, isPublic: Boolean = true): Result<Unit> {
        return try {
            firestore.collection("tournaments").document(tournamentId)
                .update(
                    mapOf(
                        "isPublic" to isPublic,
                        "public" to isPublic,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Log.d("SYNC_RNDM", "Tournament $tournamentId public broadcast set to $isPublic")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SYNC_RNDM", "Failed set tournament public broadcast", e)
            Result.failure(e)
        }
    }

    fun observeLivePublicTournaments(limit: Long = 10): Flow<List<FirestoreTournamentDto>> = callbackFlow {
        val query = firestore.collection("tournaments")
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("SYNC_RNDM", "Error observing live tournaments: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
                val list = snapshot.toObjects(FirestoreTournamentDto::class.java)
                    .filter {
                        !it.isArchived &&
                        it.isPublic &&
                        it.status == "ACTIVE" &&
                        it.stage != "COMPLETED" &&
                        it.createdAt >= oneHourAgo
                    }
                    .sortedByDescending { it.updatedAt.coerceAtLeast(it.createdAt) }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }
}
