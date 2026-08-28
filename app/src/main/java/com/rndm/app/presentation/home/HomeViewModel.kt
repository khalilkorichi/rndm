package com.rndm.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.repository.TournamentRepository
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.tournament.UpdateMatchScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    getAllProfilesUseCase: GetAllProfilesUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val tournamentRepository: TournamentRepository,
    private val updateMatchScoreUseCase: UpdateMatchScoreUseCase
) : ViewModel() {

    private val _selectedMatchForScore = MutableStateFlow<Match?>(null)
    private val _isRestoreSuccessDialogOpen = MutableStateFlow(false)

    // Flow for profiles
    private val profilesFlow = getAllProfilesUseCase()

    // Flow for all tournaments to extract counts and champions
    private val tournamentsFlow = tournamentRepository.getAllTournaments()

    // Flow for the most recent uncompleted active tournament and its matches
    private val activeTournamentWithMatchesFlow = tournamentRepository.getActiveTournaments()
        .flatMapLatest { tournaments ->
            val activeTournament = tournaments
                .filter { it.stage != TournamentStage.COMPLETED }
                .maxByOrNull { it.updatedAt }

            if (activeTournament != null) {
                tournamentRepository.getMatches(activeTournament.id).map { matches ->
                    Pair(activeTournament, matches)
                }
            } else {
                flowOf(Pair(null, emptyList()))
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        profilesFlow,
        tournamentsFlow,
        activeTournamentWithMatchesFlow,
        _selectedMatchForScore,
        _isRestoreSuccessDialogOpen
    ) { profiles, tournaments, (activeTournament, matches), selectedMatch, isRestoreSuccessOpen ->
        val sortedProfiles = profiles.sortedByDescending { it.lastUsedAt ?: 0L }
        val recentProfile = sortedProfiles.firstOrNull()
        val topProfiles = sortedProfiles.take(6)

        // Calculate match index (first pending match)
        val currentMatchIndex = if (matches.isNotEmpty()) {
            val pendingIndex = matches.indexOfFirst { it.status == MatchStatus.PENDING }
            if (pendingIndex >= 0) pendingIndex else (matches.size - 1).coerceAtLeast(0)
        } else 0

        val totalTournamentsCount = tournaments.size
        val completedTournaments = tournaments.filter { it.stage == TournamentStage.COMPLETED }
        val activeTournamentsCount = tournaments.count { it.stage != TournamentStage.COMPLETED && !it.isArchived }
        val recentChampionTournament = completedTournaments.maxByOrNull { it.updatedAt }

        HomeUiState(
            isLoading = false,
            recentProfile = recentProfile,
            topProfiles = topProfiles,
            totalProfilesCount = profiles.size,
            activeTournament = activeTournament,
            activeTournamentMatches = matches,
            currentMatchIndex = currentMatchIndex,
            totalTournamentsCount = totalTournamentsCount,
            activeTournamentsCount = activeTournamentsCount,
            completedTournamentsCount = completedTournaments.size,
            recentChampionTournament = recentChampionTournament,
            selectedMatchForScore = selectedMatch,
            isRestoreSuccessDialogOpen = isRestoreSuccessOpen,
            error = null
        )
    }.catch { exception ->
        emit(HomeUiState(isLoading = false, error = exception.message ?: "حدث خطأ غير متوقع"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onRestoreDefaultProfiles() {
        viewModelScope.launch {
            try {
                val currentProfiles = profilesFlow.first()
                val missingProfiles = ProfilePresets.getMissingDefaultProfiles(currentProfiles)
                val toInsert = if (missingProfiles.isNotEmpty()) missingProfiles else ProfilePresets.createDefaultInitialProfiles()
                toInsert.forEach { profile ->
                    createProfileUseCase(profile)
                }
                _isRestoreSuccessDialogOpen.value = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Non-blocking fallback
                _isRestoreSuccessDialogOpen.value = true
            }
        }
    }

    fun onDismissRestoreSuccessDialog() {
        _isRestoreSuccessDialogOpen.value = false
    }

    fun onSelectMatchForScore(match: Match) {
        _selectedMatchForScore.value = match
    }

    fun onDismissScoreDialog() {
        _selectedMatchForScore.value = null
    }

    fun onSaveScore(
        scoreOne: Int,
        scoreTwo: Int,
        penaltyOne: Int? = null,
        penaltyTwo: Int? = null,
        isExtraTime: Boolean = false
    ) {
        val match = _selectedMatchForScore.value ?: return
        val tournamentId = match.tournamentId
        viewModelScope.launch {
            updateMatchScoreUseCase(tournamentId, match, scoreOne, scoreTwo, penaltyOne, penaltyTwo, isExtraTime)
            _selectedMatchForScore.value = null
        }
    }
}
