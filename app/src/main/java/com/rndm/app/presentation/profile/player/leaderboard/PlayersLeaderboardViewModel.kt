package com.rndm.app.presentation.profile.player.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.PlayerLeaderboardItem
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

    private var rawPlayers: List<PlayerLeaderboardItem> = emptyList()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            getAllPlayersLeaderboardUseCase().collect { list ->
                rawPlayers = list
                _uiState.update { current ->
                    val processed = processAndSortList(
                        list = list,
                        column = current.sortColumn,
                        isAscending = current.isAscending,
                        searchQuery = current.searchQuery
                    )
                    current.copy(
                        players = list,
                        filteredPlayers = processed,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val processed = processAndSortList(
                list = rawPlayers,
                column = current.sortColumn,
                isAscending = current.isAscending,
                searchQuery = query
            )
            current.copy(
                searchQuery = query,
                filteredPlayers = processed
            )
        }
    }

    fun onSortColumnClick(column: LeaderboardColumn) {
        _uiState.update { current ->
            val newIsAscending = if (current.sortColumn == column) !current.isAscending else false
            val processed = processAndSortList(
                list = rawPlayers,
                column = column,
                isAscending = newIsAscending,
                searchQuery = current.searchQuery
            )
            current.copy(
                sortColumn = column,
                isAscending = newIsAscending,
                filteredPlayers = processed
            )
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadLeaderboard()
    }

    private fun processAndSortList(
        list: List<PlayerLeaderboardItem>,
        column: LeaderboardColumn,
        isAscending: Boolean,
        searchQuery: String
    ): List<PlayerLeaderboardItem> {
        val filtered = if (searchQuery.isBlank()) {
            list
        } else {
            val q = searchQuery.trim().lowercase()
            list.filter {
                it.playerName.lowercase().contains(q) ||
                        (it.nickname?.lowercase()?.contains(q) == true)
            }
        }

        val comparator = when (column) {
            LeaderboardColumn.RANK, LeaderboardColumn.TITLES -> compareBy<PlayerLeaderboardItem> { it.titlesCount }
                .thenBy { it.goalDifference }
                .thenBy { it.winRate }
                .thenBy { it.totalMatches }
                .thenBy { it.goalsScored }
                .thenBy { it.points }
            LeaderboardColumn.PLAYER -> compareBy<PlayerLeaderboardItem> { it.playerName }
            LeaderboardColumn.MATCHES -> compareBy<PlayerLeaderboardItem> { it.totalMatches }
                .thenBy { it.totalWins }
                .thenBy { it.titlesCount }
            LeaderboardColumn.WINS -> compareBy<PlayerLeaderboardItem> { it.totalWins }
                .thenBy { it.winRate }
                .thenBy { it.titlesCount }
            LeaderboardColumn.DRAWS -> compareBy<PlayerLeaderboardItem> { it.totalDraws }
                .thenBy { it.totalMatches }
            LeaderboardColumn.LOSSES -> compareBy<PlayerLeaderboardItem> { it.totalLosses }
                .thenBy { it.totalMatches }
            LeaderboardColumn.GOALS_FOR -> compareBy<PlayerLeaderboardItem> { it.goalsScored }
                .thenBy { it.goalDifference }
                .thenBy { it.titlesCount }
            LeaderboardColumn.GOALS_AGAINST -> compareBy<PlayerLeaderboardItem> { it.goalsConceded }
                .thenBy { it.goalDifference }
            LeaderboardColumn.GOAL_DIFF -> compareBy<PlayerLeaderboardItem> { it.goalDifference }
                .thenBy { it.goalsScored }
                .thenBy { it.titlesCount }
            LeaderboardColumn.WIN_RATE -> compareBy<PlayerLeaderboardItem> { it.winRate }
                .thenBy { it.totalWins }
                .thenBy { it.titlesCount }
            LeaderboardColumn.POINTS -> compareBy<PlayerLeaderboardItem> { it.points }
                .thenBy { it.goalDifference }
                .thenBy { it.goalsScored }
            LeaderboardColumn.CLEAN_SHEETS -> compareBy<PlayerLeaderboardItem> { it.cleanSheets }
                .thenBy { it.totalWins }
        }

        val sorted = if (isAscending) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(comparator.reversed())
        }

        return sorted.mapIndexed { index, item -> item.copy(rank = index + 1) }
    }
}
