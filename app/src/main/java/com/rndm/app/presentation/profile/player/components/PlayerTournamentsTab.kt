package com.rndm.app.presentation.profile.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.domain.model.PlayerTournamentParticipation
import com.rndm.app.domain.model.StageReachedType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerTournamentsTab(
    tournaments: List<PlayerTournamentParticipation>,
    onTournamentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tournaments.isEmpty()) {
        EmptyState(
            icon = painterResource(id = R.drawable.ic_tournament_outlined),
            title = "لا توجد بطولات مسجلة",
            description = "لم يشارك هذا اللاعب في أي بطولة بعد. ابدأ بطولة جديدة وأضف اللاعب للمنافسة!",
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }

            tournaments.forEach { item ->
                val (badgeColor, badgeIcon) = when (item.stageReachedType) {
                    StageReachedType.CHAMPION -> Pair(com.rndm.app.core.theme.GoldMedalColor, R.drawable.ic_trophy)
                    StageReachedType.RUNNER_UP -> Pair(com.rndm.app.core.theme.SilverMedalColor, R.drawable.ic_medal)
                    StageReachedType.THIRD_PLACE -> Pair(com.rndm.app.core.theme.BronzeMedalColor, R.drawable.ic_medal)
                    StageReachedType.SEMI_FINALS -> Pair(MaterialTheme.colorScheme.primary, R.drawable.ic_tournament_filled)
                    StageReachedType.QUARTER_FINALS -> Pair(MaterialTheme.colorScheme.tertiary, R.drawable.ic_tournament_filled)
                    StageReachedType.ROUND_OF_16 -> Pair(MaterialTheme.colorScheme.secondary, R.drawable.ic_tournament_filled)
                    StageReachedType.ROUND_OF_32 -> Pair(MaterialTheme.colorScheme.secondary, R.drawable.ic_tournament_filled)
                    StageReachedType.GROUPS_STAGE -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_fixtures)
                    StageReachedType.PARTICIPANT -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_person)
                }

                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTournamentClick(item.tournamentId) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.tournamentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormat.format(Date(item.tournamentDate)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Stage Reached Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = badgeIcon),
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = item.stageReachedTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }

                        // Club played with
                        item.clubName?.let { club ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "لعب بنادي: $club",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Performance Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مباريات: ${item.matchesPlayed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ف ${item.matchesWon} • ت ${item.matchesDrawn} • خ ${item.matchesLost}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أهداف: ${item.goalsFor} - ${item.goalsAgainst}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
