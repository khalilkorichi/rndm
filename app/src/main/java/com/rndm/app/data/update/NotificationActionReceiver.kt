package com.rndm.app.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rndm.app.core.di.UpdateEntryPoint
import com.rndm.app.domain.model.UpdateInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.EntryPointAccessors

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PAUSE = "com.rndm.app.action.DOWNLOAD_PAUSE"
        const val ACTION_RESUME = "com.rndm.app.action.DOWNLOAD_RESUME"
        const val ACTION_CANCEL = "com.rndm.app.action.DOWNLOAD_CANCEL"
        const val EXTRA_UPDATE_INFO = "extra_update_info"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val infoJson = intent.getStringExtra(EXTRA_UPDATE_INFO) ?: return

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val info = try {
            moshi.adapter(UpdateInfo::class.java).fromJson(infoJson)
        } catch (e: Exception) {
            null
        } ?: return

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                UpdateEntryPoint::class.java
            )
            val repository = entryPoint.updateRepository()

            when (action) {
                ACTION_PAUSE -> repository.pauseDownload(info)
                ACTION_RESUME -> repository.startDownload(info)
                ACTION_CANCEL -> repository.cancelDownload(info)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
