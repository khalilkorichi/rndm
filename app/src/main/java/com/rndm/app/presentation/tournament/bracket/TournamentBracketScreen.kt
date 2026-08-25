package com.rndm.app.presentation.tournament.bracket

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.presentation.profile.detail.components.ProfileDetailSkeleton
import com.rndm.app.presentation.tournament.bracket.components.GoogleKnockoutBracketView
import com.rndm.app.presentation.tournament.detail.components.ScoreInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentBracketScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    viewModel: TournamentBracketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tournament = uiState.tournament
    val context = LocalContext.current

    LaunchedEffect(uiState.requestFeedbackMessage) {
        uiState.requestFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = tournament?.name ?: "شجرة الأدوار الإقصائية",
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                GoogleKnockoutBracketView(
                    matches = uiState.knockoutMatches,
                    onMatchClick = viewModel::onSelectMatchForScore,
                    onPlayerClick = onNavigateToPlayer
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        uiState.selectedMatchForScore?.let { match ->
            ScoreInputDialog(
                match = match,
                isRequestMode = uiState.isRequestMode,
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
}
