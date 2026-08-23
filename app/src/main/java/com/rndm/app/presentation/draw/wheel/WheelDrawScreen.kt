package com.rndm.app.presentation.draw.wheel

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.LocalExtendedColors
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.core.util.Constants
import com.rndm.app.presentation.draw.wheel.components.DrawCategorySelector
import com.rndm.app.presentation.draw.wheel.components.DrawnMatchesFeed
import com.rndm.app.presentation.draw.wheel.components.LiveMatchDrawSimulationCard
import com.rndm.app.presentation.draw.wheel.components.WheelArrowIndicator
import com.rndm.app.presentation.draw.wheel.components.WheelCanvas
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelDrawScreen(
    profileId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    onNavigateToFixtures: () -> Unit = {},
    onNavigateToEditProfile: (Long) -> Unit = {},
    viewModel: WheelDrawViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rotationAnim = remember { Animatable(0f) }
    val extendedColors = LocalExtendedColors.current
    val spacing = RndmThemeTokens.spacing

    LaunchedEffect(profileId) {
        viewModel.initializeWithProfileId(profileId)
    }

    LaunchedEffect(uiState.selectedCategory) {
        rotationAnim.snapTo(0f)
    }

    LaunchedEffect(uiState.spinTrigger) {
        if (uiState.spinTrigger > 0L && uiState.targetRotation > 0f) {
            rotationAnim.snapTo(0f)
            rotationAnim.animateTo(
                targetValue = uiState.targetRotation,
                animationSpec = tween(
                    durationMillis = Constants.WHEEL_SPIN_DURATION_MS.toInt(),
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.2f, 1f)
                )
            )
            delay(500)
            viewModel.onSpinComplete()
            rotationAnim.snapTo(0f)
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "عجلة الحظ التفاعلية",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Add players & continue draw action
                    RndmTopBarAction(
                        onClick = { viewModel.onOpenAddPlayersDialog() },
                        icon = painterResource(id = R.drawable.ic_add),
                        contentDescription = "إضافة لاعبين واستكمال القرعة"
                    )

                    // Shortcut to view fixtures table directly
                    BadgedBox(
                        badge = {
                            if (uiState.fixtures.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("${uiState.fixtures.size}")
                                }
                            }
                        }
                    ) {
                        RndmTopBarAction(
                            onClick = onNavigateToFixtures,
                            icon = painterResource(id = R.drawable.ic_fixtures),
                            contentDescription = "جدول المباريات"
                        )
                    }

                    // Reset draw state
                    RndmTopBarAction(
                        onClick = { viewModel.resetDraw() },
                        icon = painterResource(id = R.drawable.ic_redo),
                        contentDescription = "إعادة ضبط القرعة"
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToFixtures,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fixtures),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    val count = uiState.fixtures.size
                    Text(
                        text = if (count > 0) "جدول المباريات ($count)" else "جدول المباريات",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.xs))

            // 3 Top Category Selector Cards
            DrawCategorySelector(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::onCategorySelect,
                playersCount = uiState.remainingPlayers.size,
                clubsCount = uiState.remainingClubs.size,
                teamsCount = uiState.remainingNationalTeams.size
            )

            Spacer(modifier = Modifier.height(spacing.md))

            val items = uiState.currentWheelItems
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState.selectedCategory) {
                        DrawCategory.PLAYERS -> {
                            val totalPlayers = uiState.selectedPlayersProfile?.items?.size ?: 0
                            if (totalPlayers == 0) {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_person),
                                    title = "لا يوجد أشخاص مضافون",
                                    description = "أضف أسماء اللاعبين أو الأشخاص إلى هذا البروفايل لتتمكن من تدوير العجلة وإجراء السحب والتنافس.",
                                    actionText = "إضافة أشخاص للبروفايل",
                                    actionIcon = painterResource(id = R.drawable.ic_add),
                                    onActionClick = { onNavigateToEditProfile(uiState.selectedPlayersProfile?.id ?: 0L) }
                                )
                            } else {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_tournament_filled),
                                    title = "اكتمل سحب جميع الأشخاص",
                                    description = "تم سحب كافة الأشخاص وتعيين مواجهاتهم في جدول المباريات بنجاح. يمكنك إضافة لاعبين جدد في أي وقت لاستكمال القرعة.",
                                    actionText = "إضافة لاعبين واستكمال القرعة",
                                    actionIcon = painterResource(id = R.drawable.ic_add),
                                    onActionClick = { viewModel.onOpenAddPlayersDialog() },
                                    secondaryActionText = "عرض جدول المباريات",
                                    onSecondaryActionClick = onNavigateToFixtures
                                )
                            }
                        }
                        DrawCategory.CLUBS -> {
                            val totalClubs = uiState.selectedClubsProfile?.items?.size ?: 0
                            if (totalClubs == 0) {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_shield),
                                    title = "لا توجد أندية مضافة",
                                    description = "أضف أندية إلى هذا البروفايل لتتمكن من تدوير العجلة وتوزيع الفرق على المباريات واللاعبين.",
                                    actionText = "إضافة أندية للبروفايل",
                                    actionIcon = painterResource(id = R.drawable.ic_add),
                                    onActionClick = { onNavigateToEditProfile(uiState.selectedClubsProfile?.id ?: 0L) }
                                )
                            } else {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_tournament_filled),
                                    title = "اكتمل سحب جميع الأندية",
                                    description = "تم تعيين جميع الأندية للاعبين والمباريات المسحوبة.",
                                    actionText = "عرض جدول المباريات",
                                    actionIcon = painterResource(id = R.drawable.ic_fixtures),
                                    onActionClick = onNavigateToFixtures,
                                    secondaryActionText = "إعادة ضبط السحب",
                                    onSecondaryActionClick = { viewModel.resetDraw() }
                                )
                            }
                        }
                        DrawCategory.NATIONAL_TEAMS -> {
                            val totalTeams = uiState.selectedNationalTeamsProfile?.items?.size ?: 0
                            if (totalTeams == 0) {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_globe),
                                    title = "لا توجد منتخبات مضافة",
                                    description = "أضف منتخبات إلى هذا البروفايل لإجراء القرعة الدولية وتوزيعها على المباريات.",
                                    actionText = "إضافة منتخبات للبروفايل",
                                    actionIcon = painterResource(id = R.drawable.ic_add),
                                    onActionClick = { onNavigateToEditProfile(uiState.selectedNationalTeamsProfile?.id ?: 0L) }
                                )
                            } else {
                                EmptyState(
                                    asCard = true,
                                    icon = painterResource(id = R.drawable.ic_tournament_filled),
                                    title = "اكتمل سحب جميع المنتخبات",
                                    description = "تم تعيين كافة المنتخبات للمواجهات المسحوبة.",
                                    actionText = "عرض جدول المباريات",
                                    actionIcon = painterResource(id = R.drawable.ic_fixtures),
                                    onActionClick = onNavigateToFixtures,
                                    secondaryActionText = "إعادة ضبط السحب",
                                    onSecondaryActionClick = { viewModel.resetDraw() }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .padding(spacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    WheelCanvas(
                        items = items,
                        rotation = rotationAnim.value,
                        extendedColors = extendedColors
                    )

                    WheelArrowIndicator(
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.md))

                // Action Buttons: Draw Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    RndmButton(
                        onClick = { viewModel.startSpin() },
                        enabled = !uiState.isSpinning,
                        type = RndmButtonType.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categoryIcon = when (uiState.selectedCategory) {
                            DrawCategory.PLAYERS -> R.drawable.ic_person
                            DrawCategory.CLUBS -> R.drawable.ic_shield
                            DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
                        }
                        val isSingleRemaining = items.size == 1
                        val activeIcon = if (isSingleRemaining) R.drawable.ic_check else categoryIcon

                        val buttonText = if (uiState.isSpinning) "العجلة تدور..."
                        else if (isSingleRemaining) {
                            val singleItemLabel = items.first().label
                            when (uiState.selectedCategory) {
                                DrawCategory.PLAYERS -> "تعيين [$singleItemLabel] مباشرة (المتبقي الوحيد)"
                                DrawCategory.CLUBS -> "تعيين [$singleItemLabel] مباشرة (المتبقي الوحيد)"
                                DrawCategory.NATIONAL_TEAMS -> "تعيين [$singleItemLabel] مباشرة (المتبقي الوحيد)"
                            }
                        } else when (uiState.selectedCategory) {
                            DrawCategory.PLAYERS -> "سحب اللاعب"
                            DrawCategory.CLUBS -> "سحب النادي"
                            DrawCategory.NATIONAL_TEAMS -> "سحب المنتخب"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = activeIcon),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(text = buttonText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // 1. Live Match Draw Simulation Card (Directly Below Draw Button)
            LiveMatchDrawSimulationCard(
                category = uiState.selectedCategory,
                fixtures = uiState.fixtures,
                remainingPlayersCount = uiState.remainingPlayers.size,
                remainingClubsCount = uiState.remainingClubs.size,
                remainingTeamsCount = uiState.remainingNationalTeams.size,
                onReplacePlayerClick = viewModel::onRequestReplacePlayer,
                modifier = Modifier.padding(horizontal = spacing.md)
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // 2. Drawn Matches Feed (List of completed matches with teams assigned)
            DrawnMatchesFeed(
                fixtures = uiState.fixtures,
                onViewFixturesClick = onNavigateToFixtures,
                onReplacePlayerClick = viewModel::onRequestReplacePlayer,
                modifier = Modifier.padding(horizontal = spacing.md)
            )

            Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
        }

        uiState.playerToReplace?.let { oldPlayerName ->
            com.rndm.app.core.ui.components.ReplacePlayerDialog(
                oldPlayerName = oldPlayerName,
                initialClubName = uiState.playerToReplaceClub,
                onDismiss = viewModel::onDismissReplacePlayerDialog,
                onConfirm = viewModel::onConfirmReplacePlayer
            )
        }

        if (uiState.isAddPlayersDialogOpen) {
            com.rndm.app.core.ui.components.AddPlayersToDrawDialog(
                existingPlayerNames = uiState.existingPlayerNames,
                availableProfiles = uiState.playersProfiles,
                onDismiss = viewModel::onDismissAddPlayersDialog,
                onConfirm = viewModel::onAddNewPlayers
            )
        }
    }
}
