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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.domain.model.DrawType

@Composable
fun HomeDrawModesGrid(
    onSelectDrawMode: (DrawType) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "أنماط القرعة المتاحة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DrawModeItem(
                title = "عجلة الحظ",
                subtitle = "دوران عشوائي كلاسيكي",
                icon = painterResource(id = R.drawable.ic_wheel),
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onSelectDrawMode(DrawType.WHEEL) },
                modifier = Modifier.weight(1f)
            )

            DrawModeItem(
                title = "البطاقات المقلوبة",
                subtitle = "كشف العناصر باللمس",
                icon = painterResource(id = R.drawable.ic_cards),
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = { onSelectDrawMode(DrawType.FLIP_CARDS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DrawModeItem(
                title = "القائمة الدوارة",
                subtitle = "سحب أفقي ديناميكي",
                icon = painterResource(id = R.drawable.ic_fixtures),
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = { onSelectDrawMode(DrawType.SPIN_LIST) },
                modifier = Modifier.weight(1f)
            )

            DrawModeItem(
                title = "إقران المواجهات",
                subtitle = "جدول مباريات دوري",
                icon = painterResource(id = R.drawable.ic_tournament_filled),
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onSelectDrawMode(DrawType.ROUND_ROBIN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DrawModeItem(
    title: String,
    subtitle: String,
    icon: Painter,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
