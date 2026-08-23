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
import com.rndm.app.core.ui.components.ReplacePlayerDialog
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
                            onReplacePlayerClick = viewModel::onRequestReplacePlayer
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

        uiState.playerToReplace?.let { oldPlayerName ->
            ReplacePlayerDialog(
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
                onConfirm = { names ->
                    viewModel.onDismissAddPlayersDialog()
                    viewModel.onAddNewPlayers(names)
                    onNavigateToDraw()
                }
            )
        }
    }
}
