package com.uth.taskmanagement.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.uth.taskmanagement.MainActivity
import com.uth.taskmanagement.R

// Tạo Notification Channel và hiển thị Local Notification nhắc việc
object NotificationHelper {

    const val CHANNEL_ID = "task_reminder_channel"
    private const val CHANNEL_NAME = "Nhắc việc"
    private const val CHANNEL_DESCRIPTION = "Thông báo nhắc nhở thực hiện công việc đúng thời gian"

    // Tạo Notification Channel (gọi lại nhiều lần không ảnh hưởng)
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Hiển thị thông báo nhắc việc
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String
    ) {
        createNotificationChannel(context)

        // Mở MainActivity khi nhấn vào thông báo
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
