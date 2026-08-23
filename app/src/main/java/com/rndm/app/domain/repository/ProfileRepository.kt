package com.rndm.app.domain.repository

import com.rndm.app.domain.model.Profile
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
}
