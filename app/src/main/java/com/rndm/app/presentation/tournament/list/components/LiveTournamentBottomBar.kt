package com.rndm.app.presentation.tournament.list.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.core.theme.LiveTournamentBadgeBg
import com.rndm.app.core.theme.LiveTournamentBadgeText
import com.rndm.app.core.theme.LiveTournamentGradientEnd
import com.rndm.app.core.theme.LiveTournamentGradientStart
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
    val gradientColors = remember { listOf(LiveTournamentGradientStart, LiveTournamentGradientEnd) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), spotColor = LiveTournamentGradientEnd.copy(alpha = 0.5f))
            .background(brush = Brush.horizontalGradient(gradientColors), shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onPreviewClick(tournament) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live / Trophy Badge Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tournament Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "بطولة حية متاحة! 🔥",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (!tournament.shareCode.isNullOrBlank()) {
                        Surface(
                            color = LiveTournamentBadgeBg,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            LtrForcedText(
                                text = tournament.shareCode!!,
                                style = TextStyle(
                                    color = LiveTournamentBadgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${tournament.name} • ${if (tournament.type == TournamentType.GROUPS_KNOCKOUT) "مجموعات" else "إقصائيات"}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Preview Button
                OutlinedButton(
                    onClick = { onPreviewClick(tournament) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "معاينة",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Join Button
                Button(
                    onClick = { onJoinClick(tournament) },
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LiveTournamentGradientEnd
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = LiveTournamentGradientEnd,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "انضمام",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("انضمام", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
