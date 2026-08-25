package com.rndm.app.presentation.draw.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.AddPlayersToDrawDialog
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.ShimmerBox
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.presentation.draw.setup.components.CreateProfileGroupDialog
import com.rndm.app.presentation.draw.setup.components.DrawTypeSelectionSection
import com.rndm.app.presentation.draw.setup.components.EmptyProfilesWelcomeCard
import com.rndm.app.presentation.draw.setup.components.ManageProfileDrawItemsDialog
import com.rndm.app.presentation.draw.setup.components.ProfileSelectionBentoSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawSetupScreen(
    profileId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onStartDraw: (Long, DrawType) -> Unit,
    viewModel: DrawSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val spacing = RndmThemeTokens.spacing

    LaunchedEffect(profileId) {
        viewModel.initializeWithProfileId(profileId)
    }

    // Intercept back button when in Step 2 to return to Step 1 smoothly
    BackHandler(enabled = uiState.currentStep == DrawSetupStep.SELECT_DRAW_TYPE) {
        viewModel.onBackToStep1()
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = if (uiState.currentStep == DrawSetupStep.SELECT_PARTICIPANTS) {
                    "تجهيز القرعة (1/2)"
                } else {
                    "أسلوب القرعة (2/2)"
                },
                titleIcon = painterResource(
                    id = if (uiState.currentStep == DrawSetupStep.SELECT_PARTICIPANTS) {
                        R.drawable.ic_target
                    } else {
                        R.drawable.ic_wheel
                    }
                ),
                onNavigateBack = {
                    if (uiState.currentStep == DrawSetupStep.SELECT_DRAW_TYPE) {
                        viewModel.onBackToStep1()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        bottomBar = {
            // Floating Capsule Action Button
            AnimatedVisibility(
                visible = uiState.canProceedToStep2,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (uiState.currentStep == DrawSetupStep.SELECT_PARTICIPANTS) {
                                viewModel.onProceedToStep2()
                            } else {
                                onStartDraw(uiState.selectedProfileId, uiState.selectedDrawType)
                            }
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .shadow(
                                elevation = 20.dp,
                                shape = CircleShape,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                ambientColor = Color.Black.copy(alpha = 0.40f)
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (uiState.currentStep == DrawSetupStep.SELECT_PARTICIPANTS) {
                                Text(
                                    text = "المتابعة لاختيار أسلوب القرعة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        when (uiState.selectedDrawType) {
                                            DrawType.WHEEL -> R.drawable.ic_wheel
                                            DrawType.FLIP_CARDS -> R.drawable.ic_cards
                                            DrawType.SPIN_LIST -> R.drawable.ic_spinlist
                                            DrawType.ROUND_ROBIN -> R.drawable.ic_roundrobin
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "بدء القرعة الآن",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Crossfade(
            targetState = uiState.isLoading,
            animationSpec = tween(Constants.CROSSFADE_DURATION_MS),
            label = "draw_setup_loading_crossfade"
        ) { isLoading ->
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            } else if (uiState.profiles.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        EmptyProfilesWelcomeCard(
                            onNavigateToProfiles = onNavigateToCreateProfile,
                            onQuickAddPlayersClick = { viewModel.onOpenAddPlayersDialog() },
                            onRestoreDefaultPresets = { viewModel.onRestoreDefaultPresets() }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Step Stepper Indicator
                    DrawSetupStepperHeader(
                        currentStep = uiState.currentStep,
                        onStep1Click = {
                            if (uiState.currentStep == DrawSetupStep.SELECT_DRAW_TYPE) {
                                viewModel.onBackToStep1()
                            }
                        },
                        onStep2Click = {
                            if (uiState.canProceedToStep2) {
                                viewModel.onProceedToStep2()
                            }
                        },
                        canProceedToStep2 = uiState.canProceedToStep2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Multi-Step Animated Content
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = {
                            if (targetState == DrawSetupStep.SELECT_DRAW_TYPE) {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()) togetherWith
                                        (slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut())
                            } else {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()) togetherWith
                                        (slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut())
                            }
                        },
                        label = "draw_setup_step_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { step ->
                        when (step) {
                            DrawSetupStep.SELECT_PARTICIPANTS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        ProfileSelectionBentoSection(
                                            profiles = uiState.filteredProfiles,
                                            groups = uiState.groups,
                                            selectedProfile = uiState.selectedProfile,
                                            selectedGroupId = uiState.selectedGroupId,
                                            onProfileSelected = viewModel::onProfileSelected,
                                            onSelectGroup = viewModel::onSelectGroup,
                                            onManageProfileItems = viewModel::onOpenManageProfileItems,
                                            onCreateGroupClick = viewModel::onOpenCreateGroupDialog
                                        )

                                    }

                                    // Bottom Spacer so content scrolls comfortably above the floating capsule
                                    item {
                                        Spacer(modifier = Modifier.height(84.dp))
                                    }
                                }
                            }

                            DrawSetupStep.SELECT_DRAW_TYPE -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Summary Card of Selected Profile
                                    item {
                                        SelectedProfileSummaryCard(
                                            profile = uiState.selectedProfile,
                                            onChangeClick = { viewModel.onBackToStep1() },
                                            onCustomizeClick = {
                                                uiState.selectedProfile?.let { viewModel.onOpenManageProfileItems(it) }
                                            }
                                        )
                                    }

                                    // Draw Types Section
                                    item {
                                        DrawTypeSelectionSection(
                                            selectedDrawType = uiState.selectedDrawType,
                                            onDrawTypeSelected = viewModel::onDrawTypeSelected
                                        )
                                    }

                                    // Bottom Spacer so content scrolls comfortably above the floating capsule
                                    item {
                                        Spacer(modifier = Modifier.height(84.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manage/Customize Profile Items Dialog
        uiState.editingProfile?.let { prof ->
            ManageProfileDrawItemsDialog(
                profile = prof,
                onDismiss = viewModel::onDismissManageProfileItems,
                onSaveProfile = viewModel::onSaveEditedProfile
            )
        }

        // Create Group Dialog
        if (uiState.isCreateGroupDialogOpen) {
            CreateProfileGroupDialog(
                onDismiss = viewModel::onDismissCreateGroupDialog,
                onConfirm = viewModel::onCreateGroup
            )
        }

        // Quick Add Players Dialog (+)
        if (uiState.isAddPlayersDialogOpen) {
            AddPlayersToDrawDialog(
                existingPlayerNames = emptyList(),
                availableProfiles = uiState.profiles,
                onDismiss = viewModel::onDismissAddPlayersDialog,
                onConfirm = { names ->
                    viewModel.onConfirmQuickPlayers(names) { newProfileId, drawType ->
                        onStartDraw(newProfileId, drawType)
                    }
                }
            )
        }
    }
}

@Composable
private fun DrawSetupStepperHeader(
    currentStep: DrawSetupStep,
    onStep1Click: () -> Unit,
    onStep2Click: () -> Unit,
    canProceedToStep2: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step 1 Chip
        val isStep1Active = currentStep == DrawSetupStep.SELECT_PARTICIPANTS
        Surface(
            onClick = onStep1Click,
            shape = RoundedCornerShape(10.dp),
            color = if (isStep1Active) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isStep1Active) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isStep1Active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "تجهيز المشاركين",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isStep1Active) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isStep1Active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Step 2 Chip
        val isStep2Active = currentStep == DrawSetupStep.SELECT_DRAW_TYPE
        Surface(
            onClick = onStep2Click,
            enabled = canProceedToStep2,
            shape = RoundedCornerShape(10.dp),
            color = if (isStep2Active) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isStep2Active) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isStep2Active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canProceedToStep2) 1f else 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "أسلوب القرعة",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isStep2Active) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isStep2Active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canProceedToStep2) 1f else 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SelectedProfileSummaryCard(
    profile: Profile?,
    onChangeClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (profile == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                id = when (profile.type) {
                                    ProfileType.PLAYERS -> R.drawable.ic_person
                                    ProfileType.CLUBS -> R.drawable.ic_shield
                                    ProfileType.NATIONAL_TEAMS -> R.drawable.ic_globe
                                }
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${profile.activeCount} نشط",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onCustomizeClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "تعديل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    onClick = onChangeClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_redo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "تغيير",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

