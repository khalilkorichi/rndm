package com.rndm.app.presentation.tournament.list.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rndm.app.R
import com.rndm.app.core.theme.*
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

@Composable
fun LiveTournamentPreviewSheet(
    preview: LiveTournamentPreview?,
    isLoading: Boolean,
    isJoining: Boolean,
    onJoinClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    var selectedTab by remember { mutableStateOf(PreviewTab.MATCHES) }
    var copiedCode by remember { mutableStateOf(false) }
    var matchesHeightDp by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = screenHeight * 0.82f)
                    .shadow(
                        elevation = if (isDark) 24.dp else 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.7f) else PrimaryLight.copy(alpha = 0.2f),
                        ambientColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.5f) else PrimaryLight.copy(alpha = 0.1f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    BottomBarDarkBgTop.copy(alpha = 0.98f),
                                    BottomBarDarkBgBottom.copy(alpha = 0.96f)
                                )
                            } else {
                                listOf(
                                    BottomBarLightBgTop.copy(alpha = 0.98f),
                                    BottomBarLightBgBottom.copy(alpha = 0.94f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    BottomBarDarkBorderTop.copy(alpha = 0.9f),
                                    BottomBarDarkBorderBottom.copy(alpha = 0.4f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    BottomBarLightBorderBottom.copy(alpha = 0.65f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                if (isLoading || preview == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header Info with Close Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tournament.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "المشاركون: ${participants.size}",
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

                            // Share Code & Close Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!tournament.shareCode.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(if (copiedCode) R.drawable.ic_check else R.drawable.ic_copy),
                                                contentDescription = "نسخ الكود",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            LtrForcedText(
                                                text = tournament.shareCode!!,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = onDismiss,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Capsule Tab Selector (Styled like RndmBottomBar)
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) BottomBarDarkBgBottom.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) BottomBarDarkBorderTop.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PreviewTab.entries.forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) {
                                                    if (isDark) BottomBarSelectedDarkBgTop else MaterialTheme.colorScheme.surface
                                                } else Color.Transparent
                                            )
                                            .clickable { selectedTab = tab }
                                            .padding(vertical = 7.dp)
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

                        // Scrollable Tab Content (LazyColumn inside dynamic weight)
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                        ) {
                            when (selectedTab) {
                                PreviewTab.MATCHES -> {
                                    if (matches.isEmpty()) {
                                        EmptyState(
                                            icon = painterResource(id = R.drawable.ic_gamepad),
                                            title = "لا توجد مباريات بعد",
                                            description = "لم يتم جدولة مباريات لهذه البطولة بعد",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp)
                                                .onGloballyPositioned { coords ->
                                                    if (coords.size.height > 0) {
                                                        matchesHeightDp = with(density) { coords.size.height.toDp() }
                                                    }
                                                }
                                        )
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    if (coords.size.height > 0) {
                                                        matchesHeightDp = with(density) { coords.size.height.toDp() }
                                                    }
                                                }
                                        ) {
                                            itemsIndexed(
                                                items = matches,
                                                key = { index, match ->
                                                    val rId = match.remoteId
                                                    if (!rId.isNullOrBlank()) rId else "match_${index}_${match.playerOneName}_${match.playerTwoName}_${match.roundIndex}"
                                                }
                                            ) { _, match ->
                                                PreviewMatchCard(match = match)
                                            }
                                        }
                                    }
                                }
                                PreviewTab.PARTICIPANTS -> {
                                    val participantModifier = if (matchesHeightDp != null) {
                                        Modifier
                                            .fillMaxWidth()
                                            .height(matchesHeightDp!!)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }

                                    if (participants.isEmpty()) {
                                        EmptyState(
                                            icon = painterResource(id = R.drawable.ic_person),
                                            title = "لا يوجد مشاركون",
                                            description = "لم يتم إضافة أي لاعب لهذه البطولة",
                                            modifier = participantModifier.padding(vertical = 24.dp)
                                        )
                                    } else {
                                        val grouped = remember(participants) { participants.groupBy { it.groupIndex } }
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            modifier = participantModifier
                                        ) {
                                            if (grouped.size > 1 && tournament.type == TournamentType.GROUPS_KNOCKOUT) {
                                                grouped.forEach { (groupIndex, groupMembers) ->
                                                    item(key = "group_$groupIndex") {
                                                        Text(
                                                            text = "المجموعة ${('A' + groupIndex)}",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                                        )
                                                    }
                                                    itemsIndexed(
                                                        items = groupMembers,
                                                        key = { index, participant ->
                                                            val rId = participant.remoteId
                                                            if (!rId.isNullOrBlank()) rId else "part_${groupIndex}_${index}_${participant.playerName}"
                                                        }
                                                    ) { _, participant ->
                                                        PreviewParticipantCard(participant = participant)
                                                    }
                                                }
                                            } else {
                                                itemsIndexed(
                                                    items = participants,
                                                    key = { index, participant ->
                                                        val rId = participant.remoteId
                                                        if (!rId.isNullOrBlank()) rId else "part_${index}_${participant.playerName}"
                                                    }
                                                ) { _, participant ->
                                                    PreviewParticipantCard(participant = participant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action: Join Button (Pill Container)
                        Button(
                            onClick = onJoinClick,
                            enabled = !isJoining,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isJoining) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "انضمام إلى هذه البطولة الآن",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    color = if (match.winnerName == match.playerOneName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!match.playerOneClub.isNullOrBlank()) {
                    Text(
                        text = match.playerOneClub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Score or VS badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
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
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!match.playerTwoClub.isNullOrBlank()) {
                    Text(
                        text = match.playerTwoClub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                size = 32.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!participant.clubName.isNullOrBlank()) {
                    Text(
                        text = participant.clubName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
