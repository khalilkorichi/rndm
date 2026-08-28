package com.rndm.app.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.MatchScoreBadge
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.Tournament
import kotlinx.coroutines.launch

@Composable
fun ActiveTournamentMatchesCard(
    tournament: Tournament,
    matches: List<Match>,
    currentMatchIndex: Int,
    onMatchClick: (Match) -> Unit,
    onNavigateToTournamentDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val initialPage = currentMatchIndex.coerceIn(0, matches.size - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { matches.size })

    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex in 0 until matches.size && pagerState.currentPage != currentMatchIndex) {
            pagerState.animateScrollToPage(currentMatchIndex)
        }
    }

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Tournament Title, Live Badge, and Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToTournamentDetail(tournament.id) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tournament_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Live Pulse Tag
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "نشطة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Match Index Counter
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${matches.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Swipeable HorizontalPager for Matches
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val match = matches.getOrNull(page)
                if (match != null) {
                    val isFinished = match.status == MatchStatus.FINISHED
                    val isCurrent = page == currentMatchIndex && !isFinished
                    val hasPenalties = match.penaltyScoreOne != null && match.penaltyScoreTwo != null

                    val isP1Winner = isFinished && match.winnerName == match.playerOneName
                    val isP2Winner = isFinished && match.winnerName == match.playerTwoName

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onMatchClick(match) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            // Match Sub-Header: Role badge & Stage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Match State Role Badge
                                when {
                                    isCurrent -> {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "المباراة الحالية",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    isFinished -> {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (hasPenalties) "منتهية (ض.ج)" else "منتهية",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {
                                            Text(
                                                text = "قادمة",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val stageTitle = when {
                                    match.groupIndex != null -> "المجموعة ${('أ'.code + match.groupIndex).toChar()} • ج ${match.roundIndex}"
                                    else -> match.stage.displayName
                                }
                                Text(
                                    text = stageTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            // Competitors & Score Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player 1 Box
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isP1Winner) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_profile_filled),
                                            contentDescription = null,
                                            tint = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = match.playerOneName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        match.playerOneClub?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Center Score Box / VS
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 3.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFinished && match.scoreOne != null && match.scoreTwo != null) {
                                        MatchScoreBadge(
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
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Player 2 Box
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = match.playerTwoName ?: "BYE",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.End
                                        )
                                        match.playerTwoClub?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isP2Winner) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_profile_filled),
                                            contentDescription = null,
                                            tint = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Carousel Controls & Indicators (Prev Arrow, Dots, Next Arrow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Match Button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(enabled = pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visiblePageWindow = (pagerState.currentPage - 2..pagerState.currentPage + 2)
                    matches.indices.forEach { index ->
                        if (index in visiblePageWindow || index == 0 || index == matches.size - 1) {
                            val isSelected = index == pagerState.currentPage
                            val isCurrentRole = index == currentMatchIndex
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isCurrentRole -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                },
                                label = "dotColor"
                            )
                            val dotWidth = if (isSelected) 14.dp else 4.dp

                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }

                // Next Match Button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(enabled = pagerState.currentPage < matches.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pagerState.currentPage < matches.size - 1) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
