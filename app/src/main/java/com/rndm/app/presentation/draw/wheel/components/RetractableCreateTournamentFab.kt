package com.rndm.app.presentation.draw.wheel.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.EmeraldMedium
import com.rndm.app.core.theme.SecondaryDark
import com.rndm.app.core.theme.SecondaryLight
import com.rndm.app.core.theme.UpdateSuccessGreenDark
import kotlinx.coroutines.delay

@Composable
fun RetractableCreateTournamentFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSizeDp: Int = 60,
    inactivityTimeoutSeconds: Int = 4
) {
    var isRetracted by remember { mutableStateOf(false) }
    var interactionTrigger by remember { mutableIntStateOf(0) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Inactivity countdown timer (4 seconds)
    LaunchedEffect(interactionTrigger, isRetracted) {
        if (!isRetracted) {
            delay(inactivityTimeoutSeconds * 1000L)
            isRetracted = true
        }
    }

    // Retraction slide animation with smooth spring
    val targetOffsetFraction = if (isRetracted) 0.90f else 0f
    val animatedOffsetFraction by animateFloatAsState(
        targetValue = targetOffsetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_retract_spring"
    )

    // Subtle breathing pulse animation when button is visible
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow_pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonSize = buttonSizeDp.dp
    // When retracting to the right edge: shift right by (16dp margin + 90% of button size)
    val totalShiftDp = 16f + (buttonSizeDp * 0.90f)
    val currentShiftDp = totalShiftDp * animatedOffsetFraction

    val primaryGreen = SecondaryLight
    val darkEmerald = UpdateSuccessGreenDark
    val mintHighlight = SecondaryDark

    Box(
        modifier = modifier
            .offset {
                val shiftPx = currentShiftDp.dp.roundToPx()
                // In RTL, moving to physical RIGHT requires negative X offset
                val xOffsetPx = if (isRtl) -shiftPx else shiftPx
                IntOffset(x = xOffsetPx, y = 0)
            }
            .size(buttonSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring when active and not retracted
        if (!isRetracted) {
            Box(
                modifier = Modifier
                    .size(buttonSize + 8.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryGreen.copy(alpha = pulseGlowAlpha * 0.45f),
                                mintHighlight.copy(alpha = pulseGlowAlpha * 0.15f),
                                Color.Transparent
                            )
                         )
                    )
            )
        }

        // Main Glassmorphic Circular Body
        Box(
            modifier = Modifier
                .size(buttonSize)
                .shadow(
                    elevation = if (isRetracted) 4.dp else 12.dp,
                    shape = CircleShape,
                    ambientColor = primaryGreen.copy(alpha = 0.5f),
                    spotColor = darkEmerald.copy(alpha = 0.7f)
                )
                .clip(CircleShape)
                // Frosted glass gradient
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryGreen.copy(alpha = 0.92f),
                            EmeraldMedium.copy(alpha = 0.95f),
                            darkEmerald.copy(alpha = 0.98f)
                        )
                    )
                )
                // Frosted Glass rim / border with top highlight
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            mintHighlight.copy(alpha = 0.60f),
                            Color.White.copy(alpha = 0.20f),
                            darkEmerald.copy(alpha = 0.40f)
                        )
                    ),
                    shape = CircleShape
                )
                // Top crescent glass glare overlay
                .drawBehind {
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.65f
                        ),
                        radius = size.width / 2f
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) {
                    if (isRetracted) {
                        // Tap on the 10% visible edge restores the button
                        isRetracted = false
                        interactionTrigger++
                    } else {
                        // Click when visible opens create tournament dialog
                        interactionTrigger++
                        onClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // White Checkmark Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "إنشاء البطولة",
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        scaleX = if (isRetracted) 0.85f else 1.0f
                        scaleY = if (isRetracted) 0.85f else 1.0f
                    }
            )
        }
    }
}
