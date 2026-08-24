package com.rndm.app.domain.usecase.auth

import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateUserRoleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUid: String, newRole: UserRole): Result<Unit> {
        return authRepository.updateUserRole(targetUid, newRole)
    }
}
