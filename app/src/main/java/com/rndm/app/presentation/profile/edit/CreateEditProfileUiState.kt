package com.rndm.app.presentation.profile.edit

import androidx.compose.runtime.Immutable
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.ProfileType

@Immutable
data class CreateEditProfileUiState(
    val profileId: Long = 0L,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val type: ProfileType = ProfileType.PLAYERS,
    val items: List<String> = emptyList(),
    val currentItemInput: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && items.size >= Constants.MIN_PROFILE_ITEMS
}
