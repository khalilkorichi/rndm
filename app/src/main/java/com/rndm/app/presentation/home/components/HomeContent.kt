package com.rndm.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.Match
import com.rndm.app.presentation.home.HomeUiState

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onNavigateToDrawSetup: (Long) -> Unit,
    onNavigateToDrawMode: (Long, DrawType) -> Unit,
    onNavigateToFreeWheelDraw: (Long) -> Unit = {},
    onNavigateToClubDuelDraw: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onRestoreDefaultProfilesClick: () -> Unit = {},
    onNavigateToProfiles: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onNavigateToCreateTournament: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTournamentDetail: (Long) -> Unit,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Active Tournament Live Matches Box (Only when an active tournament exists)
        if (uiState.activeTournament != null && uiState.activeTournamentMatches.isNotEmpty()) {
            item(key = "active_tournament_matches") {
                ActiveTournamentMatchesCard(
                    tournament = uiState.activeTournament,
                    matches = uiState.activeTournamentMatches,
                    currentMatchIndex = uiState.currentMatchIndex,
                    onMatchClick = onMatchClick,
                    onNavigateToTournamentDetail = onNavigateToTournamentDetail
                )
            }
        }

        // 2. Unified Hero Draw Hub (Active Profile + Instant Draw CTA + Quick Direct Modes)
        item(key = "unified_draw_hub") {
            UnifiedDrawHubCard(
                profile = uiState.recentProfile,
                onStartDrawClick = onNavigateToDrawSetup,
                onNavigateToDrawMode = onNavigateToDrawMode,
                onNavigateToFreeWheelDraw = onNavigateToFreeWheelDraw,
                onNavigateToClubDuelDraw = onNavigateToClubDuelDraw,
                onCreateProfileClick = onNavigateToCreateProfile,
                onRestoreDefaultProfilesClick = onRestoreDefaultProfilesClick,
                onNavigateToProfiles = onNavigateToProfiles
            )
        }

        // 3. Consolidated Tournaments & Stats Hub (Create tournament, Archive, Counts, Champion)
        item(key = "tournaments_stats_hub") {
            HomeStatsOverviewCard(
                totalProfilesCount = uiState.totalProfilesCount,
                activeTournamentsCount = uiState.activeTournamentsCount,
                completedTournamentsCount = uiState.completedTournamentsCount,
                recentChampionTournament = uiState.recentChampionTournament,
                onNavigateToTournaments = onNavigateToTournaments,
                onNavigateToCreateTournament = onNavigateToCreateTournament,
                onNavigateToArchive = onNavigateToArchive,
                onNavigateToProfiles = onNavigateToProfiles
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
