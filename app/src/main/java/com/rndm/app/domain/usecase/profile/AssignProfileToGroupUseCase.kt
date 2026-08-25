package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class AssignProfileToGroupUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long, groupId: Long?) {
        repository.updateProfileGroup(profileId, groupId)
    }
}
