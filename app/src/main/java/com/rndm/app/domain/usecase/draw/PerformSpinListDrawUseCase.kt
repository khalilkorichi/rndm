package com.rndm.app.domain.usecase.draw

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class PerformSpinListDrawUseCase @Inject constructor(
    private val randomProvider: RandomProvider,
    private val drawRepository: DrawRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long, items: List<ProfileItem>): DrawResult {
        require(items.isNotEmpty()) { "قائمة العناصر لا يمكن أن تكون فارغة" }
        val selectedIndex = randomProvider.nextInt(items.size)
        val selectedItem = items[selectedIndex]
        val result = DrawResult(
            drawType = DrawType.SPIN_LIST,
            selectedItem = selectedItem,
            timestamp = System.currentTimeMillis()
        )
        drawRepository.saveDrawResult(result)
        profileRepository.updateLastUsed(profileId, result.timestamp)
        return result
    }
}
