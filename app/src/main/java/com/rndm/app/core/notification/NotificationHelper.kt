package com.rndm.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rndm.app.MainActivity
import com.rndm.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_MATCHES = "rndm_matches_channel"
        const val CHANNEL_DRAWS = "rndm_draws_channel"
        const val CHANNEL_SYSTEM = "rndm_system_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val matchesChannel = NotificationChannel(
                CHANNEL_MATCHES,
                "تذكير المباريات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تذكير بمواعيد المباريات القادمة"
            }

            val drawsChannel = NotificationChannel(
                CHANNEL_DRAWS,
                "نتائج القرعة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات اكتمال القرعة والبطولات"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "تنبيهات النظام",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تنبيهات عامة حول التطبيق"
            }

            notificationManager.createNotificationChannels(listOf(matchesChannel, drawsChannel, systemChannel))
        }
    }

    fun showDrawNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DRAWS)
            .setSmallIcon(R.drawable.ic_wheel)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // Ignored if permission not granted
        }
    }

    fun scheduleMatchReminder(matchId: Long, playerOne: String, playerTwo: String, delaySeconds: Long = 60) {
        val data = workDataOf(
            "MATCH_ID" to matchId,
            "PLAYER_ONE" to playerOne,
            "PLAYER_TWO" to playerTwo
        )

        val request = OneTimeWorkRequestBuilder<MatchReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
