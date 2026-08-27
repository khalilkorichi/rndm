package com.rndm.app.presentation.tournament.list.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.LtrForcedText
import com.rndm.app.core.ui.components.PlayerAvatar
import com.rndm.app.domain.model.LiveTournamentPreview
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentType

private enum class PreviewTab(val title: String) {
    MATCHES("جدول المباريات"),
    PARTICIPANTS("المشاركون")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTournamentPreviewSheet(
    preview: LiveTournamentPreview?,
    isLoading: Boolean,
    isJoining: Boolean,
    onJoinClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(PreviewTab.MATCHES) }
    var copiedCode by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.fillMaxHeight(0.85f)
    ) {
        if (isLoading || preview == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val tournament = preview.tournament
            val participants = preview.participants
            val matches = preview.matches

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tournament.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Type badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = if (tournament.type == TournamentType.GROUPS_KNOCKOUT) "مجموعات" else "إقصائيات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "عدد المشاركين: ${participants.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "المباريات: ${matches.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Share Code Capsule with Copy
                    if (!tournament.shareCode.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            ),
                            onClick = {
                                clipboardManager.setText(AnnotatedString(tournament.shareCode!!))
                                copiedCode = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(if (copiedCode) R.drawable.ic_check else R.drawable.ic_copy),
                                    contentDescription = "نسخ الكود",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                LtrForcedText(
                                    text = tournament.shareCode!!,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Capsule Tab Selector
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PreviewTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        PreviewTab.MATCHES -> {
                            if (matches.isEmpty()) {
                                EmptyState(
                                    icon = painterResource(id = R.drawable.ic_gamepad),
                                    title = "لا توجد مباريات بعد",
                                    description = "لم يتم جدولة مباريات لهذه البطولة بعد",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = matches,
                                        key = { it.id }
                                    ) { match ->
                                        PreviewMatchCard(match = match)
                                    }
                                }
                            }
                        }
                        PreviewTab.PARTICIPANTS -> {
                            if (participants.isEmpty()) {
                                EmptyState(
                                    icon = painterResource(id = R.drawable.ic_person),
                                    title = "لا يوجد مشاركون",
                                    description = "لم يتم إضافة أي لاعب لهذه البطولة",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val grouped = remember(participants) { participants.groupBy { it.groupIndex } }
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (grouped.size > 1 && tournament.type == TournamentType.GROUPS_KNOCKOUT) {
                                        grouped.forEach { (groupIndex, groupMembers) ->
                                            item(key = "group_$groupIndex") {
                                                Text(
                                                    text = "المجموعة ${('A' + groupIndex)}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                            items(
                                                items = groupMembers,
                                                key = { it.id }
                                            ) { participant ->
                                                PreviewParticipantCard(participant = participant)
                                            }
                                        }
                                    } else {
                                        items(
                                            items = participants,
                                            key = { it.id }
                                        ) { participant ->
                                            PreviewParticipantCard(participant = participant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action: Join Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Button(
                        onClick = onJoinClick,
                        enabled = !isJoining,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "انضمام إلى هذه البطولة الآن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMatchCard(
    match: Match,
    modifier: Modifier = Modifier
) {
    val isCompleted = match.status == MatchStatus.FINISHED
    val isInProgress = match.status == MatchStatus.PLAYING

    BentoCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player 1
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = match.playerOneName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (match.winnerName == match.playerOneName) FontWeight.Bold else FontWeight.Medium,
                    color = if (match.winnerName == match.playerOneName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (!match.playerOneClub.isNullOrBlank()) {
                    Text(
                        text = match.playerOneClub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Score or VS badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.primaryContainer
                            isInProgress -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (isCompleted) {
                    Text(
                        text = "${match.scoreOne ?: 0} - ${match.scoreTwo ?: 0}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (isInProgress) {
                    Text(
                        text = "مباشر",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
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

            // Player 2
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = match.playerTwoName ?: "بانتظار المتأهل",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (match.winnerName == match.playerTwoName) FontWeight.Bold else FontWeight.Medium,
                    color = if (match.winnerName == match.playerTwoName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
                if (!match.playerTwoClub.isNullOrBlank()) {
                    Text(
                        text = match.playerTwoClub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewParticipantCard(
    participant: TournamentParticipant,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(
                avatarIcon = null,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!participant.clubName.isNullOrBlank()) {
                    Text(
                        text = participant.clubName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
