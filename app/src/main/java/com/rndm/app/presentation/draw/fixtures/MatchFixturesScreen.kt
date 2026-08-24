package com.rndm.app.presentation.draw.fixtures

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.presentation.draw.fixtures.components.FixtureMatchCard
import com.rndm.app.presentation.draw.fixtures.components.FixtureScoreDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFixturesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDraw: () -> Unit = onNavigateBack,
    viewModel: MatchFixturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = RndmThemeTokens.spacing

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "جدول المباريات والفرق",
                onNavigateBack = onNavigateBack,
                actions = {
                    // Add players & continue draw action
                    RndmTopBarAction(
                        onClick = { viewModel.onOpenAddPlayersDialog() },
                        icon = painterResource(id = R.drawable.ic_add),
                        contentDescription = "إضافة لاعبين واستكمال القرعة"
                    )

                    if (uiState.fixtures.isNotEmpty()) {
                        RndmTopBarAction(
                            onClick = {
                                val summary = viewModel.formatFixturesSummary()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("RNDM Matchups", summary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ جدول المباريات بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            icon = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "نسخ جدول المباريات",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.fixtures.isEmpty()) {
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_tournament_outlined),
                    title = "لا توجد مباريات مسحوبة بعد",
                    description = "استخدم عجلة الحظ لسحب اللاعبين والأندية لتوليد جدول المباريات تلقائياً",
                    actionText = "العودة للقرعة",
                    onActionClick = onNavigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fixture Stats Summary Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.sm, horizontal = spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إجمالي: ${uiState.totalMatches} مباريات",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "مكتملة: ${uiState.completedMatches}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            onClick = { viewModel.onOpenAddPlayersDialog() },
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "إضافة لاعبين",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    items(items = uiState.fixtures, key = { it.id }) { fixture ->
                        FixtureMatchCard(
                            fixture = fixture,
                            onEditScoreClick = { viewModel.onEditScoreClick(fixture) },
                            onReorderClick = { viewModel.onOpenReorderFixtureDialog(fixture) },
                            onSwapPlayerClick = { isSlotOne -> viewModel.onOpenSwapPlayerDialog(fixture, isSlotOne) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(spacing.xxl))
                    }
                }
            }
        }

        uiState.editingFixture?.let { fixture ->
            FixtureScoreDialog(
                fixture = fixture,
                scoreOne = uiState.inputScoreOne,
                scoreTwo = uiState.inputScoreTwo,
                onScoreOneChange = viewModel::onScoreOneChange,
                onScoreTwoChange = viewModel::onScoreTwoChange,
                onDismiss = viewModel::onDismissScoreDialog,
                onSave = viewModel::onSaveScore
            )
        }



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

        if (uiState.isAddPlayersDialogOpen) {
            com.rndm.app.core.ui.components.AddPlayersToDrawDialog(
                existingPlayerNames = uiState.existingPlayerNames,
                availableProfiles = uiState.playersProfiles,
                onDismiss = viewModel::onDismissAddPlayersDialog,
                onConfirm = { names ->
                    viewModel.onDismissAddPlayersDialog()
                    viewModel.onAddNewPlayers(names)
                    onNavigateToDraw()
                }
            )
        }
    }
}
