package com.rndm.app.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.BuildConfig
import com.rndm.app.domain.model.CheckingStep
import com.rndm.app.domain.model.DownloadState
import com.rndm.app.domain.model.UpdateInfo
import com.rndm.app.domain.usecase.update.CheckForUpdateUseCase
import com.rndm.app.domain.usecase.update.DownloadUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Immutable
sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class NoUpdate(
        val localVersion: String = BuildConfig.VERSION_NAME,
        val localCode: Int = BuildConfig.VERSION_CODE,
        val localIdentity: Long = BuildConfig.UPDATE_IDENTITY,
        val remoteVersion: String = BuildConfig.VERSION_NAME,
        val remoteCode: Int = BuildConfig.VERSION_CODE,
        val checkedAt: Long = System.currentTimeMillis(),
        val repositoryName: String = "${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
    ) : UpdateUiState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Int, val speed: String = "", val eta: String = "") : UpdateUiState()
    data class Paused(val info: UpdateInfo, val progress: Int) : UpdateUiState()
    data class DownloadFailed(val info: UpdateInfo, val error: String) : UpdateUiState()
    data class ReadyToInstall(val info: UpdateInfo, val localApkFile: File) : UpdateUiState()
    data class Error(val error: String) : UpdateUiState()
}

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _checkingStep = MutableStateFlow<CheckingStep>(CheckingStep.Idle)
    val checkingStep: StateFlow<CheckingStep> = _checkingStep.asStateFlow()

    private val _downloadedApks = MutableStateFlow<List<File>>(emptyList())
    val downloadedApks: StateFlow<List<File>> = _downloadedApks.asStateFlow()

    private var lastUpdateCheckTime: Long = 0L

    init {
        observeDownloadState()
        loadDownloadedApks()
    }

    private fun observeDownloadState() {
        viewModelScope.launch {
            downloadUpdateUseCase.downloadState.collect { state ->
                when (state) {
                    is DownloadState.Idle -> {
                        if (_uiState.value is UpdateUiState.Downloading || _uiState.value is UpdateUiState.Paused) {
                            _uiState.value = UpdateUiState.Idle
                        }
                    }
                    is DownloadState.Downloading -> {
                        _uiState.value = UpdateUiState.Downloading(state.info, state.progress, state.speed, state.eta)
                    }
                    is DownloadState.Paused -> {
                        _uiState.value = UpdateUiState.Paused(state.info, state.progress)
                    }
                    is DownloadState.Success -> {
                        _uiState.value = UpdateUiState.ReadyToInstall(state.info, state.file)
                        loadDownloadedApks()
                    }
                    is DownloadState.Error -> {
                        _uiState.value = UpdateUiState.DownloadFailed(state.info, state.message)
                    }
                }
            }
        }
    }

    fun checkForUpdates(isBackground: Boolean = false) {
        viewModelScope.launch {
            if (!isBackground) {
                _uiState.value = UpdateUiState.Checking
                _checkingStep.value = CheckingStep.ReadingLocalVersion
            }
            val result = checkForUpdateUseCase(onStep = { step ->
                if (!isBackground) {
                    _checkingStep.value = step
                }
            })
            lastUpdateCheckTime = System.currentTimeMillis()

            result.onSuccess { info ->
                if (info.hasUpdate) {
                    _uiState.value = UpdateUiState.UpdateAvailable(info)
                } else {
                    if (!isBackground) {
                        _uiState.value = UpdateUiState.NoUpdate(
                            localVersion = BuildConfig.VERSION_NAME,
                            localCode = BuildConfig.VERSION_CODE,
                            localIdentity = BuildConfig.UPDATE_IDENTITY,
                            remoteVersion = info.versionName.ifBlank { BuildConfig.VERSION_NAME },
                            remoteCode = if (info.versionCode > 0) info.versionCode else BuildConfig.VERSION_CODE,
                            checkedAt = System.currentTimeMillis(),
                            repositoryName = "${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
                        )
                    }
                }
            }.onFailure { err ->
                if (!isBackground) {
                    _uiState.value = UpdateUiState.Error(err.localizedMessage ?: "فشل فحص التحديثات")
                }
            }
        }
    }

    fun checkForUpdatesThrottled() {
        if (System.currentTimeMillis() - lastUpdateCheckTime >= 30 * 60 * 1000L) {
            checkForUpdates(isBackground = true)
        }
    }

    fun downloadUpdate(info: UpdateInfo) = downloadUpdateUseCase.startDownload(info)
    fun pauseDownload(info: UpdateInfo) = downloadUpdateUseCase.pauseDownload(info)
    fun resumeDownload(info: UpdateInfo) = downloadUpdateUseCase.startDownload(info)
    fun cancelDownload(info: UpdateInfo) = downloadUpdateUseCase.cancelDownload(info)

    fun loadDownloadedApks() {
        viewModelScope.launch {
            _downloadedApks.value = downloadUpdateUseCase.getDownloadedApks()
        }
    }

    fun deleteDownloadedApk(file: File) {
        viewModelScope.launch {
            downloadUpdateUseCase.deleteDownloadedApk(file)
            loadDownloadedApks()
        }
    }

    fun installUpdate(context: Context, info: UpdateInfo, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.value = UpdateUiState.DownloadFailed(info, "تعذر تشغيل مثبت الحزم: ${e.localizedMessage}")
        }
    }

    fun installDownloadedFile(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyApkToDownloads(file: File, filename: String, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = downloadUpdateUseCase.copyApkToDownloads(file, filename)
            onResult(uri)
        }
    }
}
