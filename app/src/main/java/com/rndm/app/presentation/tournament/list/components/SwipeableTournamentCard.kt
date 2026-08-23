package com.rndm.app.presentation.tournament.list.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.domain.model.Tournament
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Reveal widths (Physical)
private val EDIT_REVEAL_DP = 96.dp        // physical right side (swipe right-to-left)
private val ACTION_REVEAL_DP = 180.dp     // physical left side (swipe left-to-right)
private val SNAP_THRESHOLD = 0.25f        // 25% of reveal = snap open

@Composable
fun SwipeableTournamentCard(
    tournament: Tournament,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val editRevealPx = with(density) { EDIT_REVEAL_DP.toPx() }
    val actionRevealPx = with(density) { ACTION_REVEAL_DP.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun snapTo(target: Float) {
        scope.launch {
            offsetX.animateTo(
                target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {

        // ── LAYER 1: Background Action Buttons (Always LTR to guarantee physical placement) ──
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Physical LEFT: Archive + Delete (revealed when card moves physical RIGHT, offsetX > 0)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    RevealActionButton(
                        iconRes = R.drawable.ic_archive,
                        label = "أرشفة",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            scope.launch { offsetX.animateTo(0f) }
                            onArchive()
                        }
                    )
                    RevealActionButton(
                        iconRes = R.drawable.ic_delete,
                        label = "حذف",
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        onClick = {
                            scope.launch { offsetX.animateTo(0f) }
                            onDelete()
                        }
                    )
                }

                // Physical RIGHT: Edit (revealed when card moves physical LEFT, offsetX < 0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    RevealActionButton(
                        iconRes = R.drawable.ic_edit,
                        label = "تعديل",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            scope.launch { offsetX.animateTo(0f) }
                            onEdit()
                        }
                    )
                }
            }
        }

        // ── LAYER 2: Foreground Draggable Card ──
        Box(
            modifier = Modifier
                .absoluteOffset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val current = offsetX.value
                            val target = when {
                                // Swiped right enough -> snap open to show delete+archive on the left
                                current > actionRevealPx * SNAP_THRESHOLD -> actionRevealPx
                                // Swiped left enough -> snap open to show edit on the right
                                current < -editRevealPx * SNAP_THRESHOLD -> -editRevealPx
                                // Not enough -> snap back closed
                                else -> 0f
                            }
                            snapTo(target)
                        },
                        onDragCancel = {
                            snapTo(0f)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-editRevealPx, actionRevealPx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            TournamentCard(
                tournament = tournament,
                onClick = {
                    if (abs(offsetX.value) > 8f) {
                        snapTo(0f)
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun RevealActionButton(
    iconRes: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxHeight(0.82f)
            .width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
