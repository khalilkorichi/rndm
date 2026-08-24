package com.rndm.app.domain.usecase.player

import com.rndm.app.domain.repository.PlayerProfileRepository
import javax.inject.Inject

class SavePlayerCustomProfileUseCase @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository
) {
    suspend operator fun invoke(
        name: String,
        nickname: String?,
        avatarIcon: String?,
        favoriteClub: String?,
        notes: String?
    ) {
        playerProfileRepository.savePlayerCustomProfile(name, nickname, avatarIcon, favoriteClub, notes)
    }
}
