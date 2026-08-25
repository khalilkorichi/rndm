package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.model.ProfileGroup
import com.rndm.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileGroupsUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): Flow<List<ProfileGroup>> {
        return repository.observeProfileGroups()
    }
}
