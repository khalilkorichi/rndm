package com.rndm.app.presentation.tournament.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.usecase.tournament.ArchiveTournamentUseCase
import com.rndm.app.domain.usecase.tournament.DeleteTournamentUseCase
import com.rndm.app.domain.usecase.tournament.GetAllTournamentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentListViewModel @Inject constructor(
    private val getAllTournamentsUseCase: GetAllTournamentsUseCase,
    private val deleteTournamentUseCase: DeleteTournamentUseCase,
    private val archiveTournamentUseCase: ArchiveTournamentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentListUiState())
    val uiState: StateFlow<TournamentListUiState> = _uiState.asStateFlow()

    init {
        loadTournaments()
    }

    private fun loadTournaments() {
        viewModelScope.launch {
            getAllTournamentsUseCase().collect { list ->
                // Filter out archived tournaments from the active list
                _uiState.update { it.copy(tournaments = list.filter { t -> !t.isArchived }, isLoading = false) }
            }
        }
    }

    fun onFilterSelect(filter: TournamentFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onSortSelect(sort: TournamentSort) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    // ── Swipe Actions ──────────────────────────────────────────

    fun requestDelete(tournamentId: Long) {
        _uiState.update { it.copy(pendingDeleteId = tournamentId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            deleteTournamentUseCase(id)
        }
    }

    fun archiveTournament(tournamentId: Long) {
        viewModelScope.launch {
            archiveTournamentUseCase(tournamentId)
        }
    }
}

