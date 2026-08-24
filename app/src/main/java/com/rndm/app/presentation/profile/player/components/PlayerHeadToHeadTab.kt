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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.domain.model.PlayerHeadToHead

@Composable
fun PlayerHeadToHeadTab(
    headToHeadList: List<PlayerHeadToHead>,
    onOpponentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (headToHeadList.isEmpty()) {
        EmptyState(
            icon = painterResource(id = R.drawable.ic_swords),
            title = "لا توجد مواجهات مباشرة",
            description = "لم يواجه هذا اللاعب أي خصم في المباريات المكتملة بعد.",
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            headToHeadList.forEach { h2h ->
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpponentClick(h2h.opponentName) }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!h2h.opponentAvatar.isNullOrBlank()) {
                                        Text(text = h2h.opponentAvatar, fontSize = 18.sp)
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_person),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = h2h.opponentName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    h2h.opponentNickname?.let { nick ->
                                        Text(
                                            text = "«$nick»",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            // Win Rate Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (h2h.winRate >= 50f) com.rndm.app.core.theme.StatsSuccessGreen.copy(alpha = 0.15f) else com.rndm.app.core.theme.StatsErrorRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "نسبة الفوز: ${h2h.winRate.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (h2h.winRate >= 50f) com.rndm.app.core.theme.StatsSuccessGreen else com.rndm.app.core.theme.StatsErrorRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Detailed Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مواجهات: ${h2h.matchesPlayed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ف ${h2h.wins} • ت ${h2h.draws} • خ ${h2h.losses}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أهداف: ${h2h.goalsScored} - ${h2h.goalsConceded}",
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
