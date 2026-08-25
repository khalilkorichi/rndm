package com.rndm.app.domain.usecase.profile

import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateItemActiveStateUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend fun setItemActive(itemId: Long, isActive: Boolean) {
        if (itemId > 0L) {
            repository.updateItemActiveState(itemId, isActive)
        }
    }

    suspend fun setItemActiveByLabel(profileId: Long, label: String, isActive: Boolean) {
        if (profileId > 0L && label.isNotBlank()) {
            repository.updateItemActiveStateByLabel(profileId, label.trim(), isActive)
        }
    }

    suspend fun resetAllActive(profileId: Long) {
        if (profileId > 0L) {
            repository.resetAllItemsToActive(profileId)
        }
    }
}
