package com.rndm.app.domain.repository

import com.rndm.app.domain.model.DrawResult
import kotlinx.coroutines.flow.Flow

interface DrawRepository {
    fun getLatestDrawResult(): Flow<DrawResult?>
    suspend fun saveDrawResult(result: DrawResult)
}
