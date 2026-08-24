package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerCareerStats
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerCareerStatsUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(playerName: String): Flow<PlayerCareerStats> {
        return playerProfileRepository.getPlayerCareerStats(playerName)
    }
}
