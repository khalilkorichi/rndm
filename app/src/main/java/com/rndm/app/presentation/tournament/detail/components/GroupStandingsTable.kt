package com.rndm.app.presentation.tournament.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.domain.model.GroupStanding

@Composable
fun GroupStandingsTable(
    standings: List<GroupStanding>,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing

    BentoCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.sm)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Text("المشارك", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("لعب", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("ف", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("ت", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("خ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("فارق", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("نقاط", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            standings.forEach { standing ->
                val rowBackground = if (standing.isQualified) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else if (standing.isPromotionCandidate) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBackground, shape = MaterialTheme.shapes.small)
                        .padding(vertical = spacing.xs, horizontal = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${standing.rank}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = standing.participant.playerName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        standing.participant.clubName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    Text("${standing.played}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    Text("${standing.won}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("${standing.drawn}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("${standing.lost}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    val diffSign = if (standing.goalDifference > 0) "+${standing.goalDifference}" else "${standing.goalDifference}"
                    Text(diffSign, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    Text("${standing.points}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
