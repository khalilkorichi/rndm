package com.rndm.app.presentation.profile.player.leaderboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rndm.app.core.theme.BronzeMedalColor
import com.rndm.app.core.theme.GoldMedalColor
import com.rndm.app.core.theme.SilverMedalColor
import com.rndm.app.core.theme.StatsErrorRed
import com.rndm.app.core.theme.StatsSuccessGreen
import com.rndm.app.core.theme.StatsWarningAmber
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.PlayerAvatar
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.PlayerLeaderboardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersLeaderboardScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: PlayersLeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tableHorizontalScroll = rememberScrollState()

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "لوحة صدارة اللاعبين",
                titleIcon = painterResource(id = R.drawable.ic_stats_filled),
                onNavigateBack = onNavigateBack,
                actions = {
                    RndmTopBarAction(
                        onClick = viewModel::refresh,
                        icon = painterResource(id = R.drawable.ic_redo),
                        contentDescription = "تحديث الإحصائيات",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Crossfade(
            targetState = uiState.isLoading,
            animationSpec = tween(Constants.CROSSFADE_DURATION_MS),
            label = "leaderboard_crossfade"
        ) { isLoading ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.players.isEmpty()) {
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_stats_outlined),
                    title = "لا توجد إحصائيات بعد",
                    description = "ابدأ بطولات جديدة وأجرِ المباريات لتظهر إحصائيات اللاعبين وترتيبهم هنا تلقائياً وبشكل فوري!",
                    modifier = Modifier.padding(padding)
                )
            } else {
                val top3 = uiState.players.take(3)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 14.dp, end = 14.dp)
                ) {
                    // 1. Top 3 Podium Section (Shown when search is empty)
                    if (top3.isNotEmpty() && uiState.searchQuery.isBlank()) {
                        item {
                            LeaderboardPodiumSection(
                                topPlayers = top3,
                                onPlayerClick = onNavigateToPlayer
                            )
                        }
                    }

                    // 2. Search and Quick Summary Bar
                    item {
                        SearchAndCountBar(
                            query = uiState.searchQuery,
                            totalCount = uiState.filteredPlayers.size,
                            onQueryChange = viewModel::onSearchQueryChange
                        )
                    }

                    // 3. Full Unified Stats Table
                    item {
                        ComprehensiveStatsTableCard(
                            players = uiState.filteredPlayers,
                            currentSortColumn = uiState.sortColumn,
                            isAscending = uiState.isAscending,
                            horizontalScrollState = tableHorizontalScroll,
                            onSortColumnClick = viewModel::onSortColumnClick,
                            onPlayerClick = onNavigateToPlayer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndCountBar(
    query: String,
    totalCount: Int,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "بحث عن لاعب...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Text(
                text = "$totalCount لاعب",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun LeaderboardPodiumSection(
    topPlayers: List<PlayerLeaderboardItem>,
    onPlayerClick: (String) -> Unit
) {
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = GoldMedalColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "منصة التتويج",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place (Silver)
                if (topPlayers.size > 1) {
                    PodiumColumnItem(
                        player = topPlayers[1],
                        rank = 2,
                        podiumHeight = 72.dp,
                        color = SilverMedalColor,
                        onClick = { onPlayerClick(topPlayers[1].playerName) }
                    )
                }

                // 1st Place (Gold)
                if (topPlayers.isNotEmpty()) {
                    PodiumColumnItem(
                        player = topPlayers[0],
                        rank = 1,
                        podiumHeight = 98.dp,
                        color = GoldMedalColor,
                        onClick = { onPlayerClick(topPlayers[0].playerName) }
                    )
                }

                // 3rd Place (Bronze)
                if (topPlayers.size > 2) {
                    PodiumColumnItem(
                        player = topPlayers[2],
                        rank = 3,
                        podiumHeight = 56.dp,
                        color = BronzeMedalColor,
                        onClick = { onPlayerClick(topPlayers[2].playerName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumColumnItem(
    player: PlayerLeaderboardItem,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        // Avatar
        PlayerAvatar(
            avatarIcon = player.avatarIcon,
            size = 46.dp,
            iconSize = 22.dp,
            tint = color,
            backgroundColor = color.copy(alpha = 0.18f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player.playerName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trophy),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "${player.titlesCount} ألقاب",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }

        Text(
            text = "${player.winRate.toInt()}% فوز • ${player.goalsScored} هدف",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Podium Block
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = color.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
            modifier = Modifier
                .width(80.dp)
                .height(podiumHeight)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ComprehensiveStatsTableCard(
    players: List<PlayerLeaderboardItem>,
    currentSortColumn: LeaderboardColumn,
    isAscending: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSortColumnClick: (LeaderboardColumn) -> Unit,
    onPlayerClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chart),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "جدول إحصائيات اللاعبين الشامل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "انقر على أي عمود للفرز",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد نتائج تطابق البحث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Table Column Headers
                TableHeaderRow(
                    currentSortColumn = currentSortColumn,
                    isAscending = isAscending,
                    horizontalScrollState = horizontalScrollState,
                    onSortColumnClick = onSortColumnClick
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Table Rows
                players.forEachIndexed { index, player ->
                    val isAlt = index % 2 == 1
                    val rankColor = when (player.rank) {
                        1 -> GoldMedalColor
                        2 -> SilverMedalColor
                        3 -> BronzeMedalColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }

                    TableRowItem(
                        player = player,
                        rankColor = rankColor,
                        isAlternateBg = isAlt,
                        currentSortColumn = currentSortColumn,
                        horizontalScrollState = horizontalScrollState,
                        onClick = { onPlayerClick(player.playerName) }
                    )

                    if (index < players.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow(
    currentSortColumn: LeaderboardColumn,
    isAscending: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSortColumnClick: (LeaderboardColumn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fixed Sticky Player Column
        Row(
            modifier = Modifier
                .width(135.dp)
                .clickable { onSortColumnClick(LeaderboardColumn.PLAYER) }
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (currentSortColumn == LeaderboardColumn.RANK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(18.dp)
            )
            Text(
                text = "اللاعب",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (currentSortColumn == LeaderboardColumn.PLAYER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentSortColumn == LeaderboardColumn.PLAYER) {
                Text(
                    text = if (isAscending) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Scrollable Stat Metric Columns
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableColumnHeaderCell(LeaderboardColumn.TITLES, "🏆", 46.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.MATCHES, "ل", 40.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.WINS, "ف", 38.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.DRAWS, "ت", 38.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.LOSSES, "خ", 38.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.GOALS_FOR, "له", 42.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.GOALS_AGAINST, "عليه", 42.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.GOAL_DIFF, "+/-", 44.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.WIN_RATE, "%", 50.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.POINTS, "ن", 42.dp, currentSortColumn, isAscending, onSortColumnClick)
            TableColumnHeaderCell(LeaderboardColumn.CLEAN_SHEETS, "ش.ن", 42.dp, currentSortColumn, isAscending, onSortColumnClick)
        }
    }
}

@Composable
private fun TableColumnHeaderCell(
    column: LeaderboardColumn,
    label: String,
    width: androidx.compose.ui.unit.Dp,
    currentSortColumn: LeaderboardColumn,
    isAscending: Boolean,
    onClick: (LeaderboardColumn) -> Unit
) {
    val isSelected = currentSortColumn == column
    Row(
        modifier = Modifier
            .width(width)
            .clickable { onClick(column) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isSelected) {
            Text(
                text = if (isAscending) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}

@Composable
private fun TableRowItem(
    player: PlayerLeaderboardItem,
    rankColor: Color,
    isAlternateBg: Boolean,
    currentSortColumn: LeaderboardColumn,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isAlternateBg) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sticky Player Info (Rank + Avatar + Name)
        Row(
            modifier = Modifier
                .width(135.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Rank Number
            Text(
                text = "${player.rank}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = rankColor,
                modifier = Modifier.width(18.dp)
            )

            // Avatar
            PlayerAvatar(
                avatarIcon = player.avatarIcon,
                size = 28.dp,
                iconSize = 14.dp
            )

            // Player Name
            Text(
                text = player.playerName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Scrollable Metric Data Cells
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Titles (🏆)
            TableCell(
                text = "${player.titlesCount}",
                width = 46.dp,
                fontWeight = FontWeight.Black,
                color = if (player.titlesCount > 0) GoldMedalColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                isHighlighted = currentSortColumn == LeaderboardColumn.TITLES
            )

            // Matches (ل)
            TableCell(
                text = "${player.totalMatches}",
                width = 40.dp,
                isHighlighted = currentSortColumn == LeaderboardColumn.MATCHES
            )

            // Wins (ف)
            TableCell(
                text = "${player.totalWins}",
                width = 38.dp,
                fontWeight = FontWeight.Bold,
                color = if (player.totalWins > 0) StatsSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                isHighlighted = currentSortColumn == LeaderboardColumn.WINS
            )

            // Draws (ت)
            TableCell(
                text = "${player.totalDraws}",
                width = 38.dp,
                color = if (player.totalDraws > 0) StatsWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                isHighlighted = currentSortColumn == LeaderboardColumn.DRAWS
            )

            // Losses (خ)
            TableCell(
                text = "${player.totalLosses}",
                width = 38.dp,
                color = if (player.totalLosses > 0) StatsErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                isHighlighted = currentSortColumn == LeaderboardColumn.LOSSES
            )

            // Goals For (له)
            TableCell(
                text = "${player.goalsScored}",
                width = 42.dp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                isHighlighted = currentSortColumn == LeaderboardColumn.GOALS_FOR
            )

            // Goals Against (عليه)
            TableCell(
                text = "${player.goalsConceded}",
                width = 42.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                isHighlighted = currentSortColumn == LeaderboardColumn.GOALS_AGAINST
            )

            // Goal Diff (+/-)
            val diffText = if (player.goalDifference > 0) "+${player.goalDifference}" else "${player.goalDifference}"
            val diffColor = when {
                player.goalDifference > 0 -> StatsSuccessGreen
                player.goalDifference < 0 -> StatsErrorRed
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            TableCell(
                text = diffText,
                width = 44.dp,
                fontWeight = FontWeight.Bold,
                color = diffColor,
                isHighlighted = currentSortColumn == LeaderboardColumn.GOAL_DIFF
            )

            // Win Rate (%)
            TableCell(
                text = "${player.winRate.toInt()}%",
                width = 50.dp,
                fontWeight = FontWeight.Bold,
                color = if (player.winRate >= 50f) StatsSuccessGreen else MaterialTheme.colorScheme.onSurface,
                isHighlighted = currentSortColumn == LeaderboardColumn.WIN_RATE
            )

            // Points (ن)
            TableCell(
                text = "${player.points}",
                width = 42.dp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                isHighlighted = currentSortColumn == LeaderboardColumn.POINTS
            )

            // Clean Sheets (ش.ن)
            TableCell(
                text = "${player.cleanSheets}",
                width = 42.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                isHighlighted = currentSortColumn == LeaderboardColumn.CLEAN_SHEETS
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight = FontWeight.Medium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isHighlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .then(
                if (isHighlighted) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = fontWeight
            ),
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else color,
            textAlign = TextAlign.Center
        )
    }
}
