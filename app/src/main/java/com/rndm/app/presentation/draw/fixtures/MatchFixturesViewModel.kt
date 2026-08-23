package com.rndm.app.presentation.draw.fixtures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MatchFixturesViewModel @Inject constructor(
    private val drawFixtureRepository: DrawFixtureRepository,
    private val getAllProfilesUseCase: GetAllProfilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchFixturesUiState())
    val uiState: StateFlow<MatchFixturesUiState> = _uiState.asStateFlow()

    init {
        observeFixtures()
        observeProfiles()
    }

    private fun observeFixtures() {
        drawFixtureRepository.fixtures
            .onEach { fixtures ->
                _uiState.update { it.copy(fixtures = fixtures) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                val players = profiles.filter { it.type == ProfileType.PLAYERS }
                _uiState.update { it.copy(playersProfiles = players) }
            }
            .launchIn(viewModelScope)
    }

    fun onOpenAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = true) }
    }

    fun onDismissAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = false) }
    }

    fun onAddNewPlayers(names: List<String>) {
        if (names.isNotEmpty()) {
            drawFixtureRepository.queueNewPlayersForDraw(names)
        }
    }

    fun onEditScoreClick(fixture: DrawFixture) {
        _uiState.update {
            it.copy(
                editingFixture = fixture,
                inputScoreOne = fixture.scoreOne?.toString() ?: "",
                inputScoreTwo = fixture.scoreTwo?.toString() ?: ""
            )
        }
    }

    fun onDismissScoreDialog() {
        _uiState.update { it.copy(editingFixture = null) }
    }

    fun onScoreOneChange(score: String) {
        if (score.isEmpty() || score.all { it.isDigit() }) {
            _uiState.update { it.copy(inputScoreOne = score) }
        }
    }

    fun onScoreTwoChange(score: String) {
        if (score.isEmpty() || score.all { it.isDigit() }) {
            _uiState.update { it.copy(inputScoreTwo = score) }
        }
    }

    fun onSaveScore() {
        val fixture = _uiState.value.editingFixture ?: return
        val s1 = _uiState.value.inputScoreOne.toIntOrNull()
        val s2 = _uiState.value.inputScoreTwo.toIntOrNull()

        drawFixtureRepository.updateFixtureScore(fixture.id, s1, s2)
        _uiState.update { it.copy(editingFixture = null) }
    }

    fun onRequestReplacePlayer(playerName: String, clubName: String? = null) {
        _uiState.update {
            it.copy(
                playerToReplace = playerName,
                playerToReplaceClub = clubName
            )
        }
    }

    fun onDismissReplacePlayerDialog() {
        _uiState.update {
            it.copy(
                playerToReplace = null,
                playerToReplaceClub = null
            )
        }
    }

    fun onConfirmReplacePlayer(newPlayerName: String, newClubName: String?) {
        val oldPlayerName = _uiState.value.playerToReplace ?: return
        drawFixtureRepository.replacePlayer(oldPlayerName, newPlayerName, newClubName)
        _uiState.update {
            it.copy(
                playerToReplace = null,
                playerToReplaceClub = null
            )
        }
    }

    fun formatFixturesSummary(): String {
        val fixtures = _uiState.value.fixtures
        val sb = StringBuilder("جدول مباريات القرعة:\n\n")
        fixtures.forEach { f ->
            val p1 = f.playerOneName + (if (f.playerOneTeam != null) " (${f.playerOneTeam})" else "")
            val p2 = (f.playerTwoName ?: "TBD") + (if (f.playerTwoTeam != null) " (${f.playerTwoTeam})" else "")
            val score = if (f.isFinished) " [${f.scoreOne} - ${f.scoreTwo}]" else ""
            sb.append("- المباراة ${f.matchNumber}: $p1 ضد $p2$score\n")
        }
        return sb.toString()
    }
}
