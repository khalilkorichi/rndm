package com.rndm.app.domain.model

import java.io.File

data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val updateIdentity: Long,
    val apkUrl: String,
    val apkSize: Long,
    val apkSha256: String,
    val mandatory: Boolean,
    val releaseNotes: String?,
    val publishedAt: String? = null
)

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int, val speed: String, val eta: String, val info: UpdateInfo) : DownloadState()
    data class Paused(val progress: Int, val info: UpdateInfo) : DownloadState()
    data class Success(val file: File, val info: UpdateInfo) : DownloadState()
    data class Error(val message: String, val info: UpdateInfo) : DownloadState()
}

sealed class CheckingStep {
    data object Idle : CheckingStep()
    data object ReadingLocalVersion : CheckingStep()
    data object FetchingManifest : CheckingStep()
    data object FetchingReleaseFallback : CheckingStep()
    data object ComparingVersions : CheckingStep()
    data class Success(val info: UpdateInfo) : CheckingStep()
    data class Error(val message: String) : CheckingStep()
}
