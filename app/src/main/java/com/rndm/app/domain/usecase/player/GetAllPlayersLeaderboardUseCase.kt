package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerLeaderboardItem
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlayersLeaderboardUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(): Flow<List<PlayerLeaderboardItem>> {
        return playerProfileRepository.getAllPlayersLeaderboard()
    }
}
