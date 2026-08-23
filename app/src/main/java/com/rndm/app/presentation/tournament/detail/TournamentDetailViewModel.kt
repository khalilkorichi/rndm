package com.rndm.app.presentation.tournament.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.usecase.tournament.DeterminePromotionCandidatesUseCase
import com.rndm.app.domain.usecase.tournament.EvaluateBestLosersUseCase
import com.rndm.app.domain.usecase.tournament.GenerateKnockoutBracketUseCase
import com.rndm.app.domain.usecase.tournament.GetTournamentDetailUseCase
import com.rndm.app.domain.usecase.tournament.ReplacePlayerInTournamentUseCase
import com.rndm.app.domain.usecase.tournament.UpdateMatchScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTournamentDetailUseCase: GetTournamentDetailUseCase,
    private val updateMatchScoreUseCase: UpdateMatchScoreUseCase,
    private val determinePromotionCandidatesUseCase: DeterminePromotionCandidatesUseCase,
    private val generateKnockoutBracketUseCase: GenerateKnockoutBracketUseCase,
    private val evaluateBestLosersUseCase: EvaluateBestLosersUseCase,
    private val replacePlayerInTournamentUseCase: ReplacePlayerInTournamentUseCase,
    private val drawFixtureRepository: DrawFixtureRepository
) : ViewModel() {

    private val tournamentId: Long = checkNotNull(savedStateHandle["tournamentId"])
    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init {
        loadTournament()
    }

    private fun loadTournament() {
        viewModelScope.launch {
            getTournamentDetailUseCase(tournamentId).collect { tournament ->
                if (tournament != null) {
                    val allMatches = tournament.groups.flatMap { it.matches } + tournament.knockoutMatches

                    val allGroupMatchesFinished = tournament.groups.isNotEmpty() && tournament.groups.all { group ->
                        group.matches.isNotEmpty() && group.matches.all { it.status == MatchStatus.FINISHED }
                    }

                    val isPromotionOrKnockout = if (allGroupMatchesFinished && tournament.stage == TournamentStage.GROUPS) {
                        val decision = determinePromotionCandidatesUseCase(tournament.groups, tournament.qualifiersPerGroup)
                        Pair(decision.isTieBreakNeeded || decision.promotedCandidates.isNotEmpty(), !decision.isTieBreakNeeded && decision.promotedCandidates.isEmpty())
                    } else {
                        Pair(false, false)
                    }

                    // Evaluate Best Losers from initial knockout round (e.g. QF matches where player is not lucky loser, or R16 matches)
                    val initialKnockoutMatches = tournament.knockoutMatches.filter {
                        (it.stage == MatchStage.QUARTER_FINALS && !it.isPlayerTwoLuckyLoser) ||
                                (it.stage == MatchStage.ROUND_OF_16 && !it.isPlayerTwoLuckyLoser)
                    }
                    val bestLosers = evaluateBestLosersUseCase(initialKnockoutMatches)

                    _uiState.update {
                        it.copy(
                            tournament = tournament,
                            allMatches = allMatches,
                            bestLosers = bestLosers,
                            isLoading = false,
                            isPromotionReady = isPromotionOrKnockout.first,
                            isKnockoutReady = isPromotionOrKnockout.second
                        )
                    }
                }
            }
        }
    }

    fun onTabSelect(tab: TournamentDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onGroupSelect(index: Int) {
        _uiState.update { it.copy(selectedGroupIndex = index) }
    }

    fun onSelectMatchForScore(match: Match) {
        _uiState.update { it.copy(selectedMatchForScore = match) }
    }

    fun onDismissScoreDialog() {
        _uiState.update { it.copy(selectedMatchForScore = null) }
    }

    fun onSaveScore(scoreOne: Int, scoreTwo: Int, penaltyOne: Int? = null, penaltyTwo: Int? = null) {
        val match = _uiState.value.selectedMatchForScore ?: return
        viewModelScope.launch {
            updateMatchScoreUseCase(tournamentId, match, scoreOne, scoreTwo, penaltyOne, penaltyTwo)
            _uiState.update { it.copy(selectedMatchForScore = null) }
        }
    }

    fun generateDirectKnockout() {
        val tournament = _uiState.value.tournament ?: return
        val qualifiers = tournament.groups.flatMap { g -> g.standings.filter { it.isQualified }.map { it.participant } }
        viewModelScope.launch {
            generateKnockoutBracketUseCase(tournamentId, qualifiers)
            _uiState.update { it.copy(selectedTab = TournamentDetailTab.KNOCKOUT) }
        }
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
        viewModelScope.launch {
            replacePlayerInTournamentUseCase(
                tournamentId = tournamentId,
                oldPlayerName = oldPlayerName,
                newPlayerName = newPlayerName,
                newClubName = newClubName
            )
            _uiState.update {
                it.copy(
                    playerToReplace = null,
                    playerToReplaceClub = null
                )
            }
        }
    }

    fun resumeDrawForTournament(onNavigateToDraw: (profileId: Long) -> Unit) {
        val tournament = _uiState.value.tournament ?: return
        drawFixtureRepository.loadTournamentFixtures(tournament.id)
        onNavigateToDraw(tournament.playersProfileId)
    }
}
