package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long) {
        profileRepository.deleteProfile(profileId)
    }
}
