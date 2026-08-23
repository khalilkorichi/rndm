package com.rndm.app.presentation.draw.duel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.repository.TournamentRepository
import com.rndm.app.domain.usecase.draw.PerformClubDuelDrawUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.tournament.AssignDuelWinnerToTournamentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClubDuelDrawViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val tournamentRepository: TournamentRepository,
    private val performClubDuelDrawUseCase: PerformClubDuelDrawUseCase,
    private val assignDuelWinnerToTournamentUseCase: AssignDuelWinnerToTournamentUseCase,
    private val randomProvider: RandomProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialClubName: String = savedStateHandle.get<String>("targetClub") ?: ""

    private val _uiState = MutableStateFlow(
        ClubDuelDrawUiState(
            isLoading = true,
            contestedClub = initialClubName.ifBlank { "ريال مدريد" }
        )
    )
    val uiState: StateFlow<ClubDuelDrawUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
        observeActiveTournaments()
    }

    private fun observeProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                val players = profiles.filter { it.type == ProfileType.PLAYERS }
                val clubs = profiles.filter { it.type == ProfileType.CLUBS }

                _uiState.update { current ->
                    val selClubProfile = current.selectedClubsProfile ?: clubs.firstOrNull()
                    val selPlayerProfile = current.selectedPlayersProfile ?: players.firstOrNull()

                    val currentContestants = if (current.contestants.isEmpty() && selPlayerProfile != null && selPlayerProfile.items.size >= 2) {
                        listOf(selPlayerProfile.items[0].label, selPlayerProfile.items[1].label)
                    } else {
                        current.contestants
                    }

                    current.copy(
                        isLoading = false,
                        clubsProfiles = clubs,
                        playersProfiles = players,
                        selectedClubsProfile = selClubProfile,
                        selectedPlayersProfile = selPlayerProfile,
                        availableClubs = selClubProfile?.items ?: emptyList(),
                        availablePlayers = selPlayerProfile?.items ?: emptyList(),
                        contestants = currentContestants
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeActiveTournaments() {
        tournamentRepository.getActiveTournaments()
            .onEach { tournaments ->
                _uiState.update { current ->
                    val selected = current.selectedTournament ?: tournaments.firstOrNull()
                    current.copy(
                        activeTournaments = tournaments,
                        selectedTournament = selected
                    )
                }
                val currentSelected = _uiState.value.selectedTournament
                if (currentSelected != null) {
                    loadParticipants(currentSelected.id)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSelectTournament(tournament: Tournament) {
        _uiState.update { it.copy(selectedTournament = tournament) }
        loadParticipants(tournament.id)
    }

    private fun loadParticipants(tournamentId: Long) {
        tournamentRepository.getParticipants(tournamentId)
            .onEach { participants ->
                _uiState.update { it.copy(tournamentParticipants = participants) }
            }
            .launchIn(viewModelScope)
    }

    fun onContestedClubChange(newClub: String) {
        if (newClub.isBlank()) return
        _uiState.update { it.copy(contestedClub = newClub.trim()) }
    }

    fun onSelectClubProfile(profile: Profile) {
        _uiState.update {
            it.copy(
                selectedClubsProfile = profile,
                availableClubs = profile.items
            )
        }
    }

    fun onSelectPlayersProfile(profile: Profile) {
        _uiState.update {
            it.copy(
                selectedPlayersProfile = profile,
                availablePlayers = profile.items
            )
        }
    }

    fun onAddContestant(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update { current ->
            if (current.contestants.any { it.equals(trimmed, ignoreCase = true) }) {
                current.copy(errorMessage = "الاسم [$trimmed] موجود بالفعل بين المتنافسين")
            } else {
                current.copy(
                    contestants = current.contestants + trimmed,
                    errorMessage = null
                )
            }
        }
    }

    fun onRemoveContestant(index: Int) {
        _uiState.update { current ->
            if (current.contestants.size <= 2) {
                current.copy(errorMessage = "يجب أن يتوفر متنافسان على الأقل لإجراء قرعة الحسم")
            } else {
                val updated = current.contestants.toMutableList().apply { removeAt(index) }
                current.copy(contestants = updated, errorMessage = null)
            }
        }
    }

    fun onSpinWheel() {
        val state = _uiState.value
        if (!state.canSpin) return

        val itemCount = state.contestants.size
        val winningIndex = randomProvider.nextInt(itemCount)
        val segmentAngle = 360f / itemCount
        // Center of winning segment calculation matching Wheel physics
        val segmentCenterAngle = (winningIndex * segmentAngle) + (segmentAngle / 2f)
        val fullSpins = randomProvider.nextInt(5, 9) * 360f
        val calculatedTargetRotation = fullSpins + (360f - segmentCenterAngle)

        _uiState.update {
            it.copy(
                isSpinning = true,
                selectedIndex = winningIndex,
                targetRotation = calculatedTargetRotation,
                spinTrigger = System.currentTimeMillis(),
                winnerName = null,
                errorMessage = null
            )
        }
    }

    fun onSpinComplete() {
        val state = _uiState.value
        val winner = if (state.selectedIndex in state.contestants.indices) {
            state.contestants[state.selectedIndex]
        } else {
            state.contestants.firstOrNull() ?: "الفائز"
        }

        viewModelScope.launch {
            performClubDuelDrawUseCase(
                contestedClub = state.contestedClub,
                contestants = state.contestants
            )
        }

        _uiState.update {
            it.copy(
                isSpinning = false,
                winnerName = winner,
                isResultDialogOpen = true
            )
        }
    }

    fun onOpenAssignToTournament() {
        _uiState.update { it.copy(isAssignToTournamentOpen = true) }
    }

    fun onCloseAssignToTournament() {
        _uiState.update { it.copy(isAssignToTournamentOpen = false) }
    }

    fun onAssignWinnerToTournament(tournamentId: Long, targetPlayerName: String) {
        val state = _uiState.value
        val winner = state.winnerName ?: return
        val club = state.contestedClub

        viewModelScope.launch {
            try {
                assignDuelWinnerToTournamentUseCase(
                    tournamentId = tournamentId,
                    targetPlayerName = targetPlayerName,
                    winnerPlayerName = winner,
                    contestedClub = club
                )
                _uiState.update {
                    it.copy(
                        isAssignToTournamentOpen = false,
                        successMessage = "تم إسناد نادِ [$club] للفائز [$winner] في البطولة بنجاح!"
                    )
                }
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                _uiState.update {
                    it.copy(errorMessage = "حدث خطأ أثناء التعيين: ${e.localizedMessage}")
                }
            }
        }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(isResultDialogOpen = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
