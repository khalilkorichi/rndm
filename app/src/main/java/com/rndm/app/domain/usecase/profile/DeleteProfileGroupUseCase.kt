package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class DeleteProfileGroupUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(groupId: Long) {
        repository.deleteProfileGroup(groupId)
    }
}
