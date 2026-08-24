package com.rndm.app.presentation.profile.player.leaderboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.PlayerLeaderboardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersLeaderboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: PlayersLeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "لوحة صدارة اللاعبين",
                titleIcon = painterResource(id = R.drawable.ic_trophy),
                onNavigateBack = onNavigateBack
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
                    icon = painterResource(id = R.drawable.ic_trophy),
                    title = "لا توجد إحصائيات بعد",
                    description = "ابدأ بطولات جديدة وأجرِ المباريات لتظهر إحصائيات اللاعبين وترتيبهم هنا تلقائياً!",
                    modifier = Modifier.padding(padding)
                )
            } else {
                val top3 = uiState.players.take(3)
                val rest = uiState.players.drop(3)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Sorting Filter Chips
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = LeaderboardSortBy.entries,
                                key = { it.name }
                            ) { sort ->
                                FilterChip(
                                    selected = uiState.sortBy == sort,
                                    onClick = { viewModel.onSortChange(sort) },
                                    label = { Text(sort.title, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Top 3 Podium
                    if (top3.isNotEmpty()) {
                        item {
                            PodiumSection(
                                topPlayers = top3,
                                sortBy = uiState.sortBy,
                                onPlayerClick = onNavigateToPlayer
                            )
                        }
                    }

                    item {
                        Text(
                            text = "ترتيب جميع اللاعبين (${uiState.players.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Full List
                    items(
                        items = if (top3.size < 3) uiState.players else rest,
                        key = { it.playerName }
                    ) { player ->
                        LeaderboardPlayerCard(
                            player = player,
                            sortBy = uiState.sortBy,
                            onClick = { onNavigateToPlayer(player.playerName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    topPlayers: List<PlayerLeaderboardItem>,
    sortBy: LeaderboardSortBy,
    onPlayerClick: (String) -> Unit
) {
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "منصة التتويج 👑",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place (Silver)
                if (topPlayers.size > 1) {
                    PodiumColumn(
                        player = topPlayers[1],
                        rank = 2,
                        podiumHeight = 70.dp,
                        color = com.rndm.app.core.theme.SilverMedalColor,
                        sortBy = sortBy,
                        onClick = { onPlayerClick(topPlayers[1].playerName) }
                    )
                }

                // 1st Place (Gold)
                if (topPlayers.isNotEmpty()) {
                    PodiumColumn(
                        player = topPlayers[0],
                        rank = 1,
                        podiumHeight = 96.dp,
                        color = com.rndm.app.core.theme.GoldMedalColor,
                        sortBy = sortBy,
                        onClick = { onPlayerClick(topPlayers[0].playerName) }
                    )
                }

                // 3rd Place (Bronze)
                if (topPlayers.size > 2) {
                    PodiumColumn(
                        player = topPlayers[2],
                        rank = 3,
                        podiumHeight = 54.dp,
                        color = com.rndm.app.core.theme.BronzeMedalColor,
                        sortBy = sortBy,
                        onClick = { onPlayerClick(topPlayers[2].playerName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    player: PlayerLeaderboardItem,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    color: Color,
    sortBy: LeaderboardSortBy,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!player.avatarIcon.isNullOrBlank()) {
                Text(text = player.avatarIcon, fontSize = 22.sp)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player.playerName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        val metricText = when (sortBy) {
            LeaderboardSortBy.TITLES -> "🏆 ${player.titlesCount}"
            LeaderboardSortBy.GOALS -> "⚽ ${player.goalsScored}"
            LeaderboardSortBy.WIN_RATE -> "${player.winRate.toInt()}%"
            LeaderboardSortBy.MATCHES -> "🎮 ${player.totalMatches}"
        }

        Text(
            text = metricText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Podium Block
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = color.copy(alpha = 0.25f),
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
private fun LeaderboardPlayerCard(
    player: PlayerLeaderboardItem,
    sortBy: LeaderboardSortBy,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank number
                Text(
                    text = "${player.rank}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (!player.avatarIcon.isNullOrBlank()) {
                        Text(text = player.avatarIcon, fontSize = 18.sp)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = player.playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "🏆 ${player.titlesCount} • ⚽ ${player.goalsScored} • 🎮 ${player.totalMatches} • 📈 ${player.winRate.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Highlight Metric
            val primaryMetric = when (sortBy) {
                LeaderboardSortBy.TITLES -> Pair("${player.titlesCount}", "ألقاب 🏆")
                LeaderboardSortBy.GOALS -> Pair("${player.goalsScored}", "أهداف ⚽")
                LeaderboardSortBy.WIN_RATE -> Pair("${player.winRate.toInt()}%", "فوز 📈")
                LeaderboardSortBy.MATCHES -> Pair("${player.totalMatches}", "مباريات 🎮")
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = primaryMetric.first,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = primaryMetric.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
