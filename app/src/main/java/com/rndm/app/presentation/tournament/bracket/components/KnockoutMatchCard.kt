package com.rndm.app.presentation.tournament.bracket.components

import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.MatchScoreBadge
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus

@Composable
fun KnockoutMatchCard(
    match: Match,
    title: String,
    onClick: () -> Unit,
    onDirectQualifyClick: ((match: Match, isUndo: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing
    val isFinished = match.status == MatchStatus.FINISHED
    val isDirectlyQualified = isFinished && match.scoreOne == null && match.scoreTwo == null && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL

    val isEligibleForDirectQualify = !isFinished && match.stage != MatchStage.GROUP_STAGE && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL && (
        (match.isPlayerTwoLuckyLoser || match.playerTwoName == "أحسن خاسر" || match.playerTwoName.isNullOrBlank() || match.playerTwoName == "BYE") &&
        match.playerOneName.isNotBlank() && match.playerOneName != "TBD" && !match.playerOneName.startsWith("فائز ") ||
        (match.isPlayerOneLuckyLoser || match.playerOneName == "أحسن خاسر" || match.playerOneName.isBlank() || match.playerOneName == "BYE") &&
        !match.playerTwoName.isNullOrBlank() && match.playerTwoName != "TBD" && !match.playerTwoName.startsWith("فائز ")
    )

    val borderModifier = if (isFinished) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
    } else Modifier

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (isEligibleForDirectQualify && onDirectQualifyClick != null) {
                    Surface(
                        onClick = { onDirectQualifyClick(match, false) },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_rocket),
                                contentDescription = "تأهيل مباشر",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "تأهيل مباشر",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (isDirectlyQualified && onDirectQualifyClick != null) {
                    Surface(
                        onClick = { onDirectQualifyClick(match, true) },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_redo),
                                contentDescription = "تراجع عن التأهيل",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "تراجع",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Box (Centered)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isP1Winner = isFinished && match.winnerName == match.playerOneName
                    Text(
                        text = match.playerOneName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Normal,
                        color = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    match.playerOneClub?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                // Score or VS (Centered)
                if (isDirectlyQualified) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "تأهل مباشر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isFinished && match.scoreOne != null && match.scoreTwo != null) {
                    MatchScoreBadge(
                        scoreOne = match.scoreOne,
                        scoreTwo = match.scoreTwo,
                        penaltyScoreOne = match.penaltyScoreOne,
                        penaltyScoreTwo = match.penaltyScoreTwo,
                        isExtraTime = match.isExtraTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                // Player 2 Box (Centered)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isP2Winner = isFinished && match.winnerName == match.playerTwoName
                    Text(
                        text = match.playerTwoName ?: "TBD",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Normal,
                        color = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    match.playerTwoClub?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

