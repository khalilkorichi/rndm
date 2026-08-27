package com.rndm.app.presentation.draw.flipcards.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.util.Constants
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Composable
fun FlipCardItem(
    cardNumber: Int,
    itemLabel: String,
    isFlipped: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    category: DrawCategory = DrawCategory.PLAYERS,
    shuffleOffsetX: Float = 0f,
    shuffleOffsetY: Float = 0f,
    shuffleRotationZ: Float = 0f,
    shuffleScale: Float = 1f,
    shuffleElevation: Float = 0f
) {
    val haptic = LocalHapticFeedback.current

    // Rotation angle from 0 to 180 degrees
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = Constants.FLIP_CARD_DURATION_MS.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "flip_card_rotation_$cardNumber"
    )

    // Elevation & scale bump during rotation
    val scale by animateFloatAsState(
        targetValue = if (isFlipped) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 350),
        label = "flip_card_scale_$cardNumber"
    )

    val cardShape = RoundedCornerShape(18.dp)

    val categoryColor = when (category) {
        DrawCategory.PLAYERS -> MaterialTheme.colorScheme.primary
        DrawCategory.CLUBS -> MaterialTheme.colorScheme.secondary
        DrawCategory.NATIONAL_TEAMS -> MaterialTheme.colorScheme.tertiary
    }

    val totalElevation = (if (isFlipped) 14f else 3f) + shuffleElevation

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .graphicsLayer {
                translationX = shuffleOffsetX
                translationY = shuffleOffsetY
                rotationZ = shuffleRotationZ
                rotationY = rotation
                cameraDistance = 16f * density
                scaleX = scale * shuffleScale
                scaleY = scale * shuffleScale
            }
            .shadow(
                elevation = totalElevation.dp,
                shape = cardShape,
                ambientColor = if (isFlipped || shuffleElevation > 0f) categoryColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f),
                spotColor = if (isFlipped || shuffleElevation > 0f) categoryColor else Color.Black.copy(alpha = 0.3f)
            )
            .clickable(enabled = isEnabled && !isFlipped) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (rotation > 90f) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = BorderStroke(
            width = if (isFlipped || shuffleElevation > 0f) 2.dp else 1.2.dp,
            brush = if (isFlipped || shuffleElevation > 0f) {
                Brush.verticalGradient(
                    colors = listOf(
                        categoryColor,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // ==================== FRONT FACE (Face Down / Mystery) ====================
                FrontFaceContent(
                    cardNumber = cardNumber,
                    category = category,
                    categoryColor = categoryColor
                )
            } else {
                // ==================== BACK FACE (Face Up / Revealed) ====================
                // CRITICAL RTL FIX: Counter-rotate the inner Box by 180 degrees around Y-axis
                // to eliminate horizontal mirroring of Arabic text and glyphs!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f // Counter-flip back to normal readable orientation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BackFaceContent(
                            itemLabel = itemLabel,
                            category = category,
                            categoryColor = categoryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrontFaceContent(
    cardNumber: Int,
    category: DrawCategory,
    categoryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        categoryColor.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Inner Decorative Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = categoryColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // Card number badge in top corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(22.dp)
                .background(
                    color = categoryColor.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$cardNumber",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = categoryColor,
                fontSize = 11.sp
            )
        }

        // Center Mystery Question Mark / Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = when (category) {
                            DrawCategory.PLAYERS -> R.drawable.ic_person
                            DrawCategory.CLUBS -> R.drawable.ic_shield
                            DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
                        }
                    ),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "؟",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = categoryColor
            )
        }
    }
}

@Composable
private fun BackFaceContent(
    itemLabel: String,
    category: DrawCategory,
    categoryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        categoryColor.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner celebratory border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.2.dp,
                    color = categoryColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Type Icon Squircle with Success Tint
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.20f))
                    .border(
                        1.dp,
                        categoryColor.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = when (category) {
                            DrawCategory.PLAYERS -> R.drawable.ic_person
                            DrawCategory.CLUBS -> R.drawable.ic_shield
                            DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
                        }
                    ),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Winner Name / Label (Guaranteed RTL Arabic reading direction)
            Text(
                text = itemLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Chosen Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = categoryColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "تم الاختيار ✓",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = categoryColor,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
