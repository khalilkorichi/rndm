package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.model.PlayerTournamentParticipation
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerTournamentHistoryUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    operator fun invoke(playerName: String): Flow<List<PlayerTournamentParticipation>> {
        return playerProfileRepository.getPlayerTournamentHistory(playerName)
    }
}
