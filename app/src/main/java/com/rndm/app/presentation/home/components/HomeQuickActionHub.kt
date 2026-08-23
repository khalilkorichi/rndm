package com.rndm.app.presentation.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R

@Composable
fun HomeQuickActionHub(
    onNavigateToQuickDraw: () -> Unit,
    onNavigateToClubDuelDraw: () -> Unit,
    onNavigateToCreateTournament: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "الإجراءات السريعة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickActionButton(
                    icon = painterResource(id = R.drawable.ic_wheel),
                    label = "قرعة فورية",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToQuickDraw,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = painterResource(id = R.drawable.ic_swords),
                    label = "حسم الأندية",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToClubDuelDraw,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = painterResource(id = R.drawable.ic_tournament_filled),
                    label = "بطولة جديدة",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToCreateTournament,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = painterResource(id = R.drawable.ic_profile_filled),
                    label = "بروفايل",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToCreateProfile,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = painterResource(id = R.drawable.ic_archive),
                    label = "الأرشيف",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToArchive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: Painter,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
