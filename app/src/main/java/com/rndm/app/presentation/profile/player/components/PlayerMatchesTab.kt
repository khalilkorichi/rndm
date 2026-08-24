package com.rndm.app.presentation.profile.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.domain.model.MatchOutcome
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.PlayerMatchRecord

enum class MatchFilter(val title: String) {
    ALL("الكل"),
    WINS("الفوز فقط"),
    FINALS("النهائيات"),
    KNOCKOUT("الأدوار الإقصائية"),
    GROUPS("دور المجموعات")
}

@Composable
fun PlayerMatchesTab(
    playerName: String,
    matches: List<PlayerMatchRecord>,
    onTournamentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(MatchFilter.ALL) }

    val filteredMatches = remember(matches, selectedFilter) {
        matches.filter { match ->
            when (selectedFilter) {
                MatchFilter.ALL -> true
                MatchFilter.WINS -> match.outcome == MatchOutcome.WIN
                MatchFilter.FINALS -> match.stage == MatchStage.FINAL || match.stage == MatchStage.THIRD_PLACE
                MatchFilter.KNOCKOUT -> match.stage != MatchStage.GROUP_STAGE
                MatchFilter.GROUPS -> match.stage == MatchStage.GROUP_STAGE
            }
        }
    }

    if (matches.isEmpty()) {
        EmptyState(
            icon = painterResource(id = R.drawable.ic_fixtures),
            title = "لا توجد مباريات مسجلة",
            description = "لم يخض هذا اللاعب أي مباراة مكتملة بعد.",
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = MatchFilter.entries,
                    key = { it.name }
                ) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.title, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (filteredMatches.isEmpty()) {
                Text(
                    text = "لا توجد مباريات تطابق هذا الفلتر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                filteredMatches.forEach { match ->
                    val (outcomeBg, outcomeText, outcomeColor) = when (match.outcome) {
                        MatchOutcome.WIN -> Triple(com.rndm.app.core.theme.StatsSuccessGreen.copy(alpha = 0.15f), "فوز", com.rndm.app.core.theme.StatsSuccessGreen)
                        MatchOutcome.DRAW -> Triple(com.rndm.app.core.theme.StatsWarningAmber.copy(alpha = 0.15f), "تعادل", com.rndm.app.core.theme.StatsWarningAmber)
                        MatchOutcome.LOSS -> Triple(com.rndm.app.core.theme.StatsErrorRed.copy(alpha = 0.15f), "خسارة", com.rndm.app.core.theme.StatsErrorRed)
                    }

                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTournamentClick(match.tournamentId) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Match Stage & Tournament Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${match.tournamentName} • ${match.stage.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = outcomeBg
                                ) {
                                    Text(
                                        text = outcomeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = outcomeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Match Score Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player Side
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = playerName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    match.playerClub?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Score Badge
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = "${match.playerScore}  -  ${match.opponentScore}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (match.playerPenalty != null && match.opponentPenalty != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "(${match.playerPenalty} - ${match.opponentPenalty} ركلات)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Opponent Side
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = match.opponentName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                    match.opponentClub?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
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
    }
}
