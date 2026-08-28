package com.rndm.app.presentation.draw.fixtures.components

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
import androidx.compose.foundation.shape.CircleShape
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
import com.rndm.app.domain.model.DrawFixture

@Composable
fun FixtureMatchCard(
    fixture: DrawFixture,
    onEditScoreClick: () -> Unit,
    onReorderClick: (() -> Unit)? = null,
    onSwapPlayerClick: ((isSlotOne: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditScoreClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "المباراة ${fixture.matchNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                        )
                    }

                    if (onReorderClick != null && !fixture.isFinished) {
                        Surface(
                            onClick = onReorderClick,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_swap),
                                    contentDescription = "ترتيب المباراة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ترتيب",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                val statusText = when {
                    fixture.isFinished -> "منتهية"
                    fixture.playerTwoName == null -> "بانتظار سحب الخصم"
                    !fixture.isTeamsAssigned -> "بانتظار سحب الفرق"
                    else -> "جاهزة للعب"
                }

                val statusColor = when {
                    fixture.isFinished -> MaterialTheme.colorScheme.primary
                    fixture.playerTwoName == null -> MaterialTheme.colorScheme.error
                    !fixture.isTeamsAssigned -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Teams / Players Row (365Scores Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = fixture.playerOneName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (fixture.winnerName == fixture.playerOneName) FontWeight.Bold else FontWeight.Medium,
                            color = if (fixture.winnerName == fixture.playerOneName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onSwapPlayerClick != null && !fixture.isFinished) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                onClick = { onSwapPlayerClick(true) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_swap),
                                    contentDescription = "تبديل مكان اللاعب",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xs))
                    if (fixture.playerOneTeam != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fixture.playerOneTeam,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "بانتظار الفريق",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Center Score Box
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.sm)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    if (fixture.isFinished && fixture.scoreOne != null && fixture.scoreTwo != null) {
                        MatchScoreBadge(
                            scoreOne = fixture.scoreOne,
                            scoreTwo = fixture.scoreTwo,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Player 2
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = fixture.playerTwoName ?: "في انتظار السحب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (fixture.winnerName == fixture.playerTwoName) FontWeight.Bold else FontWeight.Medium,
                            color = if (fixture.winnerName == fixture.playerTwoName) MaterialTheme.colorScheme.primary else if (fixture.playerTwoName == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onSwapPlayerClick != null && fixture.playerTwoName != null && !fixture.isFinished) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                onClick = { onSwapPlayerClick(false) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_swap),
                                    contentDescription = "تبديل مكان اللاعب",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xs))
                    if (fixture.playerTwoTeam != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fixture.playerTwoTeam,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "بانتظار الفريق",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
