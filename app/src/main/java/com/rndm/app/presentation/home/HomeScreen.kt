package com.rndm.app.presentation.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.DrawType
import com.rndm.app.presentation.home.components.HomeContent
import com.rndm.app.presentation.home.components.HomeSkeleton
import com.rndm.app.presentation.tournament.detail.components.ScoreInputDialog

@Composable
fun HomeScreen(
    onNavigateToDrawSetup: (Long) -> Unit,
    onNavigateToDrawMode: (Long, DrawType) -> Unit = { id, _ -> onNavigateToDrawSetup(id) },
    onNavigateToFreeWheelDraw: (Long) -> Unit = {},
    onNavigateToClubDuelDraw: () -> Unit = {},
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onNavigateToCreateTournament: () -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    onNavigateToTournamentDetail: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "RNDM",
                titleIcon = painterResource(id = R.drawable.ic_wheel)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Crossfade(
            targetState = uiState.isLoading,
            animationSpec = tween(Constants.CROSSFADE_DURATION_MS),
            label = "home_crossfade"
        ) { isLoading ->
            if (isLoading) {
                HomeSkeleton(modifier = Modifier.padding(padding))
            } else {
                HomeContent(
                    uiState = uiState,
                    onNavigateToDrawSetup = onNavigateToDrawSetup,
                    onNavigateToDrawMode = onNavigateToDrawMode,
                    onNavigateToFreeWheelDraw = onNavigateToFreeWheelDraw,
                    onNavigateToClubDuelDraw = onNavigateToClubDuelDraw,
                    onNavigateToCreateProfile = onNavigateToCreateProfile,
                    onNavigateToProfiles = onNavigateToProfiles,
                    onNavigateToTournaments = onNavigateToTournaments,
                    onNavigateToCreateTournament = onNavigateToCreateTournament,
                    onNavigateToArchive = onNavigateToArchive,
                    onNavigateToTournamentDetail = onNavigateToTournamentDetail,
                    onMatchClick = viewModel::onSelectMatchForScore,
                    modifier = Modifier.padding(padding)
                )
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
    }
}
