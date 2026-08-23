package com.rndm.app.presentation.profile.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.usecase.profile.CreateProfileUseCase
import com.rndm.app.domain.usecase.profile.DeleteProfileUseCase
import com.rndm.app.domain.usecase.profile.DuplicateProfileUseCase
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val duplicateProfileUseCase: DuplicateProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileListUiState())
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                _uiState.update {
                    it.copy(isLoading = false, profiles = profiles, error = null)
                }
            }
            .catch { exception ->
                _uiState.update {
                    it.copy(isLoading = false, error = exception.message ?: "حدث خطأ أثناء تحميل البروفايلات")
                }
            }
            .launchIn(viewModelScope)
    }

    fun onFilterSelected(filter: ProfileFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onDeleteClick(profile: Profile) {
        _uiState.update { it.copy(profileToDelete = profile) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(profileToDelete = null) }
    }

    fun onConfirmDelete() {
        val profile = _uiState.value.profileToDelete ?: return
        viewModelScope.launch {
            try {
                deleteProfileUseCase(profile.id)
                _uiState.update { it.copy(profileToDelete = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.message, profileToDelete = null) }
            }
        }
    }

    fun onDuplicateClick(profile: Profile) {
        viewModelScope.launch {
            try {
                val newName = "${profile.name} (نسخة)"
                duplicateProfileUseCase(profile.id, newName)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onInsertDefaultProfiles() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val defaultProfiles = ProfilePresets.createDefaultInitialProfiles()
                defaultProfiles.forEach { profile ->
                    createProfileUseCase(profile)
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
