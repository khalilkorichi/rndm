package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerMatchRecord
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerMatchHistoryUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(playerName: String): Flow<List<PlayerMatchRecord>> {
        return playerProfileRepository.getPlayerMatchHistory(playerName)
    }
}
