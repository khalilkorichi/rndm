package com.rndm.app.domain.usecase.draw

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.repository.DrawRepository
import javax.inject.Inject

data class ClubDuelOutcome(
    val winnerName: String,
    val winnerIndex: Int,
    val contestedClub: String,
    val allContestants: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

class PerformClubDuelDrawUseCase @Inject constructor(
    private val randomProvider: RandomProvider,
    private val drawRepository: DrawRepository
) {
    suspend operator fun invoke(
        contestedClub: String,
        contestants: List<String>
    ): ClubDuelOutcome {
        require(contestants.isNotEmpty()) { "قائمة المتنافسين لا يمكن أن تكون فارغة" }
        require(contestedClub.isNotBlank()) { "اسم النادي المتنازع عليه لا يمكن أن يكون فارغاً" }

        val winningIndex = randomProvider.nextInt(contestants.size)
        val winnerName = contestants[winningIndex]

        val outcome = ClubDuelOutcome(
            winnerName = winnerName,
            winnerIndex = winningIndex,
            contestedClub = contestedClub,
            allContestants = contestants,
            timestamp = System.currentTimeMillis()
        )

        // Save into draw history
        val dummyProfileItem = ProfileItem(
            id = 0L,
            profileId = 0L,
            label = "$winnerName (حسم $contestedClub)",
            order = 0
        )
        drawRepository.saveDrawResult(
            DrawResult(
                drawType = DrawType.WHEEL,
                selectedItem = dummyProfileItem,
                timestamp = outcome.timestamp
            )
        )

        return outcome
    }
}
