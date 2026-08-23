package com.rndm.app.core.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rndm.app.MainActivity
import com.rndm.app.R

class MatchReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val playerOne = inputData.getString("PLAYER_ONE") ?: "اللاعب 1"
        val playerTwo = inputData.getString("PLAYER_TWO") ?: "اللاعب 2"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_MATCHES)
            .setSmallIcon(R.drawable.ic_wheel)
            .setContentTitle("مباراة قادمة جاهزة!")
            .setContentText("المواجهة بين $playerOne ضد $playerTwo ستبدأ الآن")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // Ignored if permission not granted
        }

        return Result.success()
    }
}
