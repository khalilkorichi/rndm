package com.rndm.app.presentation.draw.free

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.rndm.app.presentation.draw.free.components.FreeDrawProfileItemsDialog
import com.rndm.app.presentation.draw.free.components.FreeDrawWinnerDialog
import com.rndm.app.presentation.draw.wheel.DrawCategory
import com.rndm.app.presentation.draw.wheel.components.DrawCategorySelector
import com.rndm.app.presentation.draw.wheel.components.WheelArrowIndicator
import com.rndm.app.presentation.draw.wheel.components.WheelCanvas
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeWheelDrawScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreeWheelDrawViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rotationAnim = remember { Animatable(0f) }
    val extendedColors = LocalExtendedColors.current
    val spacing = RndmThemeTokens.spacing

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
                title = "قرعة عشوائية حرة",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Open Profile & Items Setup Dialog
                    RndmTopBarAction(
                        onClick = { viewModel.onOpenSetupDialog() },
                        icon = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "إدارة واختيار العناصر"
                    )

                    // Reset / Restore category items
                    RndmTopBarAction(
                        onClick = { viewModel.resetCategoryItems(uiState.selectedCategory) },
                        icon = painterResource(id = R.drawable.ic_redo),
                        contentDescription = "إعادة ضبط عناصر الفئة"
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val items = uiState.currentWheelItems
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Selector Tabs
            DrawCategorySelector(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.onCategorySelect(it) },
                playersCount = uiState.remainingPlayers.size,
                clubsCount = uiState.remainingClubs.size,
                teamsCount = uiState.remainingNationalTeams.size,
                modifier = Modifier.fillMaxWidth()
            )

            // Active Category / Profile info pill
            val activeCategoryName = when (uiState.selectedCategory) {
                DrawCategory.PLAYERS -> "أشخاص"
                DrawCategory.CLUBS -> "أندية"
                DrawCategory.NATIONAL_TEAMS -> "منتخبات"
            }
            val profileName = uiState.currentSelectedProfile?.name ?: "البروفايل الافتراضي"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$activeCategoryName • ${items.size} عناصر متبقية في العجلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !uiState.isSpinning) { viewModel.onOpenSetupDialog() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تعديل العناصر",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Interactive Wheel Container
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    WheelCanvas(
                        items = items,
                        rotation = rotationAnim.value,
                        extendedColors = extendedColors,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(12.dp, CircleShape)
                    )

                    // Top Arrow Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    ) {
                        WheelArrowIndicator(
                            modifier = Modifier.size(36.dp, 28.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Center Hub
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    id = when (uiState.selectedCategory) {
                                        DrawCategory.PLAYERS -> R.drawable.ic_person
                                        DrawCategory.CLUBS -> R.drawable.ic_shield
                                        DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
                                    }
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Spin Action Button
                RndmButton(
                    onClick = { viewModel.startSpin() },
                    enabled = uiState.canSpin,
                    type = RndmButtonType.PRIMARY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wheel),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSpinning) "جارِ التدوير..." else "تدوير العجلة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_wheel),
                    title = "لا توجد عناصر متبقية في عجلة $activeCategoryName",
                    description = "تم استبعاد أو سحب جميع العناصر، افتح صندوق العناصر لتفعيلها أو أعد الضبط.",
                    actionText = "إدارة وتفعيل العناصر",
                    actionIcon = painterResource(id = R.drawable.ic_edit),
                    onActionClick = { viewModel.onOpenSetupDialog() }
                )
            }

            // Recent Winners History Feed
            AnimatedVisibility(visible = uiState.recentWinners.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_trophy),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "سجل السحوبات الأخيرة (${uiState.recentWinners.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "مسح السجل",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.clearRecentWinners() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = uiState.recentWinners,
                                key = { "${it.item.id}-${it.timestamp}" }
                            ) { winner ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "★",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = winner.item.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Profile and Items Management Dialog
        if (uiState.isSetupDialogOpen) {
            FreeDrawProfileItemsDialog(
                selectedCategory = uiState.selectedCategory,
                playersProfiles = uiState.playersProfiles,
                clubsProfiles = uiState.clubsProfiles,
                nationalTeamsProfiles = uiState.nationalTeamsProfiles,
                selectedPlayersProfile = uiState.selectedPlayersProfile,
                selectedClubsProfile = uiState.selectedClubsProfile,
                selectedNationalTeamsProfile = uiState.selectedNationalTeamsProfile,
                remainingPlayers = uiState.remainingPlayers,
                excludedPlayers = uiState.excludedPlayers,
                remainingClubs = uiState.remainingClubs,
                excludedClubs = uiState.excludedClubs,
                remainingNationalTeams = uiState.remainingNationalTeams,
                excludedNationalTeams = uiState.excludedNationalTeams,
                onDismiss = { viewModel.onDismissSetupDialog() },
                onCategoryChanged = { viewModel.onCategorySelect(it) },
                onProfileSelected = { cat, prof -> viewModel.onSelectProfileForCategory(cat, prof) },
                onSaveAndApply = { cat, active, excluded ->
                    viewModel.onSaveAndApplyDialogChanges(cat, active, excluded)
                }
            )
        }

        // Winner Celebration Dialog
        if (uiState.isWinnerDialogOpen && uiState.winnerItem != null) {
            FreeDrawWinnerDialog(
                winner = uiState.winnerItem!!,
                category = uiState.selectedCategory,
                onDismiss = { viewModel.onDismissWinnerDialog() },
                onKeepAndSpinAgain = { viewModel.onKeepWinnerAndSpinAgain() },
                onExcludeAndSpinAgain = { viewModel.onExcludeWinnerAndSpinAgain() }
            )
        }
    }
}
