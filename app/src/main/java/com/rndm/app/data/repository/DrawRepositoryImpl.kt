package com.rndm.app.data.repository

import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.repository.DrawRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawRepositoryImpl @Inject constructor() : DrawRepository {
    private val _latestDrawResult = MutableStateFlow<DrawResult?>(null)

    override fun getLatestDrawResult(): Flow<DrawResult?> {
        return _latestDrawResult.asStateFlow()
    }

    override suspend fun saveDrawResult(result: DrawResult) {
        _latestDrawResult.value = result
    }
}
