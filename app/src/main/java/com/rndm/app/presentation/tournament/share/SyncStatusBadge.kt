package com.rndm.app.presentation.tournament.share

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rndm.app.domain.model.SyncStatus

@Composable
fun SyncStatusBadge(
    syncStatus: SyncStatus,
    isRemote: Boolean,
    isHost: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isRemote) return

    val (bgColor, textColor, icon, label) = when (syncStatus) {
        SyncStatus.SYNCED -> Quad(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
            Icons.Default.CloudDone,
            if (isHost) "سحابية (مباشر)" else "مشاهدة (مباشر)"
        )
        SyncStatus.PENDING_UPLOAD -> Quad(
            Color(0xFFFFB088).copy(alpha = 0.2f),
            Color(0xFFFF8A5B),
            Icons.Default.CloudQueue,
            "تحديثات معلقة"
        )
        SyncStatus.SYNC_ERROR -> Quad(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            Icons.Default.SyncProblem,
            "خطأ مزامنة"
        )
        SyncStatus.LOCAL_ONLY -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.CloudOff,
            "محلية"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
