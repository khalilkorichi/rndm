package com.rndm.app.domain.usecase.profile

import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile) {
        require(profile.name.isNotBlank()) { "اسم البروفايل لا يمكن أن يكون فارغاً" }
        require(profile.items.size >= Constants.MIN_PROFILE_ITEMS) {
            "يجب إضافة عنصرين على الأقل للبروفايل"
        }
        profileRepository.updateProfile(profile)
    }
}
