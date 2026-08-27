package com.rndm.app.presentation.draw.setup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileGroup
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.profile.CreateProfileGroupUseCase
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileGroupsUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DrawSetupUiState(
    val currentStep: DrawSetupStep = DrawSetupStep.SELECT_PARTICIPANTS,
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val profiles: List<Profile> = emptyList(),
    val groups: List<ProfileGroup> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedProfileId: Long = 0L,
    val selectedDrawType: DrawType = DrawType.WHEEL,
    val editingProfile: Profile? = null,
    val isAddPlayersDialogOpen: Boolean = false,
    val isCreateGroupDialogOpen: Boolean = false,
    val error: String? = null
) {
    val filteredProfiles: List<Profile>
        get() = if (selectedGroupId == null) {
            profiles
        } else {
            profiles.filter { it.groupId == selectedGroupId }
        }

    val selectedProfile: Profile?
        get() = profiles.find { it.id == selectedProfileId }

    val canProceedToStep2: Boolean
        get() = selectedProfile != null && (selectedProfile?.activeCount ?: 0) >= 2

    val canStart: Boolean
        get() = canProceedToStep2
}

@HiltViewModel
class DrawSetupViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val getProfileGroupsUseCase: GetProfileGroupsUseCase,
    private val createProfileGroupUseCase: CreateProfileGroupUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase,
    private val drawFixtureRepository: DrawFixtureRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawSetupUiState())
    val uiState: StateFlow<DrawSetupUiState> = _uiState.asStateFlow()

    init {
        observeUserRole()
        val initialProfileId = savedStateHandle.get<Long>("profileId") ?: 0L
        if (initialProfileId > 0L) {
            _uiState.update { it.copy(selectedProfileId = initialProfileId) }
        }
        loadProfiles()
        loadGroups()
    }

    private fun observeUserRole() {
        getCurrentUserRoleUseCase()
            .onEach { role ->
                _uiState.update { it.copy(isAdmin = role == UserRole.ADMIN) }
            }
            .launchIn(viewModelScope)
    }

    fun initializeWithProfileId(profileId: Long) {
        if (profileId > 0L && _uiState.value.selectedProfileId == 0L) {
            _uiState.update { it.copy(selectedProfileId = profileId) }
        }
    }

    private fun loadProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                _uiState.update { state ->
                    val defaultProfileId = if (state.selectedProfileId != 0L && profiles.any { it.id == state.selectedProfileId }) {
                        state.selectedProfileId
                    } else {
                        profiles.firstOrNull()?.id ?: 0L
                    }
                    val updatedEditingProfile = if (state.editingProfile != null) {
                        profiles.find { it.id == state.editingProfile.id } ?: state.editingProfile
                    } else null

                    state.copy(
                        isLoading = false,
                        profiles = profiles,
                        selectedProfileId = defaultProfileId,
                        editingProfile = updatedEditingProfile
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadGroups() {
        getProfileGroupsUseCase()
            .onEach { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
            .launchIn(viewModelScope)
    }

    fun onProfileSelected(profileId: Long) {
        _uiState.update { it.copy(selectedProfileId = profileId) }
    }

    fun onSelectGroup(groupId: Long?) {
        _uiState.update { state ->
            val matching = if (groupId == null) state.profiles else state.profiles.filter { it.groupId == groupId }
            val newSelectedId = if (matching.any { it.id == state.selectedProfileId }) {
                state.selectedProfileId
            } else {
                matching.firstOrNull()?.id ?: state.selectedProfileId
            }
            state.copy(selectedGroupId = groupId, selectedProfileId = newSelectedId)
        }
    }

    fun onOpenManageProfileItems(profile: Profile) {
        _uiState.update { it.copy(editingProfile = profile) }
    }

    fun onDismissManageProfileItems() {
        _uiState.update { it.copy(editingProfile = null) }
    }

    fun onSaveEditedProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                updateProfileUseCase(profile)
                _uiState.update { it.copy(editingProfile = null, selectedProfileId = profile.id) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onProceedToStep2() {
        if (_uiState.value.canProceedToStep2) {
            _uiState.update { it.copy(currentStep = DrawSetupStep.SELECT_DRAW_TYPE) }
        }
    }

    fun onBackToStep1() {
        _uiState.update { it.copy(currentStep = DrawSetupStep.SELECT_PARTICIPANTS) }
    }

    fun onDrawTypeSelected(drawType: DrawType) {
        if (drawType == DrawType.SPIN_LIST || drawType == DrawType.ROUND_ROBIN) return
        _uiState.update { it.copy(selectedDrawType = drawType) }
    }

    fun onOpenCreateGroupDialog() {
        _uiState.update { it.copy(isCreateGroupDialogOpen = true) }
    }

    fun onDismissCreateGroupDialog() {
        _uiState.update { it.copy(isCreateGroupDialogOpen = false) }
    }

    fun onCreateGroup(name: String, icon: String) {
        viewModelScope.launch {
            try {
                val newGroupId = createProfileGroupUseCase(name, icon)
                _uiState.update { it.copy(isCreateGroupDialogOpen = false, selectedGroupId = newGroupId) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onOpenAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = true) }
    }

    fun onDismissAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = false) }
    }

    fun onConfirmQuickPlayers(names: List<String>, onStartDraw: (Long, DrawType) -> Unit) {
        if (names.isEmpty()) return
        viewModelScope.launch {
            try {
                val quickProfile = Profile(
                    name = "سحب فوري (${names.size} لاعبين)",
                    type = ProfileType.PLAYERS,
                    items = names.mapIndexed { idx, name ->
                        ProfileItem(label = name, order = idx, isActive = true)
                    },
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis()
                )
                val generatedId = createProfileUseCase(quickProfile)
                _uiState.update { it.copy(isAddPlayersDialogOpen = false) }
                onStartDraw(generatedId, DrawType.WHEEL)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onRestoreDefaultPresets() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val missing = ProfilePresets.getMissingDefaultProfiles(_uiState.value.profiles)
                if (missing.isNotEmpty()) {
                    missing.forEach { profile ->
                        createProfileUseCase(profile)
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
