package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.mapper.toDomain
import com.rndm.app.data.mapper.toEntity
import com.rndm.app.data.remote.firebase.FirebaseAuthDataSource
import com.rndm.app.data.remote.firebase.FirestoreAuditDataSource
import com.rndm.app.data.remote.firebase.FirestoreTournamentDataSource
import com.rndm.app.data.remote.firebase.dto.FirestoreAuditLogDto
import com.rndm.app.data.remote.mapper.toDomain
import com.rndm.app.data.remote.mapper.toFirestoreDto
import com.rndm.app.domain.model.AuditLog
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.SyncStatus
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentStatus
import com.rndm.app.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val tournamentDao: TournamentDao,
    private val matchDao: MatchDao,
    private val remoteTournamentDataSource: FirestoreTournamentDataSource,
    private val remoteAuditDataSource: FirestoreAuditDataSource,
    private val authDataSource: FirebaseAuthDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : SyncRepository {
    private val locallyPublishedRemoteIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    override suspend fun publishTournament(tournamentId: Long): Result<Tournament> = withContext(ioDispatcher) {
        try {
            val tournamentEntity = tournamentDao.getTournamentById(tournamentId)
                ?: return@withContext Result.failure(IllegalArgumentException("البطولة غير موجودة محلياً"))

            val currentUid = authDataSource.currentUid ?: run {
                authDataSource.signInAnonymously().getOrNull()
            } ?: return@withContext Result.failure(IllegalStateException("تعذر التحقق من هوية المستخدم"))

            val participants = tournamentDao.getParticipantsByTournamentId(tournamentId).first()
            val matches = matchDao.getMatchesByTournamentId(tournamentId).first()

            val tournamentDomain = tournamentEntity.toDomain()
            val remoteTournamentDto = tournamentDomain.toFirestoreDto(hostUid = currentUid)
            val remoteParticipantDtos = participants.map { it.toDomain().toFirestoreDto() }
            val remoteMatchDtos = matches.map { it.toDomain().toFirestoreDto(actorUid = currentUid) }

            val publishResult = remoteTournamentDataSource.publishTournament(
                tournament = remoteTournamentDto,
                participants = remoteParticipantDtos,
                matches = remoteMatchDtos
            )

            if (publishResult.isSuccess) {
                val publishedDto = publishResult.getOrThrow()
                locallyPublishedRemoteIds.add(publishedDto.id)
                val updatedEntity = tournamentEntity.copy(
                    remoteId = publishedDto.id,
                    shareCode = publishedDto.shareCode,
                    isRemote = true,
                    isHost = true,
                    hostUid = currentUid,
                    syncStatus = SyncStatus.SYNCED,
                    lastSyncedAt = System.currentTimeMillis()
                )
                tournamentDao.updateTournament(updatedEntity)
                // Clean up any expired tournaments from the cloud in background
                kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
                    try {
                        remoteTournamentDataSource.cleanupExpiredTournaments()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
                Result.success(updatedEntity.toDomain())
            } else {
                Result.failure(publishResult.exceptionOrNull() ?: Exception("فشل نشر البطولة"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun joinTournamentByCode(shareCode: String): Result<Long> = withContext(ioDispatcher) {
        try {
            val raw = shareCode.trim().uppercase()
            val cleanCode = raw.replace("-", "").replace(" ", "")
            val dashedCode = if (cleanCode.length == 6) "${cleanCode.take(3)}-${cleanCode.drop(3)}" else raw

            // 1. Check if already present locally in Room database
            val localTournaments = tournamentDao.getAllTournaments().first()
            val existing = localTournaments.firstOrNull {
                it.shareCode.equals(raw, ignoreCase = true) ||
                it.shareCode.equals(cleanCode, ignoreCase = true) ||
                it.shareCode.equals(dashedCode, ignoreCase = true)
            }
            if (existing != null) {
                return@withContext Result.success(existing.id)
            }

            // 2. Ensure Firebase Auth session is initialized before remote queries
            var currentUid = authDataSource.currentUid
            if (currentUid == null) {
                currentUid = authDataSource.signInAnonymously().getOrNull()
            }

            // 3. Lookup remote tournament ID
            val tournamentIdResult = remoteTournamentDataSource.getTournamentIdByShareCode(raw)
            val remoteTournamentId = tournamentIdResult.getOrThrow()

            // 4. Download snapshot
            val snapshotResult = remoteTournamentDataSource.getTournamentSnapshot(remoteTournamentId)
            val (tournamentDto, participantDtos, matchDtos) = snapshotResult.getOrThrow()

            if (currentUid != null) {
                remoteTournamentDataSource.joinTournamentAsMember(remoteTournamentId, currentUid)
            }

            // 5. Save to Room
            val isCurrentUserHost = currentUid != null && currentUid == tournamentDto.hostUid
            val tournamentToInsert = tournamentDto.toDomain().copy(
                isHost = isCurrentUserHost,
                syncStatus = SyncStatus.SYNCED
            )

            val localId = tournamentDao.insertTournament(tournamentToInsert.toEntity())
            val participantsToInsert = participantDtos.map { it.toDomain(tournamentId = localId).toEntity(localId) }
            val matchesToInsert = matchDtos.map { it.toDomain(tournamentId = localId).toEntity(localId) }

            tournamentDao.insertParticipants(participantsToInsert)
            matchDao.insertMatches(matchesToInsert)

            Result.success(localId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun observeRemoteMatches(tournamentId: Long): Flow<Unit> {
        return tournamentDao.getTournamentByIdFlow(tournamentId).map { entity ->
            val remoteId = entity?.remoteId
            if (remoteId != null && entity.isRemote) {
                remoteTournamentDataSource.observeTournamentMatches(remoteId).collect { remoteMatches ->
                    val localMatches = matchDao.getMatchesByTournamentId(tournamentId).first()
                    remoteMatches.forEach { remoteDto ->
                        val existingLocalMatch = localMatches.firstOrNull {
                            (it.remoteId != null && it.remoteId == remoteDto.id) ||
                            (it.stage.name == remoteDto.stage && it.roundIndex == remoteDto.roundIndex && it.bracketMatchIndex == remoteDto.bracketMatchIndex && it.groupIndex == remoteDto.groupIndex)
                        }
                        if (existingLocalMatch != null) {
                            val updated = existingLocalMatch.copy(
                                scoreOne = remoteDto.scoreOne,
                                scoreTwo = remoteDto.scoreTwo,
                                penaltyScoreOne = remoteDto.penaltyScoreOne,
                                penaltyScoreTwo = remoteDto.penaltyScoreTwo,
                                winnerName = remoteDto.winnerName,
                                status = com.rndm.app.domain.model.MatchStatus.valueOf(remoteDto.status),
                                isPlayerOneLuckyLoser = remoteDto.isPlayerOneLuckyLoser,
                                isPlayerTwoLuckyLoser = remoteDto.isPlayerTwoLuckyLoser,
                                remoteId = remoteDto.id,
                                syncStatus = SyncStatus.SYNCED,
                                updatedAt = remoteDto.updatedAt
                            )
                            matchDao.updateMatch(updated)
                        }
                    }
                }
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun syncMatchScore(
        tournamentId: Long,
        match: Match,
        oldScoreOne: Int?,
        oldScoreTwo: Int?
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val tournamentEntity = tournamentDao.getTournamentById(tournamentId)
            val remoteId = tournamentEntity?.remoteId

            if (remoteId != null && tournamentEntity.isRemote) {
                val currentUid = authDataSource.currentUid ?: "unknown"
                val matchDto = match.toFirestoreDto(actorUid = currentUid)
                remoteTournamentDataSource.updateMatchScore(remoteId, matchDto)

                // Log audit trail
                val logDto = FirestoreAuditLogDto(
                    actorUid = currentUid,
                    actorRole = authDataSource.determineUserRole().name.lowercase(),
                    action = "MATCH_SCORE_UPDATED",
                    matchId = match.id,
                    details = "${match.playerOneName} vs ${match.playerTwoName}: ($oldScoreOne - $oldScoreTwo) -> (${match.scoreOne} - ${match.scoreTwo})",
                    timestamp = System.currentTimeMillis()
                )
                remoteAuditDataSource.logAction(remoteId, logDto)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateTournamentStatus(tournamentId: Long, status: TournamentStatus): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = tournamentDao.getTournamentById(tournamentId)
                ?: return@withContext Result.failure(IllegalArgumentException("البطولة غير موجودة"))

            val updatedEntity = existing.copy(status = status, updatedAt = System.currentTimeMillis())
            tournamentDao.updateTournament(updatedEntity)

            if (existing.isRemote && existing.remoteId != null) {
                remoteTournamentDataSource.updateTournamentStatus(existing.remoteId, status.name)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun observeAuditLogs(tournamentId: Long): Flow<List<AuditLog>> {
        val tournament = tournamentDao.getTournamentByIdFlow(tournamentId)
        return tournament.map { entity ->
            val remoteId = entity?.remoteId
            if (remoteId != null) {
                remoteAuditDataSource.observeAuditLogs(remoteId).first().map { it.toDomain(tournamentId) }
            } else {
                emptyList()
            }
        }.flowOn(ioDispatcher)
    }

    override fun observeAvailableLiveTournaments(): Flow<List<Tournament>> {
        return kotlinx.coroutines.flow.combine(
            remoteTournamentDataSource.observeLivePublicTournaments(),
            tournamentDao.getAllTournaments()
        ) { remoteList, localEntities ->
            val currentUid = authDataSource.currentUid
            val localRemoteIds = localEntities.mapNotNull { it.remoteId?.trim() }.filter { it.isNotBlank() }.toSet()
            val localShareCodes = localEntities.mapNotNull { it.shareCode?.replace("-", "")?.replace(" ", "")?.uppercase() }.filter { it.isNotBlank() }.toSet()

            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)

            remoteList.filter { remoteDto ->
                val isFresh = remoteDto.createdAt >= oneHourAgo && remoteDto.stage != "COMPLETED" && remoteDto.status == "ACTIVE"
                val isLocallyPublished = remoteDto.id in locallyPublishedRemoteIds
                val isHost = currentUid != null && remoteDto.hostUid.isNotBlank() && remoteDto.hostUid == currentUid
                val cleanRemoteCode = remoteDto.shareCode.replace("-", "").replace(" ", "").uppercase()
                val matchesLocalRemoteId = remoteDto.id in localRemoteIds
                val matchesLocalCode = cleanRemoteCode.isNotBlank() && cleanRemoteCode in localShareCodes

                val isOwnerOrAlreadyPresent = isLocallyPublished || isHost || matchesLocalRemoteId || matchesLocalCode
                isFresh && !isOwnerOrAlreadyPresent
            }.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    override suspend fun getLiveTournamentPreview(remoteTournamentId: String): Result<com.rndm.app.domain.model.LiveTournamentPreview> = withContext(ioDispatcher) {
        try {
            val snapshotResult = remoteTournamentDataSource.getTournamentSnapshot(remoteTournamentId)
            val (tournamentDto, participantDtos, matchDtos) = snapshotResult.getOrThrow()
            val domainTournament = tournamentDto.toDomain()
            val domainParticipants = participantDtos.map { it.toDomain() }
            val domainMatches = matchDtos.map { it.toDomain() }

            Result.success(
                com.rndm.app.domain.model.LiveTournamentPreview(
                    tournament = domainTournament,
                    participants = domainParticipants,
                    matches = domainMatches
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun broadcastTournamentToPublic(tournamentId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            var tournamentEntity = tournamentDao.getTournamentById(tournamentId)
                ?: return@withContext Result.failure(IllegalArgumentException("البطولة غير موجودة محلياً"))

            var remoteId = tournamentEntity.remoteId
            if (remoteId.isNullOrBlank()) {
                val publishResult = publishTournament(tournamentId)
                if (publishResult.isFailure) {
                    return@withContext Result.failure(publishResult.exceptionOrNull() ?: Exception("فشل رفع البطولة إلى السحابة"))
                }
                tournamentEntity = tournamentDao.getTournamentById(tournamentId)
                    ?: return@withContext Result.failure(IllegalArgumentException("البطولة غير موجودة محلياً"))
                remoteId = tournamentEntity.remoteId
            }

            if (remoteId.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("تعذر الحصول على المعرف السحابي للبطولة"))
            }

            val result = remoteTournamentDataSource.setTournamentPublicBroadcast(remoteId, true)
            if (result.isSuccess) {
                tournamentDao.setTournamentPublic(tournamentId, true)
                locallyPublishedRemoteIds.add(remoteId)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("فشل نشر البطولة للعامة"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun cleanupExpiredTournaments(): Result<Unit> = withContext(ioDispatcher) {
        try {
            remoteTournamentDataSource.cleanupExpiredTournaments()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
