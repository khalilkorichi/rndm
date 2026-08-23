package com.rndm.app.presentation.profile.edit

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.presentation.profile.edit.components.CreateClubProfileSection
import com.rndm.app.presentation.profile.edit.components.CreateNationalTeamProfileSection
import com.rndm.app.presentation.profile.edit.components.CreatePlayerProfileSection
import com.rndm.app.presentation.profile.edit.components.EditProfileItemDialog
import com.rndm.app.presentation.profile.edit.components.ProfileItemRow
import com.rndm.app.presentation.profile.edit.components.ProfileTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditProfileScreen(
    profileId: Long,
    typeName: String = "PLAYERS",
    onNavigateBack: () -> Unit,
    viewModel: CreateEditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val spacing = RndmThemeTokens.spacing
    val listState = rememberLazyListState()

    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var previousItemCount by remember { mutableIntStateOf(uiState.items.size) }

    LaunchedEffect(profileId, typeName) {
        viewModel.initialize(profileId, typeName)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Auto-scroll when new items are added so the user immediately sees them above the keyboard
    LaunchedEffect(uiState.items.size) {
        if (uiState.items.size > previousItemCount) {
            // Target index is the header items (4) + last item index
            val targetIndex = 4 + uiState.items.size - 1
            listState.animateScrollToItem(targetIndex)
        }
        previousItemCount = uiState.items.size
    }

    val typeColor = when (uiState.type) {
        ProfileType.PLAYERS -> com.rndm.app.core.theme.ProfilePlayersColor
        ProfileType.CLUBS -> com.rndm.app.core.theme.ProfileClubsColor
        ProfileType.NATIONAL_TEAMS -> com.rndm.app.core.theme.ProfileNationalTeamsColor
    }

    val typeIcon = when (uiState.type) {
        ProfileType.PLAYERS -> R.drawable.ic_person
        ProfileType.CLUBS -> R.drawable.ic_shield
        ProfileType.NATIONAL_TEAMS -> R.drawable.ic_globe
    }

    val screenTitle = if (uiState.isEditMode) {
        "تعديل البروفايل"
    } else {
        when (uiState.type) {
            ProfileType.PLAYERS -> "إنشاء بروفايل لاعبين"
            ProfileType.CLUBS -> "إنشاء بروفايل أندية"
            ProfileType.NATIONAL_TEAMS -> "إنشاء بروفايل منتخبات"
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = screenTitle,
                titleIcon = painterResource(id = typeIcon),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = {
                        if (uiState.canSave && !uiState.isLoading) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSave()
                        }
                    },
                    shape = CircleShape,
                    color = Color.Transparent,
                    enabled = uiState.canSave && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (uiState.canSave) 16.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = typeColor.copy(alpha = 0.5f),
                            ambientColor = Color.Black.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = if (uiState.canSave) {
                                Brush.horizontalGradient(
                                    colors = listOf(typeColor, typeColor.copy(alpha = 0.85f))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (uiState.canSave) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.isEditMode) R.drawable.ic_check else R.drawable.ic_add
                            ),
                            contentDescription = null,
                            tint = if (uiState.canSave) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = if (uiState.isEditMode) "حفظ التعديلات" else "إنشاء البروفايل",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (uiState.canSave) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Profile Name Input
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = {
                        Text(
                            when (uiState.type) {
                                ProfileType.PLAYERS -> "اسم البروفايل (مثال: أصدقاء الجمعة)"
                                ProfileType.CLUBS -> "اسم البروفايل (مثال: دوري أبطال أوروبا)"
                                ProfileType.NATIONAL_TEAMS -> "اسم البروفايل (مثال: كأس العالم للقرعة)"
                            }
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = typeColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Profile Type Selector (Switch between types)
            item {
                ProfileTypeSelector(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )
            }

            // Specialized Creation Content per Type
            item {
                when (uiState.type) {
                    ProfileType.PLAYERS -> {
                        CreatePlayerProfileSection(
                            currentInput = uiState.currentItemInput,
                            currentItems = uiState.items,
                            onInputChange = viewModel::onItemInputChange,
                            onAddItem = viewModel::onAddItem,
                            onAddSuggestion = viewModel::onAddSuggestion,
                            onAddDefaultTopPlayers = viewModel::onAddDefaultTopPlayers,
                            onGenerateSamplePlayers = viewModel::onGenerateSamplePlayers
                        )
                    }
                    ProfileType.CLUBS -> {
                        CreateClubProfileSection(
                            currentInput = uiState.currentItemInput,
                            currentItems = uiState.items,
                            onInputChange = viewModel::onItemInputChange,
                            onAddItem = viewModel::onAddItem,
                            onAddSuggestion = viewModel::onAddSuggestion,
                            onAddDefaultTopClubs = viewModel::onAddDefaultTopClubs
                        )
                    }
                    ProfileType.NATIONAL_TEAMS -> {
                        CreateNationalTeamProfileSection(
                            currentInput = uiState.currentItemInput,
                            currentItems = uiState.items,
                            onInputChange = viewModel::onItemInputChange,
                            onAddItem = viewModel::onAddItem,
                            onAddSuggestion = viewModel::onAddSuggestion,
                            onAddDefaultTopTeams = viewModel::onAddDefaultTopTeams
                        )
                    }
                }
            }

            // Items List Header & Controls
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "العناصر المضافة حالياً",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = typeColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${uiState.items.size}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (uiState.items.isNotEmpty()) {
                        Text(
                            text = "مسح الكل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onClearAllItems()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Items Rows
            if (uiState.items.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم تتم إضافة أي عناصر بعد. أضف عناصر أعلاه لحفظ البروفايل.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.items,
                    key = { index, item -> "$index-$item" }
                ) { index, item ->
                    ProfileItemRow(
                        index = index,
                        label = item,
                        profileType = uiState.type,
                        onEdit = {
                            editingItemIndex = index
                        },
                        onRemove = { viewModel.onRemoveItem(index) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Edit Profile Item Dialog
        if (editingItemIndex != null) {
            val index = editingItemIndex!!
            val currentLabel = uiState.items.getOrNull(index) ?: ""
            EditProfileItemDialog(
                initialLabel = currentLabel,
                profileType = uiState.type,
                onConfirm = { newLabel ->
                    viewModel.onEditItem(index, newLabel)
                    editingItemIndex = null
                },
                onDismiss = {
                    editingItemIndex = null
                }
            )
        }
    }
}
