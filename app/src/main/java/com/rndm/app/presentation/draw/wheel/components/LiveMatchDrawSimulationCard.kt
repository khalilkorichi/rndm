package com.rndm.app.presentation.draw.wheel.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Composable
fun LiveMatchDrawSimulationCard(
    category: DrawCategory,
    fixtures: List<DrawFixture>,
    remainingPlayersCount: Int,
    remainingClubsCount: Int,
    remainingTeamsCount: Int,
    modifier: Modifier = Modifier
) {
    val lastFixture = fixtures.lastOrNull()

    // Determine current simulation state
    when (category) {
        DrawCategory.PLAYERS -> {
            PlayerDrawSimulation(
                fixtures = fixtures,
                lastFixture = lastFixture,
                remainingPlayersCount = remainingPlayersCount,
                modifier = modifier
            )
        }
        DrawCategory.CLUBS, DrawCategory.NATIONAL_TEAMS -> {
            TeamDrawSimulation(
                category = category,
                fixtures = fixtures,
                remainingTeamsCount = if (category == DrawCategory.CLUBS) remainingClubsCount else remainingTeamsCount,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PlayerDrawSimulation(
    fixtures: List<DrawFixture>,
    lastFixture: DrawFixture?,
    remainingPlayersCount: Int,
    modifier: Modifier = Modifier
) {
    val isSlot1Filled = lastFixture != null && lastFixture.playerTwoName == null
    val currentMatchNumber = if (isSlot1Filled) lastFixture!!.matchNumber else fixtures.size + 1
    val isAllDrawn = remainingPlayersCount == 0 && (lastFixture == null || lastFixture.playerTwoName != null)

    val player1Name = if (isSlot1Filled) lastFixture!!.playerOneName else null

    // Pulse animation for waiting slot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Simulation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAllDrawn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = if (isAllDrawn) "اكتمل سحب المباريات" else "مباراة #$currentMatchNumber",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSlot1Filled) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = when {
                            isAllDrawn -> "المباريات جاهزة"
                            isSlot1Filled -> "سحب المنافس"
                            else -> "سحب اللاعب الأول"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSlot1Filled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Match Dual Slots Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slot 1 (Player 1)
                MatchParticipantSlot(
                    name = player1Name,
                    placeholder = "اللاعب الأول (؟)",
                    isFilled = isSlot1Filled,
                    isTargetSlot = !isSlot1Filled && !isAllDrawn,
                    pulseAlpha = pulseAlpha,
                    roleLabel = "الطرف 1",
                    modifier = Modifier.weight(1f)
                )

                // Center VS Pill
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Slot 2 (Player 2 / Rival)
                MatchParticipantSlot(
                    name = null,
                    placeholder = if (isSlot1Filled) "في انتظار المنافس (؟)" else "اللاعب الثاني (؟)",
                    isFilled = false,
                    isTargetSlot = isSlot1Filled && !isAllDrawn,
                    pulseAlpha = pulseAlpha,
                    roleLabel = "الطرف 2",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TeamDrawSimulation(
    category: DrawCategory,
    fixtures: List<DrawFixture>,
    remainingTeamsCount: Int,
    modifier: Modifier = Modifier
) {
    val target = getNextNeedingTeam(fixtures)
    val isClubs = category == DrawCategory.CLUBS
    val categoryTitle = if (isClubs) "النادي" else "المنتخب"

    val infiniteTransition = rememberInfiniteTransition(label = "teamPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (target == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary)
                    )
                    Text(
                        text = if (target == null) "تم تعيين $categoryTitle للمباريات"
                        else "تعيين $categoryTitle: ${target.playerName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (target == null) "مكتمل" else "سحب $categoryTitle",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (target != null) {
                // Show current target match being updated
                val fixture = target.fixture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player 1 Slot
                    TeamParticipantSlot(
                        playerName = fixture.playerOneName,
                        teamName = fixture.playerOneTeam,
                        isCurrentTarget = target.isPlayerOne,
                        isClubs = isClubs,
                        pulseAlpha = pulseAlpha,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Player 2 Slot
                    TeamParticipantSlot(
                        playerName = fixture.playerTwoName ?: "BYE",
                        teamName = fixture.playerTwoTeam,
                        isCurrentTarget = !target.isPlayerOne,
                        isClubs = isClubs,
                        pulseAlpha = pulseAlpha,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "كافة اللاعبين في جدول المباريات يمتلكون ${if (isClubs) "أندية" else "منتخبات"} معينة جاهزة للمنافسة!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchParticipantSlot(
    name: String?,
    placeholder: String,
    isFilled: Boolean,
    isTargetSlot: Boolean,
    pulseAlpha: Float,
    roleLabel: String,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isFilled -> MaterialTheme.colorScheme.primary
            isTargetSlot -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        label = "slotBorder"
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = if (isFilled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile_filled),
                    contentDescription = null,
                    tint = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = name,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "slotName"
            ) { targetName ->
                Text(
                    text = targetName ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isFilled) FontWeight.Bold else FontWeight.Normal,
                    color = if (isFilled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TeamParticipantSlot(
    playerName: String,
    teamName: String?,
    isCurrentTarget: Boolean,
    isClubs: Boolean,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val isTeamAssigned = teamName != null
    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrentTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = pulseAlpha)
            isTeamAssigned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        label = "teamSlotBorder"
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentTarget) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    isTeamAssigned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    isCurrentTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isClubs) R.drawable.ic_shield else R.drawable.ic_globe),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = when {
                            isTeamAssigned -> MaterialTheme.colorScheme.secondary
                            isCurrentTarget -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = teamName ?: if (isCurrentTarget) "جاري السحب..." else "في الانتظار",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isTeamAssigned) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isTeamAssigned -> MaterialTheme.colorScheme.secondary
                            isCurrentTarget -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class TargetPlayerInfo(
    val fixture: DrawFixture,
    val playerName: String,
    val isPlayerOne: Boolean,
    val matchLabel: String
)

private fun getNextNeedingTeam(fixtures: List<DrawFixture>): TargetPlayerInfo? {
    for (f in fixtures) {
        if (f.playerOneTeam == null) {
            return TargetPlayerInfo(f, f.playerOneName, true, "المباراة #${f.matchNumber}")
        }
        if (f.playerTwoName != null && f.playerTwoTeam == null) {
            return TargetPlayerInfo(f, f.playerTwoName, false, "المباراة #${f.matchNumber}")
        }
    }
    return null
}
