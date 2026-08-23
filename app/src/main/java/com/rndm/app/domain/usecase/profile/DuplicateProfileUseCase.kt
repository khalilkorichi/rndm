package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class DuplicateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long, newName: String): Long {
        require(newName.isNotBlank()) { "اسم البروفايل الجديد لا يمكن أن يكون فارغاً" }
        return profileRepository.duplicateProfile(profileId, newName)
    }
}
