package com.rndm.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Standardized score badge that displays match scores accurately aligned with player positions in both RTL and LTR layouts.
 *
 * In RTL layout:
 * - [penaltyScoreOne] (if present) appears on the far right.
 * - [scoreOne] appears on the right (adjacent to Player 1 on the right).
 * - " - " appears in the center.
 * - [scoreTwo] appears on the left (adjacent to Player 2 on the left).
 * - [penaltyScoreTwo] (if present) appears on the far left.
 *
 * In LTR layout:
 * - [penaltyScoreOne] (if present) appears on the far left.
 * - [scoreOne] appears on the left (adjacent to Player 1 on the left).
 * - " - " appears in the center.
 * - [scoreTwo] appears on the right (adjacent to Player 2 on the right).
 * - [penaltyScoreTwo] (if present) appears on the far right.
 */
@Composable
fun MatchScoreBadge(
    scoreOne: Int,
    scoreTwo: Int,
    penaltyScoreOne: Int? = null,
    penaltyScoreTwo: Int? = null,
    isExtraTime: Boolean = false,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
) {
    val hasPenalties = penaltyScoreOne != null && penaltyScoreTwo != null
    val penaltyFontSize = if (style.fontSize.isSpecified) (style.fontSize.value * 0.82f).sp else 12.sp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (hasPenalties) {
            Text(
                text = "($penaltyScoreOne)",
                style = style.copy(
                    fontSize = penaltyFontSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(3.dp))
        }

        Text(
            text = scoreOne.toString(),
            style = style
        )

        Text(
            text = " - ",
            style = style
        )

        Text(
            text = scoreTwo.toString(),
            style = style
        )

        if (hasPenalties) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "($penaltyScoreTwo)",
                style = style.copy(
                    fontSize = penaltyFontSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        } else if (isExtraTime) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(و.إ)",
                style = style.copy(
                    fontSize = penaltyFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}
