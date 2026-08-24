package com.rndm.app.presentation.profile.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.core.util.Constants
import com.rndm.app.presentation.profile.player.components.EditPlayerProfileBottomSheet
import com.rndm.app.presentation.profile.player.components.PlayerHeadToHeadTab
import com.rndm.app.presentation.profile.player.components.PlayerHeroHeader
import com.rndm.app.presentation.profile.player.components.PlayerMatchesTab
import com.rndm.app.presentation.profile.player.components.PlayerOverviewTab
import com.rndm.app.presentation.profile.player.components.PlayerTournamentsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerName: String,
    onNavigateBack: () -> Unit,
    onNavigateToTournament: (Long) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: PlayerProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(playerName) {
        viewModel.initializeWithPlayerName(playerName)
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "بروفايل اللاعب",
                onNavigateBack = onNavigateBack,
                actions = {
                    RndmTopBarAction(
                        onClick = viewModel::onOpenEditSheet,
                        icon = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "تعديل البروفايل",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Crossfade(
            targetState = uiState.isLoading,
            animationSpec = tween(Constants.CROSSFADE_DURATION_MS),
            label = "player_profile_crossfade"
        ) { isLoading ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                ) {
                    // 1. Hero Header
                    item {
                        PlayerHeroHeader(
                            stats = uiState.stats,
                            onEditClick = viewModel::onOpenEditSheet
                        )
                    }

                    // 2. Capsule Tab Selection Bar
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = PlayerProfileTab.entries,
                                key = { it.name }
                            ) { tab ->
                                val isSelected = uiState.selectedTab == tab
                                Surface(
                                    onClick = { viewModel.onTabSelect(tab) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Tab Content
                    item {
                        when (uiState.selectedTab) {
                            PlayerProfileTab.OVERVIEW -> {
                                PlayerOverviewTab(stats = uiState.stats)
                            }
                            PlayerProfileTab.TOURNAMENTS -> {
                                PlayerTournamentsTab(
                                    tournaments = uiState.tournamentHistory,
                                    onTournamentClick = onNavigateToTournament
                                )
                            }
                            PlayerProfileTab.MATCHES -> {
                                PlayerMatchesTab(
                                    playerName = uiState.playerName,
                                    matches = uiState.matchHistory,
                                    onTournamentClick = onNavigateToTournament
                                )
                            }
                            PlayerProfileTab.HEAD_TO_HEAD -> {
                                PlayerHeadToHeadTab(
                                    headToHeadList = uiState.headToHead,
                                    onOpponentClick = onNavigateToPlayer
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isEditSheetOpen) {
            EditPlayerProfileBottomSheet(
                sheetState = editSheetState,
                stats = uiState.stats,
                onDismiss = viewModel::onDismissEditSheet,
                onSave = viewModel::onSaveCustomProfile
            )
        }
    }
}
