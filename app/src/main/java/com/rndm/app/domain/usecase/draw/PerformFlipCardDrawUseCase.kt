package com.rndm.app.domain.usecase.draw

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class PerformFlipCardDrawUseCase @Inject constructor(
    private val randomProvider: RandomProvider,
    private val drawRepository: DrawRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long, items: List<ProfileItem>, selectedCardIndex: Int): DrawResult {
        require(items.isNotEmpty()) { "قائمة العناصر لا يمكن أن تكون فارغة" }
        val shuffled = randomProvider.shuffle(items)
        val selectedItem = shuffled[selectedCardIndex % shuffled.size]
        val result = DrawResult(
            drawType = DrawType.FLIP_CARDS,
            selectedItem = selectedItem,
            timestamp = System.currentTimeMillis()
        )
        drawRepository.saveDrawResult(result)
        profileRepository.updateLastUsed(profileId, result.timestamp)
        return result
    }
}
