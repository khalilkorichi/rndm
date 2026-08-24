package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerQuickStats
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayersQuickStatsUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(playerNames: List<String>): Flow<Map<String, PlayerQuickStats>> {
        return playerProfileRepository.getPlayersQuickStats(playerNames)
    }
}
