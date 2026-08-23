package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllProfilesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<List<Profile>> {
        return profileRepository.observeAllProfiles()
    }
}
