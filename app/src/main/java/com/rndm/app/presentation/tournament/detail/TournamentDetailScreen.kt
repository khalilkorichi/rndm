package com.rndm.app.presentation.tournament.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.core.ui.components.ReplacePlayerDialog
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.presentation.profile.detail.components.ProfileDetailSkeleton
import com.rndm.app.presentation.tournament.detail.components.ScoreInputDialog
import com.rndm.app.presentation.tournament.detail.components.TournamentCapsuleTabBar
import com.rndm.app.presentation.tournament.detail.components.TournamentKnockoutTab
import com.rndm.app.presentation.tournament.detail.components.TournamentMatchesTab
import com.rndm.app.presentation.tournament.detail.components.TournamentOverviewTab
import com.rndm.app.presentation.tournament.detail.components.TournamentStandingsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPromotion: (Long) -> Unit,
    onNavigateToBracket: (Long) -> Unit,
    onNavigateToDraw: (Long) -> Unit = {},
    viewModel: TournamentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tournament = uiState.tournament

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = tournament?.name ?: "تفاصيل البطولة",
                onNavigateBack = onNavigateBack
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
                // Top 4-Tab Capsule Navigation Bar
                TournamentCapsuleTabBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::onTabSelect
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
                                    onReplacePlayerClick = viewModel::onRequestReplacePlayer,
                                    onAddPlayersClick = { viewModel.resumeDrawForTournament(onNavigateToDraw) }
                                )
                            }
                            TournamentDetailTab.MATCHES -> {
                                TournamentMatchesTab(
                                    matches = uiState.allMatches,
                                    onMatchClick = viewModel::onSelectMatchForScore,
                                    onReplacePlayerClick = viewModel::onRequestReplacePlayer
                                )
                            }
                            TournamentDetailTab.STANDINGS -> {
                                TournamentStandingsTab(
                                    tournament = tournament,
                                    groups = tournament.groups,
                                    bestLosers = uiState.bestLosers,
                                    allMatches = uiState.allMatches,
                                    selectedGroupIndex = uiState.selectedGroupIndex,
                                    onGroupSelect = viewModel::onGroupSelect
                                )
                            }
                            TournamentDetailTab.KNOCKOUT -> {
                                TournamentKnockoutTab(
                                    knockoutMatches = tournament.knockoutMatches,
                                    onMatchClick = viewModel::onSelectMatchForScore
                                )
                            }
                        }
                    }
                }
            }
        }

        uiState.selectedMatchForScore?.let { match ->
            ScoreInputDialog(
                match = match,
                onDismiss = viewModel::onDismissScoreDialog,
                onConfirm = { s1, s2, p1, p2 ->
                    viewModel.onSaveScore(s1, s2, p1, p2)
                }
            )
        }

        uiState.playerToReplace?.let { oldPlayerName ->
            ReplacePlayerDialog(
                oldPlayerName = oldPlayerName,
                initialClubName = uiState.playerToReplaceClub,
                onDismiss = viewModel::onDismissReplacePlayerDialog,
                onConfirm = viewModel::onConfirmReplacePlayer
            )
        }
    }
}
