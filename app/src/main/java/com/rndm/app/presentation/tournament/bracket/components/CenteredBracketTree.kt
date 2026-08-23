package com.rndm.app.presentation.tournament.bracket.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage

@Composable
fun CenteredBracketTree(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing
    val finalMatch = remember(matches) { matches.firstOrNull { it.stage == MatchStage.FINAL } }
    val thirdPlaceMatch = remember(matches) { matches.firstOrNull { it.stage == MatchStage.THIRD_PLACE } }
    val semiFinals = remember(matches) { matches.filter { it.stage == MatchStage.SEMI_FINALS } }
    val quarterFinals = remember(matches) { matches.filter { it.stage == MatchStage.QUARTER_FINALS } }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Final
        if (finalMatch != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tournament_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "المباراة النهائية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            KnockoutMatchCard(
                match = finalMatch,
                title = "النهائي",
                onClick = { onMatchClick(finalMatch) }
            )
        }

        // 3rd place match
        if (thirdPlaceMatch != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_medal),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "مباراة المركز الثالث",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            KnockoutMatchCard(
                match = thirdPlaceMatch,
                title = "تحديد المركز الثالث",
                onClick = { onMatchClick(thirdPlaceMatch) }
            )
        }

        // Semi-Finals
        if (semiFinals.isNotEmpty()) {
            Text(
                text = "نصف النهائي",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            semiFinals.forEach { match ->
                KnockoutMatchCard(
                    match = match,
                    title = "نصف النهائي",
                    onClick = { onMatchClick(match) }
                )
            }
        }

        // Quarter-Finals
        if (quarterFinals.isNotEmpty()) {
            Text(
                text = "ربع النهائي",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            quarterFinals.forEach { match ->
                KnockoutMatchCard(
                    match = match,
                    title = "ربع النهائي",
                    onClick = { onMatchClick(match) }
                )
            }
        }
    }
}
