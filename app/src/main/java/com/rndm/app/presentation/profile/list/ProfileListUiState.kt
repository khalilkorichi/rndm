package com.rndm.app.presentation.profile.list

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileType

enum class ProfileFilter {
    ALL, PLAYERS, CLUBS, NATIONAL_TEAMS
}

@Immutable
data class ProfileListUiState(
    val isLoading: Boolean = true,
    val profiles: List<Profile> = emptyList(),
    val selectedFilter: ProfileFilter = ProfileFilter.ALL,
    val profileToDelete: Profile? = null,
    val error: String? = null
) {
    val filteredProfiles: List<Profile>
        get() = when (selectedFilter) {
            ProfileFilter.ALL -> profiles
            ProfileFilter.PLAYERS -> profiles.filter { it.type == ProfileType.PLAYERS }
            ProfileFilter.CLUBS -> profiles.filter { it.type == ProfileType.CLUBS }
            ProfileFilter.NATIONAL_TEAMS -> profiles.filter { it.type == ProfileType.NATIONAL_TEAMS }
        }
}
