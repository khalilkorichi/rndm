package com.rndm.app.domain.usecase.draw

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.MatchPairing
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import javax.inject.Inject

class GenerateRoundRobinPairingsUseCase @Inject constructor(
    private val randomProvider: RandomProvider,
    private val drawRepository: DrawRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long, items: List<ProfileItem>): DrawResult {
        require(items.size >= 2) { "يجب توفر لاعبين اثنين على الأقل لإجراء الإقران" }
        val shuffled = randomProvider.shuffle(items)
        val pairings = shuffled.chunked(2).map { pair ->
            MatchPairing(
                playerOne = pair[0],
                playerTwo = pair.getOrNull(1)
            )
        }
        val result = DrawResult(
            drawType = DrawType.ROUND_ROBIN,
            selectedItem = null,
            pairings = pairings,
            timestamp = System.currentTimeMillis()
        )
        drawRepository.saveDrawResult(result)
        profileRepository.updateLastUsed(profileId, result.timestamp)
        return result
    }
}
