package com.rndm.app.presentation.draw.duel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.LocalExtendedColors
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.presentation.draw.duel.components.AssignToTournamentBottomSheet
import com.rndm.app.presentation.draw.duel.components.ContestedClubHeroCard
import com.rndm.app.presentation.draw.duel.components.DuelContestantsSelector
import com.rndm.app.presentation.draw.duel.components.DuelResultDialog
import com.rndm.app.presentation.draw.wheel.components.WheelArrowIndicator
import com.rndm.app.presentation.draw.wheel.components.WheelCanvas
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDuelDrawScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClubDuelDrawViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rotationAnim = remember { Animatable(0f) }
    val extendedColors = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current
    val spacing = RndmThemeTokens.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast/Snackbar notifications
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearMessages()
        }
    }

    // Wheel spin trigger & animation
    LaunchedEffect(uiState.spinTrigger) {
        if (uiState.spinTrigger > 0L && uiState.targetRotation > 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            rotationAnim.snapTo(0f)
            rotationAnim.animateTo(
                targetValue = uiState.targetRotation,
                animationSpec = tween(
                    durationMillis = Constants.WHEEL_SPIN_DURATION_MS.toInt(),
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.2f, 1f)
                )
            )
            delay(400)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onSpinComplete()
            rotationAnim.snapTo(0f)
        }
    }

    // Map contestants list to ProfileItem for WheelCanvas
    val wheelItems = remember(uiState.contestants) {
        uiState.contestants.mapIndexed { index, name ->
            ProfileItem(id = index.toLong(), profileId = 0L, label = name, order = index)
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "قرعة حسم الأندية",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Contested Club Hero Card
            ContestedClubHeroCard(
                contestedClub = uiState.contestedClub,
                availableClubs = uiState.availableClubs,
                onClubChange = { viewModel.onContestedClubChange(it) }
            )

            // 2. Contestants Lineup Selector (VS)
            DuelContestantsSelector(
                contestants = uiState.contestants,
                availablePlayers = uiState.availablePlayers,
                onAddContestant = { viewModel.onAddContestant(it) },
                onRemoveContestant = { viewModel.onRemoveContestant(it) }
            )

            // 3. Interactive Decider Wheel Box
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (wheelItems.isNotEmpty()) {
                    WheelCanvas(
                        items = wheelItems,
                        rotation = rotationAnim.value,
                        extendedColors = extendedColors,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Arrow Indicator
                WheelArrowIndicator(
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Center Hub Circle
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_swords),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 4. Spin Decider Button
            RndmButton(
                onClick = { viewModel.onSpinWheel() },
                type = RndmButtonType.PRIMARY,
                enabled = uiState.canSpin,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wheel),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (uiState.isSpinning) "جاري حسم النزاع..." else "تدوير عجلة الحسم",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // 5. Duel Result Dialog
    if (uiState.isResultDialogOpen && uiState.winnerName != null) {
        DuelResultDialog(
            winnerName = uiState.winnerName ?: "",
            contestedClub = uiState.contestedClub,
            onDismiss = { viewModel.dismissResultDialog() },
            onOpenAssignToTournament = { viewModel.onOpenAssignToTournament() }
        )
    }

    // 6. Assign To Active Tournament Bottom Sheet
    if (uiState.isAssignToTournamentOpen) {
        AssignToTournamentBottomSheet(
            winnerName = uiState.winnerName ?: "",
            contestedClub = uiState.contestedClub,
            activeTournaments = uiState.activeTournaments,
            selectedTournament = uiState.selectedTournament,
            participants = uiState.tournamentParticipants,
            onSelectTournament = { viewModel.onSelectTournament(it) },
            onConfirmAssign = { tournamentId, targetPlayerName ->
                viewModel.onAssignWinnerToTournament(tournamentId, targetPlayerName)
            },
            onDismiss = { viewModel.onCloseAssignToTournament() }
        )
    }
}
