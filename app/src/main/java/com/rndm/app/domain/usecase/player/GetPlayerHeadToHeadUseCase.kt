package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerHeadToHead
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerHeadToHeadUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(playerName: String): Flow<List<PlayerHeadToHead>> {
        return playerProfileRepository.getPlayerHeadToHead(playerName)
    }
}
