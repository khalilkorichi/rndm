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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
                isAvailable = true,
                onClick = { onSelectDrawMode(DrawType.WHEEL) },
                modifier = Modifier.weight(1f)
            )

            DrawModeItem(
                title = "البطاقات المقلوبة",
                subtitle = "كشف العناصر باللمس",
                icon = painterResource(id = R.drawable.ic_cards),
                accentColor = MaterialTheme.colorScheme.secondary,
                isAvailable = false,
                onClick = {},
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
                isAvailable = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )

            DrawModeItem(
                title = "إقران المواجهات",
                subtitle = "جدول مباريات دوري",
                icon = painterResource(id = R.drawable.ic_tournament_filled),
                accentColor = MaterialTheme.colorScheme.primary,
                isAvailable = false,
                onClick = {},
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
    modifier: Modifier = Modifier,
    isAvailable: Boolean = true
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isAvailable) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (isAvailable) 0.7f else 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAvailable) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
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

            if (!isAvailable) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "يتوفر قريباً",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
