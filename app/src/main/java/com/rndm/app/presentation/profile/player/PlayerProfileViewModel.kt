package com.rndm.app.presentation.profile.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.usecase.player.GetPlayerCareerStatsUseCase
import com.rndm.app.domain.usecase.player.GetPlayerHeadToHeadUseCase
import com.rndm.app.domain.usecase.player.GetPlayerMatchHistoryUseCase
import com.rndm.app.domain.usecase.player.GetPlayerTournamentHistoryUseCase
import com.rndm.app.domain.usecase.player.SavePlayerCustomProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlayerCareerStatsUseCase: GetPlayerCareerStatsUseCase,
    private val getPlayerTournamentHistoryUseCase: GetPlayerTournamentHistoryUseCase,
    private val getPlayerMatchHistoryUseCase: GetPlayerMatchHistoryUseCase,
    private val getPlayerHeadToHeadUseCase: GetPlayerHeadToHeadUseCase,
    private val savePlayerCustomProfileUseCase: SavePlayerCustomProfileUseCase
) : ViewModel() {

    private val initialPlayerName: String = checkNotNull(savedStateHandle["playerName"])

    private val _uiState = MutableStateFlow(PlayerProfileUiState(playerName = initialPlayerName))
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    init {
        loadPlayerData(initialPlayerName)
    }

    fun initializeWithPlayerName(name: String) {
        if (_uiState.value.playerName != name) {
            _uiState.update { it.copy(playerName = name, isLoading = true) }
            loadPlayerData(name)
        }
    }

    private fun loadPlayerData(name: String) {
        viewModelScope.launch {
            combine(
                getPlayerCareerStatsUseCase(name),
                getPlayerTournamentHistoryUseCase(name),
                getPlayerMatchHistoryUseCase(name),
                getPlayerHeadToHeadUseCase(name)
            ) { stats, tournaments, matches, h2h ->
                _uiState.update { current ->
                    current.copy(
                        playerName = name,
                        stats = stats,
                        tournamentHistory = tournaments,
                        matchHistory = matches,
                        headToHead = h2h,
                        isLoading = false,
                        error = null
                    )
                }
            }.collect {}
        }
    }

    fun onTabSelect(tab: PlayerProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onOpenEditSheet() {
        _uiState.update { it.copy(isEditSheetOpen = true) }
    }

    fun onDismissEditSheet() {
        _uiState.update { it.copy(isEditSheetOpen = false) }
    }

    fun onSaveCustomProfile(
        nickname: String?,
        avatarIcon: String?,
        favoriteClub: String?,
        notes: String?
    ) {
        val playerName = _uiState.value.playerName
        viewModelScope.launch {
            savePlayerCustomProfileUseCase(
                name = playerName,
                nickname = nickname?.trim()?.ifBlank { null },
                avatarIcon = avatarIcon?.trim()?.ifBlank { null },
                favoriteClub = favoriteClub?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null }
            )
            _uiState.update { it.copy(isEditSheetOpen = false) }
        }
    }
}
