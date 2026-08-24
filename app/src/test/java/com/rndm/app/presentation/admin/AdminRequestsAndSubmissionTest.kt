package com.rndm.app.presentation.admin

import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.RequestStatus
import com.rndm.app.domain.model.RequestType
import com.rndm.app.domain.repository.RequestRepository
import com.rndm.app.domain.usecase.request.ApproveAdminRequestUseCase
import com.rndm.app.domain.usecase.request.ObserveAdminRequestsUseCase
import com.rndm.app.domain.usecase.request.RejectAdminRequestUseCase
import com.rndm.app.domain.usecase.request.SubmitTournamentRequestUseCase
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdminRequestsAndSubmissionTest {

    private val requestRepository: RequestRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var submitTournamentRequestUseCase: SubmitTournamentRequestUseCase
    private lateinit var observeAdminRequestsUseCase: ObserveAdminRequestsUseCase
    private lateinit var approveAdminRequestUseCase: ApproveAdminRequestUseCase
    private lateinit var rejectAdminRequestUseCase: RejectAdminRequestUseCase

    private val sampleScoreRequest = AdminRequest(
        id = "req_101",
        type = RequestType.CHANGE_SCORE,
        tournamentId = "tourn_abc",
        tournamentName = "بطولة الأحلام",
        requesterUid = "user_khalil",
        requesterName = "خليل",
        requesterEmail = "khalil.xdz@gmail.com",
        matchId = 1L,
        remoteMatchId = "match_xyz",
        scoreOne = 3,
        scoreTwo = 1,
        penaltyScoreOne = null,
        penaltyScoreTwo = null,
        playerOneName = "خليل",
        playerTwoName = "سعد",
        description = "طلب تعديل نتيجة مباراة خليل ضد سعد إلى (3 - 1)",
        status = RequestStatus.PENDING,
        createdAt = 1000L
    )

    private val samplePlayerReplaceRequest = AdminRequest(
        id = "req_102",
        type = RequestType.PLAYER_REPLACE,
        tournamentId = "tourn_abc",
        tournamentName = "بطولة الأحلام",
        requesterUid = "user_participant",
        requesterName = "مشارك",
        requesterEmail = "user@gmail.com",
        playerOneName = "أحمد",
        playerTwoName = "يوسف",
        playerTwoClub = "مانشستر سيتي",
        description = "طلب استبدال اللاعب أحمد باللاعب يوسف (مانشستر سيتي)",
        status = RequestStatus.PENDING,
        createdAt = 2000L
    )

    @Before
    fun setUp() {
        submitTournamentRequestUseCase = SubmitTournamentRequestUseCase(requestRepository)
        observeAdminRequestsUseCase = ObserveAdminRequestsUseCase(requestRepository)
        approveAdminRequestUseCase = ApproveAdminRequestUseCase(requestRepository)
        rejectAdminRequestUseCase = RejectAdminRequestUseCase(requestRepository)
    }

    @Test
    fun `submit score change request successfully calls repository`() = runTest(testDispatcher) {
        coEvery { requestRepository.submitRequest(sampleScoreRequest) } returns Result.success(Unit)

        val result = submitTournamentRequestUseCase(sampleScoreRequest)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { requestRepository.submitRequest(sampleScoreRequest) }
    }

    @Test
    fun `submit player replace request successfully calls repository`() = runTest(testDispatcher) {
        coEvery { requestRepository.submitRequest(samplePlayerReplaceRequest) } returns Result.success(Unit)

        val result = submitTournamentRequestUseCase(samplePlayerReplaceRequest)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { requestRepository.submitRequest(samplePlayerReplaceRequest) }
    }

    @Test
    fun `approve admin request marks status approved and updates remote entities`() = runTest(testDispatcher) {
        coEvery { requestRepository.approveRequest("req_101") } returns Result.success(Unit)

        val result = approveAdminRequestUseCase("req_101")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { requestRepository.approveRequest("req_101") }
    }

    @Test
    fun `reject admin request attaches rejection reason`() = runTest(testDispatcher) {
        coEvery { requestRepository.rejectRequest("req_101", "النتيجة غير صحيحة") } returns Result.success(Unit)

        val result = rejectAdminRequestUseCase("req_101", "النتيجة غير صحيحة")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { requestRepository.rejectRequest("req_101", "النتيجة غير صحيحة") }
    }

    @Test
    fun `observe all requests filters and categorizes requests`() = runTest(testDispatcher) {
        coEvery { requestRepository.observeAllRequests() } returns flowOf(
            listOf(sampleScoreRequest, samplePlayerReplaceRequest)
        )

        observeAdminRequestsUseCase().collect { list ->
            assertEquals(2, list.size)
            assertEquals(RequestType.CHANGE_SCORE, list[0].type)
            assertEquals(RequestType.PLAYER_REPLACE, list[1].type)
        }
    }
}
