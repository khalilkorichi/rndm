package com.rndm.app.presentation.tournament.bracket.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.LtrForcedText
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import kotlinx.coroutines.launch

@Composable
fun GoogleKnockoutBracketView(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onDirectQualifyClick: ((match: Match, isUndo: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Group matches into stages in chronological progression
    val stageOrder = listOf(
        MatchStage.ROUND_OF_64,
        MatchStage.ROUND_OF_32,
        MatchStage.ROUND_OF_16,
        MatchStage.QUARTER_FINALS,
        MatchStage.SEMI_FINALS,
        MatchStage.FINAL
    )

    val activeStages = remember(matches) {
        stageOrder.filter { stage -> matches.any { it.stage == stage } }
    }

    val thirdPlaceMatch = remember(matches) {
        matches.firstOrNull { it.stage == MatchStage.THIRD_PLACE }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeStages.isEmpty()) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tournament_outlined),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "الأدوار الإقصائية غير جاهزة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ستظهر شجرة خروج المغلوب فور اكتمال دور المجموعات وتوليد الأدوار الإقصائية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Scrollable Bracket Container with Round Columns
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Stage Names Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeStages.forEach { stage ->
                            Box(
                                modifier = Modifier.width(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stage.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    )

                    // Bracket Columns with Connectors
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeStages.forEachIndexed { stageIndex, stage ->
                            val stageMatches = remember(matches, stage) {
                                matches.filter { it.stage == stage }
                                    .sortedBy { it.bracketMatchIndex ?: 0 }
                            }

                            Column(
                                modifier = Modifier.width(220.dp),
                                verticalArrangement = Arrangement.SpaceAround,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                stageMatches.forEachIndexed { matchIndex, match ->
                                    GoogleMatchCard(
                                        match = match,
                                        onClick = { onMatchClick(match) },
                                        onPlayerClick = onPlayerClick,
                                        onDirectQualifyClick = onDirectQualifyClick
                                    )
                                    if (matchIndex < stageMatches.size - 1) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3rd Place Match Section (if present)
            if (thirdPlaceMatch != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_medal),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "مباراة تحديد المركز الثالث",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        GoogleMatchCard(
                            match = thirdPlaceMatch,
                            onClick = { onMatchClick(thirdPlaceMatch) },
                            onPlayerClick = onPlayerClick,
                            onDirectQualifyClick = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMatchCard(
    match: Match,
    onClick: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onDirectQualifyClick: ((match: Match, isUndo: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFinished = match.status == MatchStatus.FINISHED
    val isP1Winner = isFinished && match.winnerName == match.playerOneName
    val isP2Winner = isFinished && match.winnerName == match.playerTwoName
    val hasPenalties = match.penaltyScoreOne != null && match.penaltyScoreTwo != null
    val isDirectlyQualified = isFinished && match.scoreOne == null && match.scoreTwo == null && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL

    val isEligibleForDirectQualify = !isFinished && match.stage != MatchStage.GROUP_STAGE && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL && (
        (match.isPlayerTwoLuckyLoser || match.playerTwoName == "أحسن خاسر" || match.playerTwoName.isNullOrBlank() || match.playerTwoName == "BYE") &&
        match.playerOneName.isNotBlank() && match.playerOneName != "TBD" && !match.playerOneName.startsWith("فائز ") ||
        (match.isPlayerOneLuckyLoser || match.playerOneName == "أحسن خاسر" || match.playerOneName.isBlank() || match.playerOneName == "BYE") &&
        !match.playerTwoName.isNullOrBlank() && match.playerTwoName != "TBD" && !match.playerTwoName.startsWith("فائز ")
    )

    val statusBadgeText = when {
        isDirectlyQualified -> "تأهل مباشر"
        isFinished && hasPenalties -> "النهائية (ض.ج)"
        isFinished && match.isExtraTime -> "النهائية (و.إ)"
        isFinished -> "النهائية"
        else -> "لم تبدأ"
    }

    Surface(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFinished) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Card Header: Status on Left, Date/Round on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isFinished) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFinished) (if (isDirectlyQualified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (isEligibleForDirectQualify && onDirectQualifyClick != null) {
                        Surface(
                            onClick = { onDirectQualifyClick(match, false) },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_rocket),
                                    contentDescription = "تأهيل مباشر",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "تأهيل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (isDirectlyQualified && onDirectQualifyClick != null) {
                        Surface(
                            onClick = { onDirectQualifyClick(match, true) },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_redo),
                                    contentDescription = "تراجع عن التأهيل",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
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

                val matchNumber = (match.bracketMatchIndex?.takeIf { it > 0 }) ?: 1
                Text(
                    text = "مواجهة $matchNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Competitor 1 Row
            CompetitorRow(
                playerName = match.playerOneName,
                clubName = match.playerOneClub,
                score = match.scoreOne,
                penaltyScore = match.penaltyScoreOne,
                isWinner = isP1Winner,
                isFinished = isFinished,
                isLuckyLoser = match.isPlayerOneLuckyLoser,
                onPlayerClick = onPlayerClick
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Competitor 2 Row
            CompetitorRow(
                playerName = match.playerTwoName ?: "TBD",
                clubName = match.playerTwoClub,
                score = match.scoreTwo,
                penaltyScore = match.penaltyScoreTwo,
                isWinner = isP2Winner,
                isFinished = isFinished,
                isLuckyLoser = match.isPlayerTwoLuckyLoser,
                onPlayerClick = onPlayerClick
            )
        }
    }
}

@Composable
private fun CompetitorRow(
    playerName: String,
    clubName: String?,
    score: Int?,
    penaltyScore: Int?,
    isWinner: Boolean,
    isFinished: Boolean,
    isLuckyLoser: Boolean = false,
    onPlayerClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Winner Indicator & Score
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isWinner) {
                Text(
                    text = "◀",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            if (isFinished && score != null) {
                if (penaltyScore != null) {
                    LtrForcedText(
                        text = "($penaltyScore) $score",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    LtrForcedText(
                        text = "$score",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Right side: Player Name, Lucky Loser Badge & Avatar
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (isLuckyLoser) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "أحسن خاسر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .then(
                        if (onPlayerClick != null && playerName != "TBD" && playerName != "BYE") {
                            Modifier.clickable { onPlayerClick(playerName) }
                        } else Modifier
                    )
            ) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                        color = if (isWinner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                clubName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isWinner) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile_filled),
                    contentDescription = null,
                    tint = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
