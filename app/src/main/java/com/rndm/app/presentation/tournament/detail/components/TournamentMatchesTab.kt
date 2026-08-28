package com.rndm.app.presentation.tournament.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.LtrForcedText
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus

@Composable
fun TournamentMatchesTab(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onReorderMatchClick: ((Match) -> Unit)? = null,
    onSwapPlayerClick: ((match: Match, isSlotOne: Boolean) -> Unit)? = null,
    onDirectQualifyClick: ((match: Match, isUndo: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing
    var selectedStageFilter by remember { mutableStateOf<MatchStage?>(null) }

    val distinctStages = remember(matches) {
        matches.map { it.stage }.distinct().sortedBy { it.ordinal }
    }

    val filteredMatches = remember(matches, selectedStageFilter) {
        if (selectedStageFilter == null) matches else matches.filter { it.stage == selectedStageFilter }
    }

    val groupedMatches = remember(filteredMatches) {
        filteredMatches.groupBy { it.stage }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stage Filter Chips Row
        if (distinctStages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "الكل" Chip
                FilterChip(
                    selected = selectedStageFilter == null,
                    onClick = { selectedStageFilter = null },
                    label = { Text("جميع المباريات (${matches.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                distinctStages.forEach { stage ->
                    val stageCount = matches.count { it.stage == stage }
                    FilterChip(
                        selected = selectedStageFilter == stage,
                        onClick = { selectedStageFilter = stage },
                        label = { Text("${stage.displayName} ($stageCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (filteredMatches.isEmpty()) {
            EmptyState(
                icon = painterResource(id = R.drawable.ic_fixtures),
                title = "لا توجد مباريات",
                description = "لم يتم العثور على مباريات في هذا التصنيف حالياً.",
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            groupedMatches.forEach { (stage, stageMatches) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stage Header Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stage.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${stageMatches.count { it.status == MatchStatus.FINISHED }} / ${stageMatches.size} مكتملة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    stageMatches.forEach { match ->
                        MatchFixtureCard(
                            match = match,
                            onClick = { onMatchClick(match) },
                            onPlayerClick = onPlayerClick,
                            onReorderMatchClick = onReorderMatchClick,
                            onSwapPlayerClick = onSwapPlayerClick,
                            onDirectQualifyClick = onDirectQualifyClick
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MatchFixtureCard(
    match: Match,
    onClick: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onReorderMatchClick: ((Match) -> Unit)? = null,
    onSwapPlayerClick: ((match: Match, isSlotOne: Boolean) -> Unit)? = null,
    onDirectQualifyClick: ((match: Match, isUndo: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFinished = match.status == MatchStatus.FINISHED
    val isP1Winner = isFinished && match.winnerName == match.playerOneName
    val isP2Winner = isFinished && match.winnerName == match.playerTwoName
    val hasPenalties = match.penaltyScoreOne != null && match.penaltyScoreTwo != null

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Stage, Match Index, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerLabel = if (match.bracketMatchIndex != null) {
                    "${match.stage.displayName} - مباراة ${match.bracketMatchIndex}"
                } else if (match.groupIndex != null) {
                    "المجموعة ${('A' + match.groupIndex)} - جولة ${match.roundIndex}"
                } else {
                    "${match.stage.displayName} - جولة ${match.roundIndex}"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = headerLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    val isEligibleForDirectQualify = !isFinished && match.stage != MatchStage.GROUP_STAGE && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL && (
                        (match.isPlayerTwoLuckyLoser || match.playerTwoName == "أحسن خاسر" || match.playerTwoName.isNullOrBlank() || match.playerTwoName == "BYE") &&
                        match.playerOneName.isNotBlank() && match.playerOneName != "TBD" && !match.playerOneName.startsWith("فائز ") ||
                        (match.isPlayerOneLuckyLoser || match.playerOneName == "أحسن خاسر" || match.playerOneName.isBlank() || match.playerOneName == "BYE") &&
                        !match.playerTwoName.isNullOrBlank() && match.playerTwoName != "TBD" && !match.playerTwoName.startsWith("فائز ")
                    )

                    val isDirectlyQualified = isFinished && match.scoreOne == null && match.scoreTwo == null && match.stage != MatchStage.THIRD_PLACE && match.stage != MatchStage.FINAL

                    if (isEligibleForDirectQualify && onDirectQualifyClick != null) {
                        Surface(
                            onClick = { onDirectQualifyClick(match, false) },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_redo),
                                    contentDescription = "تراجع عن التأهيل",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "تراجع عن التأهيل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (onReorderMatchClick != null && !isFinished) {
                        Surface(
                            onClick = { onReorderMatchClick(match) },
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
                                    modifier = Modifier.size(11.dp)
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

                if (isFinished) {
                    val isDirectlyQualified = match.scoreOne == null && match.scoreTwo == null
                    val statusText = when {
                        isDirectlyQualified -> "تأهل مباشر"
                        hasPenalties -> "انتهت (ض.ج)"
                        match.isExtraTime -> "انتهت (و.إ)"
                        else -> "انتهت"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDirectlyQualified) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDirectlyQualified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "تسجيل النتيجة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teams and Score Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player One
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onPlayerClick != null) {
                                Modifier.clickable {
                                    onPlayerClick(match.playerOneName)
                                }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isP1Winner) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile_filled),
                            contentDescription = null,
                            tint = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = match.playerOneName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Medium,
                                color = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onSwapPlayerClick != null && !isFinished) {
                                Surface(
                                    onClick = { onSwapPlayerClick(match, true) },
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
                        match.playerOneClub?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Score / VS Center Display
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFinished && match.scoreOne != null && match.scoreTwo != null) {
                        com.rndm.app.core.ui.components.MatchScoreBadge(
                            scoreOne = match.scoreOne,
                            scoreTwo = match.scoreTwo,
                            penaltyScoreOne = match.penaltyScoreOne,
                            penaltyScoreTwo = match.penaltyScoreTwo,
                            isExtraTime = match.isExtraTime,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Player Two
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onPlayerClick != null && match.playerTwoName != null && !match.isPlayerTwoLuckyLoser) {
                                Modifier.clickable {
                                    match.playerTwoName?.let { p2 ->
                                        onPlayerClick(p2)
                                    }
                                }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (onSwapPlayerClick != null && match.playerTwoName != null && !isFinished && !match.isPlayerTwoLuckyLoser) {
                                Surface(
                                    onClick = { onSwapPlayerClick(match, false) },
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
                            Text(
                                text = match.playerTwoName ?: "BYE",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Medium,
                                color = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                        match.playerTwoClub?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isP2Winner) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile_filled),
                            contentDescription = null,
                            tint = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
