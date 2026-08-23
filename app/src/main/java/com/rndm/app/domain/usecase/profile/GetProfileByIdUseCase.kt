package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfileByIdUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): Profile? {
        return profileRepository.getProfileById(id)
    }
}
