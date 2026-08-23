package com.rndm.app.data.update

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rndm.app.BuildConfig
import com.rndm.app.domain.model.CheckingStep
import com.rndm.app.domain.model.DownloadState
import com.rndm.app.domain.model.UpdateInfo
import com.rndm.app.domain.repository.UpdateRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: GitHubReleaseClient
) : UpdateRepository {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun setDownloadState(state: DownloadState) {
        _downloadState.value = state
    }

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    init {
        try {
            val wm = WorkManager.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                wm.getWorkInfosForUniqueWorkFlow("apk_download").collect { list ->
                    val info = list.firstOrNull() ?: return@collect
                    val progress = info.progress.getInt("progress", -1)
                    val speed = info.progress.getString("speed") ?: ""
                    val eta = info.progress.getString("eta") ?: ""

                    val current = _downloadState.value
                    if (current is DownloadState.Downloading && progress >= 0) {
                        _downloadState.value = current.copy(progress = progress, speed = speed, eta = eta)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore WorkManager initialization errors
        }
    }

    override fun startDownload(info: UpdateInfo) {
        UpdateDownloadWorker.setPaused(info.apkUrl, false)
        val infoJson = moshi.adapter(UpdateInfo::class.java).toJson(info)

        val data = Data.Builder()
            .putString("apkUrl", info.apkUrl)
            .putString("apkSha256", info.apkSha256)
            .putString("versionName", info.versionName)
            .putLong("apkSize", info.apkSize)
            .putString("infoJson", infoJson)
            .build()

        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(data)
            .addTag("apk_download")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork("apk_download", ExistingWorkPolicy.REPLACE, request)

        val tempFile = File(context.cacheDir, "rndm-update-temp.apk")
        val startBytes = if (tempFile.exists()) tempFile.length() else 0L
        val initialPercent = if (info.apkSize > 0L) (startBytes * 100 / info.apkSize).toInt().coerceIn(0, 100) else 0
        _downloadState.value = DownloadState.Downloading(initialPercent, "", "", info)
    }

    override fun pauseDownload(info: UpdateInfo) {
        UpdateDownloadWorker.setPaused(info.apkUrl, true)
        val progress = (_downloadState.value as? DownloadState.Downloading)?.progress ?: 0
        _downloadState.value = DownloadState.Paused(progress, info)
        WorkManager.getInstance(context).cancelUniqueWork("apk_download")
    }

    override fun cancelDownload(info: UpdateInfo) {
        WorkManager.getInstance(context).cancelUniqueWork("apk_download")
        UpdateDownloadWorker.clearPaused(info.apkUrl)
        File(context.cacheDir, "rndm-update-temp.apk").delete()
        _downloadState.value = DownloadState.Idle
    }

    private fun getInstalledPackageInfo(): Pair<String, Int> {
        return try {
            val pm = context.packageManager
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName ?: BuildConfig.VERSION_NAME
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
            Pair(vName, vCode)
        } catch (e: Exception) {
            Pair(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        }
    }

    override suspend fun checkForUpdates(onStep: suspend (CheckingStep) -> Unit): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            onStep(CheckingStep.ReadingLocalVersion)
            delay(200)

            val owner = BuildConfig.GITHUB_OWNER
            val repo = BuildConfig.GITHUB_REPO

            val (localVersionName, localVersionCode) = getInstalledPackageInfo()
            val localIdentity = BuildConfig.UPDATE_IDENTITY

            // 1. Primary Source: Fetch update.json manifest with cache busting query param
            onStep(CheckingStep.FetchingManifest)
            val manifestUrl = "https://raw.githubusercontent.com/$owner/$repo/main/update.json?t=${System.currentTimeMillis()}"
            val manifest = try {
                client.fetchUpdateManifest(manifestUrl)
            } catch (e: Exception) {
                null
            }

            if (manifest != null) {
                onStep(CheckingStep.ComparingVersions)
                delay(200)

                val hasUpdate = isCandidateStrictlyNewer(
                    localName = localVersionName,
                    localCode = localVersionCode,
                    localIdentity = localIdentity,
                    remoteName = manifest.versionName,
                    remoteCode = manifest.versionCode,
                    remoteIdentity = manifest.updateIdentity
                )

                val updateInfo = UpdateInfo(
                    hasUpdate = hasUpdate,
                    versionCode = manifest.versionCode,
                    versionName = manifest.versionName,
                    updateIdentity = manifest.updateIdentity,
                    apkUrl = manifest.apkUrl,
                    apkSize = manifest.apkSize,
                    apkSha256 = manifest.apkSha256,
                    mandatory = manifest.mandatory,
                    releaseNotes = manifest.releaseNotes,
                    publishedAt = manifest.publishedAt
                )
                onStep(CheckingStep.Success(updateInfo))
                return@withContext Result.success(updateInfo)
            }

            // 2. Fallback: Query GitHub Releases API
            onStep(CheckingStep.FetchingReleaseFallback)
            val release = try {
                client.fetchLatestRelease(owner, repo)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    onStep(CheckingStep.ComparingVersions)
                    delay(150)
                    val fallbackUpToDate = UpdateInfo(
                        hasUpdate = false,
                        versionCode = localVersionCode,
                        versionName = localVersionName,
                        updateIdentity = localIdentity,
                        apkUrl = "",
                        apkSize = 0L,
                        apkSha256 = "",
                        mandatory = false,
                        releaseNotes = "لا توجد إصدارات أحدث منشورة في المستودع حالياً."
                    )
                    onStep(CheckingStep.Success(fallbackUpToDate))
                    return@withContext Result.success(fallbackUpToDate)
                } else if (e.code() == 403) {
                    throw Exception("تم تجاوز حد الطلبات المؤقت لـ GitHub (Rate Limit)، يرجى المحاولة لاحقاً.")
                } else {
                    throw Exception("تعذر الوصول لخادم GitHub (رمز الخطأ: ${e.code()})")
                }
            } catch (e: java.io.IOException) {
                throw Exception("تعذر الاتصال بخوادم GitHub، يرجى التحقق من اتصال الإنترنت.")
            }

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            val cleanTagName = release.tagName.removePrefix("v").trim()

            val hasUpdate = apkAsset != null && isCandidateStrictlyNewer(
                localName = localVersionName,
                localCode = localVersionCode,
                localIdentity = localIdentity,
                remoteName = cleanTagName,
                remoteCode = localVersionCode,
                remoteIdentity = 0L
            )

            val sha256 = release.body?.lineSequence()
                ?.mapNotNull { line -> Regex("(?i)sha-?256\\s*[:=]\\s*([a-f0-9]{64})").find(line)?.groupValues?.get(1) }
                ?.firstOrNull() ?: apkAsset?.digest?.removePrefix("sha256:")?.trim() ?: ""

            onStep(CheckingStep.ComparingVersions)
            delay(150)

            val fallbackInfo = UpdateInfo(
                hasUpdate = hasUpdate,
                versionCode = localVersionCode,
                versionName = cleanTagName,
                updateIdentity = System.currentTimeMillis(),
                apkUrl = apkAsset?.browserDownloadUrl ?: "",
                apkSize = apkAsset?.size ?: 0L,
                apkSha256 = sha256,
                mandatory = false,
                releaseNotes = release.body,
                publishedAt = release.publishedAt
            )
            onStep(CheckingStep.Success(fallbackInfo))
            Result.success(fallbackInfo)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "حدث خطأ غير متوقع أثناء فحص التحديثات"
            onStep(CheckingStep.Error(errorMsg))
            Result.failure(Exception(errorMsg))
        }
    }

    override fun verifyApkSha256(file: File, expectedSha256: String): Boolean {
        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
        if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
            return false
        }

        if (expectedSha256.isBlank()) return true

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            hex.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun copyApkToDownloads(file: File, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                uri
            } else {
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
                file.copyTo(dest, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.provider", dest)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun backupDataBeforeUpdate(): Result<Uri> = withContext(Dispatchers.IO) {
        Result.success(Uri.EMPTY)
    }

    override suspend fun saveDownloadedApk(file: File, versionName: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "updates").apply { if (!exists()) mkdirs() }
        val dest = File(dir, "rndm-v$versionName.apk")
        file.copyTo(dest, overwrite = true)
        dest
    }

    override suspend fun getDownloadedApks(): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "updates")
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles { f -> f.extension == "apk" && f.name.startsWith("rndm-v") }
            ?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }

    override suspend fun deleteDownloadedApk(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete() else false
    }

    /**
     * محرك مقارنة الإصدارات الدقيق المانع للإشعارات والتحديثات الوهمية
     * يفحص بالتسلسل الهرمي: SemVer -> VersionCode -> UpdateIdentity
     */
    private fun isCandidateStrictlyNewer(
        localName: String,
        localCode: Int,
        localIdentity: Long,
        remoteName: String,
        remoteCode: Int,
        remoteIdentity: Long
    ): Boolean {
        val cleanLocal = localName.removePrefix("v").trim()
        val cleanRemote = remoteName.removePrefix("v").trim()

        if (cleanRemote.isBlank()) return false

        // 1. استخراج ومقارنة أجزاء SemVer الرقمية (Major.Minor.Patch)
        val localParts = cleanLocal.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val remoteParts = cleanRemote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

        val maxLen = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val l = localParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false // الإصدار في المستودع أقدم من المثبت حالياً
        }

        // 2. في حال تطابق اسم الإصدار تماماً، نقارن كود البناء (VersionCode)
        if (remoteCode > localCode && remoteCode > 0 && localCode > 0) return true
        if (remoteCode < localCode && remoteCode > 0 && localCode > 0) return false

        // 3. في حال تطابق كود البناء أيضاً، نقارن المعرف الزمني للإصدار (UpdateIdentity)
        if (remoteIdentity > localIdentity && localIdentity > 0L) return true

        return false
    }
}
