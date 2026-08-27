package com.rndm.app.presentation.tournament.list.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.core.theme.*
import com.rndm.app.core.ui.components.LtrForcedText
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentType

@Composable
fun LiveTournamentBottomBar(
    tournament: Tournament,
    onPreviewClick: (Tournament) -> Unit,
    onJoinClick: (Tournament) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isJoining: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 18.dp else 12.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.6f) else PrimaryLight.copy(alpha = 0.15f),
                    ambientColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.4f) else PrimaryLight.copy(alpha = 0.08f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                BottomBarDarkBgTop.copy(alpha = 0.97f),
                                BottomBarDarkBgBottom.copy(alpha = 0.95f)
                            )
                        } else {
                            listOf(
                                BottomBarLightBgTop.copy(alpha = 0.96f),
                                BottomBarLightBgBottom.copy(alpha = 0.90f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (isDark) {
                            listOf(
                                PrimaryDark.copy(alpha = 0.7f),
                                BottomBarDarkBorderTop.copy(alpha = 0.8f),
                                PrimaryDark.copy(alpha = 0.4f)
                            )
                        } else {
                            listOf(
                                PrimaryLight.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.9f),
                                PrimaryLight.copy(alpha = 0.3f)
                            )
                        }
                    ),
                    shape = CircleShape
                )
                .clickable { onPreviewClick(tournament) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing Live Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.linearGradient(listOf(LiveTournamentGradientStart, LiveTournamentGradientEnd)),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Tournament Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!tournament.shareCode.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            LtrForcedText(
                                text = tournament.shareCode!!,
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "بطولة حية • ${if (tournament.type == TournamentType.GROUPS_KNOCKOUT) "مجموعات" else "إقصائيات"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Preview Pill Button
            Surface(
                onClick = { onPreviewClick(tournament) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "معاينة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "معاينة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Join Pill Button
            Button(
                onClick = { onJoinClick(tournament) },
                enabled = !isJoining,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "انضمام",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "انضمام",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Dismiss Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

