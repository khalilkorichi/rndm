package com.rndm.app.domain.usecase.sync

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import com.rndm.app.data.remote.firebase.FirebaseAuthDataSource
import com.rndm.app.data.remote.firebase.FirestoreAuditDataSource
import com.rndm.app.data.remote.firebase.FirestoreTournamentDataSource
import com.rndm.app.data.remote.firebase.dto.FirestoreMatchDto
import com.rndm.app.data.remote.firebase.dto.FirestoreParticipantDto
import com.rndm.app.data.remote.firebase.dto.FirestoreTournamentDto
import com.rndm.app.data.repository.SyncRepositoryImpl
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.SyncStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentStatus
import com.rndm.app.domain.model.TournamentType
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TournamentShareAndJoinComprehensiveTest {

    private val tournamentDao: TournamentDao = mockk(relaxed = true)
    private val matchDao: MatchDao = mockk(relaxed = true)
    private val remoteTournamentDataSource: FirestoreTournamentDataSource = mockk(relaxed = true)
    private val remoteAuditDataSource: FirestoreAuditDataSource = mockk(relaxed = true)
    private val authDataSource: FirebaseAuthDataSource = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var syncRepository: SyncRepositoryImpl

    private val sampleTournamentEntity = TournamentEntity(
        id = 100L,
        name = "بطولة الأبطال",
        type = TournamentType.GROUPS_KNOCKOUT,
        stage = TournamentStage.GROUPS,
        status = TournamentStatus.ACTIVE,
        playersProfileId = 1L,
        clubsProfileId = null,
        groupsCount = 2,
        qualifiersPerGroup = 2,
        isArchived = false,
        createdAt = 1000L,
        updatedAt = 1000L,
        remoteId = null,
        shareCode = null,
        isRemote = false,
        isHost = false,
        hostUid = null,
        syncStatus = SyncStatus.LOCAL_ONLY,
        lastSyncedAt = null,
        remoteVersion = 1L
    )

    private val sampleParticipantEntities = listOf(
        TournamentParticipantEntity(id = 1L, tournamentId = 100L, playerItemId = 1L, playerName = "خليل", clubName = "ريال مدريد", groupIndex = 0),
        TournamentParticipantEntity(id = 2L, tournamentId = 100L, playerItemId = 2L, playerName = "سعد", clubName = "برشلونة", groupIndex = 1)
    )

    private val sampleMatchEntities = listOf(
        MatchEntity(
            id = 1L,
            tournamentId = 100L,
            stage = MatchStage.GROUP_STAGE,
            groupIndex = 0,
            roundIndex = 1,
            bracketMatchIndex = null,
            playerOneName = "خليل",
            playerOneClub = "ريال مدريد",
            playerTwoName = "سعد",
            playerTwoClub = "برشلونة",
            scoreOne = null,
            scoreTwo = null,
            penaltyScoreOne = null,
            penaltyScoreTwo = null,
            winnerName = null,
            status = MatchStatus.PENDING,
            scheduledTimestamp = null,
            isPlayerOneLuckyLoser = false,
            isPlayerTwoLuckyLoser = false,
            remoteId = null,
            syncStatus = SyncStatus.LOCAL_ONLY,
            updatedAt = 1000L
        )
    )

    @Before
    fun setUp() {
        syncRepository = SyncRepositoryImpl(
            tournamentDao = tournamentDao,
            matchDao = matchDao,
            remoteTournamentDataSource = remoteTournamentDataSource,
            remoteAuditDataSource = remoteAuditDataSource,
            authDataSource = authDataSource,
            ioDispatcher = testDispatcher
        )
    }

    // ── 1. Code Generation and Formatting Tests ─────────────────────────────

    @Test
    fun `generateShareCode produces 7-character string formatted as XXX-XXX`() {
        val remoteDataSource = FirestoreTournamentDataSource(mockk(relaxed = true))
        val code = remoteDataSource.generateShareCode()

        assertNotNull(code)
        assertEquals(7, code.length)
        assertTrue(code.matches(Regex("^[2-9A-Z]{3}-[2-9A-Z]{3}$")))
        assertTrue(!code.contains("0") && !code.contains("1") && !code.contains("I") && !code.contains("O"))
    }

    @Test
    fun `Code normalization handles dashed, continuous, lowercase, and whitespace variations`() {
        val testInputs = listOf(
            "66T65S" to ("66T65S" to "66T-65S"),
            "66t-65s" to ("66T65S" to "66T-65S"),
            " 66T 65S " to ("66T65S" to "66T-65S"),
            "NMN-G5K" to ("NMNG5K" to "NMN-G5K"),
            "nmn-g5k" to ("NMNG5K" to "NMN-G5K")
        )

        testInputs.forEach { (input, expected) ->
            val raw = input.trim().uppercase()
            val cleanCode = raw.replace("-", "").replace(" ", "")
            val dashedCode = if (cleanCode.length == 6) "${cleanCode.take(3)}-${cleanCode.drop(3)}" else raw

            assertEquals(expected.first, cleanCode)
            assertEquals(expected.second, dashedCode)
        }
    }

    // ── 2. Tournament Publishing Flow ────────────────────────────────────────

    @Test
    fun `publishTournament uploads tournament, participants, and matches to remote cloud and updates Room`() = runTest(testDispatcher) {
        // Arrange
        coEvery { tournamentDao.getTournamentById(100L) } returns sampleTournamentEntity
        coEvery { authDataSource.currentUid } returns "user_host_123"
        coEvery { tournamentDao.getParticipantsByTournamentId(100L) } returns flowOf(sampleParticipantEntities)
        coEvery { matchDao.getMatchesByTournamentId(100L) } returns flowOf(sampleMatchEntities)

        val expectedPublishedDto = FirestoreTournamentDto(
            id = "tourn_cloud_abc",
            name = "بطولة الأبطال",
            shareCode = "66T-65S",
            hostUid = "user_host_123",
            stage = "GROUPS",
            status = "ACTIVE"
        )
        coEvery { remoteTournamentDataSource.publishTournament(any(), any(), any()) } returns Result.success(expectedPublishedDto)

        // Act
        val result = syncRepository.publishTournament(100L)

        // Assert
        assertTrue(result.isSuccess)
        val published = result.getOrThrow()
        assertEquals("66T-65S", published.shareCode)
        assertEquals("tourn_cloud_abc", published.remoteId)
        assertTrue(published.isRemote)
        assertTrue(published.isHost)

        // Verify Room entity update
        coVerify(exactly = 1) {
            tournamentDao.updateTournament(match {
                it.id == 100L &&
                it.remoteId == "tourn_cloud_abc" &&
                it.shareCode == "66T-65S" &&
                it.isRemote &&
                it.isHost &&
                it.syncStatus == SyncStatus.SYNCED
            })
        }
    }

    // ── 3. Joining Tournament by Share Code ──────────────────────────────────

    @Test
    fun `joinTournamentByCode successfully resolves code, downloads snapshot, and saves to Room`() = runTest(testDispatcher) {
        // Arrange
        coEvery { tournamentDao.getAllTournaments() } returns flowOf(emptyList()) // No local duplicate
        coEvery { authDataSource.currentUid } returns "guest_user_999"
        coEvery { remoteTournamentDataSource.getTournamentIdByShareCode("66T-65S") } returns Result.success("tourn_cloud_abc")

        val remoteTournamentDto = FirestoreTournamentDto(
            id = "tourn_cloud_abc",
            name = "بطولة الأبطال السحابية",
            shareCode = "66T-65S",
            hostUid = "host_uid_777",
            type = "GROUPS_KNOCKOUT",
            stage = "GROUPS",
            status = "ACTIVE"
        )
        val remoteParticipants = listOf(
            FirestoreParticipantDto(id = "p1", playerName = "خليل", clubName = "ريال مدريد", groupIndex = 0),
            FirestoreParticipantDto(id = "p2", playerName = "سعد", clubName = "برشلونة", groupIndex = 1)
        )
        val remoteMatches = listOf(
            FirestoreMatchDto(id = "m1", playerOneName = "خليل", playerTwoName = "سعد", stage = "GROUPS", roundIndex = 1)
        )

        coEvery { remoteTournamentDataSource.getTournamentSnapshot("tourn_cloud_abc") } returns Result.success(
            Triple(remoteTournamentDto, remoteParticipants, remoteMatches)
        )
        coEvery { tournamentDao.insertTournament(any()) } returns 500L
        coEvery { tournamentDao.insertParticipants(any()) } just Runs
        coEvery { matchDao.insertMatches(any()) } just Runs

        // Act
        val result = syncRepository.joinTournamentByCode("66T-65S")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(500L, result.getOrThrow())

        // Verify that member registration was called
        coVerify(exactly = 1) { remoteTournamentDataSource.joinTournamentAsMember("tourn_cloud_abc", "guest_user_999") }

        // Verify Room persistence
        coVerify(exactly = 1) {
            tournamentDao.insertTournament(match {
                it.name == "بطولة الأبطال السحابية" &&
                it.remoteId == "tourn_cloud_abc" &&
                it.shareCode == "66T-65S" &&
                !it.isHost // Joining user is viewer, not host
            })
        }
        coVerify(exactly = 1) { tournamentDao.insertParticipants(match { it.size == 2 }) }
        coVerify(exactly = 1) { matchDao.insertMatches(match { it.size == 1 }) }
    }

    @Test
    fun `joinTournamentByCode with continuous clean code (66T65S) resolves to existing tournament`() = runTest(testDispatcher) {
        // Arrange
        val existingTournament = sampleTournamentEntity.copy(
            id = 77L,
            shareCode = "66T-65S",
            isRemote = true
        )
        coEvery { tournamentDao.getAllTournaments() } returns flowOf(listOf(existingTournament))

        // Act
        val result = syncRepository.joinTournamentByCode("66t65s")

        // Assert (returns local ID directly without remote round-trip)
        assertTrue(result.isSuccess)
        assertEquals(77L, result.getOrThrow())
        coVerify(exactly = 0) { remoteTournamentDataSource.getTournamentIdByShareCode(any()) }
    }

    @Test
    fun `joinTournamentByCode fails with descriptive error when code not found on server`() = runTest(testDispatcher) {
        // Arrange
        coEvery { tournamentDao.getAllTournaments() } returns flowOf(emptyList())
        coEvery { authDataSource.currentUid } returns "user_guest"
        coEvery { remoteTournamentDataSource.getTournamentIdByShareCode("NON-EX1") } returns Result.failure(
            IllegalArgumentException("كود البطولة غير موجود على السحابة، يرجى التأكد من قيام المنظم بنشر البطولة أولاً")
        )

        // Act
        val result = syncRepository.joinTournamentByCode("NON-EX1")

        // Assert
        assertTrue(result.isFailure)
        assertEquals(
            "كود البطولة غير موجود على السحابة، يرجى التأكد من قيام المنظم بنشر البطولة أولاً",
            result.exceptionOrNull()?.message
        )
    }

    // ── 4. Master Admin and Role Verification ────────────────────────────────

    @Test
    fun `Master admin emails are strictly recognized as ADMIN`() {
        val masterEmails = listOf(
            "khalil.xdz@gmail.com",
            "KHALIL.XDZ@GMAIL.COM",
            "abdousaad430@gmail.com",
            "admin@rndm.app"
        )
        val regularEmails = listOf(
            "user@gmail.com",
            "guest@outlook.com",
            "khalil_other@gmail.com"
        )

        val auth = FirebaseAuthDataSource(mockk(relaxed = true), mockk(relaxed = true))

        masterEmails.forEach { email ->
            assertTrue("Expected $email to be master admin", auth.isMasterAdminEmail(email))
        }

        regularEmails.forEach { email ->
            assertFalse("Expected $email NOT to be master admin", auth.isMasterAdminEmail(email))
        }
    }
}
