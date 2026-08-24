package com.rndm.app.presentation.tournament.list

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.presentation.profile.list.components.ProfileListSkeleton
import com.rndm.app.presentation.tournament.list.components.CreateTournamentBottomSheet
import com.rndm.app.presentation.tournament.list.components.SwipeableTournamentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    onNavigateToTournamentDetail: (Long) -> Unit,
    onNavigateToCreateTournament: () -> Unit,
    onNavigateToDrawSetup: (Long) -> Unit,
    onNavigateToEditTournament: (Long) -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToJoinTournament: () -> Unit = {},
    viewModel: TournamentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = RndmThemeTokens.spacing
    var showCreateOptionsSheet by remember { mutableStateOf(false) }

    // Create Tournament Selection Bottom Sheet
    if (showCreateOptionsSheet) {
        CreateTournamentBottomSheet(
            onDismiss = { showCreateOptionsSheet = false },
            onSelectDrawTournament = {
                showCreateOptionsSheet = false
                onNavigateToDrawSetup(0L)
            },
            onSelectGroupsTournament = {
                showCreateOptionsSheet = false
                onNavigateToCreateTournament()
            },
            onSelectJoinTournament = {
                showCreateOptionsSheet = false
                onNavigateToJoinTournament()
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "حذف البطولة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف هذه البطولة؟ لا يمكن التراجع عن هذه الخطوة وسيتم حذف كافة المباريات والإحصائيات الخاصة بها.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() }
                ) {
                    Text(
                        text = "حذف",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "البطولات",
                titleIcon = painterResource(id = R.drawable.ic_tournament_filled),
                actions = {
                    Surface(
                        onClick = onNavigateToArchive,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_archive),
                            contentDescription = "الأرشيف",
                            modifier = Modifier
                                .size(36.dp)
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateOptionsSheet = true },
                modifier = Modifier.padding(bottom = 76.dp),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "إنشاء بطولة جديدة"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Bar with Scrollable Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == TournamentFilter.ALL,
                    onClick = { viewModel.onFilterSelect(TournamentFilter.ALL) },
                    label = { Text("الكل") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == TournamentFilter.DRAW_TOURNAMENTS,
                    onClick = { viewModel.onFilterSelect(TournamentFilter.DRAW_TOURNAMENTS) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_target),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("قرعة") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == TournamentFilter.GROUPS_TOURNAMENTS,
                    onClick = { viewModel.onFilterSelect(TournamentFilter.GROUPS_TOURNAMENTS) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tournament_filled),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("مجموعات") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == TournamentFilter.COMPLETED,
                    onClick = { viewModel.onFilterSelect(TournamentFilter.COMPLETED) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("مكتملة") }
                )
            }

            Crossfade(targetState = uiState.isLoading, label = "tournaments_crossfade") { isLoading ->
                if (isLoading) {
                    ProfileListSkeleton(modifier = Modifier.padding(16.dp))
                } else if (uiState.filteredTournaments.isEmpty()) {
                    EmptyState(
                        icon = painterResource(id = R.drawable.ic_tournament_outlined),
                        title = "لا توجد بطولات",
                        description = "ابدأ بإنشاء بطولة قرعة أو بطولة مجموعات لتنظيم مبارياتك ومتابعة النتائج.",
                        actionText = "إنشاء بطولة",
                        onActionClick = { showCreateOptionsSheet = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Winner Hall of Fame if there are winners
                        if (uiState.winnerStats.isNotEmpty()) {
                            item {
                                BentoCard(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_star),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(spacing.sm))
                                            Text(
                                                text = "لوحة الأبطال",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(spacing.sm))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                                        ) {
                                            uiState.winnerStats.forEach { stat ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_medal),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = stat.winnerName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = "${stat.winsCount}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = uiState.filteredTournaments,
                            key = { it.id }
                        ) { tournament ->
                            SwipeableTournamentCard(
                                tournament = tournament,
                                onClick = { onNavigateToTournamentDetail(tournament.id) },
                                onEdit = { onNavigateToEditTournament(tournament.id) },
                                onArchive = { viewModel.archiveTournament(tournament.id) },
                                onDelete = { viewModel.requestDelete(tournament.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
