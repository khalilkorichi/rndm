package com.rndm.app.presentation.draw.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.domain.model.DrawType

@Composable
fun DrawTypeSelectionSection(
    selectedDrawType: DrawType,
    onDrawTypeSelected: (DrawType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Step Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "أسلوب القرعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "حدد طريقة العرض والسحب المفضلة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Wheel of Fortune
        DrawTypeCard(
            title = "عجلة الحظ",
            description = "دوران عشوائي تفاعلي مع توقف تدريجي",
            iconRes = R.drawable.ic_wheel,
            isSelected = selectedDrawType == DrawType.WHEEL,
            isAvailable = true,
            accentGradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
            onClick = { onDrawTypeSelected(DrawType.WHEEL) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Flip Cards
        DrawTypeCard(
            title = "البطاقات المقلوبة",
            description = "كشف العناصر عشوائياً باللمس",
            iconRes = R.drawable.ic_cards,
            isSelected = selectedDrawType == DrawType.FLIP_CARDS,
            isAvailable = true,
            accentGradient = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer),
            onClick = { onDrawTypeSelected(DrawType.FLIP_CARDS) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Spin List
        DrawTypeCard(
            title = "القائمة الدوارة",
            description = "شريط سحب سريع يتباطأ تدريجياً",
            iconRes = R.drawable.ic_spinlist,
            isSelected = selectedDrawType == DrawType.SPIN_LIST,
            isAvailable = false,
            accentGradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer),
            onClick = { onDrawTypeSelected(DrawType.SPIN_LIST) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Round Robin Pairing
        DrawTypeCard(
            title = "إقران المواجهات",
            description = "توزيع وتوليد جدول المباريات تلقائياً",
            iconRes = R.drawable.ic_roundrobin,
            isSelected = selectedDrawType == DrawType.ROUND_ROBIN,
            isAvailable = false,
            accentGradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
            onClick = { onDrawTypeSelected(DrawType.ROUND_ROBIN) }
        )
    }
}
