package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthUseCasesTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)

    private lateinit var initializeGuestSessionUseCase: InitializeGuestSessionUseCase
    private lateinit var loginAdminUseCase: LoginAdminUseCase
    private lateinit var logoutAdminUseCase: LogoutAdminUseCase
    private lateinit var getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase

    @Before
    fun setUp() {
        initializeGuestSessionUseCase = InitializeGuestSessionUseCase(authRepository)
        loginAdminUseCase = LoginAdminUseCase(authRepository)
        logoutAdminUseCase = LogoutAdminUseCase(authRepository)
        getCurrentUserRoleUseCase = GetCurrentUserRoleUseCase(authRepository)
    }

    @Test
    fun `initializeGuestSessionUseCase calls repository initializeGuestSession`() = runTest {
        coEvery { authRepository.initializeGuestSession() } returns Result.success("guest_uid_123")

        val result = initializeGuestSessionUseCase()

        assertTrue(result.isSuccess)
        assertEquals("guest_uid_123", result.getOrNull())
        coVerify { authRepository.initializeGuestSession() }
    }

    @Test
    fun `loginAdminUseCase with valid credentials calls repository loginAdmin`() = runTest {
        coEvery { authRepository.loginAdmin("admin", "secret123") } returns Result.success("admin_uid_456")

        val result = loginAdminUseCase("admin", "secret123")

        assertTrue(result.isSuccess)
        assertEquals("admin_uid_456", result.getOrNull())
        coVerify { authRepository.loginAdmin("admin", "secret123") }
    }

    @Test
    fun `loginAdminUseCase with blank username returns failure without repository call`() = runTest {
        val result = loginAdminUseCase("", "secret123")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { authRepository.loginAdmin(any(), any()) }
    }

    @Test
    fun `logoutAdminUseCase calls repository logoutAdmin`() = runTest {
        coEvery { authRepository.logoutAdmin() } returns Result.success(Unit)

        val result = logoutAdminUseCase()

        assertTrue(result.isSuccess)
        coVerify { authRepository.logoutAdmin() }
    }

    @Test
    fun `getCurrentUserRoleUseCase observes role flow from repository`() = runTest {
        every { authRepository.currentUserRole } returns flowOf(UserRole.ADMIN)

        val role = getCurrentUserRoleUseCase().first()

        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun `getCurrentUserRoleUseCase getFastRole returns repository fast role synchronously`() {
        every { authRepository.getFastRole() } returns UserRole.ADMIN

        val role = getCurrentUserRoleUseCase.getFastRole()

        assertEquals(UserRole.ADMIN, role)
        coVerify(exactly = 1) { authRepository.getFastRole() }
    }

    @Test
    fun `getCurrentUserProfileUseCase getFastProfile returns repository fast profile synchronously`() {
        val useCase = GetCurrentUserProfileUseCase(authRepository)
        val profile = com.rndm.app.domain.model.UserProfile(
            uid = "uid_123",
            email = "user@test.com",
            username = "test",
            displayName = "Tester",
            role = UserRole.USER
        )
        every { authRepository.getFastUserProfile() } returns profile

        val result = useCase.getFastProfile()

        assertEquals(profile, result)
        coVerify(exactly = 1) { authRepository.getFastUserProfile() }
    }
}
