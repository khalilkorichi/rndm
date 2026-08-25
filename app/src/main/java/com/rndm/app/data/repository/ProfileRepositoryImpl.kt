package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.ProfileGroupDao
import com.rndm.app.data.local.dao.ProfileItemDao
import com.rndm.app.data.mapper.toDomain
import com.rndm.app.data.mapper.toEntity
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileGroup
import com.rndm.app.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val profileItemDao: ProfileItemDao,
    private val profileGroupDao: ProfileGroupDao,
    private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override fun observeAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfilesWithItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getProfileById(id: Long): Profile? = withContext(ioDispatcher) {
        profileDao.getProfileWithItems(id)?.toDomain()
    }

    override suspend fun getRecentProfile(): Profile? = withContext(ioDispatcher) {
        profileDao.getRecentProfile()?.toDomain()
    }

    override suspend fun createProfile(profile: Profile): Long = withContext(ioDispatcher) {
        val profileEntity = profile.toEntity()
        val generatedProfileId = profileDao.insertProfile(profileEntity)

        val itemEntities = profile.items.mapIndexed { index, item ->
            item.toEntity(profileId = generatedProfileId).copy(order = index)
        }
        profileItemDao.insertItems(itemEntities)
        generatedProfileId
    }

    override suspend fun updateProfile(profile: Profile) = withContext(ioDispatcher) {
        profileDao.updateProfile(profile.toEntity())
        profileItemDao.deleteItemsByProfileId(profile.id)
        val itemEntities = profile.items.mapIndexed { index, item ->
            item.toEntity(profileId = profile.id).copy(order = index)
        }
        profileItemDao.insertItems(itemEntities)
    }

    override suspend fun deleteProfile(profileId: Long) = withContext(ioDispatcher) {
        profileDao.deleteProfileById(profileId)
    }

    override suspend fun duplicateProfile(profileId: Long, newName: String): Long = withContext(ioDispatcher) {
        val original = profileDao.getProfileWithItems(profileId) ?: return@withContext 0L
        val newProfileEntity = original.profile.copy(
            id = 0,
            name = newName,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null
        )
        val newId = profileDao.insertProfile(newProfileEntity)
        val newItems = original.items.map { it.copy(id = 0, profileId = newId) }
        profileItemDao.insertItems(newItems)
        newId
    }

    override suspend fun updateLastUsed(profileId: Long, timestamp: Long) = withContext(ioDispatcher) {
        profileDao.updateLastUsed(profileId, timestamp)
    }

    override fun observeProfileGroups(): Flow<List<ProfileGroup>> {
        return profileGroupDao.getAllGroups()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun createProfileGroup(group: ProfileGroup): Long = withContext(ioDispatcher) {
        profileGroupDao.insertGroup(group.toEntity())
    }

    override suspend fun deleteProfileGroup(groupId: Long) = withContext(ioDispatcher) {
        profileGroupDao.deleteGroupById(groupId)
    }

    override suspend fun updateProfileGroup(profileId: Long, groupId: Long?) = withContext(ioDispatcher) {
        profileDao.updateProfileGroup(profileId, groupId)
    }

    override suspend fun updateItemActiveState(itemId: Long, isActive: Boolean) = withContext(ioDispatcher) {
        profileItemDao.updateItemActiveState(itemId, isActive)
    }

    override suspend fun updateItemActiveStateByLabel(profileId: Long, label: String, isActive: Boolean) = withContext(ioDispatcher) {
        profileItemDao.updateItemActiveStateByLabel(profileId, label, isActive)
    }

    override suspend fun resetAllItemsToActive(profileId: Long) = withContext(ioDispatcher) {
        profileItemDao.resetAllItemsToActive(profileId)
    }
}

