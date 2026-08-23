package com.rndm.app.presentation.profile.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.ConfirmDialog
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.ui.components.RndmTopBarAction
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.presentation.profile.list.components.CreateProfileBottomSheet
import com.rndm.app.presentation.profile.list.components.ProfileCard
import com.rndm.app.presentation.profile.list.components.ProfileFilterBar
import com.rndm.app.presentation.profile.list.components.ProfileListSkeleton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onNavigateToCreateProfileWithType: (ProfileType) -> Unit,
    onNavigateToEditProfile: (Long) -> Unit,
    onNavigateToProfileDetail: (Long) -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "البروفايلات",
                titleIcon = painterResource(id = R.drawable.ic_profile_filled),
                actions = {
                    // Quick Action: Add Default Presets if needed
                    RndmTopBarAction(
                        onClick = { viewModel.onInsertDefaultProfiles() },
                        icon = painterResource(id = R.drawable.ic_star),
                        contentDescription = "إدراج البروفايلات الافتراضية"
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isBottomSheetVisible = true },
                modifier = Modifier.padding(bottom = 76.dp),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "إنشاء بروفايل جديد"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ProfileFilterBar(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            Crossfade(
                targetState = uiState.isLoading,
                animationSpec = tween(Constants.CROSSFADE_DURATION_MS),
                label = "profile_list_crossfade"
            ) { isLoading ->
                if (isLoading) {
                    ProfileListSkeleton()
                } else if (uiState.filteredProfiles.isEmpty()) {
                    EmptyState(
                        icon = painterResource(id = R.drawable.ic_profile_outlined),
                        title = "لا توجد بروفايلات بعد",
                        description = "أنشئ بروفايلك الأول أو أدرج البروفايلات الجاهزة للأندية والمنتخبات العالمية",
                        actionText = "إنشاء بروفايل جديد",
                        actionIcon = painterResource(id = R.drawable.ic_add),
                        onActionClick = { isBottomSheetVisible = true },
                        secondaryActionText = "إدراج البروفايلات الجاهزة تلقائياً",
                        onSecondaryActionClick = { viewModel.onInsertDefaultProfiles() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredProfiles,
                            key = { it.id }
                        ) { profile ->
                            ProfileCard(
                                profile = profile,
                                onClick = { onNavigateToProfileDetail(profile.id) },
                                onEditClick = { onNavigateToEditProfile(profile.id) },
                                onDuplicateClick = { viewModel.onDuplicateClick(profile) },
                                onDeleteClick = { viewModel.onDeleteClick(profile) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.profileToDelete != null) {
            ConfirmDialog(
                title = "حذف البروفايل",
                message = "هل تريد بالتأكيد حذف بروفايل \"${uiState.profileToDelete?.name}\"؟ هذا الإجراء لا يمكن التراجع عنه.",
                confirmText = "حذف",
                dismissText = "إلغاء",
                onConfirm = viewModel::onConfirmDelete,
                onDismiss = viewModel::onDismissDeleteDialog,
                isDestructive = true
            )
        }

        if (isBottomSheetVisible) {
            CreateProfileBottomSheet(
                sheetState = sheetState,
                onDismiss = { isBottomSheetVisible = false },
                onSelectType = { type ->
                    scope.launch {
                        sheetState.hide()
                        isBottomSheetVisible = false
                        onNavigateToCreateProfileWithType(type)
                    }
                }
            )
        }
    }
}
