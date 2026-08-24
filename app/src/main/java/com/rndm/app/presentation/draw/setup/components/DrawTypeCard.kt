package com.rndm.app.presentation.draw.setup.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R

@Composable
fun DrawTypeCard(
    title: String,
    description: String,
    iconRes: Int,
    isSelected: Boolean,
    accentGradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAvailable: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    val cardScale by animateFloatAsState(
        targetValue = if (isSelected && isAvailable) 1.01f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (isSelected && isAvailable) accentGradient.first().copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "icon_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected && isAvailable) accentGradient.first()
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "icon_tint"
    )

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        onClick = {
            if (isAvailable) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        enabled = isAvailable,
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && isAvailable) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = if (isAvailable) 1f else 0.5f)
            },
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = if (isSelected && isAvailable) {
            BorderStroke(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(accentGradient)
            )
        } else {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (isAvailable) 0.2f else 0.1f)
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isAvailable) {
                            Modifier
                                .blur(5.dp)
                                .alpha(0.35f)
                        } else Modifier
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Squircle Container
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconContainerColor)
                        .then(
                            if (isSelected && isAvailable) {
                                Modifier.border(
                                    1.dp,
                                    accentGradient.first().copy(alpha = 0.35f),
                                    RoundedCornerShape(14.dp)
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Text Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected && isAvailable) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Selection Indicator Radio / Check
                Surface(
                    shape = CircleShape,
                    color = if (isSelected && isAvailable) accentGradient.first() else Color.Transparent,
                    border = if (!isSelected || !isAvailable) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    } else null,
                    modifier = Modifier.size(22.dp)
                ) {
                    if (isSelected && isAvailable) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            if (!isAvailable) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "يتوفر قريباً",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
