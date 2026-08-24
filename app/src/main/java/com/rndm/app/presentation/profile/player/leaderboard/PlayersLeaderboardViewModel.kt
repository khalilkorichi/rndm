package com.rndm.app.presentation.profile.player.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.usecase.player.GetAllPlayersLeaderboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayersLeaderboardViewModel @Inject constructor(
    private val getAllPlayersLeaderboardUseCase: GetAllPlayersLeaderboardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayersLeaderboardUiState())
    val uiState: StateFlow<PlayersLeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            getAllPlayersLeaderboardUseCase().collect { list ->
                _uiState.update { current ->
                    val sorted = sortList(list, current.sortBy)
                    current.copy(
                        players = sorted,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSortChange(sort: LeaderboardSortBy) {
        _uiState.update { current ->
            current.copy(
                sortBy = sort,
                players = sortList(current.players, sort)
            )
        }
    }

    private fun sortList(list: List<com.rndm.app.domain.model.PlayerLeaderboardItem>, sort: LeaderboardSortBy): List<com.rndm.app.domain.model.PlayerLeaderboardItem> {
        val sorted = when (sort) {
            LeaderboardSortBy.TITLES -> list.sortedWith(
                compareByDescending<com.rndm.app.domain.model.PlayerLeaderboardItem> { it.titlesCount }
                    .thenByDescending { it.goalsScored }
                    .thenByDescending { it.winRate }
            )
            LeaderboardSortBy.GOALS -> list.sortedWith(
                compareByDescending<com.rndm.app.domain.model.PlayerLeaderboardItem> { it.goalsScored }
                    .thenByDescending { it.titlesCount }
                    .thenByDescending { it.winRate }
            )
            LeaderboardSortBy.WIN_RATE -> list.sortedWith(
                compareByDescending<com.rndm.app.domain.model.PlayerLeaderboardItem> { it.winRate }
                    .thenByDescending { it.titlesCount }
                    .thenByDescending { it.totalWins }
            )
            LeaderboardSortBy.MATCHES -> list.sortedWith(
                compareByDescending<com.rndm.app.domain.model.PlayerLeaderboardItem> { it.totalMatches }
                    .thenByDescending { it.totalWins }
                    .thenByDescending { it.titlesCount }
            )
        }
        return sorted.mapIndexed { index, item -> item.copy(rank = index + 1) }
    }
}
