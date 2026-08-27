package com.rndm.app.presentation.draw.flipcards.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Composable
fun FlipCardExclusionHub(
    category: DrawCategory,
    excludedCount: Int,
    remainingCount: Int,
    isRevealing: Boolean,
    isShuffling: Boolean,
    onOpenExcludeDialog: () -> Unit,
    onShuffleCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (category) {
        DrawCategory.PLAYERS -> MaterialTheme.colorScheme.primary
        DrawCategory.CLUBS -> MaterialTheme.colorScheme.secondary
        DrawCategory.NATIONAL_TEAMS -> MaterialTheme.colorScheme.tertiary
    }

    val categoryIcon = when (category) {
        DrawCategory.PLAYERS -> R.drawable.ic_person
        DrawCategory.CLUBS -> R.drawable.ic_shield
        DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
    }

    val shuffleIconRotation by animateFloatAsState(
        targetValue = if (isShuffling) 360f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "shuffle_icon_rotation"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Exclusion Bar
        Surface(
            onClick = onOpenExcludeDialog,
            enabled = !isRevealing && !isShuffling,
            shape = RoundedCornerShape(14.dp),
            color = if (excludedCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                if (excludedCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (excludedCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else categoryColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = categoryIcon),
                                contentDescription = null,
                                tint = if (excludedCount > 0) MaterialTheme.colorScheme.error else categoryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "إدارة الاستبعاد",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (excludedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$excludedCount مستبعد",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "$remainingCount متاح",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null,
                        tint = if (excludedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Quick Shuffle Action Button
        Surface(
            onClick = onShuffleCards,
            enabled = !isRevealing && !isShuffling && remainingCount > 1,
            shape = RoundedCornerShape(14.dp),
            color = if (isShuffling) categoryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                if (isShuffling) categoryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swap),
                    contentDescription = "خلط البطاقات",
                    tint = if (remainingCount > 1) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = shuffleIconRotation
                        }
                )
                Text(
                    text = if (isShuffling) "جاري الخلط..." else "خلط",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remainingCount > 1) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
