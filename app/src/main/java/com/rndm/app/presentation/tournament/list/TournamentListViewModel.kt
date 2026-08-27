package com.rndm.app.presentation.tournament.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.usecase.sync.GetLiveTournamentPreviewUseCase
import com.rndm.app.domain.usecase.sync.JoinTournamentByCodeUseCase
import com.rndm.app.domain.usecase.sync.ObserveAvailableLiveTournamentsUseCase
import com.rndm.app.domain.usecase.tournament.ArchiveTournamentUseCase
import com.rndm.app.domain.usecase.tournament.DeleteTournamentUseCase
import com.rndm.app.domain.usecase.tournament.GetAllTournamentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val archiveTournamentUseCase: ArchiveTournamentUseCase,
    private val observeAvailableLiveTournamentsUseCase: ObserveAvailableLiveTournamentsUseCase,
    private val getLiveTournamentPreviewUseCase: GetLiveTournamentPreviewUseCase,
    private val joinTournamentByCodeUseCase: JoinTournamentByCodeUseCase,
    private val cleanupExpiredTournamentsUseCase: com.rndm.app.domain.usecase.sync.CleanupExpiredTournamentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentListUiState())
    val uiState: StateFlow<TournamentListUiState> = _uiState.asStateFlow()

    init {
        loadTournaments()
        observeLiveTournaments()
    }

    private fun loadTournaments() {
        viewModelScope.launch {
            getAllTournamentsUseCase().collect { list ->
                // Filter out archived tournaments from the active list
                _uiState.update { it.copy(tournaments = list.filter { t -> !t.isArchived }, isLoading = false) }
            }
        }
    }

    private fun observeLiveTournaments() {
        viewModelScope.launch {
            observeAvailableLiveTournamentsUseCase().collect { liveList ->
                _uiState.update { it.copy(liveTournaments = liveList) }
            }
        }
    }

    fun onFilterSelect(filter: TournamentFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onSortSelect(sort: TournamentSort) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                cleanupExpiredTournamentsUseCase()
                kotlinx.coroutines.delay(600)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    // ── Live Tournaments Actions ────────────────────────────────

    fun dismissLiveTournament(remoteId: String) {
        _uiState.update { current ->
            current.copy(dismissedLiveTournamentIds = current.dismissedLiveTournamentIds + remoteId)
        }
    }

    fun openLiveTournamentPreview(tournament: Tournament) {
        val remoteId = tournament.remoteId ?: return
        _uiState.update { it.copy(selectedPreviewTournament = tournament, isPreviewLoading = true, liveTournamentPreview = null) }
        viewModelScope.launch {
            try {
                val result = getLiveTournamentPreviewUseCase(remoteId)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isPreviewLoading = false, liveTournamentPreview = result.getOrNull()) }
                } else {
                    _uiState.update { it.copy(isPreviewLoading = false, liveTournamentErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "تعذر تحميل معاينة البطولة") }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isPreviewLoading = false, liveTournamentErrorMessage = e.localizedMessage ?: "حدث خطأ أثناء تحميل المعاينة") }
            }
        }
    }

    fun dismissLiveTournamentPreview() {
        _uiState.update { it.copy(selectedPreviewTournament = null, liveTournamentPreview = null, isPreviewLoading = false) }
    }

    fun joinLiveTournament(tournament: Tournament) {
        val shareCode = tournament.shareCode ?: return
        _uiState.update { it.copy(isJoiningLiveTournament = true, liveTournamentErrorMessage = null) }
        viewModelScope.launch {
            try {
                val result = joinTournamentByCodeUseCase(shareCode)
                if (result.isSuccess) {
                    val localId = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            isJoiningLiveTournament = false,
                            selectedPreviewTournament = null,
                            liveTournamentPreview = null,
                            joinedTournamentLocalId = localId
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isJoiningLiveTournament = false,
                            liveTournamentErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "فشل الانضمام للبطولة"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isJoiningLiveTournament = false,
                        liveTournamentErrorMessage = e.localizedMessage ?: "حدث خطأ أثناء الانضمام للبطولة"
                    )
                }
            }
        }
    }

    fun clearJoinedTournamentEvent() {
        _uiState.update { it.copy(joinedTournamentLocalId = null) }
    }

    fun clearLiveTournamentError() {
        _uiState.update { it.copy(liveTournamentErrorMessage = null) }
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


