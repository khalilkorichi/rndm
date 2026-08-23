package com.rndm.app.presentation.tournament.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.presentation.tournament.create.components.GroupSettingsSection
import com.rndm.app.presentation.tournament.create.components.ProfileSelectSection
import com.rndm.app.presentation.tournament.create.components.TournamentHeaderCard
import com.rndm.app.presentation.tournament.create.components.TournamentNameInputSection
import com.rndm.app.presentation.tournament.create.components.TournamentStructurePreviewCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onNavigateBack: () -> Unit,
    onTournamentCreated: (Long) -> Unit,
    onNavigateToCreateProfile: (() -> Unit)? = null,
    viewModel: CreateTournamentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = RndmThemeTokens.spacing

    LaunchedEffect(uiState.isCreated) {
        uiState.isCreated?.let { id ->
            onTournamentCreated(id)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RndmTopAppBar(
                title = "إنشاء بطولة المجموعات",
                subtitle = "توزيع المجموعات والتصفيات",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                    ) {
                        RndmButton(
                            onClick = viewModel::createTournament,
                            enabled = uiState.canCreate,
                            type = RndmButtonType.PRIMARY,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جارٍ إجراء القرعة وتوليد البطولة...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tournament_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إنشاء البطولة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.playersProfiles.isEmpty()) {
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_profile_outlined),
                    title = "لا توجد بروفايلات لاعبين",
                    description = "لإنشاء بطولة بنظام المجموعات، يجب أولاً إنشاء بروفايل يحتوي على 3 لاعبين على الأقل",
                    actionText = "إنشاء بروفايل لاعبين جديد",
                    actionIcon = painterResource(id = R.drawable.ic_add),
                    onActionClick = onNavigateToCreateProfile,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Header Card
                    TournamentHeaderCard()

                    // 2. Tournament Name & Identity
                    TournamentNameInputSection(
                        name = uiState.name,
                        onNameChange = viewModel::onNameChange
                    )

                    // 3. Players & Clubs Selection
                    ProfileSelectSection(
                        playersProfiles = uiState.playersProfiles,
                        selectedPlayerProfileId = uiState.selectedPlayersProfileId,
                        onPlayerProfileSelect = viewModel::onPlayersProfileSelect,
                        clubsProfiles = uiState.clubsProfiles,
                        selectedClubProfileId = uiState.selectedClubsProfileId,
                        onClubProfileSelect = viewModel::onClubsProfileSelect,
                        isClubsLotteryEnabled = uiState.isClubsLotteryEnabled,
                        onToggleClubsLottery = viewModel::onToggleClubsLottery,
                        onNavigateToCreateProfile = onNavigateToCreateProfile
                    )

                    // 4. Groups & Qualifiers Configuration
                    GroupSettingsSection(
                        groupsCount = uiState.groupsCount,
                        onGroupsCountChange = viewModel::onGroupsCountChange,
                        qualifiersPerGroup = uiState.qualifiersPerGroup,
                        onQualifiersPerGroupChange = viewModel::onQualifiersPerGroupChange
                    )

                    // 5. Real-time Live Tournament Structure Simulation Preview
                    TournamentStructurePreviewCard(
                        groupsCount = uiState.groupsCount,
                        qualifiersPerGroup = uiState.qualifiersPerGroup,
                        totalPlayers = uiState.totalPlayers,
                        playersPerGroupBase = uiState.playersPerGroupBase,
                        extraPlayersCount = uiState.extraPlayersCount,
                        totalQualifiers = uiState.totalQualifiers,
                        knockoutStageName = uiState.knockoutStageName,
                        estimatedMatchesCount = uiState.estimatedGroupMatchesCount
                    )

                    // Extra space at bottom to ensure comfortable scrolling
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
