package com.rndm.app.presentation.profile.list.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.domain.model.ProfileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectType: (ProfileType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val spacing = RndmThemeTokens.spacing

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "إنشاء بروفايل جديد",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "اختر نوع البروفايل للبدء",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Players Profile Card
            ProfileTypeOptionCard(
                title = "لاعبين / أشخاص",
                subtitle = "قائمة بأسماء المشاركين في القرعة والبطولات",
                icon = painterResource(id = R.drawable.ic_person),
                badgeText = "أفراد",
                gradientColors = listOf(
                    com.rndm.app.core.theme.ProfilePlayersColor.copy(alpha = 0.20f),
                    com.rndm.app.core.theme.ProfilePlayersColorLight.copy(alpha = 0.08f)
                ),
                iconBgColor = com.rndm.app.core.theme.ProfilePlayersColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectType(ProfileType.PLAYERS)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Clubs Profile Card
            ProfileTypeOptionCard(
                title = "أندية كرة القدم",
                subtitle = "أندية عالمية ومحلية جاهزة أو مخصصة",
                icon = painterResource(id = R.drawable.ic_shield),
                badgeText = "أندية",
                gradientColors = listOf(
                    com.rndm.app.core.theme.ProfileClubsColor.copy(alpha = 0.20f),
                    com.rndm.app.core.theme.ProfileClubsColorDark.copy(alpha = 0.08f)
                ),
                iconBgColor = com.rndm.app.core.theme.ProfileClubsColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectType(ProfileType.CLUBS)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. National Teams Profile Card
            ProfileTypeOptionCard(
                title = "المنتخبات الوطنية",
                subtitle = "منتخبات دولية جاهزة للمواجهات",
                icon = painterResource(id = R.drawable.ic_globe),
                badgeText = "منتخبات",
                gradientColors = listOf(
                    com.rndm.app.core.theme.ProfileNationalTeamsColor.copy(alpha = 0.20f),
                    com.rndm.app.core.theme.ProfileNationalTeamsColorDark.copy(alpha = 0.08f)
                ),
                iconBgColor = com.rndm.app.core.theme.ProfileNationalTeamsColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectType(ProfileType.NATIONAL_TEAMS)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProfileTypeOptionCard(
    title: String,
    subtitle: String,
    icon: Painter,
    badgeText: String,
    gradientColors: List<Color>,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBgColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = iconBgColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = iconBgColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
