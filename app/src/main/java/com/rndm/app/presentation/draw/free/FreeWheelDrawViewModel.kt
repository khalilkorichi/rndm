package com.rndm.app.presentation.draw.free

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import com.rndm.app.presentation.draw.wheel.DrawCategory
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
class FreeWheelDrawViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val randomProvider: RandomProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialProfileId: Long = savedStateHandle.get<Long>("initialProfileId") ?: 0L

    private val _uiState = MutableStateFlow(FreeWheelDrawUiState(isLoading = true, isSpinning = false))
    val uiState: StateFlow<FreeWheelDrawUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                val players = profiles.filter { it.type == ProfileType.PLAYERS }
                val clubs = profiles.filter { it.type == ProfileType.CLUBS }
                val teams = profiles.filter { it.type == ProfileType.NATIONAL_TEAMS }

                _uiState.update { current ->
                    val selPlayer = current.selectedPlayersProfile ?: players.firstOrNull { it.id == initialProfileId } ?: players.firstOrNull()
                    val selClub = current.selectedClubsProfile ?: clubs.firstOrNull { it.id == initialProfileId } ?: clubs.firstOrNull()
                    val selTeam = current.selectedNationalTeamsProfile ?: teams.firstOrNull { it.id == initialProfileId } ?: teams.firstOrNull()

                    val remPlayers = if (current.remainingPlayers.isEmpty() && selPlayer != null) {
                        selPlayer.activeItems.ifEmpty { selPlayer.items }
                    } else current.remainingPlayers

                    val remClubs = if (current.remainingClubs.isEmpty() && selClub != null) {
                        selClub.activeItems.ifEmpty { selClub.items }
                    } else current.remainingClubs

                    val remTeams = if (current.remainingNationalTeams.isEmpty() && selTeam != null) {
                        selTeam.activeItems.ifEmpty { selTeam.items }
                    } else current.remainingNationalTeams

                    val exclPlayers = if (current.excludedPlayers.isEmpty() && selPlayer != null) selPlayer.excludedItems else current.excludedPlayers
                    val exclClubs = if (current.excludedClubs.isEmpty() && selClub != null) selClub.excludedItems else current.excludedClubs
                    val exclTeams = if (current.excludedNationalTeams.isEmpty() && selTeam != null) selTeam.excludedItems else current.excludedNationalTeams

                    val activeCategory = if (initialProfileId > 0L) {
                        when {
                            players.any { it.id == initialProfileId } -> DrawCategory.PLAYERS
                            clubs.any { it.id == initialProfileId } -> DrawCategory.CLUBS
                            teams.any { it.id == initialProfileId } -> DrawCategory.NATIONAL_TEAMS
                            else -> current.selectedCategory
                        }
                    } else current.selectedCategory

                    current.copy(
                        isLoading = false,
                        selectedCategory = activeCategory,
                        playersProfiles = players,
                        clubsProfiles = clubs,
                        nationalTeamsProfiles = teams,
                        selectedPlayersProfile = selPlayer,
                        selectedClubsProfile = selClub,
                        selectedNationalTeamsProfile = selTeam,
                        remainingPlayers = remPlayers,
                        remainingClubs = remClubs,
                        remainingNationalTeams = remTeams,
                        excludedPlayers = exclPlayers,
                        excludedClubs = exclClubs,
                        excludedNationalTeams = exclTeams
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCategorySelect(category: DrawCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                targetRotation = 0f,
                isSpinning = false,
                selectedIndex = -1
            )
        }
    }

    fun onSelectProfileForCategory(category: DrawCategory, profile: Profile) {
        val activeItems = profile.activeItems.ifEmpty { profile.items }
        val excludedItems = if (profile.activeItems.isEmpty()) emptyList() else profile.excludedItems

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    selectedPlayersProfile = profile,
                    remainingPlayers = activeItems,
                    excludedPlayers = excludedItems,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    selectedClubsProfile = profile,
                    remainingClubs = activeItems,
                    excludedClubs = excludedItems,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    selectedNationalTeamsProfile = profile,
                    remainingNationalTeams = activeItems,
                    excludedNationalTeams = excludedItems,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
            }
        }
    }

    fun onOpenSetupDialog() {
        _uiState.update { it.copy(isSetupDialogOpen = true) }
    }

    fun onDismissSetupDialog() {
        _uiState.update { it.copy(isSetupDialogOpen = false) }
    }

    fun onSaveAndApplyDialogChanges(
        category: DrawCategory,
        activeItems: List<ProfileItem>,
        excludedItems: List<ProfileItem>
    ) {
        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    selectedCategory = category,
                    remainingPlayers = activeItems,
                    excludedPlayers = excludedItems,
                    isSetupDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    selectedCategory = category,
                    remainingClubs = activeItems,
                    excludedClubs = excludedItems,
                    isSetupDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    selectedCategory = category,
                    remainingNationalTeams = activeItems,
                    excludedNationalTeams = excludedItems,
                    isSetupDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
            }
        }
    }

    fun startSpin() {
        val state = _uiState.value
        val items = state.currentWheelItems
        if (state.isSpinning || items.isEmpty()) return

        if (items.size == 1) {
            val winner = items.first()
            _uiState.update {
                it.copy(
                    winnerItem = winner,
                    isWinnerDialogOpen = true,
                    recentWinners = listOf(FreeWheelWinner(winner, it.selectedCategory)) + it.recentWinners
                )
            }
            return
        }

        val selectedIndex = randomProvider.nextInt(items.size)
        val sliceAngle = 360f / items.size
        val baseRotations = 360f * 6
        val targetAngle = baseRotations + (360f - (selectedIndex * sliceAngle + sliceAngle / 2f))

        _uiState.update {
            it.copy(
                isSpinning = true,
                selectedIndex = selectedIndex,
                targetRotation = targetAngle,
                spinTrigger = it.spinTrigger + 1
            )
        }
    }

    fun onSpinComplete() {
        val state = _uiState.value
        val items = state.currentWheelItems
        if (state.selectedIndex !in items.indices) {
            _uiState.update { it.copy(isSpinning = false, targetRotation = 0f) }
            return
        }

        val winner = items[state.selectedIndex]
        _uiState.update {
            it.copy(
                isSpinning = false,
                winnerItem = winner,
                isWinnerDialogOpen = true,
                recentWinners = listOf(FreeWheelWinner(winner, it.selectedCategory)) + it.recentWinners
            )
        }
    }

    fun onKeepWinnerAndSpinAgain() {
        _uiState.update {
            it.copy(
                isWinnerDialogOpen = false,
                targetRotation = 0f,
                selectedIndex = -1
            )
        }
    }

    fun onExcludeWinnerAndSpinAgain() {
        val state = _uiState.value
        val winner = state.winnerItem ?: return

        _uiState.update { current ->
            when (current.selectedCategory) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = current.remainingPlayers.filter { it.id != winner.id || it.label != winner.label },
                    excludedPlayers = (current.excludedPlayers + winner.copy(isActive = false)).distinctBy { it.label },
                    isWinnerDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = current.remainingClubs.filter { it.id != winner.id || it.label != winner.label },
                    excludedClubs = (current.excludedClubs + winner.copy(isActive = false)).distinctBy { it.label },
                    isWinnerDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = current.remainingNationalTeams.filter { it.id != winner.id || it.label != winner.label },
                    excludedNationalTeams = (current.excludedNationalTeams + winner.copy(isActive = false)).distinctBy { it.label },
                    isWinnerDialogOpen = false,
                    targetRotation = 0f,
                    selectedIndex = -1
                )
            }
        }
    }

    fun onDismissWinnerDialog() {
        _uiState.update { it.copy(isWinnerDialogOpen = false) }
    }

    fun clearRecentWinners() {
        _uiState.update { it.copy(recentWinners = emptyList()) }
    }

    fun resetCategoryItems(category: DrawCategory) {
        _uiState.update { current ->
            val profile = when (category) {
                DrawCategory.PLAYERS -> current.selectedPlayersProfile
                DrawCategory.CLUBS -> current.selectedClubsProfile
                DrawCategory.NATIONAL_TEAMS -> current.selectedNationalTeamsProfile
            }
            val defaultItems = profile?.items.orEmpty()

            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = defaultItems.map { it.copy(isActive = true) },
                    excludedPlayers = emptyList(),
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = defaultItems.map { it.copy(isActive = true) },
                    excludedClubs = emptyList(),
                    targetRotation = 0f,
                    selectedIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = defaultItems.map { it.copy(isActive = true) },
                    excludedNationalTeams = emptyList(),
                    targetRotation = 0f,
                    selectedIndex = -1
                )
            }
        }
    }
}
