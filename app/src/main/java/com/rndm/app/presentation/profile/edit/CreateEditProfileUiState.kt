package com.rndm.app.presentation.profile.edit

import androidx.compose.runtime.Immutable
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.ProfilePresets
import com.rndm.app.domain.model.ProfileType
import java.util.UUID

@Immutable
data class ProfileEditableItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String
)

@Immutable
data class CreateEditProfileUiState(
    val profileId: Long = 0L,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val name: String = "",
    val type: ProfileType = ProfileType.PLAYERS,
    val items: List<ProfileEditableItem> = emptyList(),
    val currentItemInput: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && items.size >= Constants.MIN_PROFILE_ITEMS

    val itemLabels: List<String>
        get() = items.map { it.label }

    val isDefaultProfile: Boolean
        get() = ProfilePresets.isDefaultProfile(name)

    val canDeleteItems: Boolean
        get() = !isDefaultProfile || isAdmin
}


