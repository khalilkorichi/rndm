package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.model.ProfileGroup
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class CreateProfileGroupUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(name: String, icon: String = "ic_folder", colorHex: String? = null): Long {
        require(name.isNotBlank()) { "اسم المجموعة لا يمكن أن يكون فارغاً" }
        val group = ProfileGroup(
            name = name.trim(),
            icon = icon,
            colorHex = colorHex
        )
        return repository.createProfileGroup(group)
    }
}
