package com.rndm.app.presentation.tournament.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.TournamentType
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.sync.PublishTournamentUseCase
import com.rndm.app.domain.usecase.tournament.CreateTournamentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTournamentViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val createTournamentUseCase: CreateTournamentUseCase,
    private val publishTournamentUseCase: PublishTournamentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTournamentUiState())
    val uiState: StateFlow<CreateTournamentUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            getAllProfilesUseCase().collect { profiles ->
                val players = profiles.filter { it.type == ProfileType.PLAYERS && it.items.isNotEmpty() }
                val clubs = profiles.filter { it.type == ProfileType.CLUBS && it.items.isNotEmpty() }
                _uiState.update { current ->
                    current.copy(
                        playersProfiles = players,
                        clubsProfiles = clubs,
                        selectedPlayersProfileId = current.selectedPlayersProfileId ?: players.firstOrNull()?.id,
                        selectedClubsProfileId = current.selectedClubsProfileId ?: clubs.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onTypeChange(type: TournamentType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onPlayersProfileSelect(profileId: Long) {
        _uiState.update { it.copy(selectedPlayersProfileId = profileId) }
    }

    fun onClubsProfileSelect(profileId: Long) {
        _uiState.update { it.copy(selectedClubsProfileId = profileId) }
    }

    fun onToggleClubsLottery(enabled: Boolean) {
        _uiState.update { it.copy(isClubsLotteryEnabled = enabled) }
    }

    fun onGroupsCountChange(count: Int) {
        if (count in 2..8) {
            _uiState.update { it.copy(groupsCount = count) }
        }
    }

    fun onQualifiersPerGroupChange(count: Int) {
        if (count in 1..4) {
            _uiState.update { it.copy(qualifiersPerGroup = count) }
        }
    }

    fun createTournament() {
        val state = _uiState.value
        val name = state.name.ifBlank { "بطولة RNDM" }
        val playerProfile = state.playersProfiles.firstOrNull { it.id == state.selectedPlayersProfileId }
        val clubProfile = if (state.isClubsLotteryEnabled) {
            state.clubsProfiles.firstOrNull { it.id == state.selectedClubsProfileId }
        } else null

        val activePlayers = playerProfile?.activeItems?.ifEmpty { playerProfile.items } ?: emptyList()
        if (playerProfile == null || activePlayers.size < 3) {
            _uiState.update { it.copy(errorMessage = "يجب اختيار بروفايل لاعبين يحتوي على 3 لاعبين نشطين على الأقل") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val tournamentId = createTournamentUseCase(
                    name = name,
                    type = state.type,
                    playersProfile = playerProfile,
                    clubsProfile = clubProfile,
                    groupsCount = state.groupsCount,
                    qualifiersPerGroup = state.qualifiersPerGroup
                )
                // Auto-publish in background to Cloud Firestore
                launch {
                    try {
                        publishTournamentUseCase(tournamentId)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
                _uiState.update { it.copy(isLoading = false, isCreated = tournamentId) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "حدث خطأ أثناء إنشاء البطولة") }
            }
        }
    }
}
