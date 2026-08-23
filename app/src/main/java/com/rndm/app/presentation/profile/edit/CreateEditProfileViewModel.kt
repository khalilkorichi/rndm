package com.rndm.app.presentation.profile.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import com.rndm.app.domain.usecase.profile.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEditProfileViewModel @Inject constructor(
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditProfileUiState())
    val uiState: StateFlow<CreateEditProfileUiState> = _uiState.asStateFlow()

    init {
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
                        items = profile.items.map { item -> item.label }
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
        val item = _uiState.value.currentItemInput.trim()
        if (item.isNotBlank() && item !in _uiState.value.items) {
            _uiState.update {
                it.copy(
                    items = it.items + item,
                    currentItemInput = ""
                )
            }
        }
    }

    fun onAddSuggestion(suggestion: String) {
        val trimmed = suggestion.trim()
        if (trimmed.isNotBlank() && trimmed !in _uiState.value.items) {
            _uiState.update {
                it.copy(items = it.items + trimmed)
            }
        }
    }

    fun onAddDefaultTopClubs() {
        _uiState.update { current ->
            val newItems = (current.items + ProfilePresets.DEFAULT_TOP_CLUBS).distinct()
            val newName = if (current.name.isBlank()) "أقوى الأندية الأوروبية" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onAddDefaultTopTeams() {
        _uiState.update { current ->
            val newItems = (current.items + ProfilePresets.DEFAULT_TOP_NATIONAL_TEAMS).distinct()
            val newName = if (current.name.isBlank()) "أقوى 10 منتخبات عالمية" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onAddDefaultTopPlayers() {
        _uiState.update { current ->
            val newItems = (current.items + ProfilePresets.DEFAULT_PLAYERS).distinct()
            val newName = if (current.name.isBlank()) "دوري الأصدقاء" else current.name
            current.copy(
                items = newItems,
                name = newName
            )
        }
    }

    fun onGenerateSamplePlayers(count: Int) {
        val sampleList = ProfilePresets.generateSamplePlayers(count)
        _uiState.update { current ->
            val newName = if (current.name.isBlank()) "دوري الأصدقاء" else current.name
            current.copy(
                items = sampleList,
                name = newName
            )
        }
    }

    fun onClearAllItems() {
        _uiState.update { it.copy(items = emptyList()) }
    }

    fun onRemoveItem(index: Int) {
        _uiState.update {
            val updated = it.items.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            it.copy(items = updated)
        }
    }

    fun onEditItem(index: Int, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isNotBlank() && trimmed.length <= Constants.MAX_ITEM_LABEL_LENGTH) {
            _uiState.update {
                val updated = it.items.toMutableList()
                if (index in updated.indices) {
                    updated[index] = trimmed
                }
                it.copy(items = updated)
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val profileItems = state.items.mapIndexed { index, label ->
                    ProfileItem(
                        id = 0L,
                        profileId = state.profileId,
                        label = label,
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
