package com.rndm.app.presentation.draw.wheel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Composable
fun ExcludeFromDrawDialog(
    category: DrawCategory,
    profileName: String,
    remainingItems: List<ProfileItem>,
    excludedItems: List<ProfileItem>,
    onExcludeItem: (ProfileItem) -> Unit,
    onRestoreItem: (ProfileItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val spacing = RndmThemeTokens.spacing

    val categoryTitle = when (category) {
        DrawCategory.PLAYERS -> "اللاعبين"
        DrawCategory.CLUBS -> "الأندية"
        DrawCategory.NATIONAL_TEAMS -> "المنتخبات"
    }

    val categoryIcon = when (category) {
        DrawCategory.PLAYERS -> R.drawable.ic_person
        DrawCategory.CLUBS -> R.drawable.ic_shield
        DrawCategory.NATIONAL_TEAMS -> R.drawable.ic_globe
    }

    val filteredRemaining = remember(remainingItems, searchQuery) {
        if (searchQuery.isBlank()) remainingItems
        else remainingItems.filter { it.label.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val filteredExcluded = remember(excludedItems, searchQuery) {
        if (searchQuery.isBlank()) excludedItems
        else excludedItems.filter { it.label.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "استبعاد من القرعة الحالية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$profileName (${remainingItems.size} متبقي في العجلة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 الاستبعاد يزيل العنصر من قرعة هذه الجلسة فقط دون حذفه من البروفايل الأصلي.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Search field if many items
                if (remainingItems.size + excludedItems.size > 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "بحث عن $categoryTitle...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = categoryIcon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "مسح البحث",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.sm)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    if (filteredRemaining.isEmpty() && filteredExcluded.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد نتائج مطابقة للبحث",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (filteredRemaining.isNotEmpty()) {
                        item {
                            Text(
                                text = "العناصر المشاركة في العجلة (${filteredRemaining.size}):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = spacing.xs)
                            )
                        }

                        items(filteredRemaining, key = { "active_${it.id}_${it.label}" }) { item ->
                            CandidateExclusionRow(
                                item = item,
                                categoryIcon = categoryIcon,
                                isExcluded = false,
                                onActionClick = { onExcludeItem(item) }
                            )
                        }
                    }

                    if (filteredExcluded.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(spacing.sm))
                            Text(
                                text = "المستبعدون من القرعة الحالية (${filteredExcluded.size}):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = spacing.xs)
                            )
                        }

                        items(filteredExcluded, key = { "excluded_${it.id}_${it.label}" }) { item ->
                            CandidateExclusionRow(
                                item = item,
                                categoryIcon = categoryIcon,
                                isExcluded = true,
                                onActionClick = { onRestoreItem(item) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "تم",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun CandidateExclusionRow(
    item: ProfileItem,
    categoryIcon: Int,
    isExcluded: Boolean,
    onActionClick: () -> Unit
) {
    val spacing = RndmThemeTokens.spacing

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isExcluded) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isExcluded) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = categoryIcon),
                    contentDescription = null,
                    tint = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isExcluded) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (isExcluded) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isExcluded) {
                TextButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_redo),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "استعادة",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "استبعاد ${item.label}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
