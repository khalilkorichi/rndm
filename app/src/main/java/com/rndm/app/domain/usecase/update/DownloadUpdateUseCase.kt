package com.rndm.app.domain.usecase.update

import android.net.Uri
import com.rndm.app.domain.model.DownloadState
import com.rndm.app.domain.model.UpdateInfo
import com.rndm.app.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

class DownloadUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    val downloadState: StateFlow<DownloadState>
        get() = updateRepository.downloadState

    fun startDownload(info: UpdateInfo) = updateRepository.startDownload(info)
    fun pauseDownload(info: UpdateInfo) = updateRepository.pauseDownload(info)
    fun cancelDownload(info: UpdateInfo) = updateRepository.cancelDownload(info)

    suspend fun getDownloadedApks(): List<File> = updateRepository.getDownloadedApks()
    suspend fun deleteDownloadedApk(file: File): Boolean = updateRepository.deleteDownloadedApk(file)
    suspend fun backupDataBeforeUpdate(): Result<Uri> = updateRepository.backupDataBeforeUpdate()
    suspend fun copyApkToDownloads(file: File, filename: String): Uri? = updateRepository.copyApkToDownloads(file, filename)
}
