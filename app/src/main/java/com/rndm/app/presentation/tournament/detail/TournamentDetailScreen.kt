package com.rndm.app.presentation.tournament.detail

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.presentation.profile.detail.components.ProfileDetailSkeleton
import com.rndm.app.presentation.tournament.detail.components.ScoreInputDialog
import com.rndm.app.presentation.tournament.detail.components.TournamentCapsuleTabBar
import com.rndm.app.presentation.tournament.detail.components.TournamentKnockoutTab
import com.rndm.app.presentation.tournament.detail.components.TournamentMatchesTab
import com.rndm.app.presentation.tournament.detail.components.TournamentOverviewTab
import com.rndm.app.presentation.tournament.detail.components.TournamentStandingsTab
import com.rndm.app.presentation.tournament.share.ShareTournamentDialog
import com.rndm.app.presentation.tournament.share.SyncStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPromotion: (Long) -> Unit,
    onNavigateToBracket: (Long) -> Unit,
    onNavigateToDraw: (Long) -> Unit = {},
    onNavigateToPlayer: (String) -> Unit = {},
    viewModel: TournamentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tournament = uiState.tournament
    val context = LocalContext.current

    LaunchedEffect(uiState.publishErrorMessage) {
        uiState.publishErrorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.requestFeedbackMessage) {
        uiState.requestFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = tournament?.name ?: "تفاصيل البطولة",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (tournament != null) {
                        // My Requests Button (if remote tournament)
                        if (tournament.isRemote) {
                            val pendingCount = uiState.myRequests.count { it.status == com.rndm.app.domain.model.RequestStatus.PENDING }
                            IconButton(onClick = viewModel::onOpenMyRequestsSheet) {
                                BadgedBox(
                                    badge = {
                                        if (pendingCount > 0) {
                                            Badge { Text("$pendingCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ListAlt,
                                        contentDescription = "طلباتي في البطولة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val shareCode = uiState.activeShareCode ?: tournament.shareCode
                        if (tournament.isRemote && !shareCode.isNullOrBlank()) {
                            IconButton(onClick = viewModel::onOpenShareDialog) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة كود البطولة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (!tournament.isRemote) {
                            if (uiState.isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(onClick = viewModel::publishTournament) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "نشر ومزامنة سحابياً",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || tournament == null) {
            ProfileDetailSkeleton(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Viewer / Read-only status banner if user is viewing a remote tournament as guest
                if (tournament.isRemote && !uiState.canEditScores) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أنت في وضع المشاهدة (متابعة البث المباشر للنتائج)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Top 4-Tab Capsule Navigation Bar
                TournamentCapsuleTabBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::onTabSelected
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Content Area for the selected tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabContentAnimation"
                    ) { tab ->
                        when (tab) {
                            TournamentDetailTab.OVERVIEW -> {
                                TournamentOverviewTab(
                                    tournament = tournament,
                                    allMatches = uiState.allMatches,
                                    isPromotionReady = uiState.isPromotionReady,
                                    isKnockoutReady = uiState.isKnockoutReady,
                                    onNavigateToPromotion = { onNavigateToPromotion(tournament.id) },
                                    onGenerateKnockout = viewModel::generateDirectKnockout,
                                    onMatchClick = viewModel::onSelectMatchForScore,
                                    onPlayerClick = onNavigateToPlayer,
                                    onReorderMatchClick = viewModel::onOpenReorderMatchDialog,
                                    onSwapPlayerClick = viewModel::onOpenSwapPlayerDialog,
                                    onEditParticipant = viewModel::onOpenPlayerEditDialog,
                                    onAddPlayersClick = { viewModel.resumeDrawForTournament(onNavigateToDraw) }
                                )
                            }
                            TournamentDetailTab.MATCHES -> {
                                TournamentMatchesTab(
                                    matches = uiState.allMatches,
                                    onMatchClick = viewModel::onSelectMatchForScore,
                                    onPlayerClick = onNavigateToPlayer,
                                    onReorderMatchClick = viewModel::onOpenReorderMatchDialog,
                                    onSwapPlayerClick = viewModel::onOpenSwapPlayerDialog
                                )
                            }
                            TournamentDetailTab.STANDINGS -> {
                                TournamentStandingsTab(
                                    tournament = tournament,
                                    groups = tournament.groups,
                                    bestLosers = uiState.bestLosers,
                                    allMatches = uiState.allMatches,
                                    selectedGroupIndex = uiState.selectedGroupIndex,
                                    onGroupSelect = viewModel::onGroupSelected,
                                    onPlayerClick = onNavigateToPlayer
                                )
                            }
                            TournamentDetailTab.KNOCKOUT -> {
                                TournamentKnockoutTab(
                                    knockoutMatches = tournament.knockoutMatches,
                                    onMatchClick = viewModel::onSelectMatchForScore,
                                    onPlayerClick = onNavigateToPlayer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Score Dialog (for all authorized users or request submitters)
        if (uiState.canEditScores) {
            uiState.selectedMatchForScore?.let { match ->
                val isRequestMode = tournament?.isRemote == true && !uiState.isAdmin && tournament?.isHost != true
                ScoreInputDialog(
                    match = match,
                    isRequestMode = isRequestMode,
                    onDismiss = viewModel::onDismissScoreDialog,
                    onConfirm = { s1, s2, p1, p2 ->
                        viewModel.onSaveScore(s1, s2, p1, p2)
                    },
                    onConfirmRequest = { s1, s2, p1, p2, note ->
                        viewModel.onSaveScore(s1, s2, p1, p2, note)
                    }
                )
            }
        }

        // Player Edit Request Dialog
        uiState.editingParticipant?.let { participant ->
            com.rndm.app.presentation.tournament.detail.components.RequestPlayerEditDialog(
                participant = participant,
                onDismiss = viewModel::onDismissPlayerEditDialog,
                onSubmitRequest = viewModel::onRequestPlayerEdit
            )
        }

        // User Requests Sheet
        if (uiState.isMyRequestsSheetOpen) {
            com.rndm.app.presentation.tournament.detail.components.UserRequestsStatusSheet(
                requests = uiState.myRequests,
                onDismissRequest = viewModel::onDismissMyRequestsSheet
            )
        }

        // Share Dialog
        val resolvedShareCode = uiState.activeShareCode ?: tournament?.shareCode
        if (uiState.isShareDialogOpen && tournament != null && !resolvedShareCode.isNullOrBlank()) {
            ShareTournamentDialog(
                tournamentName = tournament.name,
                shareCode = resolvedShareCode,
                isUploading = uiState.isPublishing,
                isHost = tournament.isHost,
                isBroadcasting = uiState.isBroadcasting,
                isBroadcasted = uiState.isBroadcasted,
                onBroadcastToPublic = viewModel::onBroadcastToPublic,
                onDismissRequest = viewModel::onDismissShareDialog
            )
        }

        uiState.reorderingMatch?.let { currentMatch ->
            val otherMatches = remember(uiState.allMatches, currentMatch) {
                uiState.allMatches
                    .filter { m ->
                        m.id != currentMatch.id && (
                            if (currentMatch.groupIndex != null) {
                                m.stage == currentMatch.stage && m.groupIndex == currentMatch.groupIndex
                            } else {
                                m.stage == currentMatch.stage
                            }
                        )
                    }
                    .map { m ->
                        val title = if (m.groupIndex != null) {
                            "المجموعة ${('أ'.code + m.groupIndex).toChar()} • الجولة ${m.roundIndex}"
                        } else {
                            "${m.stage.displayName} • مباراة ${m.bracketMatchIndex}"
                        }
                        com.rndm.app.core.ui.components.ReorderMatchOption(
                            matchIdentifier = m.id,
                            matchNumberText = title,
                            playerOneName = m.playerOneName,
                            playerOneClub = m.playerOneClub,
                            playerTwoName = m.playerTwoName,
                            playerTwoClub = m.playerTwoClub
                        )
                    }
            }

            val currentTitle = if (currentMatch.groupIndex != null) {
                "المجموعة ${('أ'.code + currentMatch.groupIndex).toChar()} • الجولة ${currentMatch.roundIndex}"
            } else {
                "${currentMatch.stage.displayName} • مباراة ${currentMatch.bracketMatchIndex}"
            }

            com.rndm.app.core.ui.components.ReorderMatchDialog(
                currentMatch = com.rndm.app.core.ui.components.ReorderMatchOption(
                    matchIdentifier = currentMatch.id,
                    matchNumberText = currentTitle,
                    playerOneName = currentMatch.playerOneName,
                    playerOneClub = currentMatch.playerOneClub,
                    playerTwoName = currentMatch.playerTwoName,
                    playerTwoClub = currentMatch.playerTwoClub,
                    isCurrent = true
                ),
                otherMatches = otherMatches,
                onSelectMatchToSwap = { targetId ->
                    viewModel.onSwapMatchOrder(currentMatch.id, targetId as Long)
                },
                onMoveUp = { viewModel.onMoveMatchOrder(currentMatch, isUp = true) },
                onMoveDown = { viewModel.onMoveMatchOrder(currentMatch, isUp = false) },
                onDismiss = viewModel::onDismissReorderDialog
            )
        }

        uiState.swappingPlayerSlot?.let { (currentMatch, isSlotOne) ->
            val sourceName = if (isSlotOne) currentMatch.playerOneName else (currentMatch.playerTwoName ?: "BYE")
            val sourceClub = if (isSlotOne) currentMatch.playerOneClub else currentMatch.playerTwoClub
            val currentTitle = if (currentMatch.groupIndex != null) {
                "المجموعة ${('أ'.code + currentMatch.groupIndex).toChar()} • الجولة ${currentMatch.roundIndex}"
            } else {
                "${currentMatch.stage.displayName} • مباراة ${currentMatch.bracketMatchIndex}"
            }

            val otherTournamentMatches = remember(uiState.allMatches, currentMatch.id) {
                uiState.allMatches.filter { it.id != currentMatch.id }
            }

            val candidates = remember(otherTournamentMatches, currentMatch, isSlotOne) {
                val list = mutableListOf<com.rndm.app.core.ui.components.SwapPlayerCandidate>()

                // Opponent in same match
                if (isSlotOne && currentMatch.playerTwoName != null) {
                    list.add(
                        com.rndm.app.core.ui.components.SwapPlayerCandidate(
                            matchIdentifier = currentMatch.id,
                            matchTitle = currentTitle,
                            isSlotOne = false,
                            playerName = currentMatch.playerTwoName!!,
                            playerClub = currentMatch.playerTwoClub,
                            isSameMatchOpponent = true
                        )
                    )
                } else if (!isSlotOne) {
                    list.add(
                        com.rndm.app.core.ui.components.SwapPlayerCandidate(
                            matchIdentifier = currentMatch.id,
                            matchTitle = currentTitle,
                            isSlotOne = true,
                            playerName = currentMatch.playerOneName,
                            playerClub = currentMatch.playerOneClub,
                            isSameMatchOpponent = true
                        )
                    )
                }

                // Players in other matches of same stage / tournament
                otherTournamentMatches.forEach { m ->
                    val mTitle = if (m.groupIndex != null) {
                        "المجموعة ${('أ'.code + m.groupIndex).toChar()} • الجولة ${m.roundIndex}"
                    } else {
                        "${m.stage.displayName} • مباراة ${m.bracketMatchIndex}"
                    }

                    list.add(
                        com.rndm.app.core.ui.components.SwapPlayerCandidate(
                            matchIdentifier = m.id,
                            matchTitle = mTitle,
                            isSlotOne = true,
                            playerName = m.playerOneName,
                            playerClub = m.playerOneClub
                        )
                    )
                    m.playerTwoName?.let { p2 ->
                        list.add(
                            com.rndm.app.core.ui.components.SwapPlayerCandidate(
                                matchIdentifier = m.id,
                                matchTitle = mTitle,
                                isSlotOne = false,
                                playerName = p2,
                                playerClub = m.playerTwoClub
                            )
                        )
                    }
                }
                list
            }

            com.rndm.app.core.ui.components.SwapPlayersDialog(
                sourcePlayerName = sourceName,
                sourcePlayerClub = sourceClub,
                sourceMatchTitle = currentTitle,
                candidates = candidates,
                onSelectCandidateToSwap = { candidate ->
                    viewModel.onConfirmSwapPlayers(candidate.matchIdentifier as Long, candidate.isSlotOne)
                },
                onDismiss = viewModel::onDismissSwapPlayerDialog
            )
        }
    }
}
