package com.rndm.app.presentation.tournament.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.usecase.tournament.DeleteTournamentUseCase
import com.rndm.app.domain.usecase.tournament.GetArchivedTournamentsUseCase
import com.rndm.app.domain.usecase.tournament.UnarchiveTournamentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TournamentArchiveUiState(
    val archivedTournaments: List<Tournament> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDeleteId: Long? = null
)

@HiltViewModel
class TournamentArchiveViewModel @Inject constructor(
    private val getArchivedTournamentsUseCase: GetArchivedTournamentsUseCase,
    private val unarchiveTournamentUseCase: UnarchiveTournamentUseCase,
    private val deleteTournamentUseCase: DeleteTournamentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentArchiveUiState())
    val uiState: StateFlow<TournamentArchiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getArchivedTournamentsUseCase().collect { list ->
                _uiState.update { it.copy(archivedTournaments = list, isLoading = false) }
            }
        }
    }

    fun unarchive(tournamentId: Long) {
        viewModelScope.launch {
            unarchiveTournamentUseCase(tournamentId)
        }
    }

    fun requestDelete(tournamentId: Long) {
        _uiState.update { it.copy(pendingDeleteId = tournamentId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            deleteTournamentUseCase(id)
        }
    }
}
