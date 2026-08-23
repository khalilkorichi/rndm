package com.rndm.app.presentation.draw.fixtures

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.Profile

@Immutable
data class MatchFixturesUiState(
    val isLoading: Boolean = false,
    val fixtures: List<DrawFixture> = emptyList(),
    val playersProfiles: List<Profile> = emptyList(),
    val editingFixture: DrawFixture? = null,
    val inputScoreOne: String = "",
    val inputScoreTwo: String = "",
    val playerToReplace: String? = null,
    val playerToReplaceClub: String? = null,
    val isAddPlayersDialogOpen: Boolean = false,
    val error: String? = null
) {
    val totalMatches: Int
        get() = fixtures.size

    val completedMatches: Int
        get() = fixtures.count { it.isFinished }

    val readyMatches: Int
        get() = fixtures.count { it.isReady }

    val existingPlayerNames: List<String>
        get() = fixtures.flatMap { listOfNotNull(it.playerOneName, it.playerTwoName) }.distinct()
}

