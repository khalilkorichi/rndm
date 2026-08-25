package com.rndm.app.domain.repository

import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileGroup
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(id: Long): Profile?
    suspend fun getRecentProfile(): Profile?
    suspend fun createProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profileId: Long)
    suspend fun duplicateProfile(profileId: Long, newName: String): Long
    suspend fun updateLastUsed(profileId: Long, timestamp: Long)

    // Profile Groups
    fun observeProfileGroups(): Flow<List<ProfileGroup>>
    suspend fun createProfileGroup(group: ProfileGroup): Long
    suspend fun deleteProfileGroup(groupId: Long)
    suspend fun updateProfileGroup(profileId: Long, groupId: Long?)

    // Active Item Persistence
    suspend fun updateItemActiveState(itemId: Long, isActive: Boolean)
    suspend fun updateItemActiveStateByLabel(profileId: Long, label: String, isActive: Boolean)
    suspend fun resetAllItemsToActive(profileId: Long)
}

