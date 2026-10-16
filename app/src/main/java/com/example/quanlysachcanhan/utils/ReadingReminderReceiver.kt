package com.example.quanlysachcanhan.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.ui.main.MainActivity

class ReadingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref = PreferenceManager(context)
        if (!pref.reminderEnabled) return

        sendNotification(context, isTest = false)

        // Lên lịch tiếp cho ngày hôm sau
        ReadingReminderHelper.scheduleReminder(context, pref.reminderHour, pref.reminderMinute)
    }

    companion object {
        const val CHANNEL_ID = "reading_reminder_channel"
        const val NOTIFICATION_ID = 1001

        fun sendNotification(context: Context, isTest: Boolean = false) {
            createNotificationChannel(context)

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isTest) {
                "${context.getString(R.string.notification_reminder_title)} [Test]"
            } else {
                context.getString(R.string.notification_reminder_title)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_open)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_reminder_message))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_reminder_message))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Android 13+ permission might not be granted yet
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.notification_channel_name)
                val descriptionText = context.getString(R.string.notification_channel_desc)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
