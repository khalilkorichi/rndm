package com.rndm.app.presentation.tournament.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.RequestType
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.TournamentRepository
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.request.ObserveAdminRequestsUseCase
import com.rndm.app.domain.usecase.request.SubmitTournamentRequestUseCase
import com.rndm.app.domain.usecase.sync.ObserveRemoteTournamentUseCase
import com.rndm.app.domain.usecase.sync.PublishTournamentUseCase
import com.rndm.app.domain.usecase.tournament.DeterminePromotionCandidatesUseCase
import com.rndm.app.domain.usecase.tournament.EvaluateBestLosersUseCase
import com.rndm.app.domain.usecase.tournament.GenerateKnockoutBracketUseCase
import com.rndm.app.domain.usecase.tournament.GetTournamentDetailUseCase
import com.rndm.app.domain.usecase.tournament.UpdateMatchScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val tournamentRepository: TournamentRepository,
    private val drawFixtureRepository: DrawFixtureRepository,
    private val publishTournamentUseCase: PublishTournamentUseCase,
    private val observeRemoteTournamentUseCase: ObserveRemoteTournamentUseCase,
    private val getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase,
    private val submitTournamentRequestUseCase: SubmitTournamentRequestUseCase,
    private val observeAdminRequestsUseCase: ObserveAdminRequestsUseCase
) : ViewModel() {

    private val tournamentId: Long = checkNotNull(savedStateHandle["tournamentId"])
    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init {
        observeUserRole()
        loadTournament()
        startRemoteSyncListener()
        observeRequests()
    }

    private fun observeUserRole() {
        viewModelScope.launch {
            getCurrentUserRoleUseCase().collect { role ->
                _uiState.update { it.copy(userRole = role) }
            }
        }
    }

    private fun startRemoteSyncListener() {
        viewModelScope.launch {
            observeRemoteTournamentUseCase(tournamentId)
                .catch { /* Silently handle offline/disconnected state */ }
                .collect { /* Room updates will automatically trigger loadTournament flow */ }
        }
    }

    private fun observeRequests() {
        viewModelScope.launch {
            observeAdminRequestsUseCase().collect { allRequests ->
                val tournamentRemoteId = _uiState.value.tournament?.remoteId
                val filtered = allRequests.filter { req ->
                    req.tournamentId == tournamentRemoteId || req.tournamentId == tournamentId.toString()
                }
                _uiState.update { it.copy(myRequests = filtered) }
            }
        }
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

                    val bestLosers = if (tournament.stage == TournamentStage.GROUPS) {
                        evaluateBestLosersUseCase(allMatches)
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            tournament = tournament,
                            allMatches = allMatches,
                            isLoading = false,
                            isPromotionReady = isPromotionOrKnockout.first,
                            isKnockoutReady = isPromotionOrKnockout.second,
                            bestLosers = bestLosers,
                            activeShareCode = if (tournament.shareCode != null) tournament.shareCode else it.activeShareCode
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTabSelected(tab: TournamentDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onGroupSelected(index: Int) {
        _uiState.update { it.copy(selectedGroupIndex = index) }
    }

    fun onSelectMatchForScore(match: Match) {
        _uiState.update { it.copy(selectedMatchForScore = match) }
    }

    fun onDismissScoreDialog() {
        _uiState.update { it.copy(selectedMatchForScore = null) }
    }

    fun onSaveScore(scoreOne: Int, scoreTwo: Int, penaltyOne: Int? = null, penaltyTwo: Int? = null, note: String = "") {
        val match = _uiState.value.selectedMatchForScore ?: return
        val tournament = _uiState.value.tournament ?: return
        val role = _uiState.value.userRole

        viewModelScope.launch {
            if (tournament.isRemote && role != UserRole.ADMIN && !tournament.isHost) {
                // Submit Request to Admin
                val noteSnippet = if (note.isNotBlank()) " | ملاحظة: $note" else ""
                val request = AdminRequest(
                    id = "",
                    type = RequestType.CHANGE_SCORE,
                    tournamentId = tournament.remoteId ?: tournamentId.toString(),
                    tournamentName = tournament.name,
                    requesterUid = "",
                    requesterName = "",
                    requesterEmail = "",
                    matchId = match.id,
                    remoteMatchId = match.remoteId,
                    scoreOne = scoreOne,
                    scoreTwo = scoreTwo,
                    penaltyScoreOne = penaltyOne,
                    penaltyScoreTwo = penaltyTwo,
                    playerOneName = match.playerOneName,
                    playerTwoName = match.playerTwoName,
                    description = "طلب تعديل نتيجة مباراة (${match.playerOneName} ضد ${match.playerTwoName}) إلى ($scoreOne - $scoreTwo)$noteSnippet"
                )
                val result = submitTournamentRequestUseCase(request)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            selectedMatchForScore = null,
                            requestFeedbackMessage = "تم إرسال طلب تعديل النتيجة للأدمن بنجاح 📨 بانتظار المراجعة والاعتماد"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            selectedMatchForScore = null,
                            requestFeedbackMessage = "تعذر إرسال الطلب، يرجى التأكد من اتصال الإنترنت"
                        )
                    }
                }
            } else {
                // Direct update (Local or Admin or Host)
                updateMatchScoreUseCase(tournamentId, match, scoreOne, scoreTwo, penaltyOne, penaltyTwo)
                _uiState.update { it.copy(selectedMatchForScore = null) }
            }
        }
    }

    fun publishTournament() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, publishErrorMessage = null) }
            val result = publishTournamentUseCase(tournamentId)
            if (result.isSuccess) {
                val published = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        activeShareCode = published.shareCode,
                        isShareDialogOpen = true
                    )
                }
            } else {
                val error = result.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        publishErrorMessage = error?.localizedMessage ?: "فشل نشر البطولة سحابياً، يرجى التحقق من اتصال الإنترنت"
                    )
                }
            }
        }
    }

    fun onOpenShareDialog() {
        val code = _uiState.value.activeShareCode ?: _uiState.value.tournament?.shareCode
        _uiState.update { it.copy(activeShareCode = code, isShareDialogOpen = true) }
        // Ensure tournament data and share code are fresh in the cloud
        viewModelScope.launch {
            publishTournamentUseCase(tournamentId)
        }
    }

    fun onDismissShareDialog() {
        _uiState.update { it.copy(isShareDialogOpen = false) }
    }

    fun generateDirectKnockout() {
        val tournament = _uiState.value.tournament ?: return
        val qualifiers = tournament.groups.flatMap { g -> g.standings.filter { it.isQualified }.map { it.participant } }
        viewModelScope.launch {
            generateKnockoutBracketUseCase(tournamentId, qualifiers)
            _uiState.update { it.copy(selectedTab = TournamentDetailTab.KNOCKOUT) }
        }
    }

    fun onOpenReorderMatchDialog(match: Match) {
        _uiState.update { it.copy(reorderingMatch = match) }
    }

    fun onDismissReorderDialog() {
        _uiState.update { it.copy(reorderingMatch = null) }
    }

    fun onSwapMatchOrder(matchId1: Long, matchId2: Long) {
        val tournament = _uiState.value.tournament ?: return
        val role = _uiState.value.userRole

        viewModelScope.launch {
            if (tournament.isRemote && role != UserRole.ADMIN && !tournament.isHost) {
                val m1 = _uiState.value.allMatches.firstOrNull { it.id == matchId1 }
                val m2 = _uiState.value.allMatches.firstOrNull { it.id == matchId2 }
                val request = AdminRequest(
                    id = "",
                    type = RequestType.SWAP_MATCH_ORDER,
                    tournamentId = tournament.remoteId ?: tournamentId.toString(),
                    tournamentName = tournament.name,
                    requesterUid = "",
                    requesterName = "",
                    requesterEmail = "",
                    matchId1 = matchId1,
                    matchId2 = matchId2,
                    matchOneDesc = "${m1?.playerOneName} ضد ${m1?.playerTwoName}",
                    matchTwoDesc = "${m2?.playerOneName} ضد ${m2?.playerTwoName}",
                    description = "طلب تبديل ترتيب المباراتين (${m1?.playerOneName} ضد ${m1?.playerTwoName}) مع (${m2?.playerOneName} ضد ${m2?.playerTwoName})"
                )
                submitTournamentRequestUseCase(request)
                _uiState.update {
                    it.copy(
                        reorderingMatch = null,
                        requestFeedbackMessage = "تم إرسال طلب تغيير ترتيب المباريات للأدمن للموافقة"
                    )
                }
            } else {
                tournamentRepository.swapMatchOrder(tournamentId, matchId1, matchId2)
                _uiState.update { it.copy(reorderingMatch = null) }
            }
        }
    }

    fun onMoveMatchOrder(match: Match, isUp: Boolean) {
        val matches = if (match.groupIndex != null) {
            _uiState.value.allMatches.filter { it.stage == match.stage && it.groupIndex == match.groupIndex }
        } else {
            _uiState.value.allMatches.filter { it.stage == match.stage }
        }.sortedBy { it.roundIndex }

        val currentIndex = matches.indexOfFirst { it.id == match.id }
        val targetIndex = if (isUp) currentIndex - 1 else currentIndex + 1
        if (targetIndex in matches.indices) {
            val targetMatch = matches[targetIndex]
            onSwapMatchOrder(match.id, targetMatch.id)
        }
    }

    fun onOpenSwapPlayerDialog(match: Match, isSlotOne: Boolean) {
        _uiState.update { it.copy(swappingPlayerSlot = Pair(match, isSlotOne)) }
    }

    fun onDismissSwapPlayerDialog() {
        _uiState.update { it.copy(swappingPlayerSlot = null) }
    }

    fun onConfirmSwapPlayers(targetMatchId: Long, isTargetSlotOne: Boolean) {
        val slot = _uiState.value.swappingPlayerSlot ?: return
        val currentMatch = slot.first
        val isCurrentSlotOne = slot.second
        val tournament = _uiState.value.tournament ?: return
        val role = _uiState.value.userRole

        viewModelScope.launch {
            if (tournament.isRemote && role != UserRole.ADMIN && !tournament.isHost) {
                val p1Name = if (isCurrentSlotOne) currentMatch.playerOneName else currentMatch.playerTwoName
                val targetMatch = _uiState.value.allMatches.firstOrNull { it.id == targetMatchId }
                val p2Name = if (isTargetSlotOne) targetMatch?.playerOneName else targetMatch?.playerTwoName
                val request = AdminRequest(
                    id = "",
                    type = RequestType.SWAP_PLAYERS,
                    tournamentId = tournament.remoteId ?: tournamentId.toString(),
                    tournamentName = tournament.name,
                    requesterUid = "",
                    requesterName = "",
                    requesterEmail = "",
                    matchId1 = currentMatch.id,
                    matchId2 = targetMatchId,
                    isSlot1A = isCurrentSlotOne,
                    isSlot1B = isTargetSlotOne,
                    playerOneName = p1Name,
                    playerTwoName = p2Name,
                    description = "طلب تبديل اللاعب ($p1Name) مع اللاعب ($p2Name)"
                )
                submitTournamentRequestUseCase(request)
                _uiState.update {
                    it.copy(
                        swappingPlayerSlot = null,
                        requestFeedbackMessage = "تم إرسال طلب تبديل اللاعبين للأدمن بنجاح"
                    )
                }
            } else {
                tournamentRepository.swapPlayersInMatches(
                    tournamentId = tournamentId,
                    matchId1 = currentMatch.id,
                    isSlot1A = isCurrentSlotOne,
                    matchId2 = targetMatchId,
                    isSlot1B = isTargetSlotOne
                )
                _uiState.update { it.copy(swappingPlayerSlot = null) }
            }
        }
    }

    fun onOpenPlayerEditDialog(participant: TournamentParticipant) {
        _uiState.update { it.copy(editingParticipant = participant) }
    }

    fun onDismissPlayerEditDialog() {
        _uiState.update { it.copy(editingParticipant = null) }
    }

    fun onRequestPlayerEdit(newName: String, newClub: String?, reason: String) {
        val participant = _uiState.value.editingParticipant ?: return
        val tournament = _uiState.value.tournament ?: return
        val role = _uiState.value.userRole

        viewModelScope.launch {
            if (tournament.isRemote && role != UserRole.ADMIN && !tournament.isHost) {
                val reasonSnippet = if (reason.isNotBlank()) " | السبب: $reason" else ""
                val request = AdminRequest(
                    id = "",
                    type = RequestType.PLAYER_REPLACE,
                    tournamentId = tournament.remoteId ?: tournamentId.toString(),
                    tournamentName = tournament.name,
                    requesterUid = "",
                    requesterName = "",
                    requesterEmail = "",
                    playerOneName = participant.playerName,
                    playerTwoName = newName,
                    playerTwoClub = newClub,
                    description = "طلب تعديل بيانات اللاعب (${participant.playerName}) إلى ($newName ${if (!newClub.isNullOrBlank()) "[$newClub]" else ""})$reasonSnippet"
                )
                val result = submitTournamentRequestUseCase(request)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            editingParticipant = null,
                            requestFeedbackMessage = "تم إرسال طلب تعديل بيانات اللاعب للأدمن بنجاح 📨"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            editingParticipant = null,
                            requestFeedbackMessage = "تعذر إرسال الطلب، يرجى التأكد من اتصال الإنترنت"
                        )
                    }
                }
            } else {
                // Direct local/admin update
                tournamentRepository.replaceParticipant(tournamentId, participant.playerName, newName, newClub)
                _uiState.update { it.copy(editingParticipant = null) }
            }
        }
    }

    fun onOpenMyRequestsSheet() {
        _uiState.update { it.copy(isMyRequestsSheetOpen = true) }
    }

    fun onDismissMyRequestsSheet() {
        _uiState.update { it.copy(isMyRequestsSheetOpen = false) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(requestFeedbackMessage = null) }
    }

    fun resumeDrawForTournament(onNavigateToDraw: (profileId: Long) -> Unit) {
        val tournament = _uiState.value.tournament ?: return
        drawFixtureRepository.loadTournamentFixtures(tournament.id)
        onNavigateToDraw(tournament.playersProfileId)
    }
}
