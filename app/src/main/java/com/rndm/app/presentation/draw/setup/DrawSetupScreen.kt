package com.rndm.app.presentation.draw.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.ShimmerBox
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.DrawType
import com.rndm.app.presentation.draw.setup.components.DrawTypeSelectionSection
import com.rndm.app.presentation.draw.setup.components.ProfileDropdownSection

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

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "إعداد القرعة",
                titleIcon = painterResource(id = R.drawable.ic_target),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            // Floating Capsule "Start Draw" Button — Centered identically to RndmBottomBar
            AnimatedVisibility(
                visible = uiState.canStart,
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
                            onStartDraw(uiState.selectedProfileId, uiState.selectedDrawType)
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
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                                text = "بدء القرعة",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
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
            label = "draw_setup_crossfade"
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
                            .height(90.dp)
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
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_profile_outlined),
                    title = "لا توجد بروفايلات محفوظة",
                    description = "يجب إنشاء بروفايل يحتوي على لاعبين أو أندية لإجراء القرعة",
                    actionText = "إنشاء بروفايل جديد",
                    onActionClick = onNavigateToCreateProfile,
                    modifier = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        ProfileDropdownSection(
                            profiles = uiState.profiles,
                            selectedProfile = uiState.selectedProfile,
                            onProfileSelected = viewModel::onProfileSelected
                        )
                    }

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
