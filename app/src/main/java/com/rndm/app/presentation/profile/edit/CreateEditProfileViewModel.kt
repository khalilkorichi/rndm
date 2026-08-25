package com.rndm.app.presentation.profile.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.UserRole
import com.rndm.app.domain.usecase.auth.GetCurrentUserRoleUseCase
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateEditProfileEvent {
    data class ScrollToItem(val index: Int) : CreateEditProfileEvent
    data class ShowToast(val message: String) : CreateEditProfileEvent
}

@HiltViewModel
class CreateEditProfileViewModel @Inject constructor(
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getCurrentUserRoleUseCase: GetCurrentUserRoleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditProfileUiState())
    val uiState: StateFlow<CreateEditProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateEditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeUserRole()
        val profileId = savedStateHandle.get<Long>("profileId") ?: 0L
        val typeName = savedStateHandle.get<String>("typeName")
        val initialType = typeName?.let {
            try {
                ProfileType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        } ?: ProfileType.PLAYERS

        if (profileId > 0L) {
            loadProfile(profileId)
        } else {
            _uiState.update { it.copy(type = initialType) }
        }
    }

    private fun observeUserRole() {
        getCurrentUserRoleUseCase()
            .onEach { role ->
                _uiState.update { it.copy(isAdmin = role == UserRole.ADMIN) }
            }
            .launchIn(viewModelScope)
    }

    fun initialize(profileId: Long, typeName: String? = null) {
        if (profileId > 0L) {
            if (_uiState.value.profileId != profileId) {
                loadProfile(profileId)
            }
        } else if (typeName != null && !_uiState.value.isEditMode) {
            val parsedType = try {
                ProfileType.valueOf(typeName)
            } catch (e: Exception) {
                ProfileType.PLAYERS
            }
            _uiState.update { it.copy(type = parsedType) }
        }
    }

    private fun loadProfile(profileId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = getProfileByIdUseCase(profileId)
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        profileId = profile.id,
                        isEditMode = true,
                        isLoading = false,
                        name = profile.name,
                        type = profile.type,
                        items = profile.items.map { item ->
                            ProfileEditableItem(
                                id = "profile_item_${item.id}_${item.order}",
                                label = item.label
                            )
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "لم يتم العثور على البروفايل") }
            }
        }
    }

    fun onNameChange(newName: String) {
        if (newName.length <= Constants.MAX_PROFILE_NAME_LENGTH) {
            _uiState.update { it.copy(name = newName) }
        }
    }

    fun onTypeChange(newType: ProfileType) {
        _uiState.update { it.copy(type = newType) }
    }

    fun onItemInputChange(input: String) {
        if (input.length <= Constants.MAX_ITEM_LABEL_LENGTH) {
            _uiState.update { it.copy(currentItemInput = input) }
        }
    }

    fun onAddItem() {
        val itemLabel = _uiState.value.currentItemInput.trim()
        if (itemLabel.isNotBlank() && _uiState.value.items.none { it.label.equals(itemLabel, ignoreCase = true) }) {
            val newItem = ProfileEditableItem(label = itemLabel)
            val newItems = _uiState.value.items + newItem
            _uiState.update {
                it.copy(
                    items = newItems,
                    currentItemInput = ""
                )
            }
            viewModelScope.launch {
                _events.send(CreateEditProfileEvent.ScrollToItem(newItems.lastIndex))
            }
        }
    }

    fun onAddSuggestion(suggestion: String) {
        val trimmed = suggestion.trim()
        if (trimmed.isNotBlank() && _uiState.value.items.none { it.label.equals(trimmed, ignoreCase = true) }) {
            val newItem = ProfileEditableItem(label = trimmed)
            val newItems = _uiState.value.items + newItem
            _uiState.update {
                it.copy(items = newItems)
            }
            viewModelScope.launch {
                _events.send(CreateEditProfileEvent.ScrollToItem(newItems.lastIndex))
            }
        }
    }

    fun onAddDefaultTopClubs() {
        _uiState.update { current ->
            val existingLabels = current.items.map { it.label.trim().lowercase() }.toSet()
            val newPresets = ProfilePresets.DEFAULT_TOP_CLUBS
                .filter { it.trim().lowercase() !in existingLabels }
                .map { ProfileEditableItem(label = it) }
            val newItems = current.items + newPresets
            val newName = if (current.name.isBlank()) "أقوى الأندية الأوروبية" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onAddDefaultTopTeams() {
        _uiState.update { current ->
            val existingLabels = current.items.map { it.label.trim().lowercase() }.toSet()
            val newPresets = ProfilePresets.DEFAULT_TOP_NATIONAL_TEAMS
                .filter { it.trim().lowercase() !in existingLabels }
                .map { ProfileEditableItem(label = it) }
            val newItems = current.items + newPresets
            val newName = if (current.name.isBlank()) "أقوى 10 منتخبات عالمية" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onAddDefaultTopPlayers() {
        _uiState.update { current ->
            val existingLabels = current.items.map { it.label.trim().lowercase() }.toSet()
            val newPresets = ProfilePresets.DEFAULT_PLAYERS
                .filter { it.trim().lowercase() !in existingLabels }
                .map { ProfileEditableItem(label = it) }
            val newItems = current.items + newPresets
            val newName = if (current.name.isBlank()) "دوري الأصدقاء" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onGenerateSamplePlayers(count: Int) {
        val sampleList = ProfilePresets.generateSamplePlayers(count).map { ProfileEditableItem(label = it) }
        _uiState.update { current ->
            val newName = if (current.name.isBlank()) "دوري الأصدقاء" else current.name
            current.copy(
                items = sampleList,
                name = newName
            )
        }
    }

    fun onClearAllItems() {
        val state = _uiState.value
        if (state.isDefaultProfile && !state.isAdmin) {
            viewModelScope.launch {
                _events.send(CreateEditProfileEvent.ShowToast("مسح عناصر البروفايل الافتراضي متاح للمسؤولين (Admins) فقط."))
            }
            return
        }
        _uiState.update { it.copy(items = emptyList()) }
    }

    fun onRemoveItem(id: String) {
        val state = _uiState.value
        if (state.isDefaultProfile && !state.isAdmin) {
            viewModelScope.launch {
                _events.send(CreateEditProfileEvent.ShowToast("حذف اللاعبين كلياً من البروفايل الافتراضي متاح للمسؤولين (Admins) فقط. يمكنك استبعادهم أثناء إعداد القرعة."))
            }
            return
        }
        _uiState.update { current ->
            current.copy(items = current.items.filter { it.id != id })
        }
    }

    fun onEditItem(id: String, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isNotBlank() && trimmed.length <= Constants.MAX_ITEM_LABEL_LENGTH) {
            _uiState.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        if (item.id == id) item.copy(label = trimmed) else item
                    }
                )
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val profileItems = state.items.mapIndexed { index, item ->
                    ProfileItem(
                        id = 0L,
                        profileId = state.profileId,
                        label = item.label,
                        order = index
                    )
                }
                val profile = Profile(
                    id = state.profileId,
                    name = state.name.trim(),
                    type = state.type,
                    items = profileItems,
                    createdAt = System.currentTimeMillis()
                )

                if (state.isEditMode) {
                    updateProfileUseCase(profile)
                } else {
                    createProfileUseCase(profile)
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "فشل حفظ البروفايل") }
            }
        }
    }
}

