package com.rndm.app.data.repository

import com.rndm.app.data.remote.firebase.FirebaseAuthDataSource
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override val currentUserRole: Flow<UserRole> = flow {
        // Emit fast synchronous role immediately (0ms) to eliminate loading flicker
        emit(authDataSource.getFastUserRole())
        authDataSource.observeAuthState().collect { user ->
            if (user == null || user.isAnonymous) {
                emit(UserRole.GUEST)
            } else {
                emit(authDataSource.getFastUserRole())
                val resolvedRole = authDataSource.determineUserRole()
                emit(resolvedRole)
            }
        }
    }.distinctUntilChanged()

    override val currentUid: Flow<String?> = authDataSource.observeAuthState().map { it?.uid }.distinctUntilChanged()

    override val currentUserProfile: Flow<UserProfile?> = flow {
        // Emit fast synchronous profile immediately (0ms) to eliminate loading flicker
        emit(authDataSource.getFastUserProfile())
        authDataSource.observeAuthState().collect { user ->
            if (user == null || user.isAnonymous) {
                emit(null)
            } else {
                emit(authDataSource.getFastUserProfile())
                val resolvedProfile = authDataSource.getCurrentUserProfile()
                if (resolvedProfile != null) {
                    emit(resolvedProfile)
                }
            }
        }
    }.distinctUntilChanged()

    override fun getFastRole(): UserRole = authDataSource.getFastUserRole()

    override fun getFastUserProfile(): UserProfile? = authDataSource.getFastUserProfile()

    override suspend fun initializeGuestSession(): Result<String> = withContext(ioDispatcher) {
        authDataSource.signInAnonymously()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<String> = withContext(ioDispatcher) {
        authDataSource.signUpWithEmail(email, password, displayName)
    }

    override suspend fun login(email: String, password: String): Result<String> = withContext(ioDispatcher) {
        authDataSource.signInWithEmailAndPassword(email, password)
    }

    override suspend fun loginAdmin(email: String, password: String): Result<String> = withContext(ioDispatcher) {
        authDataSource.signInWithEmailAndPassword(email, password)
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        authDataSource.signOut()
    }

    override suspend fun logoutAdmin(): Result<Unit> = withContext(ioDispatcher) {
        authDataSource.signOut()
    }

    override suspend fun getCurrentRole(): UserRole = withContext(ioDispatcher) {
        authDataSource.determineUserRole()
    }

    override suspend fun getCurrentUserProfile(): UserProfile? = withContext(ioDispatcher) {
        authDataSource.getCurrentUserProfile()
    }

    override fun getAllUsers(): Flow<List<UserProfile>> {
        return authDataSource.observeAllUsers().map { dtoList ->
            dtoList.map { dto ->
                val isMaster = authDataSource.isMasterAdminEmail(dto.email)
                val role = if (isMaster || dto.role.equals("admin", ignoreCase = true)) {
                    UserRole.ADMIN
                } else {
                    UserRole.USER
                }
                UserProfile(
                    uid = dto.uid,
                    email = dto.email,
                    username = dto.username,
                    displayName = dto.displayName.ifBlank { dto.email.substringBefore("@") },
                    role = role,
                    createdAt = dto.createdAt
                )
            }
        }
    }

    override suspend fun updateUserRole(targetUid: String, newRole: UserRole): Result<Unit> = withContext(ioDispatcher) {
        val roleStr = if (newRole == UserRole.ADMIN) "admin" else "user"
        authDataSource.updateUserRole(targetUid, roleStr)
    }

    override suspend fun promoteUserByEmail(email: String): Result<Unit> = withContext(ioDispatcher) {
        authDataSource.promoteUserByEmail(email)
    }
}
