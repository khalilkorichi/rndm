package com.rndm.app.data.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rndm.app.MainActivity
import com.rndm.app.R
import com.rndm.app.core.di.UpdateEntryPoint
import com.rndm.app.domain.model.DownloadState
import com.rndm.app.domain.model.UpdateInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.EntryPointAccessors
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class UpdateDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "rndm_downloads"
        const val NOTIFICATION_ID = 8827
        private val pausedUrls = ConcurrentHashMap<String, Boolean>()

        fun setPaused(url: String, paused: Boolean) {
            pausedUrls[url] = paused
        }

        fun isPaused(url: String): Boolean = pausedUrls[url] == true
        fun clearPaused(url: String) = pausedUrls.remove(url)
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "RNDM-App-Android")
                .build()
            chain.proceed(req)
        }
        .build()

    override suspend fun doWork(): Result {
        val apkUrl = inputData.getString("apkUrl") ?: return Result.failure()
        val apkSha256 = inputData.getString("apkSha256") ?: ""
        val versionName = inputData.getString("versionName") ?: "1.0.0"
        val apkSize = inputData.getLong("apkSize", 0L)
        val infoJson = inputData.getString("infoJson") ?: ""

        createNotificationChannel()
        clearPaused(apkUrl)

        val tempFile = UpdateRepositoryImpl.getTempApkFile(context, versionName)
        var startBytes = if (tempFile.exists()) tempFile.length() else 0L

        val repository = getRepository()

        // Check if file is already fully downloaded and intact
        if (apkSize > 0L && startBytes >= apkSize) {
            if (repository != null && repository.verifyApkSha256(tempFile, apkSha256)) {
                val savedFile = repository.saveDownloadedApk(tempFile, versionName)
                tempFile.delete()
                showSuccessNotification(savedFile, versionName)
                val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                updateRepositoryState(DownloadState.Success(savedFile, info))
                return Result.success()
            } else {
                tempFile.delete()
                startBytes = 0L
            }
        }

        // Prepare request with Range header for resumable download if tempFile exists
        var requestBuilder = Request.Builder().url(apkUrl)
        if (startBytes > 0L) {
            requestBuilder.addHeader("Range", "bytes=$startBytes-")
        }
        var request = requestBuilder.build()

        try {
            setForeground(createForegroundInfo(0, "جاري البدء...", "جاري الحساب...", false, infoJson, apkUrl, apkSize, apkSha256, versionName))

            var response = client.newCall(request).execute()

            // Automatically handle 416 Range Not Satisfiable by clearing invalid temp file and starting fresh
            if (response.code == 416 && startBytes > 0L) {
                response.close()
                tempFile.delete()
                startBytes = 0L
                request = Request.Builder().url(apkUrl).build()
                response = client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    404 -> "ملف الـ APK غير متوفر على الخادم (رابط التحميل 404)"
                    403 -> "تم رفض الاتصال بالخادم (403 Forbidden)"
                    416 -> "تعارض في نطاق استكمال الملف (416 Range Not Satisfiable)"
                    429 -> "تم تجاوز حد الطلبات المؤقت لـ GitHub (Rate Limit)"
                    else -> "فشل الاستجابة من الخادم (رمز الخطأ: ${response.code})"
                }
                response.close()
                throw IOException(errorMsg)
            }

            val body = response.body ?: throw IOException("استجابة الخادم فارغة (Empty body)")
            val remainingLength = body.contentLength()
            val totalLength = if (startBytes > 0L && response.code == 206) {
                remainingLength + startBytes
            } else if (apkSize > 0L) {
                apkSize
            } else {
                remainingLength
            }

            val isAppend = startBytes > 0L && response.code == 206
            if (response.code == 200) {
                startBytes = 0L
            }

            var totalBytesRead = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isAppend).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var lastEmittedTime = System.currentTimeMillis()
                    var lastEmittedBytes = 0L
                    val speedWindow = LinkedList<Long>()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isPaused(apkUrl)) {
                            val currentTotal = totalBytesRead + (if (isAppend) startBytes else 0L)
                            val percent = if (totalLength > 0L) ((currentTotal * 100) / totalLength).toInt().coerceIn(0, 100) else 0
                            showPausedNotification(percent, infoJson, apkUrl, apkSize, apkSha256, versionName)
                            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                            updateRepositoryState(DownloadState.Paused(percent, info))
                            return Result.success()
                        }

                        if (isStopped) return Result.failure()

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val now = System.currentTimeMillis()
                        val timeDelta = now - lastEmittedTime

                        if (timeDelta >= 1000L) {
                            val byteDelta = totalBytesRead - lastEmittedBytes
                            val speedBytesPerSec = (byteDelta * 1000L) / timeDelta
                            if (speedWindow.size >= 5) speedWindow.removeFirst()
                            speedWindow.add(speedBytesPerSec)
                            val avgSpeed = speedWindow.average().toLong()

                            val currentTotal = totalBytesRead + (if (isAppend) startBytes else 0L)
                            val progressPercent = if (totalLength > 0L) ((currentTotal * 100) / totalLength).toInt().coerceIn(0, 100) else 0

                            val speedStr = formatSpeed(avgSpeed)
                            val etaStr = formatEta(totalLength - currentTotal, avgSpeed)

                            setForeground(createForegroundInfo(progressPercent, speedStr, etaStr, false, infoJson, apkUrl, apkSize, apkSha256, versionName))
                            setProgress(workDataOf("progress" to progressPercent, "speed" to speedStr, "eta" to etaStr))

                            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                            updateRepositoryState(DownloadState.Downloading(progressPercent, speedStr, etaStr, info))

                            lastEmittedTime = now
                            lastEmittedBytes = totalBytesRead
                        }
                    }
                }
            }

            // Verify that the stream was not abruptly terminated prematurely
            if (remainingLength > 0L && totalBytesRead < remainingLength) {
                throw IOException("انقطع الاتصال قبل اكتمال تحميل الملف (تم استلام $totalBytesRead من $remainingLength بايت)")
            }

            if (tempFile.length() < 500_000L) {
                throw IOException("حجم ملف التحديث المستلم غير صالح أو تالف (${tempFile.length()} بايت)")
            }

            // Final file integrity verification
            val currentRepo = getRepository()
            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)

            if (currentRepo != null) {
                val isValid = currentRepo.verifyApkSha256(tempFile, apkSha256)
                if (isValid) {
                    val savedFile = currentRepo.saveDownloadedApk(tempFile, versionName)
                    tempFile.delete()
                    showSuccessNotification(savedFile, versionName)
                    updateRepositoryState(DownloadState.Success(savedFile, info))
                    return Result.success()
                } else {
                    tempFile.delete()
                    val errorMsg = "فشلت عملية التحقق من سلامة حزمة التحديث (ملف تالف أو غير متطابق)"
                    showFailureNotification(errorMsg)
                    updateRepositoryState(DownloadState.Error(errorMsg, info))
                    return Result.failure()
                }
            } else {
                return Result.failure()
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "فشل تحميل التحديث"
            showFailureNotification(errorMsg)
            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
            updateRepositoryState(DownloadState.Error(errorMsg, info))
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحديثات التطبيق",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تحميل وتثبيت تحديثات التطبيق"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        progress: Int,
        speed: String,
        eta: String,
        isPaused: Boolean,
        infoJson: String,
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_download)
        remoteViews.setViewVisibility(R.id.notification_btn_install, android.view.View.GONE)
        remoteViews.setViewVisibility(R.id.notification_btn_pause, android.view.View.VISIBLE)
        remoteViews.setViewVisibility(R.id.notification_btn_cancel, android.view.View.VISIBLE)

        val title = if (isPaused) "تم إيقاف تحميل التحديث مؤقتاً" else "جاري تحميل التحديث (v$versionName)..."
        remoteViews.setTextViewText(R.id.notification_title, title)
        remoteViews.setTextViewText(R.id.notification_progress_text, "$progress%")
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, progress, false)
        remoteViews.setTextViewText(
            R.id.notification_info,
            if (isPaused) "تم الإيقاف عند $progress%" else "السرعة: $speed • المتبقي: $eta"
        )

        val actionAction = if (isPaused) NotificationActionReceiver.ACTION_RESUME else NotificationActionReceiver.ACTION_PAUSE
        val actionText = if (isPaused) "استئناف" else "إيقاف مؤقت"

        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionAction
            putExtra(NotificationActionReceiver.EXTRA_UPDATE_INFO, infoJson)
        }
        remoteViews.setTextViewText(R.id.notification_btn_pause, actionText)
        remoteViews.setOnClickPendingIntent(
            R.id.notification_btn_pause,
            PendingIntent.getBroadcast(
                context, 1, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL
            putExtra(NotificationActionReceiver.EXTRA_UPDATE_INFO, infoJson)
        }
        remoteViews.setOnClickPendingIntent(
            R.id.notification_btn_cancel,
            PendingIntent.getBroadcast(
                context, 2, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setOngoing(!isPaused)
            .setAutoCancel(false)
            .build()
    }

    private fun createForegroundInfo(
        progress: Int, speed: String, eta: String, isPaused: Boolean,
        infoJson: String, apkUrl: String, apkSize: Long, apkSha256: String, versionName: String
    ): ForegroundInfo {
        val notification = buildNotification(progress, speed, eta, isPaused, infoJson, apkUrl, apkSize, apkSha256, versionName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showPausedNotification(
        progress: Int, infoJson: String, apkUrl: String,
        apkSize: Long, apkSha256: String, versionName: String
    ) {
        val notification = buildNotification(progress, "", "", true, infoJson, apkUrl, apkSize, apkSha256, versionName)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showSuccessNotification(file: File, versionName: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingInstall = PendingIntent.getActivity(
            context, 10, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_download).apply {
            setTextViewText(R.id.notification_title, "اكتمل تحميل التحديث 🎉")
            setTextViewText(R.id.notification_progress_text, "100%")
            setProgressBar(R.id.notification_progress_bar, 100, 100, false)
            setTextViewText(R.id.notification_info, "الإصدار v$versionName جاهز للتثبيت الآن.")
            setViewVisibility(R.id.notification_btn_pause, android.view.View.GONE)
            setViewVisibility(R.id.notification_btn_cancel, android.view.View.GONE)
            setViewVisibility(R.id.notification_btn_install, android.view.View.VISIBLE)
            setOnClickPendingIntent(R.id.notification_btn_install, pendingInstall)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingInstall)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("فشل تحميل التحديث")
            .setContentText(errorMessage)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB/s", bytesPerSec.toDouble() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec.toDouble() / 1024)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatEta(remainingBytes: Long, bytesPerSec: Long): String {
        if (bytesPerSec <= 0 || remainingBytes <= 0) return "حساب..."
        val totalSecs = remainingBytes / bytesPerSec
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return if (mins > 0) "${mins}د و ${secs}ث" else "${secs}ثانية"
    }

    private fun getRepository(): UpdateRepositoryImpl? {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                UpdateEntryPoint::class.java
            )
            entryPoint.updateRepository() as? UpdateRepositoryImpl
        } catch (e: Exception) {
            null
        }
    }

    private fun updateRepositoryState(state: DownloadState) {
        getRepository()?.setDownloadState(state)
    }

    private fun getUpdateInfoFromJson(
        infoJson: String, apkUrl: String, apkSize: Long,
        apkSha256: String, versionName: String
    ): UpdateInfo {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            moshi.adapter(UpdateInfo::class.java).fromJson(infoJson)
                ?: createFallback(apkUrl, apkSize, apkSha256, versionName)
        } catch (e: Exception) {
            createFallback(apkUrl, apkSize, apkSha256, versionName)
        }
    }

    private fun createFallback(apkUrl: String, apkSize: Long, apkSha256: String, versionName: String): UpdateInfo {
        return UpdateInfo(
            hasUpdate = true,
            versionCode = 1,
            versionName = versionName,
            updateIdentity = System.currentTimeMillis(),
            apkUrl = apkUrl,
            apkSize = apkSize,
            apkSha256 = apkSha256,
            mandatory = false,
            releaseNotes = null
        )
    }
}
