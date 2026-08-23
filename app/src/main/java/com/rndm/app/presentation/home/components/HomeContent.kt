package com.rndm.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    onNavigateToClubDuelDraw: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onNavigateToCreateTournament: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTournamentDetail: (Long) -> Unit,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing
    val activeProfileId = uiState.recentProfile?.id ?: 0L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Active Tournament Live Matches Box (if an active tournament exists)
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

        // 2. Quick Action Hub (5 Primary Actions)
        item(key = "quick_actions") {
            HomeQuickActionHub(
                onNavigateToQuickDraw = { onNavigateToDrawSetup(activeProfileId) },
                onNavigateToClubDuelDraw = onNavigateToClubDuelDraw,
                onNavigateToCreateTournament = onNavigateToCreateTournament,
                onNavigateToCreateProfile = onNavigateToCreateProfile,
                onNavigateToArchive = onNavigateToArchive
            )
        }

        // 3. Quick Draw Banner Card
        item(key = "quick_draw") {
            QuickDrawCard(
                profile = uiState.recentProfile,
                onStartDrawClick = onNavigateToDrawSetup,
                onCreateProfileClick = onNavigateToCreateProfile
            )
        }

        // 4. Draw Modes Interactive Grid (4 Modes)
        item(key = "draw_modes") {
            HomeDrawModesGrid(
                onSelectDrawMode = { drawType ->
                    onNavigateToDrawMode(activeProfileId, drawType)
                }
            )
        }

        // 5. Recent Profiles Carousel
        if (uiState.topProfiles.isNotEmpty()) {
            item(key = "recent_profiles") {
                RecentProfilesSection(
                    profiles = uiState.topProfiles,
                    onProfileClick = onNavigateToDrawSetup,
                    onViewAllProfilesClick = onNavigateToProfiles
                )
            }
        }

        // 6. Stats & Activity Overview
        item(key = "stats_overview") {
            HomeStatsOverviewCard(
                totalProfilesCount = uiState.totalProfilesCount,
                activeTournamentsCount = uiState.activeTournamentsCount,
                completedTournamentsCount = uiState.completedTournamentsCount,
                recentChampionTournament = uiState.recentChampionTournament,
                onNavigateToTournaments = onNavigateToTournaments,
                onNavigateToProfiles = onNavigateToProfiles
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
