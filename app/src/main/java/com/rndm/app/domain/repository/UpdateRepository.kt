package com.rndm.app.domain.repository

import android.net.Uri
import com.rndm.app.domain.model.CheckingStep
import com.rndm.app.domain.model.DownloadState
import com.rndm.app.domain.model.UpdateInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface UpdateRepository {
    val downloadState: StateFlow<DownloadState>
    fun startDownload(info: UpdateInfo)
    fun pauseDownload(info: UpdateInfo)
    fun cancelDownload(info: UpdateInfo)

    suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit = {}): Result<UpdateInfo>
    fun verifyApkSha256(file: File, expectedSha256: String): Boolean
    suspend fun copyApkToDownloads(file: File, filename: String): Uri?
    suspend fun backupDataBeforeUpdate(): Result<Uri>
    suspend fun saveDownloadedApk(file: File, versionName: String): File
    suspend fun getDownloadedApks(): List<File>
    suspend fun deleteDownloadedApk(file: File): Boolean
}
