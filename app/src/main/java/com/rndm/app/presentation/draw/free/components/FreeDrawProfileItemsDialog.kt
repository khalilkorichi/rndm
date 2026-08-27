package com.rndm.app.presentation.draw.free.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.presentation.draw.wheel.DrawCategory

private fun parseRawInputItems(raw: String): List<String> {
    return raw.split(",", "\n", "،")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeDrawProfileItemsDialog(
    selectedCategory: DrawCategory,
    playersProfiles: List<Profile>,
    clubsProfiles: List<Profile>,
    nationalTeamsProfiles: List<Profile>,
    selectedPlayersProfile: Profile?,
    selectedClubsProfile: Profile?,
    selectedNationalTeamsProfile: Profile?,
    remainingPlayers: List<ProfileItem>,
    excludedPlayers: List<ProfileItem>,
    remainingClubs: List<ProfileItem>,
    excludedClubs: List<ProfileItem>,
    remainingNationalTeams: List<ProfileItem>,
    excludedNationalTeams: List<ProfileItem>,
    onDismiss: () -> Unit,
    onCategoryChanged: (DrawCategory) -> Unit,
    onProfileSelected: (DrawCategory, Profile) -> Unit,
    onSaveAndApply: (
        category: DrawCategory,
        activeItems: List<ProfileItem>,
        excludedItems: List<ProfileItem>
    ) -> Unit
) {
    var activeCategory by remember { mutableStateOf(selectedCategory) }
    var profileDropdownExpanded by remember { mutableStateOf(false) }

    // Current category profiles & selected profile
    val currentProfiles = when (activeCategory) {
        DrawCategory.PLAYERS -> playersProfiles
        DrawCategory.CLUBS -> clubsProfiles
        DrawCategory.NATIONAL_TEAMS -> nationalTeamsProfiles
    }

    val currentSelectedProfile = when (activeCategory) {
        DrawCategory.PLAYERS -> selectedPlayersProfile
        DrawCategory.CLUBS -> selectedClubsProfile
        DrawCategory.NATIONAL_TEAMS -> selectedNationalTeamsProfile
    }

    // Initialize items working list from remaining + excluded items of active category
    val currentActive = when (activeCategory) {
        DrawCategory.PLAYERS -> remainingPlayers
        DrawCategory.CLUBS -> remainingClubs
        DrawCategory.NATIONAL_TEAMS -> remainingNationalTeams
    }
    val currentExcluded = when (activeCategory) {
        DrawCategory.PLAYERS -> excludedPlayers
        DrawCategory.CLUBS -> excludedClubs
        DrawCategory.NATIONAL_TEAMS -> excludedNationalTeams
    }

    val workingItemsList = remember { mutableStateListOf<ProfileItem>() }

    LaunchedEffect(activeCategory, currentSelectedProfile) {
        workingItemsList.clear()
        val combined = (currentActive.map { it.copy(isActive = true) } +
                currentExcluded.map { it.copy(isActive = false) })
            .distinctBy { it.label }
        if (combined.isNotEmpty()) {
            workingItemsList.addAll(combined)
        } else if (currentSelectedProfile != null && currentSelectedProfile.items.isNotEmpty()) {
            workingItemsList.addAll(currentSelectedProfile.items)
        }
    }

    var newItemText by remember { mutableStateOf("") }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    fun addNewItem() {
        val raw = newItemText.trim()
        if (raw.isBlank()) return

        val parts = parseRawInputItems(raw)
        parts.forEach { name ->
            if (workingItemsList.none { it.label.equals(name, ignoreCase = true) }) {
                workingItemsList.add(
                    ProfileItem(
                        id = System.currentTimeMillis() + workingItemsList.size,
                        label = name,
                        order = workingItemsList.size,
                        isActive = true
                    )
                )
            }
        }
        newItemText = ""
    }

    val placeholderText = when (activeCategory) {
        DrawCategory.PLAYERS -> "إضافة لاعبين جدد..."
        DrawCategory.CLUBS -> "إضافة أندية جديدة..."
        DrawCategory.NATIONAL_TEAMS -> "إضافة منتخبات جديدة..."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(0.96f),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Tabs: (أشخاص / أندية / منتخبات)
                TabRow(
                    selectedTabIndex = when (activeCategory) {
                        DrawCategory.PLAYERS -> 0
                        DrawCategory.CLUBS -> 1
                        DrawCategory.NATIONAL_TEAMS -> 2
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        val index = when (activeCategory) {
                            DrawCategory.PLAYERS -> 0
                            DrawCategory.CLUBS -> 1
                            DrawCategory.NATIONAL_TEAMS -> 2
                        }
                        if (index in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = activeCategory == DrawCategory.PLAYERS,
                        onClick = {
                            activeCategory = DrawCategory.PLAYERS
                            onCategoryChanged(DrawCategory.PLAYERS)
                        },
                        text = {
                            Text(
                                text = "أشخاص",
                                fontWeight = if (activeCategory == DrawCategory.PLAYERS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Tab(
                        selected = activeCategory == DrawCategory.CLUBS,
                        onClick = {
                            activeCategory = DrawCategory.CLUBS
                            onCategoryChanged(DrawCategory.CLUBS)
                        },
                        text = {
                            Text(
                                text = "أندية",
                                fontWeight = if (activeCategory == DrawCategory.CLUBS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Tab(
                        selected = activeCategory == DrawCategory.NATIONAL_TEAMS,
                        onClick = {
                            activeCategory = DrawCategory.NATIONAL_TEAMS
                            onCategoryChanged(DrawCategory.NATIONAL_TEAMS)
                        },
                        text = {
                            Text(
                                text = "منتخبات",
                                fontWeight = if (activeCategory == DrawCategory.NATIONAL_TEAMS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_globe),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Selector Dropdown (if multiple profiles exist in this category)
                if (currentProfiles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = profileDropdownExpanded,
                        onExpandedChange = { profileDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentSelectedProfile?.name ?: "اختر البروفايل...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = profileDropdownExpanded,
                            onDismissRequest = { profileDropdownExpanded = false }
                        ) {
                            currentProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${profile.name} (${profile.items.size})",
                                            fontWeight = if (profile.id == currentSelectedProfile?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        profileDropdownExpanded = false
                                        onProfileSelected(activeCategory, profile)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 440.dp)
            ) {
                // Add new element input field with '+' button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = newItemText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                addNewItem()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add),
                                contentDescription = "إضافة",
                                tint = if (newItemText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addNewItem() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats and Bulk Actions Bar: عناصر البروفايل (X): | تفعيل الكل | استبعاد الكل
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عناصر البروفايل (${workingItemsList.size}):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "تفعيل الكل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    for (i in workingItemsList.indices) {
                                        workingItemsList[i] = workingItemsList[i].copy(isActive = true)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Text(
                            text = "استبعاد الكل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    for (i in workingItemsList.indices) {
                                        workingItemsList[i] = workingItemsList[i].copy(isActive = false)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Items List
                if (workingItemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عناصر. أضف عناصر جديدة أعلاه للبدء بالقرعة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = workingItemsList,
                            key = { index, item -> "${item.id}-$index" }
                        ) { index, item ->
                            val isEditingThis = editingItemIndex == index

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (item.isActive) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isActive) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Delete & Edit Actions
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                workingItemsList.removeAt(index)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_delete),
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isEditingThis) {
                                                    if (editingText.isNotBlank()) {
                                                        workingItemsList[index] = item.copy(label = editingText.trim())
                                                    }
                                                    editingItemIndex = null
                                                } else {
                                                    editingItemIndex = index
                                                    editingText = item.label
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = if (isEditingThis) R.drawable.ic_check else R.drawable.ic_edit),
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Status Pill: [X مستبعد] / [✓ مشارك]
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.isActive) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                            } else {
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                            },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    workingItemsList[index] = item.copy(isActive = !item.isActive)
                                                }
                                        ) {
                                            Text(
                                                text = if (item.isActive) "✓ مشارك" else "✕ مستبعد",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (item.isActive) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Right: Name and Index (as in screenshot)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        if (isEditingThis) {
                                            OutlinedTextField(
                                                value = editingText,
                                                onValueChange = { editingText = it },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = {
                                                    if (editingText.isNotBlank()) {
                                                        workingItemsList[index] = item.copy(label = editingText.trim())
                                                    }
                                                    editingItemIndex = null
                                                }),
                                                modifier = Modifier.fillMaxWidth(0.8f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        } else {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (item.isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                    textDecoration = if (item.isActive) TextDecoration.None else TextDecoration.LineThrough
                                                ),
                                                color = if (item.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.35f)
                ) {
                    Text(
                        text = "إلغاء",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RndmButton(
                    onClick = {
                        val active = workingItemsList.filter { it.isActive }
                        val excluded = workingItemsList.filter { !it.isActive }
                        onSaveAndApply(activeCategory, active, excluded)
                    },
                    type = RndmButtonType.PRIMARY,
                    modifier = Modifier.weight(0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ وتطبيق التغييرات",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        },
        dismissButton = null
    )
}
