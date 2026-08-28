package com.rndm.app.presentation.draw.flipcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.presentation.draw.flipcards.components.FlipCardExclusionHub
import com.rndm.app.presentation.draw.flipcards.components.FlipCardItem
import com.rndm.app.presentation.draw.wheel.DrawCategory
import com.rndm.app.presentation.draw.wheel.components.DrawCategorySelector
import com.rndm.app.presentation.draw.wheel.components.DrawPromptBanner
import com.rndm.app.presentation.draw.wheel.components.DrawnMatchesFeed
import com.rndm.app.presentation.draw.wheel.components.ExcludeFromDrawDialog
import com.rndm.app.presentation.draw.wheel.components.LiveMatchDrawSimulationCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipCardDrawScreen(
    profileId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToFixtures: () -> Unit = {},
    onNavigateToEditProfile: (Long) -> Unit = {},
    viewModel: FlipCardDrawViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = RndmThemeTokens.spacing
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val shuffleAnim = remember { Animatable(0f) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(profileId) {
        viewModel.initializeWithProfileId(profileId)
    }

    // 3D Deck Real Slot Swap & Riffle Haptics Orchestration
    LaunchedEffect(uiState.shuffleTrigger) {
        if (uiState.shuffleTrigger > 0L) {
            launch {
                // Ensure grid stays firmly anchored at the top
                gridState.scrollToItem(0)
                // Multi-pulse riffle haptics during card flight
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(140)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(140)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(160)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = FastOutSlowInEasing
                )
            )
            shuffleAnim.snapTo(0f)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                RndmTopAppBar(
                    title = "البطاقات المقلوبة",
                    onNavigateBack = onNavigateBack,
                    actions = {
                        // 1. Add players dialog button
                        RndmTopBarAction(
                            onClick = { viewModel.onOpenAddPlayersDialog() },
                            icon = painterResource(id = R.drawable.ic_add),
                            contentDescription = "إضافة لاعبين واستكمال القرعة"
                        )

                        // 2. Fixtures table shortcut badge
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

                        // 3. Reset draw state
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

                // 1. 3 Top Category Selector Cards
                DrawCategorySelector(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelect,
                    playersCount = uiState.remainingPlayers.size,
                    clubsCount = uiState.remainingClubs.size,
                    teamsCount = uiState.remainingNationalTeams.size
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                // 2. Dynamic Draw Prompt Banner
                if (uiState.currentDrawingPrompt.isNotBlank()) {
                    DrawPromptBanner(
                        prompt = uiState.currentDrawingPrompt,
                        modifier = Modifier.padding(horizontal = spacing.md)
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                }

                // 3. Flip Cards Deck Area / Category Empty States
                val cards = uiState.currentCards
                val remainingCount = uiState.currentRemainingItems.size

                if (cards.isEmpty()) {
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
                                        description = "أضف أسماء اللاعبين أو الأشخاص إلى هذا البروفايل لتتمكن من سحب البطاقات المقلوبة وإجراء المنافسة.",
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
                                        description = "أضف أندية إلى هذا البروفايل لسحب بطاقات الأندية وتوزيع الفرق على المباريات واللاعبين.",
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Exclusion & Quick Shuffle Controls Hub
                        FlipCardExclusionHub(
                            category = uiState.selectedCategory,
                            excludedCount = uiState.currentExcludedItems.size,
                            remainingCount = remainingCount,
                            isRevealing = uiState.isRevealing,
                            isShuffling = uiState.isShuffling,
                            onOpenExcludeDialog = { viewModel.onOpenExcludeDialog() },
                            onShuffleCards = { viewModel.onShuffleCards() }
                        )

                        // 3-Column Responsive Flip Cards Grid with LazyVerticalGrid and Fixed Scroll State
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val totalWidth = maxWidth
                            val horizSpacing = 10.dp
                            val vertSpacing = 10.dp
                            val cardWidth = (totalWidth - (horizSpacing * 2)) / 3
                            val cardHeight = cardWidth / 0.72f

                            val stepXPx = with(density) { (cardWidth + horizSpacing).toPx() }
                            val stepYPx = with(density) { (cardHeight + vertSpacing).toPx() }

                            val shuffleProgress = shuffleAnim.value
                            val arcCurve = if (shuffleProgress > 0f) sin(shuffleProgress * PI).toFloat() else 0f

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(horizSpacing),
                                verticalArrangement = Arrangement.spacedBy(vertSpacing)
                            ) {
                                items(
                                    count = cards.size,
                                    // Crucial: Use index key so Compose does NOT auto-scroll or jump when item order changes!
                                    key = { index -> index }
                                ) { index ->
                                    val cardState = cards[index]
                                    val isFlipped = uiState.flippedCardIndex == index
                                    val isDrawn = cardState.isDrawn
                                    val item = cardState.item
                                    val revealedLabel = if (isFlipped) {
                                        uiState.drawResult?.selectedItem?.label ?: item.label
                                    } else {
                                        item.label
                                    }

                                    // Symmetrical Parabolic Arc Shuffle Movement
                                    val col = index % 3
                                    val row = index / 3
                                    val dirX = when (col) {
                                        0 -> -1f
                                        2 -> 1f
                                        else -> if (row % 2 == 0) 0.6f else -0.6f
                                    }
                                    val dirY = if (row % 2 == 0) -0.7f else 0.7f

                                    val offsetX = if (isDrawn) 0f else stepXPx * dirX * 0.95f * arcCurve
                                    val offsetY = if (isDrawn) 0f else stepYPx * dirY * 0.35f * arcCurve
                                    val tiltZ = if (isDrawn) 0f else (dirX * -9f + dirY * 4f) * arcCurve
                                    val scale = if (isDrawn) 1f else 1f + (0.07f * arcCurve)
                                    val elevation = if (isDrawn) 0f else if ((row + col) % 2 == 0) 16f * arcCurve else 4f * arcCurve

                                    FlipCardItem(
                                        cardNumber = index + 1,
                                        itemLabel = revealedLabel,
                                        isFlipped = isFlipped,
                                        isDrawn = isDrawn,
                                        isEnabled = !uiState.isRevealing && !uiState.isShuffling && !isDrawn,
                                        onClick = { viewModel.onCardClick(index) },
                                        category = uiState.selectedCategory,
                                        shuffleOffsetX = offsetX,
                                        shuffleOffsetY = offsetY,
                                        shuffleRotationZ = tiltZ,
                                        shuffleScale = scale,
                                        shuffleElevation = elevation
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))

                // 4. Live Match Draw Simulation Card (Directly Below Cards Grid)
                LiveMatchDrawSimulationCard(
                    category = uiState.selectedCategory,
                    fixtures = uiState.fixtures,
                    remainingPlayersCount = uiState.remainingPlayers.size,
                    remainingClubsCount = uiState.remainingClubs.size,
                    remainingTeamsCount = uiState.remainingNationalTeams.size,
                    modifier = Modifier.padding(horizontal = spacing.md)
                )

                Spacer(modifier = Modifier.height(spacing.md))

                // 5. Drawn Matches Feed (List of completed matches with Reorder & Swap actions)
                DrawnMatchesFeed(
                    fixtures = uiState.fixtures,
                    onViewFixturesClick = onNavigateToFixtures,
                    onReorderClick = viewModel::onOpenReorderFixtureDialog,
                    onSwapPlayerClick = viewModel::onOpenSwapPlayerDialog,
                    modifier = Modifier.padding(horizontal = spacing.md)
                )

                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }

            // ==================== DIALOGS ====================

            // 1. Reorder Match Dialog
            uiState.reorderingFixture?.let { currentFixture ->
                val otherMatches = remember(uiState.fixtures, currentFixture.id) {
                    uiState.fixtures
                        .filter { it.id != currentFixture.id && it.playerTwoName != null }
                        .map { f ->
                            com.rndm.app.core.ui.components.ReorderMatchOption(
                                matchIdentifier = f.id,
                                matchNumberText = "المباراة #${f.matchNumber}",
                                playerOneName = f.playerOneName,
                                playerOneClub = f.playerOneTeam,
                                playerTwoName = f.playerTwoName,
                                playerTwoClub = f.playerTwoTeam
                            )
                        }
                }

                com.rndm.app.core.ui.components.ReorderMatchDialog(
                    currentMatch = com.rndm.app.core.ui.components.ReorderMatchOption(
                        matchIdentifier = currentFixture.id,
                        matchNumberText = "المباراة #${currentFixture.matchNumber}",
                        playerOneName = currentFixture.playerOneName,
                        playerOneClub = currentFixture.playerOneTeam,
                        playerTwoName = currentFixture.playerTwoName,
                        playerTwoClub = currentFixture.playerTwoTeam,
                        isCurrent = true
                    ),
                    otherMatches = otherMatches,
                    onSelectMatchToSwap = { targetId ->
                        viewModel.onSwapFixtures(currentFixture.id, targetId.toString())
                    },
                    onMoveUp = { viewModel.onMoveFixtureUp(currentFixture) },
                    onMoveDown = { viewModel.onMoveFixtureDown(currentFixture) },
                    onDismiss = viewModel::onDismissReorderDialog
                )
            }

            // 2. Swap Players Dialog
            uiState.swappingPlayerSlot?.let { (currentFixture, isSlotOne) ->
                val sourceName = if (isSlotOne) currentFixture.playerOneName else (currentFixture.playerTwoName ?: "BYE")
                val sourceClub = if (isSlotOne) currentFixture.playerOneTeam else currentFixture.playerTwoTeam

                val otherFixtures = remember(uiState.fixtures, currentFixture.id) {
                    uiState.fixtures.filter { it.id != currentFixture.id }
                }

                val candidates = remember(otherFixtures, currentFixture, isSlotOne) {
                    val list = mutableListOf<com.rndm.app.core.ui.components.SwapPlayerCandidate>()

                    // Opponent in same match
                    if (isSlotOne && currentFixture.playerTwoName != null) {
                        list.add(
                            com.rndm.app.core.ui.components.SwapPlayerCandidate(
                                matchIdentifier = currentFixture.id,
                                matchTitle = "المباراة #${currentFixture.matchNumber}",
                                isSlotOne = false,
                                playerName = currentFixture.playerTwoName!!,
                                playerClub = currentFixture.playerTwoTeam,
                                isSameMatchOpponent = true
                            )
                        )
                    } else if (!isSlotOne) {
                        list.add(
                            com.rndm.app.core.ui.components.SwapPlayerCandidate(
                                matchIdentifier = currentFixture.id,
                                matchTitle = "المباراة #${currentFixture.matchNumber}",
                                isSlotOne = true,
                                playerName = currentFixture.playerOneName,
                                playerClub = currentFixture.playerOneTeam,
                                isSameMatchOpponent = true
                            )
                        )
                    }

                    // Players in other matches
                    otherFixtures.forEach { f ->
                        list.add(
                            com.rndm.app.core.ui.components.SwapPlayerCandidate(
                                matchIdentifier = f.id,
                                matchTitle = "المباراة #${f.matchNumber}",
                                isSlotOne = true,
                                playerName = f.playerOneName,
                                playerClub = f.playerOneTeam
                            )
                        )
                        f.playerTwoName?.let { p2 ->
                            list.add(
                                com.rndm.app.core.ui.components.SwapPlayerCandidate(
                                    matchIdentifier = f.id,
                                    matchTitle = "المباراة #${f.matchNumber}",
                                    isSlotOne = false,
                                    playerName = p2,
                                    playerClub = f.playerTwoTeam
                                )
                            )
                        }
                    }
                    list
                }

                com.rndm.app.core.ui.components.SwapPlayersDialog(
                    sourcePlayerName = sourceName,
                    sourcePlayerClub = sourceClub,
                    sourceMatchTitle = "المباراة #${currentFixture.matchNumber}",
                    candidates = candidates,
                    onSelectCandidateToSwap = { candidate ->
                        viewModel.onConfirmSwapPlayers(candidate.matchIdentifier.toString(), candidate.isSlotOne)
                    },
                    onDismiss = viewModel::onDismissSwapPlayerDialog
                )
            }

            // 3. Add Players to Draw Dialog
            if (uiState.isAddPlayersDialogOpen) {
                com.rndm.app.core.ui.components.AddPlayersToDrawDialog(
                    existingPlayerNames = uiState.existingPlayerNames,
                    excludedPlayerNames = uiState.excludedPlayers.map { it.label },
                    availableProfiles = uiState.playersProfiles,
                    onDismiss = viewModel::onDismissAddPlayersDialog,
                    onConfirm = viewModel::onAddNewPlayers
                )
            }

            // 4. Exclude from Draw Dialog
            if (uiState.isExcludeDialogOpen) {
                ExcludeFromDrawDialog(
                    category = uiState.selectedCategory,
                    profileName = uiState.currentProfileName,
                    remainingItems = uiState.currentCardsItems,
                    excludedItems = uiState.currentExcludedItems,
                    onExcludeItem = { item -> viewModel.excludeItem(uiState.selectedCategory, item) },
                    onRestoreItem = { item -> viewModel.restoreExcludedItem(uiState.selectedCategory, item) },
                    onExcludeAll = { viewModel.excludeAll(uiState.selectedCategory) },
                    onRestoreAll = { viewModel.restoreAll(uiState.selectedCategory) },
                    onDismiss = viewModel::onDismissExcludeDialog
                )
            }
        }
    }
}
