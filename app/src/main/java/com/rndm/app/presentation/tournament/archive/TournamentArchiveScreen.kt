package com.rndm.app.presentation.tournament.archive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.domain.model.Tournament
import com.rndm.app.presentation.tournament.list.components.TournamentCard
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val RESTORE_REVEAL_DP = 96.dp    // physical left side (slides right): restore (استعادة)
private val DELETE_REVEAL_DP  = 96.dp    // physical right side (slides left): delete (حذف)
private val SNAP_THRESHOLD = 0.25f

@Composable
fun TournamentArchiveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTournamentDetail: (Long) -> Unit,
    viewModel: TournamentArchiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Delete Dialog ──────────────────────────────────────────
    if (uiState.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "حذف البطولة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف هذه البطولة نهائياً؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RndmTopAppBar(
            title = "الأرشيف",
            titleIcon = painterResource(R.drawable.ic_archive),
            onNavigateBack = onNavigateBack
        )

        // Archive info banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_archive),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "اسحب يميناً لاستعادة البطولة • اسحب يساراً لحذفها نهائياً",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (uiState.archivedTournaments.isEmpty()) {
            EmptyState(
                icon = painterResource(id = R.drawable.ic_archive),
                title = "الأرشيف فارغ",
                description = "البطولات التي تضعها في الأرشيف ستظهر هنا. يمكنك أرشفة أي بطولة من الشاشة الرئيسية.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = uiState.archivedTournaments, key = { it.id }) { tournament ->
                    ArchiveTournamentItem(
                        tournament = tournament,
                        onClick = { onNavigateToTournamentDetail(tournament.id) },
                        onUnarchive = { viewModel.unarchive(tournament.id) },
                        onDelete = { viewModel.requestDelete(tournament.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
private fun ArchiveTournamentItem(
    tournament: Tournament,
    onClick: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val restorePx = with(density) { RESTORE_REVEAL_DP.toPx() }
    val deletePx  = with(density) { DELETE_REVEAL_DP.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun snapTo(target: Float) {
        scope.launch {
            offsetX.animateTo(target, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        // ── LAYER 1: Background Action Buttons ──
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Physical LEFT side: Restore button (card slides RIGHT, offsetX > 0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ArchiveActionButton(
                        iconRes = R.drawable.ic_redo,
                        label = "استعادة",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            scope.launch { offsetX.animateTo(0f) }
                            onUnarchive()
                        }
                    )
                }

                // Physical RIGHT side: Delete button (card slides LEFT, offsetX < 0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ArchiveActionButton(
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
            }
        }

        // ── LAYER 2: Draggable card ──
        Box(
            modifier = Modifier
                .absoluteOffset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val cur = offsetX.value
                            val target = when {
                                cur > restorePx * SNAP_THRESHOLD -> restorePx
                                cur < -deletePx * SNAP_THRESHOLD -> -deletePx
                                else -> 0f
                            }
                            snapTo(target)
                        },
                        onDragCancel = { snapTo(0f) },
                        onHorizontalDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + drag).coerceIn(-deletePx, restorePx)
                                )
                            }
                        }
                    )
                }
        ) {
            Box {
                TournamentCard(
                    tournament = tournament,
                    onClick = {
                        if (abs(offsetX.value) > 8f) snapTo(0f) else onClick()
                    }
                )
                // Archive badge
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_archive),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "مؤرشف",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveActionButton(
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
            modifier = Modifier.padding(6.dp),
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
